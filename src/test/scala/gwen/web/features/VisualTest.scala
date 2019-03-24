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

class VisualTest extends BaseFeatureTest {

  "visual test dry runs" should "pass" in {
    evaluate(List("features/visual/gwen-reports", "features/visual/todo"), parallel = false, dryRun = true, "target/reports/visual-test-dry-run", None)
  }

  "sequential visual tests" should "pass" in {
    sys.env.get("APPLITOOLS_API_KEY").foreach { _ =>
      evaluate(List("features/visual/gwen-reports", "features/visual/todo"), parallel = false, dryRun = false, "target/reports/visual-test-sequential", None)
    }
  }

  "parallel visual tests" should "pass" in {
    sys.env.get("APPLITOOLS_API_KEY").foreach { _ =>
      evaluate(List("features/visual/gwen-reports", "features/visual/todo"), parallel = true, dryRun = false, "target/reports/visual-test-parallel", None)
    }
  }
  
}