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

package gwen.web.engine.binding

import gwen.web.engine.WebContext

import gwen.core.engine.binding.Binding

import org.apache.commons.text.StringEscapeUtils
import org.openqa.selenium.WebElement

import scala.concurrent.duration.Duration

object LocatorBinding {

  def apply(name: String, selectorType: SelectorType.Value, expression: String, container: Option[LocatorBinding], timeout: Option[Duration], index: Option[Int], ctx: WebContext): LocatorBinding = {
    new LocatorBinding(name, List(Selector(selectorType, expression, container, timeout, index)), ctx)
  }
  
  def apply(name: String, selectorType: SelectorType.Value, expression: String, container: Option[LocatorBinding], index: Option[Int], ctx: WebContext): LocatorBinding = {
    new LocatorBinding(name, List(Selector(selectorType, expression, container, None, index)), ctx)
  }
  
  def apply(binding: LocatorBinding, selector: Selector, ctx: WebContext): LocatorBinding = {
    new LocatorBinding(binding.name, List(selector), ctx)
  }

}

class LocatorBinding(val name: String, val selectors: List[Selector], ctx: WebContext) extends Binding[WebContext, WebElement](name, ctx) {
  
  lazy val timeoutSeconds: Long = selectors.map(_.timeoutSeconds).sum

  override def resolve(): WebElement = ctx.locator.locate(this)
  def resolveAll(): List[WebElement] = ctx.locator.locateAll(this)
  
  /** Gets the javascript equivalent of this locator binding (used as fallback on stale element reference). */
  def jsEquivalent: LocatorBinding = {
    val jsSelectors = selectors.map { loc =>
      val isListSelector = name.endsWith("/list")
      val jsExpression = loc.selectorType match {
        case SelectorType.id =>
          if (!isListSelector) s"document.getElementById('${loc.expression}')"
          else s"document.querySelectorAll('#${loc.expression}')"
        case SelectorType.`css selector` =>
          s"document.querySelector${if (isListSelector) "All" else ""}('${StringEscapeUtils.escapeEcmaScript(loc.expression)}')"
        case SelectorType.xpath =>
          s"document.evaluate('${StringEscapeUtils.escapeEcmaScript(loc.expression)}', document, null, XPathResult.${if (isListSelector) "ORDERED_NODE_ITERATOR_TYPE" else "FIRST_ORDERED_NODE_TYPE"}, null)${if (isListSelector) "" else ".singleNodeValue"}"
        case SelectorType.name =>
          s"document.getElementsByName('${loc.expression}')${if (isListSelector) "" else "[0]"}"
        case SelectorType.`class name` =>
          s"document.getElementsByClassName('${loc.expression}')${if (isListSelector) "" else "[0]"}"
        case SelectorType.`tag name` =>
          s"document.getElementsByTagName('${loc.expression}')${if (isListSelector) "" else "[0]"}"
        case SelectorType.`link text` =>
          s"""document.evaluate('//a[text()="${StringEscapeUtils.escapeEcmaScript(loc.expression)}"]', document, null, XPathResult.${if (isListSelector) "ORDERED_NODE_ITERATOR_TYPE" else "FIRST_ORDERED_NODE_TYPE"}, null)${if (isListSelector) "" else ".singleNodeValue"}"""
        case SelectorType.`partial link text` =>
          s"""document.evaluate('//a[contains(text(), "${StringEscapeUtils.escapeEcmaScript(loc.expression)}")]', document, null, XPathResult.${if (isListSelector) "ORDERED_NODE_ITERATOR_TYPE" else "FIRST_ORDERED_NODE_TYPE"}, null)${if (isListSelector) "" else ".singleNodeValue"}"""
        case _ => loc.expression
      }
      Selector(SelectorType.javascript, jsExpression, loc.container, loc.timeout, loc.index)
    }
    new LocatorBinding(name, jsSelectors, ctx)
  }

  def withJSEquivalent = new LocatorBinding(name, selectors ++ List(jsEquivalent.selectors.head), ctx)
  
  override def toString = s"$name [locator${if (selectors.size > 1) "s" else ""}: ${selectors.mkString(", ")}]"

}
