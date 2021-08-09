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

import gwen.web.eval.WebSettings

import scala.concurrent.duration.Duration

/**
  * Captures a selector.
  *
  * @param selectorType the seletor type
  * @param expression the selector expression
  * @param relative optional relative selector and binding
  * @param isContainer true if this is a selector for a container element, false otherwise
  * @param timeout optional timeout (defaults to `gwen.web.locator.wait.seconds` if not provided)
  * @param index optional index (if selector returns more than one element then index is required)
  */
case class Selector(selectorType: SelectorType, expression: String, relative: Option[(RelativeSelectorType, LocatorBinding, Option[Int])], isContainer: Boolean, timeout: Option[Duration], index: Option[Int]) {

  override def toString: String =
    s"$selectorType=$expression${relative.map((s, e, px) => s" $s $e${px.map(p => s" within $p pixel(s)").getOrElse("")}").getOrElse("")}${index.map(i => s" at index $i").getOrElse("")}"

  lazy val timeoutSeconds = timeout.map(_.toSeconds).getOrElse(WebSettings.`gwen.web.locator.wait.seconds`)

  lazy val timeoutMilliseconds = timeoutSeconds * 1000
}

/** Locator factory companion. */
object Selector {

  def apply(selectorType: SelectorType, expression: String, relative: Option[(RelativeSelectorType, LocatorBinding, Option[Int])], timeout: Option[Duration], index: Option[Int]): Selector = {
    Selector(selectorType, expression, relative, isContainer = false, timeout, index)
  }

  def apply(selectorType: SelectorType, expression: String): Selector = {
    Selector(selectorType, expression, None, isContainer = false, None, None)
  }

  def apply(selector: Selector, timeout: Option[Duration], index: Option[Int]): Selector = {
    Selector(selector.selectorType, selector.expression, selector.relative, selector.isContainer, timeout, index)
  }

}
