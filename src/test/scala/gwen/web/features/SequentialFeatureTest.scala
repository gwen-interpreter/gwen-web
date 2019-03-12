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

import gwen.Settings

class SequentialFeatureTest extends BaseFeatureTest {

  "Sequential mode" should "evaluate all features in sequence" in {
    evaluate(List("features/floodio", "features/blogs/automationByMeta", "features/todo/CompleteItems.feature"), parallel = false, dryRun = false, "target/reports/sequential", None)
    sys.env.get("APPLITOOLS_API_KEY").foreach { _ =>
      evaluate(List("features/visual"), parallel = false, dryRun = false, "target/reports/visual", None)
    }
  }
  
}