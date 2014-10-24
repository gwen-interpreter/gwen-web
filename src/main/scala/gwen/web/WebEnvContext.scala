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
import gwen.eval.DataScopes

/**
 * Defines the web environment context. This includes the configured selenium web
 * driver instance and the feature and page scopes.
 *
 *  @author Branko Juric
 */
class WebEnvContext(val driverName: String, val dataScopes: DataScopes) extends EnvContext(dataScopes) {

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
  private[web] def loadWebDriver(driverName: String): WebDriver = {
    val userAgent = gwenSetting.getOpt("gwen.web.useragent")
	val authorizePlugins = gwenSetting.getOpt("gwen.authorize.plugins")
    driverName.toLowerCase() match {
      case "firefox" => 
        userAgent.fold(new FirefoxDriver) { agent =>
          new FirefoxDriver(new FirefoxProfile() tap { _.setPreference("general.useragent.override", agent) })
        }
      case "ie" => new InternetExplorerDriver()
      case "chrome" =>
        new ChromeDriver(new ChromeOptions() tap { options =>
	      userAgent foreach { agent => options.addArguments(s"--user-agent=$agent") }
		  authorizePlugins foreach { authorize => 
		    if (authorize.toBoolean) {
		      options.addArguments(s"--always-authorize-plugins") 
			}
		  }
	      options.addArguments("--test-type")
	    })
      case "safari" => new SafariDriver
      case _ => sys.error(s"Unsupported webdriver: $driverName")
    }
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
  
  def executeScript(javascript: String): Any = 
    webDriver.asInstanceOf[JavascriptExecutor].executeScript(javascript) tap { result =>
      logger.debug(s"Evaluating javascript: $javascript")
    }
  
  /**
   * Waits until a given condition is ready. Errors if times out 
   * after "gwen.web.wait.seconds".
   * 
   * @param reason the reason for waiting (used to report timeout error)
   * @param condition the boolean condition to wait for (until true)
   */
  def waitUntil(reason: String)(condition: => Boolean) {
    waitUntil(reason, gwenSetting.get("gwen.web.wait.seconds").toInt) { condition }
  }
  
  /**
   * Waits until a given condition is ready for a given number of seconds. 
   * Errors on given timeout out seconds.
   * 
   * @param reason the reason for waiting (used to report timeout error)
   * @param timeoutSecs the number of seconds to wait before timing out
   * @param condition the boolean condition to wait for (until true)
   */
  def waitUntil(reason: String, timeoutSecs: Int)(condition: => Boolean) {
    try {
      new WebDriverWait(webDriver, timeoutSecs).until(
        new ExpectedCondition[Boolean] {
          override def apply(driver: WebDriver): Boolean = condition
        }
      )
    } catch {
      case e: TimeoutException => throw new TimeoutException(s"Timed out $reason", e)
    }
  }
  
  /**
   * Highlights (blinks) a Webdriver element.
   * In pure javascript, as suggested by https://github.com/alp82.
   *
   * @param element
   * 			the element to highlight
   * @param msecs
   * 			the time in milliseconds to keep the highlight active
   */
  def highlight(element: WebElement) {
    val msecs = gwenSetting.getOpt("gwen.web.highlight.msces").getOrElse("200").toLong
    webDriver.asInstanceOf[JavascriptExecutor].executeScript(s"""
        element = arguments[0];
        original_style = element.getAttribute('style');
        element.setAttribute('style', original_style + "; background: yellow; border: 2px solid gold;");
        setTimeout(function(){
            element.setAttribute('style', original_style);
        }, ${msecs});""", element)
    Thread.sleep(msecs);
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
