/*
 * Copyright 2025 Branko Juric, Brady Wood
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

import gwen.web.eval.ElementEvent
import gwen.web.eval.WebContext
import gwen.web.eval.binding.ElementFunctionBinding

import gwen.core.LoadStrategy
import gwen.core.behavior.BehaviorType
import gwen.core.eval.action.UnitStepAction
import gwen.core.eval.binding.LoadStrategyBinding
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.util.chaining._

class BindElementFunction(target: String, function: String, element: String, masked: Boolean) extends UnitStepAction[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    step tap { _ =>
      ctx.getLocatorBinding(element)
      ElementFunctionBinding.bind(target, function, element, masked, ctx)
      step.loadStrategy foreach { strategy =>
        val value = {
          if (strategy == LoadStrategy.Eager) Option(
            ElementFunctionBinding(target, ctx).resolve()
          )
          else None
        }
        LoadStrategyBinding.bind(target, value, strategy, ctx)
      }
    }
  }

}
