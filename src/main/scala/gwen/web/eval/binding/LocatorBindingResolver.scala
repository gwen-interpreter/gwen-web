/*
 * Copyright 2014-2024 Branko Juric, Brady Wood
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

import gwen.web.eval.WebBrowser
import gwen.web.eval.WebContext
import gwen.web.eval.WebErrors._
import gwen.web.eval.WebSettings

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.WebElement

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.util.chaining._

import java.util.concurrent.TimeUnit
import org.xml.sax.Locator

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
  def getBinding(name: String, optional: Boolean = false): Option[LocatorBinding] = {
    ctx.topScope.getObject(name) match {
      case None =>
        val locatorKey = LocatorKey.baseKey(name)
        ctx.topScope.getOpt(locatorKey) match {
          case Some(boundValue) =>
            val selectors = boundValue.split(",") flatMap { boundValue =>
              val selectorType = Try(SelectorType.parse(boundValue)) getOrElse {
                locatorBindingError(s"Unsupported selector type defined for: $name")
              }
              val selectorKey = ctx.interpolate(LocatorKey.selectorKey(name, selectorType))
              ctx.topScope.getOpt(selectorKey) match {
                case Some(expression) =>
                  val selector = ctx.interpolate(expression)
                  val rKeys = RelativeSelectorType.values map { rSelector => 
                    LocatorKey.relativeKey(name, selectorType, rSelector)
                  }
                  val rKeyAndElement: Option[(String, String)] = (rKeys flatMap { rKey => 
                    ctx.topScope.getOpt(ctx.interpolate(rKey)) map { rElement => 
                      (rKey, rElement)
                    }
                  }).headOption
                  if (ctx.options.dryRun) {
                    rKeyAndElement.foreach((k, e) => getBinding(e, optional))
                  }
                  val relative = rKeyAndElement map { (rKey, rElement) => 
                    val rSelector = RelativeSelectorType.valueOf(rKey.substring(rKey.lastIndexOf("/") + 1))
                    val rBinding = getBinding(rElement, false).get
                    val rKeyWithinPixels = LocatorKey.relativeKeyWithinPixels(name, selectorType, rSelector)
                    val withinPixels = ctx.topScope.getOpt(ctx.interpolate(rKeyWithinPixels)).map(_.toInt)
                    (rSelector, rBinding, withinPixels)
                  }
                  val timeout = ctx.topScope.getOpt(ctx.interpolate(LocatorKey.timeoutSecsKey(name, selectorType))).map { timeoutSecs =>
                    Duration.create(timeoutSecs.toLong, TimeUnit.SECONDS)
                  }
                  val index = ctx.topScope.getOpt(ctx.interpolate(LocatorKey.indexKey(name, selectorType))).map(_.toInt)
                  val isShadowRoot = ctx.topScope.getOpt(ctx.interpolate(LocatorKey.shadowRootKey(name, selectorType))).map(_.toBoolean).getOrElse(false)
                  Some(Selector(selectorType, selector, relative, timeout, index, isShadowRoot))
                case None =>
                  if (optional) None else locatorBindingError(s"Undefined selector for: $name")
              }
            }
            if (selectors.nonEmpty) {
              Some(new LocatorBinding(name, selectors.toList, ctx))
            }
            else None
          case None => if (optional) None else locatorBindingError(s"Undefined selector for: $name")
        }
      case Some(x) if x.isInstanceOf[WebElement] || ctx.options.dryRun => Some(LocatorBinding(name, SelectorType.cache, name, None, None, None, false, ctx))
      case _ => None
    }
  } tap { binding =>
      binding foreach { b => logger.debug(s"getLocatorBinding($name,$optional)='$b'") }
  }

}
