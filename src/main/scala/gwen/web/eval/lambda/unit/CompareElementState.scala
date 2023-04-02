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

import gwen.web.eval.ElementState
import gwen.web.eval.WebContext
import gwen.web.eval.WebSettings

import gwen.core.behavior.BehaviorType
import gwen.core.eval.lambda.UnitStep
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.concurrent.duration.Duration
import scala.util.chaining._
import scala.util.Failure
import scala.util.Try

class CompareElementState(element: String, state: ElementState, negate: Boolean, message: Option[String], timeout: Option[Duration]) extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    step tap { _ =>
      checkStepRules(step, BehaviorType.Assertion, ctx)
      val binding = ctx.getLocatorBinding(element)
      val waitSecs = timeout.map(_.toSeconds).getOrElse(WebSettings.`gwen.web.assertions.wait.seconds`)
      var assertionError: Option[Throwable] = None
      ctx.waitUntil(waitSecs, s"waiting for ${binding.displayName} to ${if(negate) "not " else ""}be '$state'") {
        Try(ctx.checkElementState(binding, state, negate, message)) match {
          case Failure(e) =>
            if (e.isInstanceOf[AssertionError]) assertionError = Some(e)
            else throw e
          case _ => assertionError = None
        }
        assertionError.isEmpty
      }
      assertionError.foreach(e => throw e)
    }
  }

}
