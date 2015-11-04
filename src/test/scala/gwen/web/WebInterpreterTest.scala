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

package gwen.web

import java.io.File
import org.scalatest.FlatSpec
import gwen.dsl.Failed
import gwen.dsl.Passed
import gwen.eval.GwenOptions
import gwen.Settings
import gwen.eval.GwenLauncher

abstract class WebInterpreterTest extends FlatSpec {

  private[web] def evaluate(features: List[String], parallel: Boolean, dryRun: Boolean, reportDir: String, csvFile: Option[String]) {
    Settings.synchronized {
      var args = Array("-b", "-r", reportDir)
      if (parallel) args = args ++ Array("--parallel")
      if (dryRun) args = args ++ Array("-n")
      if (csvFile.nonEmpty) args = args ++ Array("-c", csvFile.get)
      args = args ++ features.toArray.asInstanceOf[Array[String]]
      val options = GwenOptions(WebInterpreter.getClass, args)
      val launcher = new GwenLauncher(new WebInterpreter)
      launcher.run(options, None) match {
        case Passed(_) => // woo hoo
        case Failed(_, error) => error.printStackTrace(); fail(error.getMessage())
        case _ => fail("evaluation expected but got noop")
      }
    }
  }
  
}