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

import gwen.web.eval.DropdownSelection
import gwen.web.eval.WebContext

import gwen.core.Assert
import gwen.core.Errors
import gwen.core.Formatting
import gwen.core.ValueLiteral
import gwen.core.behavior.BehaviorType
import gwen.core.eval.ComparisonOperator
import gwen.core.eval.lambda.UnitStep
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success
import scala.util.chaining._

class CompareValueOrSelectionToValue(element: String, selection: Option[DropdownSelection], expression: String, operator: ComparisonOperator, negate: Boolean, message: Option[String], timeout: Option[Duration], trim: Boolean, ignoreCase: Boolean) extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    checkStepRules(step, BehaviorType.Assertion, ctx)
    if (element == "I") Errors.undefinedStepError(step)
    if (element == "the current URL") {
      val url = ctx.captureCurrentUrl
      ctx.topScope.set(element, url)
    }
    val expected = ctx.parseExpression(operator, expression)
    val actual = () => ctx.boundAttributeOrSelection(element, selection, timeout)
    val formattedActual = () => Formatting.format(ctx.boundAttributeOrSelection(element, selection, timeout), trim, ignoreCase)
    step tap { _ =>
      ctx.perform {
        if (ctx.topScope.findEntry { case (n, _) => n.startsWith(element) } forall { case (n, _) => n != element }) {
          val nameSuffix = selection.map(sel => s" $sel")
          ctx.compare(s"$element${nameSuffix.getOrElse("")}", Formatting.format(expected, trim, ignoreCase), formattedActual, operator, negate, nameSuffix, message, timeout.map(_.toSeconds), step.assertionMode)
        } else {
          val actualValue = ctx.topScope.getOpt(element).getOrElse(actual())
          val result = ctx.compare(element, Formatting.format(expected, trim, ignoreCase), Formatting.format(actualValue, trim, ignoreCase), operator, negate)
          result match {
            case Success(assertion) =>
              val binding = ctx.getLocatorBinding(element, optional = true)
              ctx.assertWithError(
                assertion, 
                message, 
                Assert.formatFailed(binding.map(_.toString).getOrElse(element), expected, actualValue, negate, operator),
                step.assertionMode)
            case Failure(error) =>
              ctx.assertWithError(assertion = false, message, error.getMessage, step.assertionMode)
          }
        }
      } getOrElse {
        actual()
      }
    }
  }

}
