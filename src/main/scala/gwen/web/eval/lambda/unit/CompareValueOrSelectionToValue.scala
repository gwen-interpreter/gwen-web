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
import gwen.web.eval.WebContext

import gwen.core.Errors
import gwen.core.behavior.BehaviorType
import gwen.core.eval.ComparisonOperator
import gwen.core.eval.lambda.UnitStep
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success
import scala.util.chaining._

class CompareValueOrSelectionToValue(element: String, selection: Option[DropdownSelection], expression: String, operator: ComparisonOperator, negate: Boolean, message: Option[String], timeout: Option[Duration]) extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    checkStepRules(step, BehaviorType.Assertion, ctx)
    if (element == "I") Errors.undefinedStepError(step)
    if (element == "the current URL") {
      val url = ctx.captureCurrentUrl
      ctx.topScope.set(element, url)
    }
    val expected = ctx.parseExpression(operator, expression)
    val actual = ctx.boundAttributeOrSelection(element, selection)
    step tap { _ =>
      ctx.perform {
        if (ctx.scopes.findEntry { case (n, _) => n.startsWith(element) } forall { case (n, _) => n != element }) {
          val nameSuffix = selection.map(sel => s" $sel")
          ctx.compare(s"$element${nameSuffix.getOrElse("")}", expected, actual, operator, negate, nameSuffix, message, timeout.map(_.toSeconds))
        } else {
          val actualValue = ctx.scopes.getOpt(element).getOrElse(actual())
          val result = ctx.compare(element, expected, actualValue, operator, negate)
          result match {
            case Success(assertion) =>
              val binding = ctx.getLocatorBinding(element, optional = true)
              Errors.assertWithError(assertion, message, s"Expected ${binding.map(_.toString).getOrElse(element)} to ${if(negate) "not " else ""}$operator ${if (expected.isEmpty()) "blank" else s"'$expected'"}${if (operator == ComparisonOperator.be && actualValue == expected) "" else s" but got '$actualValue'"}")
            case Failure(error) =>
              Errors.assertWithError(assertion = false, message, error.getMessage)
          }
        }
      } getOrElse {
        actual()
      }
    }
  }

}
