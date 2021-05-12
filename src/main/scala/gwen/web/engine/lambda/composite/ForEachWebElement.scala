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

package gwen.web.engine.lambda.composite

import gwen.web.engine.WebContext
import gwen.web.engine.binding.LocatorBinding
import gwen.web.engine.binding.SelectorType

import gwen.core.engine.EvalEngine
import gwen.core.engine.lambda.composite.ForEach
import gwen.core.model._
import gwen.core.model.gherkin.Step

import scala.concurrent.duration.Duration

class ForEachWebElement(doStep: String, element: String, selectorType: SelectorType.Value, lookupExpr: String, container: Option[String], timeout: Option[Duration], engine: EvalEngine[WebContext]) extends ForEach[WebContext](engine) {

  override def apply(parent: Identifiable, step: Step, ctx: WebContext): Step = {
    val containerBinding = container.map(ctx.getLocatorBinding)
    val binding = LocatorBinding(s"$element/list", selectorType, lookupExpr, containerBinding, timeout, None, ctx)
    ctx.withEnv { env =>
      ctx.evaluate(evaluateForEach(() => List("$[dryRun:webElements]"), element, parent, step, doStep, env, ctx)) {
        evaluateForEach(() => binding.resolveAll(), element, parent, step, doStep, env, ctx)
      }
    }
    
  }

}
