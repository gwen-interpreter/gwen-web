/*
 * Copyright 2014-2021 Branko Juric, Brady Wood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gwen.web.eval.binding

import gwen.core._
import gwen.web.WebErrors._
import gwen.web.WebSettings
import gwen.web.eval.WebContext

import scala.concurrent.duration.Duration
import scala.util.Try

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.WebElement

import java.util.concurrent.TimeUnit

/**
  *  Resolves locator bindings to actual web elements.
  */
class LocatorBindingResolver(ctx: WebContext) extends LazyLogging {

  /**
    * Finds and resolves a locator binding to a single element.
    *
    * @param name the binding name
    * @return a web element
    */
  def resolve(name: String): WebElement = getBinding(name).get.resolve()

  /**
    * Finds and resolves a locator binding to a zero or more elements.
    *
    * @param name the binding name
    * @return zero or more web elements
    */
  def resolveAll(name: String): List[WebElement] = getBinding(name).get.resolveAll()

  /**
   * Gets a locator binding.
   * 
   * @param name the name of the web element
   * @param optional true to return None if not found; false to throw error
   */
  def getBinding(name: String, optional: Boolean = false): Option[LocatorBinding] = ctx.withEnv { env =>
    env.topScope.getObject(name) match {
      case None =>
        val locatorKey = LocatorKey.baseKey(name)
        env.scopes.getOpt(locatorKey) match {
          case Some(boundValue) =>
            val selectors = boundValue.split(",") flatMap { boundValue =>
              val selectorType = Try(SelectorType.parse(boundValue)) getOrElse {
                locatorBindingError(s"Unsupported selector type defined for $name: $boundValue")
              }
              if (selectorType == SelectorType.xpath && WebSettings.`gwen.web.browser` == "ie" ) {
                locatorBindingError("Cannot locate element by XPath because IE does not support it")
              }
              val selectorKey = ctx.interpolate(LocatorKey.selectorKey(name, selectorType))
              env.scopes.getOpt(selectorKey) match {
                case Some(expression) =>
                  val selector = ctx.interpolate(expression)
                  val container: Option[String] = env.scopes.getOpt(ctx.interpolate(LocatorKey.containerKey(name, selectorType)))
                  if (ctx.options.dryRun) {
                    container.foreach(c => getBinding(c, optional))
                  }
                  val timeout = env.scopes.getOpt(ctx.interpolate(LocatorKey.timeoutSecsKey(name, selectorType))).map { timeoutSecs =>
                    Duration.create(timeoutSecs.toLong, TimeUnit.SECONDS)
                  }
                  val index = env.scopes.getOpt(ctx.interpolate(LocatorKey.indexKey(name, selectorType))).map(_.toInt)
                  Some(Selector(selectorType, selector, container.flatMap(c => getBinding(c, optional)), timeout, index))
                case None =>
                  if (optional) None else locatorBindingError(s"Undefined locator lookup binding for $name: $selectorKey")
              }
            }
            if (selectors.nonEmpty) {
              val binding = new LocatorBinding(name, selectors.toList, ctx)
              if (WebSettings.`gwen.web.implicit.js.locators`) {
                Some(binding.jsEquivalent)
              } else {
                Some(binding)
              }
            }
            else None
          case None => if (optional) None else locatorBindingError(s"Undefined locator binding for $name: $locatorKey")
        }
      case Some(x) if x.isInstanceOf[WebElement] || ctx.options.dryRun => Some(LocatorBinding(name, SelectorType.cache, name, None, None, None, ctx))
      case _ => None
    }
  } tap { binding =>
      binding foreach { b => logger.debug(s"getLocatorBinding($name,$optional)='$b'") }
  }

}
