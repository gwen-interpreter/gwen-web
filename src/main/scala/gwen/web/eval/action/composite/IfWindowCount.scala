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

import gwen.core.eval.EvalContext
import gwen.core.eval.engine.StepDefEngine
import gwen.core.eval.action.CompositeStepAction
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Annotations
import gwen.core.node.gherkin.Scenario
import gwen.core.node.gherkin.Step
import gwen.core.node.gherkin.Tag
import gwen.core.status.Passed
import gwen.core.status.Pending

import gwen.web.eval.WebContext

import org.openqa.selenium.WindowType

import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.concurrent.duration.Duration

import java.util.concurrent.TimeUnit

 class IfWindowCount[T <: EvalContext](doStep: String, count: Int, winType: WindowType, engine: StepDefEngine[WebContext]) extends CompositeStepAction[WebContext](doStep) {
  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    val cond = s"$count open ${winType.name.toLowerCase}${if (count == 1) "" else "s"}"
    ctx.getStepDef(doStep, None) foreach { stepDef =>
      checkStepDefRules(step.copy(withName = doStep, withStepDef = Some(stepDef)), ctx)
    }
    val iStep = step.copy(withEvalStatus = Pending)
    val ifTag = Tag(Annotations.If)
    val tags = List(Tag(Annotations.Synthetic), ifTag, Tag(Annotations.StepDef))
    val iStepDef = Scenario(None, tags, ifTag.toString, cond, None, Nil, None, List(step.copy(withName = doStep)), Nil, Nil, Nil)
    val sdCall = () => engine.callStepDef(step, iStepDef, iStep, ctx)
    val attachments = ctx.popAttachments()
    ctx.evaluate(sdCall()) {
      val satisfied = Try(ctx.waitForWindows(count, winType)).map(_ => true).getOrElse(false)
      val result = if (satisfied) {
        logger.info(s"Processing conditional step ($cond = true): ${step.keyword} $doStep")
        sdCall()
      } else {
        logger.info(s"Skipping conditional step ($cond = false): ${step.keyword} $doStep")
        step.copy(withEvalStatus = Passed(0, abstained = !ctx.options.dryRun))
      }
      result.addAttachments(attachments)
    }
  }
 }
