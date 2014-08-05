/*
 * Copyright 2014 Brady Wood, Branko Juric
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

import java.util.concurrent.TimeUnit
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.WebDriverWait
import gwen.Predefs.Kestrel
import gwen.dsl.Failed
import gwen.dsl.Step
import gwen.eval.EnvContext
import gwen.gwenSetting
import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.http.util.ExceptionUtils
import java.io.PrintWriter
import java.io.StringWriter
import org.openqa.selenium.TimeoutException

/**
 * Defines the web environment context. This includes the configured selenium web
 * driver instance and the feature and page scopes.
 *
 *  @author Branko Juric
 */
class WebEnvContext(val driverName: String) extends EnvContext {

  /**
   * Provides access to the user scopes.
   */
  def featureScopes = dataScope("feature")

  /**
   * Provides access to the page scopes.
   */
  def pageScopes = dataScope("page")

  /**
   * Selenium web driver (lazily loaded).
   */
  private var isWebDriverSet = false
  lazy val webDriver: WebDriver = {
    loadWebDriver(driverName) tap { webdriver =>
      isWebDriverSet = true;
      gwenSetting.getOpt("gwen.web.wait.seconds") foreach { wait =>
        webdriver.manage().timeouts().implicitlyWait(wait.toLong, TimeUnit.SECONDS)
      }
    }
  }

  /**
   * Gets the selenium webdriver.
   *
   * @param driverName
   * 			the name of the driver to get
   */
  private[web] def loadWebDriver(driverName: String): WebDriver = 
    loadWebDriver(driverName, gwenSetting.getOpt("gwen.web.useragent"))
  
  /**
   * Gets the selenium webdriver.
   *
   * @param driverName
   * 			the name of the driver to get
   * @param userAgent
   * 			optional user agent to set in header
   */
  private def loadWebDriver(driverName: String, userAgent: Option[String]): WebDriver = driverName match {
    case "Firefox" => 
      userAgent.fold(new FirefoxDriver) { agent =>
        new FirefoxDriver(new FirefoxProfile() tap { _.setPreference("general.useragent.override", agent) })
      }
    case "IE" => new InternetExplorerDriver()
    case "Chrome" =>
      userAgent.fold(new ChromeDriver) { agent =>
        new ChromeDriver(new ChromeOptions() tap { _.addArguments(s"--user-agent=$agent") })
      }
    case "Safari" => new SafariDriver
    case _ => sys.error(s"Unsupported webdriver: $driverName")
  }

  /**
   * Closes this context and the web driver (if it has loaded).  Once closed,
   * this context should not be used again.
   */
  override def close() {
    if (isWebDriverSet) {
      webDriver.quit()
      isWebDriverSet = false
    }
  }
  
  /**
   * Waits until a given condition is ready.
   * 
   * @param timeoutSecs the number of seconds to wait before timing out
   * @param until the boolean condition to wait for (until true)
   */
  def wait(timeoutSecs: Int)(until: => Boolean) {
    try {
      new WebDriverWait(webDriver, timeoutSecs).until(
        new ExpectedCondition[Boolean] {
          override def apply(driver: WebDriver): Boolean = until
        }
      )
    } catch {
      case timeout: TimeoutException => throw timeout.getCause();
    }
  }
  
  /**
   * Highlights (blinks) a Webdriver element.
    In pure javascript, as suggested by https://github.com/alp82.
   */
  def highlight(element: WebElement) {
    webDriver.asInstanceOf[JavascriptExecutor].executeScript("""
        element = arguments[0];
        original_style = element.getAttribute('style');
        element.setAttribute('style', original_style + "; background: yellow; border: 2px solid red;");
        setTimeout(function(){
            element.setAttribute('style', original_style);
        }, 300);
    """, element)
  }
  
  /**
   * Includes a screenshot in the list of failure attachments.
   * 
   * @param failed
   * 			the failed status
   */
  override def createAttachments(failure: Failed): List[(String, File)] =
    ("Screenshot", webDriver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)) :: super.createAttachments(failure)
    
}
