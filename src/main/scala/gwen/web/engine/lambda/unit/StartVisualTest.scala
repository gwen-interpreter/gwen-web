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
import gwen.web.engine.eyes.EyesSettings

import gwen.core.Errors
import gwen.core.engine.lambda.UnitStep
import gwen.core.model._
import gwen.core.model.gherkin.Step

import com.applitools.eyes.RectangleSize

class StartVisualTest(name: String, width: Option[Int], height: Option[Int]) extends UnitStep[WebContext] {

  override def apply(parent: Identifiable, step: Step, ctx: WebContext): Step = {
    step tap { _ =>
      checkStepRules(step, BehaviorType.Action, ctx)
      if (EyesSettings.`gwen.applitools.eyes.enabled`) {
        val viewportSize = width flatMap { width => 
          height map { height => 
            new RectangleSize(width, height)
          }
        }
        ctx.startVisualTest(name, viewportSize)
      } else {
        Errors.disabledStepError(step)
      }
    }
  }

}
