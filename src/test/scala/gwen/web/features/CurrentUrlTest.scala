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

class CurrentUrlTest extends BaseFeatureTest {

  forAll (levels) { level => 
    s"Current URL tests using $level-level state" should "pass" in {
      withSetting("gwen.state.level", level) {
        evaluate(List("features/current-url"), parallel = false, dryRun = false, s"target/reports/current-url/$level-level", None)
      }
    }
  }
  
}