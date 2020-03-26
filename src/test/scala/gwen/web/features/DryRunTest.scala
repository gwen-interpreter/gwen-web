/*
 * Copyright 2014-2020 Brady Wood, Branko Juric
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

import gwen.dsl.BehaviorRules

class DryRunTest extends BaseFeatureTest {

  "Sequential dry run using feature-level state" should "validate all features" in {
    BehaviorRules.values.foreach { rule => 
      withSetting("gwen.behavior.rules", rule.toString) {
        withSetting("gwen.state.level", "feature") {
          evaluate(
            List(
              "features",
              "features-visual",
              "gwen-workspace/samples"), 
            parallel = false, 
            dryRun = true, 
            s"target/reports/features-dryRun/sequential/feature-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Parallel dry run using feature-level state" should "validate all features" in {
    BehaviorRules.values.foreach { rule => 
      withSetting("gwen.behavior.rules", rule.toString) {
        withSetting("gwen.state.level", "feature") {
          evaluate(
            List(
              "features",
              "features-visual",
              "gwen-workspace/samples"),
            parallel = true, 
            dryRun = true, 
           s"target/reports/features-dryRun/parallel/feature-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Sequential dry run using scenario-level state" should "validate all features" in {
    BehaviorRules.values.foreach { rule => 
      withSetting("gwen.behavior.rules", rule.toString) {
        withSetting("gwen.state.level", "scenario") {
          evaluate(
            List(
              "features/flow",
              "features/multi-locators",
              "gwen-workspace/samples/todo/scenario-level",
              "gwen-workspace/samples/todo/single-scenario"), 
            parallel = false, 
            dryRun = true, 
            s"target/reports/features-dryRun/sequential/scenario-level/$rule-rules", 
            None)
        }
      }
    }
  }

  "Parallel dry run using scenario-level state" should "validate all features" in {
    BehaviorRules.values.foreach { rule => 
      withSetting("gwen.behavior.rules", rule.toString) {
        withSetting("gwen.state.level", "scenario") {
          evaluate(
            List(
              "features/flow",
              "features/multi-locators",
              "gwen-workspace/samples/todo/scenario-level",
              "gwen-workspace/samples/todo/single-scenario"), 
            parallel = true, 
            dryRun = true, 
            s"target/reports/features-dryRun/parallel/scenario-level/$rule-rules", 
            None)
        }
      }
    }
  }
  
}