/*
 * Copyright 2021 Branko Juric, Brady Wood
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

package gwen.web.engine.lambda.unit

import gwen.web.engine.WebContext
import gwen.web.engine.WebSettings

import gwen.core.engine.lambda.UnitStep
import gwen.core.model._
import gwen.core.model.gherkin.Step

class SendValueToElement(element: String, value: String, sendEnterKey: Boolean) extends UnitStep[WebContext] {

  override def apply(parent: Identifiable, step: Step, ctx: WebContext): Unit = {
    checkStepRules(step, BehaviorType.Action, ctx)
    val binding = ctx.getLocatorBinding(element)
    val clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`
    val clearFirst = WebSettings.`gwen.web.sendKeys.clearFirst`
    ctx.sendValue(binding, value, clickFirst, clearFirst, sendEnterKey)
  }

}
