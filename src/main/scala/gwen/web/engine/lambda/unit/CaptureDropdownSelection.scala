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
import gwen.web.engine.WebErrors

import gwen.core._
import gwen.core.engine.EvalContext
import gwen.core.engine.EvalEngine
import gwen.core.engine.lambda.UnitStep
import gwen.core.model._
import gwen.core.model.gherkin.Step

class CaptureDropdownSelection[T <: EvalContext](target: Option[String], element: String, selection: DropdownSelection.Value, engine: EvalEngine[WebContext], ctx: WebContext) extends UnitStep[WebContext](engine, ctx) {

  override def apply(parent: Identifiable, step: Step): Unit = {
    engine.checkStepRules(step, BehaviorType.Action, env)
    try {
      val name = target.getOrElse(element)
      val value = ctx.boundAttributeOrSelection(element, Option(selection))
      env.topScope.set(name, value() tap { content =>
        env.addAttachment(name, "txt", content)
      })
    } catch {
      case _: WebErrors.LocatorBindingException =>
        engine.translate(parent, step, env, ctx)
    }
  }

}
