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
import gwen.web.eval.WebErrors

import gwen.core._
import gwen.core.behavior.BehaviorType
import gwen.core.eval.lambda.UnitStep
import gwen.core.eval.lambda.unit.Capture
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

class CaptureDropdownOrElement(target: Option[String], element: String, selection: DropdownSelection) extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    checkStepRules(step, BehaviorType.Action, ctx)
    val name = target.getOrElse(element)
    try {
      val content = ctx.boundAttributeOrSelection(element, Option(selection))
      ctx.topScope.set(name, content)
      step.addAttachment(name, "txt", content)
    } catch {
      case e: WebErrors.LocatorBindingException =>
        element match {
          case r"""(.+?)$source as (.+?)$suffix""" =>
            new Capture(s"$suffix $selection", source).apply(parent, step, ctx)
          case _ => throw e
        }
    }
  }

}
