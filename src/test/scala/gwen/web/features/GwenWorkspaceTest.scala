/*
 * Copyright 2019 Brady Wood, Branko Juric
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

class GwenWorkspaceTest extends BaseFeatureTest {

  "Workspace samples using feature level state in parallel" should "should evaluate" in {
    withSetting("gwen.state.level", "feature") {
      evaluate(List("gwen-workspace/samples"), parallel = true, dryRun = false, "target/reports/gwen-workspace/samples/feature-level-parallel", None)
    }
  }

  "Workspace samples using feature level state in dry run mode" should "should evaluate" in {
    withSetting("gwen.state.level", "feature") {
      evaluate(List("gwen-workspace/samples"), parallel = false, dryRun = true, "target/reports/gwen-workspace/samples/feature-level-dryRun", None)
    }
  }

  "Todo sample using scenario level state" should "should evaluate" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(List("gwen-workspace/samples/todo/scenario-level", "gwen-workspace/samples/todo/single-scenario"), parallel = false, dryRun = false, "target/reports/gwen-workspace/samples-todo/scenario-level", None)
    }
  }

  "Todo sample using scenario level state in parallel" should "should evaluate" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(List("gwen-workspace/samples/todo/scenario-level", "gwen-workspace/samples/todo/single-scenario"), parallel = true, dryRun = false, "target/reports/gwen-workspace/samples-todo/scenario-level-parallel", None)
    }
  }

  "Todo sample using scenario level state in dry run mode" should "should evaluate" in {
    withSetting("gwen.state.level", "scenario") {
      evaluate(List("gwen-workspace/samples/todo/scenario-level", "gwen-workspace/samples/todo/single-scenario"), parallel = false, dryRun = true, "target/reports/gwen-workspace/samples-todo/scenario-level-dryRun", None)
    }
  }
  
}