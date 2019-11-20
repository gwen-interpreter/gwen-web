/*
 * Copyright 2014-2019 Brady Wood, Branko Juric
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

package gwen.web.features

import org.scalatest.prop.TableDrivenPropertyChecks.forAll

class BindingsFeatureTest extends BaseFeatureTest {

  "Bindings with scenario initializer using feature state level" should "pass exeuction" in {
    withSetting("gwen.state.level", "feature") {
      evaluate(List("features/bindings/ScopedBindings1.feature"), parallel = true, dryRun = false, "target/reports/bindings1/feature-level", None)
    }
  }

  forAll (levels) { level => 
    s"Bindings with background initializer using $level state level" should "pass" in {
      withSetting("gwen.state.level", level) {
        evaluate(List("features/bindings/ScopedBindings2.feature"), parallel = false, dryRun = false, s"target/reports/bindings2/$level-level", None)
      }
    }
  }

  "Bindings with background initializer" should "pass parallel scenario exeuction" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(List("features/bindings/ScopedBindings2.feature"), parallel = true, dryRun = false, "target/reports/bindings2-parallel", None)
    }
  }
  
}