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

package gwen.web.eval.action.unit

import gwen.web.eval.ElementState
import gwen.web.eval.WebContext
import gwen.core.eval.binding.JSBinding

import gwen.core.behavior.BehaviorType
import gwen.core.eval.action.UnitStepAction
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.util.chaining._

class WaitForElementState(element: String, state: ElementState, negate: Boolean) extends UnitStepAction[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    step tap { _ =>
      val bCondition = s"$element is${if (negate) " not" else ""} $state"
      ctx.topScope.getOpt(JSBinding.key(bCondition)) match {
        case None =>
          checkStepRules(step, BehaviorType.Action, ctx)
          val binding = ctx.getLocatorBinding(element)
          ctx.waitForElementState(binding, state, negate)
        case Some(javascript) =>
          new WaitForCondition(javascript, None, None).apply(parent, step, ctx)
      }
    }
  }

}
