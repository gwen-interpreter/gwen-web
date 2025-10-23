/*
 * Copyright 2025 Branko Juric, Brady Wood
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

package gwen.web.eval.action.unit

import gwen.web.eval.WebContext
import gwen.web.eval.WebErrors

import gwen.core.behavior.BehaviorType
import gwen.core.eval.action.UnitStepAction
import gwen.core.eval.binding.JSBinding
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.util.chaining._
import scala.util.Success
import scala.util.Failure
import gwen.web.eval.binding.LocatorBinding

class PerformElementActionByJS(jsAction: Option[String], jsActionRef: Option[String], element: String) extends UnitStepAction[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    step tap { _ =>
      checkStepRules(step, BehaviorType.Action, ctx)
      val elemBinding = ctx.getLocatorBinding(element)
      jsActionRef match {
        case Some(ref) =>
          JSBinding.find(ref, ctx) match {
            case Success(_) => 
              performJSAction(ctx.topScope.get(JSBinding.key(ref)), elemBinding, ctx)
            case Failure(e) => 
              throw e
          }
        case None =>
          jsAction foreach { func => 
            performJSAction(func, elemBinding, ctx)
          }
      }
    }
  }

  private def performJSAction(js: String, elemBinding: LocatorBinding, ctx: WebContext): Unit = {
    ctx.parseArrowFunction(js).filter(_.argNames.size == 1) map { f =>
      ctx.perform {
        ctx.applyJSToElement(f.argNames(0), f.body, elemBinding)
      }
    } getOrElse {
      WebErrors.functionSignatureError("Single argument arrow function expected")
    }
  }

}
