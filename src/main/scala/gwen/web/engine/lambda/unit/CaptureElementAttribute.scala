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

import gwen.web.engine.WebContext

import gwen.core._
import gwen.core.engine.binding.JavaScriptBinding
import gwen.core.engine.lambda.UnitStep
import gwen.core.model._
import gwen.core.model.gherkin.Step

class CaptureElementAttribute(element: String, attribute: String, javascript: String) extends UnitStep[WebContext] {

  override def apply(parent: Identifiable, step: Step, ctx: WebContext): Unit = {
    ctx.withEnv { env =>
      ctx.checkStepRules(step, BehaviorType.Action, env)
      val binding = ctx.getLocatorBinding(element)
      env.scopes.set(JavaScriptBinding.key(attribute), javascript)
      try {
        ctx.perform {
          env.topScope.pushObject(s"${JavaScriptBinding.key(attribute)}/param/webElement", binding.resolve())
        }
        val value = ctx.getBoundReferenceValue(attribute)
        env.topScope.set(attribute, value tap { content =>
          env.addAttachment(attribute, "txt", content)
        })
      } finally {
        ctx.perform {
          env.topScope.popObject(s"${JavaScriptBinding.key(attribute)}/param/webElement")
        }
      }
    }
  }

}
