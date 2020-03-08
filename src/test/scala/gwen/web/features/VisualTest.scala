/*
 * Copyright 2017 Brady Wood, Branko Juric
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

import gwen.web.EyesSettings

class VisualTest extends BaseFeatureTest {

  "sequential visual tests using feature-level state" should "pass" in {
    if (EyesSettings.`gwen.applitools.eyes.enabled`) {
      sys.env.get("APPLITOOLS_API_KEY").foreach { _ =>
        withSetting("gwen.state.level", "feature") {
          evaluate(List("features/floodio", "features/blogs", "features/google"), parallel = false, dryRun = false, "target/reports/sequential/feature-level", None)
          evaluate(List("features/visual/gwen-reports", "features/visual/todo"), parallel = false, dryRun = false, "target/reports/visual-test-sequential/feature-level", None)
        }
      }
    }
  }

  "parallel visual tests using feature-level state" should "pass" in {
    if (EyesSettings.`gwen.applitools.eyes.enabled`) {
      sys.env.get("APPLITOOLS_API_KEY").foreach { _ =>
        withSetting("gwen.state.level", "feature") {
          evaluate(List("features/visual/gwen-reports", "features/visual/todo"), parallel = true, dryRun = false, "target/reports/visual-test-parallel/feature-level", None)
        }
      }
    }
  }
  
}