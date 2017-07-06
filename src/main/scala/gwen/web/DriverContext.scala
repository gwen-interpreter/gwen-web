/*
 * Copyright 2015 Brady Wood, Branko Juric
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

import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import org.openqa.selenium.{JavascriptExecutor, OutputType, TakesScreenshot, WebDriver}
import gwen.Predefs.Kestrel

/**
  * The web driver context. All web driver interactions happen here.
  */
class DriverContext(env: WebEnvContext) extends LazyLogging {

  /** The web driver manager. */
  private val driverManager = new DriverManager()

  /** Last captured screenshot file size. */
  private var lastScreenshotSize: Option[Long] = None

  /** Resets the driver context. */
  def reset() {
    driverManager.reset()
    lastScreenshotSize = None
  }

  /** Closes all browsers and associated web drivers (if any have loaded). */
  def close() {
    driverManager.quit()
  }

  /**
    * Invokes a function that performs an operation on the current web driver
    * session and conditionally captures the current screenshot if the specified
    * takeScreenShot is true.
    *
    * @param f the function to perform
    * @param takeScreenShot true to take screenshot after performing the function
    */
  private def withWebDriver[T](f: WebDriver => T)(implicit takeScreenShot: Boolean = false): T = {
    driverManager.withWebDriver { driver =>
      f(driver) tap { _ =>
        if (takeScreenShot) {
          captureScreenshot(false)
        }
      }
    }
  }

  /** Captures and the current screenshot and adds it to the attachments list. */
  private def captureScreenshot(unconditional: Boolean) {
    Thread.sleep(WebSettings.`gwen.web.throttle.msecs` / 2)
    val screenshot = driverManager.withWebDriver { driver =>
      driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)
    }
    val keep = unconditional || WebSettings.`gwen.web.capture.screenshots.duplicates` || lastScreenshotSize.fold(true) { _ != screenshot.length}
    if (keep) {
      if (!WebSettings.`gwen.web.capture.screenshots.duplicates`) lastScreenshotSize = Some(screenshot.length())
      env.addAttachment("Screenshot", screenshot.getName.substring(screenshot.getName.lastIndexOf('.') + 1), null) tap {
        case (_, file) => FileUtils.copyFile(screenshot, file)
      }
    }
  }

  /**
    * Injects and executes a javascript on the current page through web driver.
    *
    * @param javascript the script expression to execute
    * @param params optional parameters to the script
    * @param takeScreenShot true to take screenshot after performing the function
    */
  def executeJS(javascript: String, params: Any*)(implicit takeScreenShot: Boolean = false): Any = withWebDriver { webDriver =>
    webDriver.asInstanceOf[JavascriptExecutor].executeScript(s"$javascript", params.map(_.asInstanceOf[AnyRef]) : _*) tap { result =>
      if (takeScreenShot && WebSettings.`gwen.web.capture.screenshots`) {
        captureScreenshot(false)
      }
      logger.debug(s"Evaluated javascript: $javascript, result='$result'")
      if (result.isInstanceOf[Boolean] && result.asInstanceOf[Boolean]) {
        Thread.sleep(WebSettings.`gwen.web.throttle.msecs`)
      }
    }
  }

}
