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

import gwen.web.eval.DropdownSelection
import gwen.web.eval.WebContext

import gwen.core.Errors
import gwen.core.Formatting
import gwen.core.behavior.BehaviorType
import gwen.core.eval.ComparisonOperator
import gwen.core.eval.action.UnitStepAction
import gwen.core.eval.action.unit.Compare
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.concurrent.duration.Duration
import scala.util.chaining._

class CompareValueOrSelectionToBoundValue(element: String, selection: Option[DropdownSelection], source: String, operator: ComparisonOperator, negate: Boolean) extends UnitStepAction[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    checkStepRules(step, BehaviorType.Assertion, ctx)
    if (element == "I") Errors.undefinedStepError(step)
    if (element == "the current URL") {
      val url = ctx.captureCurrentUrl
      ctx.topScope.set(element, url)
    }
    val message = step.message
    val timeout = step.timeoutOpt
    val trim = step.isTrim
    val ignoreCase = step.isIgnoreCase
    if (ctx.isWebBinding(element) || ctx.isWebBinding(source)) {
      val timeoutOverride = {
        if (ctx.getLocatorBindingOpt(source).nonEmpty || ctx.getLocatorBindingOpt(element).nonEmpty) {
          timeout
        } else {
          Some(Duration.Zero)
        }
      }
      val expected = ctx.getBoundValue(source, timeout)
      val actual = () => ctx.boundAttributeOrSelection(element, selection, timeout)
      val formattedActual = () => Formatting.format(ctx.boundAttributeOrSelection(element, selection, timeout), trim, ignoreCase)
      step tap { _ =>
        ctx.perform {
          val nameSuffix = selection.map(sel => s" $sel")
          ctx.compare(s"$element${nameSuffix.getOrElse("")}", Formatting.format(expected, trim, ignoreCase), formattedActual, operator, negate, nameSuffix, message, timeoutOverride.map(_.toSeconds), step.assertionMode)
        } getOrElse  {
          actual()
        }
      }
    } else {
      new Compare(element, source, operator, negate).apply(parent, step, ctx)
    }
  }

}
