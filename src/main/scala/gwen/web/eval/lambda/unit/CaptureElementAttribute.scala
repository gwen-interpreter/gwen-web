/*
 * Copyright 2021-2024 Branko Juric, Brady Wood
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

package gwen.web.eval.lambda.unit

import gwen.web.eval.WebContext

import gwen.core.behavior.BehaviorType
import gwen.core.eval.binding.JSBinding
import gwen.core.eval.lambda.UnitStep
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

class CaptureElementAttribute(element: String, attribute: String, javascript: String) extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    checkStepRules(step, BehaviorType.Action, ctx)
    val binding = ctx.getLocatorBinding(element)
    ctx.topScope.set(JSBinding.key(attribute), javascript)
    try {
      ctx.perform {
        ctx.topScope.pushObject(s"${JSBinding.key(attribute)}/param/webElement", binding.resolve())
      }
      val content = ctx.getBoundValue(attribute)
      ctx.topScope.set(attribute, content)
      step.addAttachment(attribute, "txt", content)
    } finally {
      ctx.perform {
        ctx.topScope.popObject(s"${JSBinding.key(attribute)}/param/webElement")
      }
    }
  }

}
