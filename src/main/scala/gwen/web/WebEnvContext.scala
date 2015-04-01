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
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import gwen.Predefs.Kestrel
import gwen.Predefs.RegexContext
import gwen.Settings
import gwen.dsl.Failed
import gwen.dsl.Step
import gwen.eval.EnvContext
import gwen.eval.ScopedDataStack
import gwen.eval.support.RegexSupport
import gwen.eval.support.XPathSupport

/**
  * Defines the web environment context. This includes the configured selenium web
  * driver instance, feature and page scopes, and web element functions.
  *
  *  @author Branko Juric
  */
class WebEnvContext(val scopes: ScopedDataStack) extends EnvContext(scopes) with WebElementLocator with DriverManager with RegexSupport with XPathSupport {

   /** Resets the current context and closes the web browser. */
  override def reset() {
    super.reset()
    close()
  }

  /** Closes the current web driver. */
  override def close() {
    quit()
  }
  
  /**
    * Injects and executes a javascript on the current page.
    * 
    * @param javascript the script expression to execute
    * @param params optional parameters to the script
    */
  def executeScript(javascript: String, params: Any*): Any = 
    webDriver.asInstanceOf[JavascriptExecutor].executeScript(javascript, params.map(_.asInstanceOf[AnyRef]) : _*) tap { result =>
      logger.debug(s"Evaluated javascript: $javascript, result='$result'")
      if (result.isInstanceOf[Boolean] && result.asInstanceOf[Boolean]) {
        Thread.sleep(WebSettings.`gwen.web.throttle.msecs`)
      }
    }
  
  /**
    * Waits for a given condition to be true. Errors on time out 
    * after "gwen.web.wait.seconds" (default is 10 seconds)
    * 
    * @param reason the reason for waiting (used to report timeout error)
    * @param condition the boolean condition to wait for (until true)
    */
  def waitUntil(reason: String)(condition: => Boolean) {
    waitUntil(reason, WebSettings.`gwen.web.wait.seconds`) { condition }
  }
  
  /**
    * Waits or a given condition to be true. Errors on time out 
    * after "gwen.web.wait.seconds" (default is 10 seconds)
    * 
    * @param condition the boolean condition to wait for (until true)
    */
  def waitUntil(condition: => Boolean) {
    waitUntil(WebSettings.`gwen.web.wait.seconds`) { condition }
  }
  
  /**
    * Waits for a given condition to be true for a given number of seconds. 
    * Errors after given timeout out seconds.
    * 
    * @param reason the reason for waiting (used to report timeout error)
    * @param timeoutSecs the number of seconds to wait before timing out
    * @param condition the boolean condition to wait for (until true)
    */
  def waitUntil(reason: String, timeoutSecs: Long)(condition: => Boolean) {
    waitUntil(Some(reason), timeoutSecs)(condition)
  }
  
  /**
    * Waits for a given condition to be true for a given number of seconds. 
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
    * Highlights and then un-highlights a browser element.
    * Uses pure javascript, as suggested by https://github.com/alp82.
    * The duration of the highlight lasts for `gwen.web.throttle.msecs`.
    * The look and feel of the highlight is controlled by the 
    * `gwen.web.highlight.style` setting.
    *
    * @param element the element to highlight
    */
  def highlight(element: WebElement) {
    val msecs = WebSettings`gwen.web.throttle.msecs`
    val style = WebSettings.`gwen.web.highlight.style` 
    executeScript(s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } original_style = element.getAttribute('style'); element.setAttribute('style', original_style + '; ${style}'); setTimeout(function() { element.setAttribute('style', original_style); }, ${msecs});", element)
    Thread.sleep(msecs);
  }
  
  /**
    * Creates a list of error attachments which includes the current 
    * screenshot and all current error attachments.
    * 
    * @param failed the failed status
    */
  override def createErrorAttachments(failure: Failed): List[(String, File)] =
    captureScreenshot :: super.createErrorAttachments(failure)
    
