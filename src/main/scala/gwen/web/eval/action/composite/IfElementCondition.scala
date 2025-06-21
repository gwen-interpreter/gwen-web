/*
 * Copyright 2022-2024 Branko Juric, Brady Wood
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

package gwen.web.eval.action.composite

import gwen.core.Errors.UnboundAttributeException
import gwen.core.eval.EvalContext
import gwen.core.eval.EvalEngine
import gwen.core.eval.binding.LoadStrategyBinding
import gwen.core.eval.engine.StepDefEngine
import gwen.core.eval.action.CompositeStepAction
import gwen.core.eval.action.composite.IfCondition
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Annotations
import gwen.core.node.gherkin.Scenario
import gwen.core.node.gherkin.Step
import gwen.core.node.gherkin.Tag
import gwen.core.status.Passed
import gwen.core.status.Pending

import gwen.web.eval.WebContext
import gwen.web.eval.ElementState

import scala.util.Try
import scala.util.Success
import scala.util.Failure
 import scala.concurrent.duration.Duration
 import java.util.concurrent.TimeUnit

 class IfElementCondition[T <: EvalContext](doStep: String, element: String, state: ElementState, negate: Boolean, engine: StepDefEngine[WebContext]) extends CompositeStepAction[WebContext](doStep) {
  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    val cond = s"$element is${if (negate) " not" else ""} $state"
    val ifCondition = IfCondition(doStep, cond, false, engine)
    Try(ifCondition.apply(parent, step, ctx)) match {
      case Success(s) => s
      case Failure(e) => 
        if (e.isInstanceOf[UnboundAttributeException] && e.getMessage.contains(cond)) {
          val binding = ctx.getLocatorBinding(element)
          ctx.getStepDef(doStep, None) foreach { stepDef =>
            checkStepDefRules(step.copy(withName = doStep, withStepDef = Some(stepDef)), ctx)
          }
          val iStep = step.copy(withEvalStatus = Pending)
          val ifTag = Tag(Annotations.If)
          val tags = List(Tag(Annotations.Synthetic), ifTag, Tag(Annotations.StepDef))
          val iStepDef = Scenario(None, tags, ifTag.toString, cond, None, Nil, None, List(step.copy(withName = doStep)), Nil, Nil, Nil)
          val sdCall = () => engine.callStepDef(step, iStepDef, iStep, ctx)
          ctx.evaluate(sdCall()) {
            var timeoutSecs = binding.timeoutSecondsOpt.map(_ / 10L).filter(_ > 0).getOrElse(if (binding.timeoutSeconds > 0) 1L else 0)
            val satisfied = Try(ctx.waitForElementState(binding.withTimeout(Some(Duration(timeoutSecs, TimeUnit.SECONDS))), state, negate)).map(_ => true).getOrElse(false)
            LoadStrategyBinding.bindIfLazy(binding.name, satisfied.toString, ctx)
            if (satisfied) {
              logger.info(s"Processing conditional step ($cond = true): ${step.keyword} $doStep")
              sdCall()
            } else {
              logger.info(s"Skipping conditional step ($cond = false): ${step.keyword} $doStep")
              step.copy(withEvalStatus = Passed(0, abstained = !ctx.options.dryRun))
            }
          }
        }
        else throw e
    }
  }
 }
