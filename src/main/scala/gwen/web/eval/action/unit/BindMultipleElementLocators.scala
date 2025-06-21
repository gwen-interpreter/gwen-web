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

package gwen.web.eval.action.unit

import gwen.web.eval.WebContext
import gwen.web.eval.binding.RelativeSelectorType
import gwen.web.eval.binding.SelectorType
import gwen.web.eval.binding.LocatorKey

import gwen.core.behavior.BehaviorType
import gwen.core.eval.action.UnitStepAction
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step

import scala.util.chaining._
import scala.util.Try
import scala.util.Failure
import scala.util.Success

class BindMultipleElementLocators(name: String, container: Option[String], index: Option[Int]) extends UnitStepAction[WebContext] {

  override def apply(parent: GwenNode, step: Step, ctx: WebContext): Step = {
    step tap { _ =>
      checkStepRules(step, BehaviorType.Context, ctx)
      container foreach { cont =>
        ctx.getLocatorBinding(cont)
      }
      val selectors = step.table.zipWithIndex flatMap { case ((_, row), idx) =>
        Try(SelectorType.parse(row.head)) match {
          case Failure(e) => if (idx == 0) None else throw e
          case Success(value) => Some(value)
        } map { selectorType => 
          val expression = row(1)
          new BindElementLocator(name, selectorType, expression, container.map(c => (RelativeSelectorType.in, c, None)), index).apply(parent, step, ctx)
          selectorType
        }
      }
      if (selectors.nonEmpty) {
        ctx.topScope.set(LocatorKey.baseKey(name), selectors.mkString(","))
      }
    }
  }

}
