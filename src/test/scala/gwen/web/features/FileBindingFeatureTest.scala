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

import org.scalatest.prop.TableDrivenPropertyChecks.forAll

class FileBindingFeatureTest extends BaseFeatureTest {

  forAll (levels) { level => 
    withSetting("gwen.state.level", level) {
      s"File bindings tests using $level-level state" should "pass" in {
        evaluate(List("features/file"), parallel = false, dryRun = false, s"target/reports/file/$level-level", None)
      }
    }
  }
  
}