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

import gwen.core.behavior.BehaviorMode

class DryRunTest extends BaseFeatureTest {

  "Sequential dry run using feature-level state" should "validate all features" in {
    BehaviorMode.values.foreach { rule => 
      withSetting("gwen.behavior.rules", rule.toString) {
        withSetting("gwen.state.level", "feature") {
          evaluate(
            List(
              "src/test/features"), 
            parallel = false, 
            dryRun = true, 
            s"feature-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Parallel dry run using feature-level state" should "validate all features" in {
    BehaviorMode.values.foreach { rule => 
      withSetting("gwen.behavior.rules", rule.toString) {
        withSetting("gwen.state.level", "feature") {
          evaluate(
            List(
              "src/test/features"),
            parallel = true, 
            dryRun = true, 
           s"feature-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Sequential dry run using scenario-level state" should "validate all features" in {
    BehaviorMode.values.foreach { rule => 
      withSetting("gwen.behavior.rules", rule.toString) {
        withSetting("gwen.state.level", "scenario") {
          evaluate(
            List(
              "src/test/features/flow",
              "src/test/features/functions",
              "src/test/features/samples/todo/scenario-level",
              "src/test/features/samples/todo/single-scenario"), 
            parallel = false, 
            dryRun = true, 
            s"scenario-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Parallel dry run using scenario-level state" should "validate all features" in {
    BehaviorMode.values.foreach { rule => 
      withSetting("gwen.behavior.rules", rule.toString) {
        withSetting("gwen.state.level", "scenario") {
          evaluate(
            List(
              "src/test/features/errors",
              "src/test/features/flow",
              "src/test/features/functions",
              "src/test/features/samples/todo/scenario-level",
              "src/test/features/samples/todo/single-scenario"), 
            parallel = true, 
            dryRun = true, 
            s"parallel-scenarios/$rule-rules", 
            None)
        }
      }
    }
  }
  
}
