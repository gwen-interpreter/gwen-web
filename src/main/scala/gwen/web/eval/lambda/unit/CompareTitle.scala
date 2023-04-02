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

import gwen.web.eval.WebContext
import gwen.web.eval.WebSettings

import gwen.core.behavior.BehaviorType
import gwen.core.eval.ComparisonOperator
import gwen.core.eval.lambda.UnitStep
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.concurrent.duration.Duration
import scala.util.chaining._

class CompareTitle(name: String, value: String, bound: Boolean, operator: ComparisonOperator, negate: Boolean, message: Option[String], timeout: Option[Duration]) extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    step tap { _ =>
      checkStepRules(step, BehaviorType.Assertion, ctx)
      val expected = if (bound) {
        ctx.getBoundReferenceValue(value)
      } else {
        ctx.parseExpression(operator, value)
      }
      ctx.perform {
        ctx.compare(name, expected, () => ctx.getTitle, operator, negate, None, message, timeout.map(_.toSeconds))
      }
    }
  }

}
