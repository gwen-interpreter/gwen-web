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

package gwen.web.eval.step.composite

import gwen.core.eval.EvalContext
import gwen.core.eval.EvalEngine
import gwen.core.eval.step.composite.ForEach
import gwen.core.model._
import gwen.core.model.gherkin.Step

import gwen.web.eval.WebContext

class ForEachWebElementInIteration[T <: EvalContext](doStep: String, element: String, iteration: String, engine: EvalEngine[WebContext], ctx: WebContext) extends ForEach[WebContext](engine, ctx) {

  override def apply(parent: Identifiable, step: Step): Step = {
    val binding = ctx.getLocatorBinding(iteration)
    ctx.evaluate(evaluateForEach(() => List("$[dryRun:webElements]"), element, parent, step, doStep)) {
      evaluateForEach(() => binding.resolveAll(), element, parent, step, doStep)
    }
    
  }

}
