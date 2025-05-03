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

import java.util.concurrent.TimeUnit

/**
  * Captures a selector.
  *
  * @param selectorType the seletor type
  * @param expression the selector expression
  * @param relative optional relative selector and binding
  * @param timeout optional timeout
  * @param index optional index (if selector returns more than one element then index is required)
  */
case class Selector(selectorType: SelectorType, expression: String, relative: Option[(RelativeSelectorType, LocatorBinding, Option[Int])], timeout: Duration, index: Option[Int], isShadowRoot: Boolean) {

  def hasTimeoutOverride = timeout.toMillis != Selector.DefaultTimeout.toMillis
  override def toString: String =
    s"$selectorType=$expression${relative.map((s, e, px) => s" $s $e${px.map(p => s" within $p pixel(s)").getOrElse("")}").getOrElse("")}${index.map(i => s" at index $i").getOrElse("")}"

  lazy val timeoutSeconds = timeout.toSeconds

  lazy val timeoutMilliseconds = timeoutSeconds * 1000
}

/** Locator factory companion. */
object Selector {

  val DefaultTimeout = Duration(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  def apply(selectorType: SelectorType, expression: String, relative: Option[(RelativeSelectorType, LocatorBinding, Option[Int])], timeout: Option[Duration], index: Option[Int], isShadowRoot: Boolean): Selector = {
    Selector(selectorType, expression, relative, timeout.getOrElse(DefaultTimeout), index, isShadowRoot)
  }

  def apply(selectorType: SelectorType, expression: String, isShadowRoot: Boolean): Selector = {
    Selector(selectorType, expression, None, DefaultTimeout, None, isShadowRoot)
  }

  def apply(selector: Selector, timeout: Option[Duration], index: Option[Int], isShadowRoot: Boolean): Selector = {
    Selector(selector.selectorType, selector.expression, selector.relative, timeout.getOrElse(DefaultTimeout), index, isShadowRoot)
  }

}
