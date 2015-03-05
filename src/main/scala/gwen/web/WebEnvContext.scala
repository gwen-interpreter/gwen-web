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
import java.util.concurrent.TimeUnit
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
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
import gwen.Predefs.RegexContext
import gwen.dsl.Failed
import gwen.dsl.Step
import gwen.eval.EnvContext
import gwen.eval.ScopedDataStack
import gwen.Settings

/**
 * Defines the web environment context. This includes the configured selenium web
 * driver instance and the feature and page scopes.
 *
 *  @author Branko Juric
 */
class WebEnvContext(val driverName: String, val scopes: ScopedDataStack) extends EnvContext(scopes) with WebElementLocator {

  /**
   * Selenium web driver (lazily loaded).
   */
  private var _webDriver: Option[WebDriver] = None
  def webDriver: WebDriver = _webDriver match {
    case None => 
      _webDriver = Some(loadWebDriver(driverName))
      _webDriver.get
    case _ => 
      _webDriver.get
  }
  
  /**
   * Gets the selenium webdriver.
   *
   * @param driverName
   * 			the name of the driver to get
   */
  private[web] def loadWebDriver(driverName: String): WebDriver = {
    (driverName.toLowerCase() match {
      case "firefox" =>
        new FirefoxDriver(new FirefoxProfile() tap { profile =>
          GwenWebSettings.`gwen.web.useragent` foreach { profile.setPreference("general.useragent.override", _) }
          profile.setAcceptUntrustedCertificates(true);
	      if (GwenWebSettings.`gwen.authorize.plugins`) {
            profile.setPreference("security.enable_java", true);
            profile.setPreference("plugin.state.java", 2);
          }
        })
      case "ie" => new InternetExplorerDriver()
      case "chrome" =>
        new ChromeDriver(new ChromeOptions() tap { options =>
	      GwenWebSettings.`gwen.web.useragent` foreach { agent => options.addArguments(s"--user-agent=$agent") }
          if (GwenWebSettings.`gwen.authorize.plugins`) {
		    options.addArguments(s"--always-authorize-plugins") 
		  }
	      options.addArguments("--test-type")
	    })
      case "safari" => new SafariDriver
      case _ => sys.error(s"Unsupported webdriver: $driverName")
    }) tap { driver =>
      driver.manage().timeouts().implicitlyWait(GwenWebSettings.`gwen.web.wait.seconds`, TimeUnit.SECONDS)
      if (GwenWebSettings.`gwen.web.maximize`) {
        driver.manage().window().maximize() 
      }
    }
    
  }
  
   /**
   * Resets the current context and closes the web driver session.
   */
  override def reset() {
    super.reset()
    close()
  }

  /**
   * Closes this context and the web driver (if it has loaded).  Once closed,
   * this context should not be used again.
   */
  override def close() {
    _webDriver foreach { _.quit() }
    _webDriver = None
  }
  
  /**
   * Executes a javascript expression.
   * 
   * @param javascript
   * 			the script expression to execute
   * @param params
   * 			optional parameters to script expression
   */
  def executeScript(javascript: String, params: Any*): Any = 
    webDriver.asInstanceOf[JavascriptExecutor].executeScript(javascript, params.map(_.asInstanceOf[AnyRef]) : _*) tap { result =>
      logger.debug(s"Evaluated javascript: $javascript")
      if (result.isInstanceOf[Boolean] && result.asInstanceOf[Boolean]) {
	    Thread.sleep(GwenWebSettings.`gwen.web.throttle.msecs`)
	  }
    }
  
  /**
   * Waits until a given condition is ready. Errors if times out 
   * after "gwen.web.wait.seconds" (default is 10 seconds)
   * 
   * @param reason the reason for waiting (used to report timeout error)
   * @param condition the boolean condition to wait for (until true)
   */
  def waitUntil(reason: String)(condition: => Boolean) {
    waitUntil(reason, GwenWebSettings.`gwen.web.wait.seconds`) { condition }
  }
  
