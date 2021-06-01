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

object LocatorKey {

  def baseKey(name: String) = s"$name/locator"
  def selectorKey(name: String, selectorType: SelectorType.Value) = s"${baseKey(name)}/$selectorType"
  def containerKey(name: String, selectorType: SelectorType.Value) = createKey(name, selectorType, "container") 
  def timeoutSecsKey(name: String, selectorType: SelectorType.Value) = createKey(name, selectorType, "timeoutSecs")
  def indexKey(name: String, selectorType: SelectorType.Value) = createKey(name, selectorType, "index")

  private def createKey(name: String, selectorType: SelectorType.Value, suffix: String) = s"${selectorKey(name, selectorType)}/$suffix"

}
