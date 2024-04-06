/*
 * Copyright 2014-2021 Brady Wood, Branko Juric
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

class FeaturesTest extends BaseFeatureTest {

  "Parallel mode using feature-level state" should "evaluate all features" in {
    withSetting("gwen.state.level", "feature") {
      evaluate(
        List(
          "src/test/features"), 
        parallel = true, 
        dryRun = false, 
        "feature-level",
        None)
    }
  }

  "Parallel mode using scenario-level state " should "evaluate all features" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(
        List(
          "src/test/features/bindings/ScopedBindings2.feature",
          "src/test/features/errors",
          "src/test/features/samples/todo/scenario-level", 
          "src/test/features/samples/todo/single-scenario"), 
        parallel = true,
        dryRun = false, 
        "parallel-scenarios",
        None)
    }
  }

  "Implicit javascript locators" should "evaluate" in {
    withSetting("gwen.web.implicit.js.locators", "true") {
      evaluate(
        List("src/test/features/samples/se-test"),
        parallel = true, 
        dryRun = false, 
        "implicit-js-locators",
        None)
    }
  }
  
}