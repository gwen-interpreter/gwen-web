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

import gwen.web.eval.WebContext
import gwen.web.eval.binding.SelectorType
import gwen.web.eval.binding.LocatorKey

import gwen.core.behavior.BehaviorType
import gwen.core.eval.lambda.UnitStep
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.util.chaining._

class BindElementLocator(name: String, selectorType: SelectorType, expression: String, container: Option[String], timeoutSecs: Option[Long], index: Option[Int]) extends UnitStep[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {

    step tap { _ =>

      checkStepRules(step, BehaviorType.Context, ctx)
      container foreach { cont =>
        ctx.getLocatorBinding(cont)
      }

      ctx.scopes.set(LocatorKey.baseKey(name), selectorType.toString)
      ctx.scopes.set(LocatorKey.selectorKey(name, selectorType), expression)

      val cKey = LocatorKey.containerKey(name, selectorType)
      if (container.isEmpty) {
        ctx.scopes.getOpt(cKey) foreach { _ =>
          ctx.scopes.set(cKey, null)
        }
      } else {
        container foreach { cont =>
          ctx.scopes.set(cKey, cont)
        }
      }

      val iKey = LocatorKey.indexKey(name, selectorType)
      if (index.isEmpty) {
        ctx.scopes.getOpt(iKey) foreach { _ =>
          ctx.scopes.set(iKey, null)
        }
      } else {
        index foreach { idx =>
          ctx.scopes.set(iKey, idx.toString)
        }
      }

      val tKey = LocatorKey.timeoutSecsKey(name, selectorType)
      if (timeoutSecs.isEmpty) {
        ctx.scopes.getOpt(tKey) foreach { _ =>
          ctx.scopes.set(tKey, null)
        }
      } else {
        timeoutSecs foreach { secs =>
          ctx.scopes.set(tKey, secs.toString)
        }
      }

    }

  }

}
