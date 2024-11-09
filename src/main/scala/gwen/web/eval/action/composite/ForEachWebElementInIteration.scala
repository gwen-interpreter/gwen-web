/*
 * Copyright 2021-2024 Branko Juric, Brady Wood
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

import gwen.web.eval.WebContext

import gwen.core.eval.EvalEngine
import gwen.core.eval.binding.DryValueBinding
import gwen.core.eval.action.composite.ForEach
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

class ForEachWebElementInIteration(doStep: String, element: String, iteration: String, engine: EvalEngine[WebContext]) extends ForEach[WebContext](engine, doStep) {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    val binding = ctx.getLocatorBinding(iteration)
    ctx.evaluate(evaluateForEach(() => List(DryValueBinding.unresolved("webElements")), element, parent, step, ctx)) {
      evaluateForEach(() => binding.resolveAll(), element, parent, step, ctx)
    }
    
  }

}
