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

import gwen.core.behaviour.BehaviourMode

class DryRunTest extends BaseFeatureTest {

  "Sequential dry run using feature-level state" should "validate all features" in {
    BehaviourMode.values.foreach { rule => 
      withSetting("gwen.behaviour.rules", rule.toString) {
        withSetting("gwen.state.level", "feature") {
          evaluate(
            List(
              "src/test/features"), 
            parallel = false, 
            parallelFeatures = false,
            dryRun = true, 
            s"feature-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Parallel dry run using feature-level state" should "validate all features" in {
    BehaviourMode.values.foreach { rule => 
      withSetting("gwen.behaviour.rules", rule.toString) {
        withSetting("gwen.state.level", "feature") {
          evaluate(
            List(
              "src/test/features"),
            parallel = true, 
            parallelFeatures = false,
            dryRun = true, 
           s"feature-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Sequential dry run using scenario-level state" should "validate all features" in {
    BehaviourMode.values.foreach { rule => 
      withSetting("gwen.behaviour.rules", rule.toString) {
        withSetting("gwen.state.level", "scenario") {
          evaluate(
            List(
              "src/test/features/flow",
              "src/test/features/multi-locators",
              "src/test/features/samples/todo/scenario-level",
              "src/test/features/samples/todo/single-scenario"), 
            parallel = false, 
            parallelFeatures = false,
            dryRun = true, 
            s"scenario-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Parallel dry run using scenario-level state" should "validate all features" in {
    BehaviourMode.values.foreach { rule => 
      withSetting("gwen.behaviour.rules", rule.toString) {
        withSetting("gwen.state.level", "scenario") {
          List(false, true).foreach { parallelFeatures =>
            evaluate(
              List(
                "src/test/features/flow",
                "src/test/features/multi-locators",
                "src/test/features/samples/todo/scenario-level",
                "src/test/features/samples/todo/single-scenario"), 
              parallel = true, 
              parallelFeatures = parallelFeatures,
              dryRun = true, 
              s"parallel-${if (parallelFeatures) "features" else "scenarios"}/scenario-level/$rule-rules", 
              None)
          }
        }
      }
    }
  }
  
}
