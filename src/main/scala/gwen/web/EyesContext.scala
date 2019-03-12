/*
 * Copyright 2019 Brady Wood, Branko Juric
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

import com.applitools.eyes.selenium.Eyes
import com.applitools.eyes.{BatchInfo, RectangleSize}
import com.typesafe.scalalogging.LazyLogging
import gwen.GwenSettings
import gwen.Predefs.Formatting
import gwen.Predefs.Kestrel
import gwen.web.errors.visualCheckError
import org.openqa.selenium.WebDriver

/**
  * AppliTools Eyes context for performing visual checks.
  */
class EyesContext(env: WebEnvContext) extends LazyLogging {

  private lazy val eyesBatch = new BatchInfo(GwenSettings.`gwen.sut.name`.getOrElse("Gwen"))
  private var eyesSession: Option[Eyes] = None
  private var eyesCount = 0

  /**
    * Performs a visual checkpoint of the contents in the current browser window.
    *
    * @param checkpoint the checkpoint name
    * @param fullPage true to checkpoint full page, false otherwise
    * @param viewportSize optional viewport size
    */
  def checkpointVisual(checkpoint: String, fullPage: Boolean, viewportSize: Option[RectangleSize], driver: WebDriver): Unit = {
    if (eyesSession.isEmpty) {
      eyesCount = eyesCount + 1
      val appName = env.scopes.getOpt("gwen.feature.file.path").getOrElse("Gwen REPL")
      val testName = s"${Formatting.padWithZeroes(eyesCount)}: ${env.scopes.getOpt("gwen.feature.name").getOrElse("Test")}"
      val eyes = new Eyes()
      eyesSession = Some(eyes)
      eyes.setBatch(eyesBatch)
      viewportSize.fold(eyes.open(driver, appName, testName)) { size =>
        eyes.open(driver, appName, testName, size)
      }
    }
    eyesSession.foreach { eyes =>
      eyes.setForceFullPageScreenshot(fullPage)
      eyes.checkWindow(checkpoint)
    }
  }

  /**
    * Checks the result of all visual checkpoints. Logs info messages in case of New and Passed, and throws error
    * otherwise.
    */
  def checkVisuals(): Unit = {
    eyesSession.foreach { eyes =>
      eyesSession = None
      eyes.close(false) tap { result =>
        if (result.isNew) {
          logger.info(s"Visual baseline created: ${result.getUrl}")
        } else if (result.isPassed) {
          logger.info(s"Visual check passed: ${result.getUrl}")
        } else {
          visualCheckError(result.getUrl)
        }
      }
    }
  }

  def close(): Unit = {
    checkVisuals()
    eyesSession.foreach(_.abortIfNotClosed())
  }
}
