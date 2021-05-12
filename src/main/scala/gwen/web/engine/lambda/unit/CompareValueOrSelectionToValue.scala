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

import gwen.web.engine.DropdownSelection
import gwen.web.engine.WebContext

import gwen.core.Errors
import gwen.core.engine.ComparisonOperator
import gwen.core.engine.lambda.UnitStep
import gwen.core.model._
import gwen.core.model.gherkin.Step

import scala.util.Failure
import scala.util.Success

class CompareValueOrSelectionToValue(element: String, selection: Option[DropdownSelection.Value], expression: String, operator: ComparisonOperator.Value, negate: Boolean) extends UnitStep[WebContext] {

  override def apply(parent: Identifiable, step: Step, ctx: WebContext): Unit = {
    checkStepRules(step, BehaviorType.Assertion, ctx)
    if (element == "I") Errors.undefinedStepError(step)
    if (element == "the current URL") ctx.captureCurrentUrl(None)
    val expected = ctx.parseExpression(operator, expression)
    val actual = ctx.boundAttributeOrSelection(element, selection)
    ctx.perform {
      if (ctx.scopes.findEntry { case (n, _) => n.startsWith(element) } forall { case (n, _) => n != element }) {
        val nameSuffix = selection.map(sel => s" $sel")
        ctx.compare(s"$element${nameSuffix.getOrElse("")}", expected, actual, operator, negate, nameSuffix)
      } else {
        val actualValue = ctx.scopes.getOpt(element).getOrElse(actual())
        val result = ctx.compare(element, expected, actualValue, operator, negate)
        result match {
          case Success(assertion) =>
            val binding = ctx.getLocatorBinding(element, optional = true)
            assert(assertion, s"Expected ${binding.map(_.toString).getOrElse(element)} to ${if(negate) "not " else ""}$operator '$expected' but got '$actualValue'")
          case Failure(error) =>
            assert(assertion = false, error.getMessage)
        }
      }
    } getOrElse {
      actual()
    }
  }

}
