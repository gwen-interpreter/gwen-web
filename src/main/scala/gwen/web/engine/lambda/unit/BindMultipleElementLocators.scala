
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
import gwen.web.engine.binding.SelectorType
import gwen.web.engine.binding.LocatorKey

import gwen.core.engine.EvalContext
import gwen.core.engine.EvalEngine
import gwen.core.engine.lambda.UnitStep
import gwen.core.model._
import gwen.core.model.gherkin.Step

class BindMultipleElementLocators[T <: EvalContext](name: String, container: Option[String], timeoutSecs: Option[Long], index: Option[Int], engine: EvalEngine[WebContext], ctx: WebContext) extends UnitStep[WebContext](engine, ctx) {

  override def apply(parent: Identifiable, step: Step): Unit = {
    engine.checkStepRules(step, BehaviorType.Context, env)
    container foreach { cont =>
      ctx.getLocatorBinding(cont)
    }
    env.scopes.set(LocatorKey.baseKey(name), step.table.map(_._2.head).mkString(","))
    step.table foreach { case (_, row ) =>
      val selectorType = SelectorType.parse(row.head)
      val expression = row(1)
      new BindElementLocator(name, selectorType, expression, container, timeoutSecs, index, engine, ctx).apply(parent, step)
    }
  }

}
