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

class DryRunTest extends BaseFeatureTest {

  "Sequential dry run using feature-level state" should "validate all features" in {
    withSetting("gwen.state.level", "feature") {
      evaluate(
        List(
          "features",
          "gwen-workspace/samples"), 
        parallel = false, 
        dryRun = true, 
        "target/reports/dry-run/feature-level/sequential", 
        None)
    }
  }

  "Parallel dry run using feature-level state" should "validate all features" in {
    withSetting("gwen.state.level", "feature") {
      evaluate(
        List(
          "features",
          "gwen-workspace/samples"),
        parallel = true, 
        dryRun = true, 
        "target/reports/dry-run/feature-level/parallel", 
        None)
    }
  }

  "Sequential dry run using scenario-level state" should "validate all features" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(
        List(
          "features/flow",
          "features/multi-locators",
          "features/csvdriven",
          "gwen-workspace/samples/todo/scenario-level",
          "gwen-workspace/samples/todo/single-scenario"), 
        parallel = false, 
        dryRun = true, 
        "target/reports/dry-run/scenario-level/sequential", 
        None)
    }
  }

  "Parallel dry run using scenario-level state" should "validate all features" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(
        List(
          "features/flow",
          "features/multi-locators",
          "features/csvdriven",
          "gwen-workspace/samples/todo/scenario-level",
          "gwen-workspace/samples/todo/single-scenario"), 
        parallel = true, 
        dryRun = true, 
        "target/reports/dry-run/scenario-level/parallel", 
        None)
    }
  }
  
  "Dry run explicit CSV data-driven scenario-level feature" should "pass" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(List("features/csvdriven/FloodIO.feature"), parallel = false, dryRun = true, "target/reports/dry-run/scenario-level/sequential/explicit-csv", Some("features/csvdriven/FloodIO.csv"))
    }
  }
  
}