  /** Captures and returns the current screenshot as an attachment (name-file pair). */
  private def captureScreenshot: (String, File) = ("Screenshot", webDriver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE))
   
  /**
    * Performs a function on a web element and transparently re-locates elements and 
    * re-attempts the function if the web driver throws an exception.
    * 
    * @param action the action string
    * @param element the element reference to perform the action on
    * @param f the function to perform on the element
    */
  def withWebElement[T](action: String, element: String)(f: WebElement => T): T =
     withWebElement(Some(action), element)(f)
  
  /**
    * Performs a function on a web element and transparently re-locates elements and 
    * re-attempts the function if the web driver throws an exception.
    * 
    * @param element the element reference to perform the action on
    * @param f the function to perform on the element
    */
  def withWebElement[T](element: String)(f: WebElement => T): T =
     withWebElement(None, element)(f)
     
  /**
    * Performs a function on a web element and transparently re-locates elements and 
    * re-attempts the function if the web driver throws an exception.
    * 
    * @param action optional action string
    * @param element the element reference to perform the action on
    * @param f the function to perform on the element
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
    * Invokes the given function `f` and then captures the current browser
    * screenshot if the `gwen.web.catpure.screenshots` setting is set.
    */
  def withScreenShot(f: => Unit): Unit = { 
    f
    if (WebSettings.`gwen.web.capture.screenshots`) {
      Thread.sleep(WebSettings.`gwen.web.throttle.msecs`)
      addAttachment(captureScreenshot)
    }
  }
  
  /**
    * Resolves any string concatenations in the given step.
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
  
  /**
    * Gets a bound value from memory. A search for the value is made in 
    * the following order and the first value found is returned:
    *  - Web element text on the current page
    *  - Currently active page scope
    *  - The global feature scope
    *  - Settings
    *  
    * @param name the name of the bound value to find
    */
  def getBoundValue(name: String): String = 
    (Try(getElementText(name)) match {
      case Success(text) => text
      case Failure(e1) => Try(getAttribute(name)) match {
        case Success(text) => text
        case Failure(e2) => Settings.getOpt(name) match { 
          case Some(text) => text
          case _ => sys.error(s"Bound value not found: ${name}")
        }
      }
    }) tap { value =>
      logger.debug(s"getBoundValue(${name})='${value}'")
    }
  
  /**
    * Gets the text value of a web element on the current page. 
    * A search for the text is made in the following order and the first value 
    * found is returned:
    *  - Web element text
    *  - Web element text attribute
    *  - Web element value attribute
    * If a value is found, its value is bound to the current page 
    * scope as `name/text`.
    * @param name the name of the web element
    */
  def getElementText(name: String): String = 
    (withWebElement(name) { webElement =>
      (Option(webElement.getText) match {
        case None | Some("") => Option(webElement.getAttribute("text")) match {
          case None | Some("") => webElement.getAttribute("value")
          case Some(value) => value
        }
        case Some(value) => value
      }) tap { text => 
        bindAndWait(name, "text", text)
      }
    }) tap { value =>
      logger.debug(s"getElementText(${name})='${value}'")
    }
  
  /**
    * Gets a bound attribute value from memory. A search for the value is made 
    * in the current and global feature scopes in the following order and the 
    * first value found is returned:
    *  - name
    *  - name/text
    *  - name/javascript
    *  - name/xpath
    *  - name/regex
    *  
    * @param name the name of the bound attribute to find
    */
  def getAttribute(name: String): String = 
    (scopes.getOpt(name) match {
      case None | Some("") => scopes.getOpt(s"$name/text") match {
        case None | Some("") => scopes.getOpt(s"$name/javascript") match {
          case None | Some("") => scopes.getOpt(s"$name/xpath") match {
            case None | Some("") => scopes.getOpt(s"$name/regex") match {
              case None | Some("") => scopes.get(name)
              case _ =>
                val source = getBoundValue(scopes.get(s"$name/regex/source"))
                val expression = getBoundValue(scopes.get(s"$name/regex/expression"))
                extractByRegex(expression, source)
            }
            case _ =>
              val source = getBoundValue(scopes.get(s"$name/xpath/source"))
              val targetType = getBoundValue(scopes.get(s"$name/xpath/targetType"))
              val expression = getBoundValue(scopes.get(s"$name/xpath/expression"))
              evaluateXPath(expression, source, XMLNodeType.withName(targetType))
          }
          case Some(javascript) => executeScript(s"return $javascript").toString
        }
        case Some(value) => value
      }
      case Some(value) => value
    }) tap { value =>
      logger.debug(s"getAttribute(${name})='${value}'")
    }
  
  /**
    * Binds the given name and value to a given action (name/action=value) 
    * and then waits for any bound post conditions to be satisfied.
    * 
    * @param name the name to bind the value to
    * @param action the action to bind the value to
    * @param value the value to bind
    */
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
  
  /** Gets the title of the current page in the browser.*/
  def getTitle: String = webDriver.getTitle() tap { title =>
    bindAndWait("page", "title", title)
  }
  
  /**
    * Sends a value to a web element (one character at a time).
    * 
    * @param element the name of the element to send the value to
    * @param value the value to send
    * @param clearFirst true to clear field first (if element is a text field)
    * @param sendEnterKey true to send the Enter key after sending the value
    */
  def sendKeys(element: String, value: String, clearFirst: Boolean, sendEnterKey: Boolean) {
    withWebElement(element) { webElement =>
      if (clearFirst) {
        clearText(webElement, element)
      }
      webElement.sendKeys(value)
      bindAndWait(element, "type", value)
      if (sendEnterKey) {
        webElement.sendKeys(Keys.RETURN)
        bindAndWait(element, "enter", "true")
      }
    }
  }
  
  def clearText(element: String) {
    withWebElement(element) { clearText(_, element) }
  }
  
  private def clearText(webElement: WebElement, name: String) {
    webElement.clear()
    bindAndWait(name, "clear", "true")
  }
  
  /**
    * Selects a value in a dropdown (select control) by visible text.
    * 
    * @param element the name of the dropdown element (select control)
    * @param value the value to select
    */
  def selectByVisibleText(element: String, value: String) {
    withWebElement(element) { webElement =>
      new Select(webElement).selectByVisibleText(value)
      bindAndWait(element, "select", value)
    }
  }
  
  /**
    * Selects a value in a dropdown (select control) by index.
    * 
    * @param element the name of the dropdown element (select control)
    * @param index the index to select (first index is 1)
    */
  def selectByIndex(element: String, index: Int) {
    withWebElement(element) { webElement =>
      val select = new Select(webElement)
      select.selectByIndex(index)
      bindAndWait(element, "select", select.getFirstSelectedOption().getText())
    }
  }
  
  /**
    * Waits for text to appear in the given web element.
    * 
    * @param element the element to wait for text on 
    */
  def waitForText(element: String): Boolean = {
    val text = getElementText(element)
    text != null && text.length > 0
  }
  
}

/** Thrown when a fluent wait times out. */
class TimeoutOnWaitException(reason: String) extends Exception(s"Timed out ${reason.head.toLower}${reason.tail}.")
