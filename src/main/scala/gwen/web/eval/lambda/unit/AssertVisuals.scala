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

import gwen.core.behavior.BehaviorType
import gwen.web.eval.WebContext
import gwen.web.eval.WebErrors
import gwen.web.eval.eyes.EyesSettings

import gwen.core.Errors
import gwen.core.eval.lambda.UnitStep
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.util.chaining._

class AssertVisuals() extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    checkStepRules(step, BehaviorType.Assertion, ctx)
    if (EyesSettings.`gwen.applitools.eyes.enabled`) {
      ctx.asertVisuals() match {
        case Some(results) =>
          val aStep = step.addAttachment("AppliTools dashboard", "url", results.getUrl)
          ctx.withStep(aStep) { s =>
            try {
              s tap { _ =>
                assert(results.isNew || results.isPassed, s"Expected visual check to pass but status was: ${results.getStatus}")
              }
            } catch {
              case e: AssertionError => WebErrors.visualAssertionError(e.getMessage)
            }
          }
        case _ => step
      }
    } else {
      Errors.disabledStepError(step)
    }

  }

}
