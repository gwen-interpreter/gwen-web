/*
 * Copyright 2014-2021 Brady Wood, Branko Juric
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

import gwen.web.BaseTest
import gwen.web.GwenWebInterpreter

import gwen.core.GwenOptions
import gwen.core.Settings
import gwen.core.behavior.BehaviorMode
import gwen.core.status.Passed
import gwen.core.status.Failed

abstract class BaseFeatureTest extends BaseTest {

  private [features] def evaluate(features: List[String], parallel: Boolean, dryRun: Boolean, reportDir: String, dataFile: Option[String]): Unit = {
    Settings.exclusively {
      val reportPath = s"${this.getClass.getSimpleName}${if (dryRun) "-dryRun" else ""}"
      val execModePath = if (parallel) "parallel" else "sequential"
      val runRP = !dryRun && Settings.getOpt("rp").map(_.toBoolean).getOrElse(false)
      var args = Array("-b", "-r", s"target/reports/$reportPath/$execModePath/$reportDir", "-f", s"html,results,junit,json${if (runRP) ",rp" else ""}")
      if (parallel) args = args ++ Array("--parallel")
      if (dryRun) args = args ++ Array("-n")
      if (dataFile.nonEmpty) args = args ++ Array("-i", dataFile.get)
      if (BehaviorMode.isStrict) args = args ++ Array("-t", "~@Lenient")
      args = args ++ features.toArray.asInstanceOf[Array[String]]
      val options = GwenOptions(args)
      val interpreter = GwenWebInterpreter
      interpreter.run(options, None) match {
        case _: Passed => // woo hoo
        case Failed(_, error) => error.printStackTrace(); fail(error.getMessage)
        case _ => fail("evaluation expected but got No-op")
      }
    }
  }
  
}