  /**
   * Waits until a given condition is ready. Errors if times out 
   * after "gwen.web.wait.seconds" (default is 10 seconds)
   * 
   * @param condition the boolean condition to wait for (until true)
   */
  def waitUntil(condition: => Boolean) {
    waitUntil(GwenWebSettings.`gwen.web.wait.seconds`) { condition }
  }
  
  /**
   * Waits until a given condition is ready for a given number of seconds. 
   * Errors on given timeout out seconds.
   * 
   * @param reason the reason for waiting (used to report timeout error)
   * @param timeoutSecs the number of seconds to wait before timing out
   * @param condition the boolean condition to wait for (until true)
   */
  def waitUntil(reason: String, timeoutSecs: Long)(condition: => Boolean) {
    waitUntil(Some(reason), timeoutSecs)(condition)
  }
  
  /**
   * Waits until a given condition is ready for a given number of seconds. 
   * Errors on given timeout out seconds.
   * 
   * @param timeoutSecs the number of seconds to wait before timing out
   * @param condition the boolean condition to wait for (until true)
   */
  def waitUntil(timeoutSecs: Long)(condition: => Boolean) {
    waitUntil(None, timeoutSecs)(condition)
  }
  
  /**
   * Waits until a given condition is ready for a given number of seconds. 
   * Errors on given timeout out seconds.
   * 
   * @param reason optional reason for waiting (used to report timeout error)
   * @param timeoutSecs the number of seconds to wait before timing out
   * @param condition the boolean condition to wait for (until true)
   */
  private def waitUntil(reason: Option[String], timeoutSecs: Long)(condition: => Boolean) {
    try {
      reason foreach { logger.info(_) }
      new WebDriverWait(webDriver, timeoutSecs).until(
        new ExpectedCondition[Boolean] {
          override def apply(driver: WebDriver): Boolean = condition
        }
      )
    } catch {
      case e: TimeoutException => throw new TimeoutOnWaitException(reason.getOrElse("waiting"));
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
	val msecs = GwenWebSettings`gwen.web.throttle.msecs`
    val style = GwenWebSettings.`gwen.web.highlight.style` 
    executeScript(s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } original_style = element.getAttribute('style'); element.setAttribute('style', original_style + '; ${style}'); setTimeout(function() { element.setAttribute('style', original_style); }, ${msecs});", element)
    Thread.sleep(msecs);
  }
  
  /**
   * Includes a screenshot in the list of error attachments.
   * 
   * @param failed
   * 			the failed status
   */
  override def createErrorAttachments(failure: Failed): List[(String, File)] =
    captureScreenshot :: super.createErrorAttachments(failure)
    
  /**
   * Captures and returns the current screenshot as an attachment (name-file pair).
   */
  private def captureScreenshot = ("Screenshot", webDriver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE))
   
  /**
   * Performs a function on a web element and transparently re-locates elements and 
   * re-attempts the function if the web driver throws an exception.
   * 
   * @param action 
   * 			the action string
   * @param element 
   * 			the element reference to perform the action on
   * @param f
   * 			the function to perform on the element
   */
  def withWebElement[T](action: String, element: String)(f: WebElement => T): T =
     withWebElement(Some(action), element)(f)
  
  /**
   * Performs a function on a web element and transparently re-locates elements and 
   * re-attempts the function if the web driver throws an exception.
   * 
   * @param element 
   * 			the element reference to perform the action on
   * @param f
   * 			the function to perform on the element
   */
  def withWebElement[T](element: String)(f: WebElement => T): T =
     withWebElement(None, element)(f)
     
