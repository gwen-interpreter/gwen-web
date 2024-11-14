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

import gwen.web.eval.WebContext

import gwen.core.Wait
import gwen.core.eval.binding.Binding

import org.apache.commons.text.StringEscapeUtils
import org.openqa.selenium.WebElement

import scala.concurrent.duration.Duration

import java.util.concurrent.TimeUnit

object LocatorBinding {

  def apply(name: String, selectorType: SelectorType, expression: String, relative: Option[(RelativeSelectorType, LocatorBinding, Option[Int])], timeout: Option[Duration], index: Option[Int], ctx: WebContext): LocatorBinding = {
    new LocatorBinding(name, List(Selector(selectorType, expression, relative, timeout, index)), ctx)
  }
  
  def apply(name: String, selectorType: SelectorType, expression: String, relative: Option[(RelativeSelectorType, LocatorBinding, Option[Int])], index: Option[Int], ctx: WebContext): LocatorBinding = {
    new LocatorBinding(name, List(Selector(selectorType, expression, relative, None, index)), ctx)
  }
  
  def apply(binding: LocatorBinding, selector: Selector, ctx: WebContext): LocatorBinding = {
    new LocatorBinding(binding.name, List(selector), ctx)
  }

}

class LocatorBinding(val name: String, val selectors: List[Selector], ctx: WebContext) extends Binding[WebContext, WebElement](name, ctx) {
  
  lazy val timeoutSeconds: Long = selectors.map(_.timeoutSeconds).sum

  override def resolve(): WebElement = ctx.webElementlocator.locate(this)
  def resolveAll(): List[WebElement] = ctx.webElementlocator.locateAll(this)  
  def withFastTimeout: LocatorBinding = withTimeout(Duration(200, TimeUnit.MILLISECONDS))
  def withTimeout(timeout: Option[Duration]): LocatorBinding = timeout.map(withTimeout).getOrElse(this)
  private def withTimeout(timeout: Duration): LocatorBinding = {
    val newSelectors = selectors map { s => 
      Selector(s, Some(timeout), s.index)
    }
    new LocatorBinding(name, newSelectors, ctx)
  }
  
  override def toString = s"$name [locator${if (selectors.size > 1) "s" else ""}: ${selectors.mkString(", ")}]"

}
