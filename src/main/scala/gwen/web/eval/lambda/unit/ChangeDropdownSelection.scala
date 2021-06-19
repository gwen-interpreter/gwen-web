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

package gwen.web.eval.lambda.unit

import gwen.web.eval.DropdownSelection
import gwen.web.eval.ElementAction
import gwen.web.eval.WebContext

import gwen.core.behavior.BehaviorType
import gwen.core.eval.lambda.UnitStep
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.util.chaining._

class ChangeDropdownSelection(element: String, by: DropdownSelection.Value, value: String, bound: Boolean, action: ElementAction.Value) extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    step tap { _ =>
      checkStepRules(step, BehaviorType.Action, ctx)
      val binding = ctx.getLocatorBinding(element)
      action match {
        case ElementAction.select =>
          by match {
            case DropdownSelection.index =>
              ctx.selectByIndex(binding, value.toInt - 1)
            case DropdownSelection.text =>
              val optionText = if (bound) ctx.getBoundReferenceValue(value) else value
              ctx.selectByVisibleText(binding, optionText)
            case DropdownSelection.value =>
              val optionValue = if (bound) ctx.getBoundReferenceValue(value) else value
              ctx.selectByValue(binding, optionValue)
          }
        case ElementAction.deselect =>
          by match {
            case DropdownSelection.index =>
              ctx.deselectByIndex(binding, value.toInt - 1)
            case DropdownSelection.text =>
              val optionText = if (bound) ctx.getBoundReferenceValue(value) else value
              ctx.deselectByVisibleText(binding, optionText)
            case DropdownSelection.value =>
              val optionValue = if (bound) ctx.getBoundReferenceValue(value) else value
              ctx.deselectByValue(binding, optionValue)
          }
        case _ => // noop
      }
    }
  }

}
