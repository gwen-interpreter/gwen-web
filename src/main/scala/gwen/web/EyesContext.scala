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
import com.applitools.eyes.{BatchInfo, MatchLevel, RectangleSize, TestResults}
import com.typesafe.scalalogging.LazyLogging
import gwen.errors.licenseError
import gwen.Predefs.Kestrel
import gwen.web.errors.invalidVisualSessionStateError
import org.openqa.selenium.WebDriver

/**
  * AppliTools Eyes context for performing visual checks.
  */
class EyesContext(env: WebEnvContext) extends LazyLogging {

  private val API_KEY_NAME = "APPLITOOLS_API_KEY"

  private lazy val eyesBatch = new BatchInfo(EyesSettings.`gwen.applitools.eyes.batchName`)
  private var eyesSession: Option[Eyes] = None

  private def withApiKey[T](f: String => T): T = {
    sys.env.get(API_KEY_NAME).map(f).getOrElse {
      licenseError(s"This operation integrates with AppliTools and requires the $API_KEY_NAME environment variable to be set to your licensed API key. Please set the variable or visit https://applitools.com/ to acquire a license if you don't have one.")
    }
  }

  /**
    * Opens a new Eyes session.
    *
    * @param driver the web driver
    * @param testName the name of the visual test
    * @param viewportSize optional viewport size
    * @return a new Eyes instance
    */
  def open[T](driver: WebDriver, testName: String, viewportSize: Option[RectangleSize]): Eyes = withApiKey { apiKey =>
    new Eyes() tap { eyes =>
      eyes.setApiKey(apiKey)
      eyes.setBatch(eyesBatch)
      val appName = EyesSettings.`gwen.applitools.eyes.appName` getOrElse {
        env.scopes.getOpt("gwen.feature.file.path").map(path => path.substring(0, path.lastIndexOf('.'))).getOrElse("Gwen REPL")
      }
      viewportSize.fold(eyes.open(driver, appName, testName)) { size =>
        eyes.open(driver, appName, testName, size)
      }
      eyesSession = Some(eyes)
    }
  }

  /**
    * Performs a visual checkpoint of the contents in the current browser window.
    *
    * @param checkpoint the checkpoint name
    * @param fullPage true to checkpoint full page, false for visible viewport only
    * @param matchLevel optional match Level
    */
  def check(checkpoint: String, fullPage: Boolean, matchLevel: Option[MatchLevel]): Unit = withApiKey { _ =>
    if (eyesSession.isEmpty) {
      invalidVisualSessionStateError("""Please start a visual test first by calling DSL: I start visual test as "your test name" in width x height viewport""")
    }
    eyesSession foreach { eyes =>
      eyes.setForceFullPageScreenshot(fullPage)
      eyes.setMatchLevel(matchLevel.orNull)
      eyes.checkWindow(checkpoint)
    }
  }

  /**
    * Closes the Eyes session and checks the result of all visual checkpoints.
    */
  def results(): TestResults = withApiKey { _ =>
    eyesSession.map(getResults).getOrElse {
      invalidVisualSessionStateError("""Please start a visual test first by calling DSL: I start visual test as "your test name" in width x height viewport""")
    }
  }

  private def getResults(eyes: Eyes): TestResults = {
    try {
      eyes.close(false) tap { result =>
        val msg = s"Visual check status: ${result.getStatus}. Details at: ${result.getUrl}"
        if (result.isNew) {
          logger.info(s"Visual baseline created. $msg")
        } else if (result.isPassed) {
          logger.info(msg)
        } else {
          logger.error(msg)
        }
      }
    } finally {
      eyesSession = None
    }
  }

  /**
    * Closes the current eyes context.
    */
  def close(): Unit = {
    eyesSession foreach { eyes =>
      try {
        getResults(eyes)
      } finally {
        eyes.abortIfNotClosed()
      }
    }
    eyesSession = None
  }

}
