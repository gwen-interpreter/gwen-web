/*
 * Copyright 2014-2015 Brady Wood, Branko Juric
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

class ParallelFeatureTest extends BaseFeatureTest {

  "Parallel mode using feature-level state" should "evaluate all features in parallel" in {
    withSetting("gwen.state.level", "feature") {
      evaluate(List("features/floodio", "features/bindings", "features/todo/CompleteItems.feature", "features/google"), parallel = true, dryRun = false, "target/reports/parallel/feature-level", None)
    }
  }

  "Workspace samples using feature level state in parallel" should "should evaluate" in {
    withSetting("gwen.state.level", "feature") {
      evaluate(List("gwen-workspace/samples"), parallel = true, dryRun = false, "target/reports/gwen-workspace/samples/feature-level-parallel", None)
    }
  }

  "Workspace Todo sample using scenario level state in parallel" should "should evaluate" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(List("gwen-workspace/samples/todo/scenario-level", "gwen-workspace/samples/todo/single-scenario"), parallel = true, dryRun = false, "target/reports/gwen-workspace/samples-todo/scenario-level-parallel", None)
    }
  }
  
}