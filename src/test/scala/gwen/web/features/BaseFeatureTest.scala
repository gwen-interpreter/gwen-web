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

import gwen.dsl.BehaviorRules
import gwen.dsl.{Failed, Passed}
import gwen.eval.{GwenLauncher, GwenOptions}
import gwen.Settings
import gwen.web.BaseTest
import gwen.web.WebInterpreter

abstract class BaseFeatureTest extends BaseTest {

  private[web] def evaluate(features: List[String], parallel: Boolean, parallelFeatures: Boolean, dryRun: Boolean, reportDir: String, dataFile: Option[String]): Unit = {
    Settings.synchronized {
      val reportPath = s"${this.getClass.getSimpleName}${if (dryRun) "-dryRun" else ""}"
      val execModePath = if (parallel) "parallel" else "sequential"
      var args = Array("-b", "-r", s"target/reports/$reportPath/$execModePath/$reportDir", "-f", s"junit,html,json")
      //var args = Array("-b", "-r", s"target/reports/$reportPath/$execModePath/$reportDir", "-f", s"junit,html,json${if (!dryRun) ",rp" else ""}")
      if (parallel) args = args ++ Array("--parallel")
      if (parallelFeatures) args = args ++ Array("--parallel-features")
      if (dryRun) args = args ++ Array("-n")
      if (dataFile.nonEmpty) args = args ++ Array("-i", dataFile.get)
      if (BehaviorRules.isStrict) args = args ++ Array("-t", "~@Lenient")
      args = args ++ features.toArray.asInstanceOf[Array[String]]
      val options = GwenOptions(args)
      val launcher = new GwenLauncher(new WebInterpreter)
      launcher.run(options, None) match {
        case Passed(_) => // woo hoo
        case Failed(_, error) => error.printStackTrace(); fail(error.getMessage)
        case _ => fail("evaluation expected but got noop")
      }
    }
  }
  
}