  /**
   * Performs a function on a web element and transparently re-locates elements and 
   * re-attempts the function if the web driver throws an exception.
   * 
   * @param action 
   * 			optional action string
   * @param element 
   * 			the element reference to perform the action on
   * @param f
   * 			the function to perform on the element
   */
  private def withWebElement[T](action: Option[String], element: String)(f: WebElement => T): T =
     try {
       val webElement = locate(this, element)
       action.foreach { actionString =>
         logger.info(s"${actionString match {
           case "click" => "Clicking"
           case "submit" => "Submitting"
           case "check" => "Checking"
           case "uncheck" => "Unchecking"
         }} $element")
       }
       f(webElement)
     } catch {
       case _: WebDriverException => f(locate(this, element))
     }

  /**
   * Invokes the given function before and then captures the current browser
   * screenshot if the gwen.web.catpure.screenshots setting is set.
   */
  def withScreenShot(f: => Unit): Unit = { 
    f
    if (GwenWebSettings.`gwen.web.capture.screenshots`) {
      Thread.sleep(GwenWebSettings.`gwen.web.throttle.msecs`)
      addAttachment(captureScreenshot)
    }
  }
  
  /**
   * Substitutes dynamic data in concatenated string literals.
   * 
   * @param step the step to resolve
   * @return the resolved step
   */
  override def resolve(step: Step): Step = {
    step.expression match {
      // resolve concatenation: "prefix" + binding + "suffix"
      case r"""(.+?)$prefix"\s*\+\s*(.+?)$binding\s*\+\s*"(.+?)$suffix""" => 
        resolve(Step(step.keyword, s"$prefix${getBoundValue(binding)}$suffix"))
      // resolve concatenation: "prefix" + binding
      case r"""(.+?)$prefix"\s*\+\s*(.+?)$binding\s*""" => 
        resolve(Step(step.keyword, s"""$prefix${getBoundValue(binding)}""""))
      case _ => step
    }
  }
  
  def getBoundValue(binding: String): String = 
    Try(getElementText(binding)) match {
      case Success(text) => text
      case Failure(e1) => Try(getAttribute(binding)) match {
        case Success(text) => text
        case Failure(e2) => Settings.getOpt(binding) match { 
          case Some(text) => text
          case _ => sys.error(s"Bound value not found: ${binding}")
        }
      }
    }
  
  def getElementText(element: String): String = 
    withWebElement(element) { webElement =>
      (Option(webElement.getText) match {
        case None | Some("") => Option(webElement.getAttribute("text")) match {
          case None | Some("") => webElement.getAttribute("value")
          case Some(value) => value
        }
        case Some(value) => value
      }) tap { text => 
        bindAndWait(element, "text", text)
      }
    }
  
  def getAttribute(name: String): String = 
    scopes.getOpt(name) match {
      case None | Some("") => scopes.getOpt(s"$name/text") match {
        case None | Some("") => scopes.getOpt(s"$name/javascript") match {
          case None | Some("") => scopes.get(name)
          case Some(javascript) => executeScript(s"return $javascript").toString
        }
        case Some(value) => value
      }
      case Some(value) => value
    }
  
  def bindAndWait(element: String, action: String, value: String) {
    scopes.set(s"$element/$action", value)
    
    // sleep if wait time is configured for this action
	scopes.getOpt(s"$element/$action/wait") foreach { secs => 
	  logger.info(s"Waiting for ${secs} second(s) (post-$action wait)")
      Thread.sleep(secs.toLong * 1000)
    }
	
	// wait for javascript post condition if one is configured for this action
	scopes.getOpt(s"$element/$action/condition") foreach { condition =>
	  val javascript = scopes.get(s"$condition/javascript")
	  logger.debug(s"Waiting for script to return true: ${javascript}")
	  waitUntil(s"Waiting until $condition (post-$action condition)") {
	    executeScript(s"return $javascript").asInstanceOf[Boolean]
	  }
    }
  }
  
}

/**
 * Thrown when a fluent wait times out
 */
class TimeoutOnWaitException(reason: String) extends Exception(s"Timed out ${reason.head.toLower}${reason.tail}.")
