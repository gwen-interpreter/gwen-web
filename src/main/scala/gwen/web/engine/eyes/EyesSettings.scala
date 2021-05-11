/*
 * Copyright 2019-2021 Brady Wood, Branko Juric
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

package gwen.web.engine.eyes

import gwen.core.Settings

/**
  * Provides access to gwen Eyes settings used to configure AppliTools.
  *
  * @author Branko Juric
  */
object EyesSettings {

  /**
    * Provides access to the `gwen.applitools.eyes.enabled` setting used to control whether or not Eyes are enabled.
    * (default is enabled). Valid values include: true or false.
    */
  def `gwen.applitools.eyes.enabled`: Boolean =
    Settings.getOpt("gwen.applitools.eyes.enabled").map(_.toBoolean).getOrElse(true)

  /**
    * Provides access to the `gwen.applitools.eyes.batchName` setting used to set the AppliTools batch name (default
    * value is 'Gwen visual tests').
    */
  def `gwen.applitools.eyes.batchName`: String =
    Settings.getOpt("gwen.applitools.eyes.batchName").getOrElse("Gwen visual tests")

  /**
    * Provides access to the optional `gwen.applitools.eyes.appName`. If not set then the appName will be derived
    * from the file path of the current feature being executed. If the execution is in the REPL the it will be
    * 'Gwen REPL'.
    */
  def `gwen.applitools.eyes.appName`: Option[String] = Settings.getOpt("gwen.applitools.eyes.appName")



}