/*
 * Copyright 2025 Brady Wood, Branko Juric
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

package gwen.web.eval

import gwen.web._

import org.scalatest.matchers.should.Matchers

import scala.sys.process.stringToProcess
import scala.sys.process.stringSeqToProcess

class ProjectTest extends BaseTest with Matchers {
  "Newly initialised project" should "should run" in {
    Seq("/bin/sh", "-c", "./project.sh test").! should be (0)
  }

}
