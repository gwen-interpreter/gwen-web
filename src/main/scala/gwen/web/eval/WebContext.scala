/*
 * Copyright 2015-2022 Brady Wood, Branko Juric
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
package gwen.web.eval

import WebErrors._
import gwen.web.eval.binding._
import gwen.web.eval.driver.DriverManager
import gwen.web.eval.driver.event.WebSessionEvent
import gwen.web.eval.driver.event.WebSessionEventListener

import gwen.core._
import gwen.core.Errors._
import gwen.core.eval.ComparisonOperator
import gwen.core.eval.EvalContext
import gwen.core.eval.binding.BindingType
import gwen.core.eval.binding.JSBinding
import gwen.core.eval.binding.TextBinding
import gwen.core.eval.support.JSCondition
import gwen.core.node.gherkin.Step
import gwen.core.state.StateLevel
import gwen.core.state.EnvState
import gwen.core.state.SensitiveData
import gwen.core.status.Failed


import scala.concurrent.duration.Duration
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}
import scala.util.chaining._

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium._
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Select

import java.io.File

/**
  * The web evaluatioin context.
  */
class WebContext(options: GwenOptions, envState: EnvState, driverManager: DriverManager) extends EvalContext(options, envState) with LazyLogging with WebSessionEventListener {

  Try(logger.info(s"GWEN_CLASSPATH = ${sys.env("GWEN_CLASSPATH")}"))
  Try(logger.info(s"SELENIUM_HOME = ${sys.env("SELENIUM_HOME")}"))

  private val locatorBindingResolver = new LocatorBindingResolver(this)
  private var lastScreenshotSize: Option[Long] = None

  driverManager.addWebSessionEventListener(this)

  override def sessionOpened(event: WebSessionEvent): Unit = { 
    if (WebSettings.videoEnabled) {
      driverManager.getSessionId(event.driver) foreach { sessionId =>
        addVideo(new File(GwenSettings.`gwen.video.dir`, s"$sessionId.mp4"))
      }
    }
  }

  def locator = new WebElementLocator(this)

  /** Resets the driver context. */
  def reset(): Unit = {
    driverManager.reset()
    lastScreenshotSize = None
  }

  /** Resets the context for the given state level. */
  override def reset(level: StateLevel): Unit = {
    super.reset(level)
    reset()
    close()
  }


  /** Closes the context and all browsers and associated web drivers (if any have loaded). */
  override def close(): Unit = {
    closeDriverSession(None)
    super.close()
  }

  /** Closes a named browser and associated web driver. */
  def close(name: String): Unit = {
    closeDriverSession(Some(name))
  }

  private def closeDriverSession(name: Option[String]): Unit = {
    perform {
      name match {
        case Some(n) => driverManager.quit(n)
        case _ => driverManager.quit()
      }
    }
  }

  /**
   * Adds web engine dsl steps to super implementation. The entries
   * returned by this method are used for tab completion in the REPL.
   */
  override def dsl: List[String] =
    Source.fromInputStream(getClass.getResourceAsStream("/gwen-web.dsl")).getLines().toList ++ super.dsl

  /**
    * Appends a return keyword in front of the given javascript expression in preparation for execute-with-return
    * (since web driver requires return prefix).
    *
    * @param javascript the javascript function
    */
  override def formatJSReturn(javascript: String) = s"return $javascript"

  /**
    * Executes a javascript expression on the current page through the web driver.
    *
    * @param javascript the script expression to execute
    * @param params optional parameters to the script
    */
  override def evaluateJS(javascript: String, params: Any*): Any =
    executeJS(javascript, params.map(_.asInstanceOf[AnyRef])*)

  /**
    * Gets a bound value from memory. A search for the value is made in
    * the following order and the first value found is returned:
    *  - Web element text on the current page
    *  - Currently active page scope
    *  - The top scope
    *  - Settings
    *
    * @param name the name of the bound value to find
    */
  override def getBoundReferenceValue(name: String): String = {
    if (name == "the current URL") {
      val url = captureCurrentUrl
      topScope.set(name, url)
    }
    (getLocatorBinding(name, optional = true) match {
      case Some(binding) =>
        Try(getElementText(binding)) match {
          case Success(text) => text.getOrElse(getAttribute(name))
          case Failure(e) => throw e
        }
      case _ => getAttribute(name)
    }) tap { value =>
      logger.debug(s"getBoundReferenceValue($name)='$value'")
    }
  }

  /**
    * Resolves a bound attribute value from the visible scope.
    *
    * @param name the name of the bound attribute to find
    */
  def getAttribute(name: String): String = {
    getCachedWebElement(s"${JSBinding.key(name)}/param/webElement") map { webElement =>
      val javascript = interpolate(scopes.get(JSBinding.key(name)))
      val jsFunction = s"return (function(element) { return $javascript })(arguments[0])"
      Option(executeJS(jsFunction, webElement)).map(_.toString).getOrElse("")
    } getOrElse {
      Try(super.getBoundReferenceValue(name)) match {
        case Success(value) => value
        case Failure(e) => e match {
          case _: UnboundAttributeException =>
            Try(getLocatorBinding(name).selectors.map(_.expression).mkString(",")).getOrElse(unboundAttributeError(name, e))
          case _ => throw e
        }
      }
    }
  }

  def boundAttributeOrSelection(element: String, selection: Option[DropdownSelection]): () => String = () => {
    selection match {
      case None => getBoundReferenceValue(element)
      case Some(sel) =>
        try {
          getBoundReferenceValue(s"$element $sel")
        } catch {
          case _: UnboundAttributeException =>
            getElementSelection(element, sel).getOrElse(getBoundReferenceValue(element))
          case e: Throwable => throw e
        }
    }
  }

  /**
    * Gets a named locator binding.
    *
    * @param name the name of the web element
    */
  def getLocatorBinding(name: String): LocatorBinding = {
    locatorBindingResolver.getBinding(name, optional = false).get
  }

  /**
   * Gets a named locator binding.
   *
   * @param name the name of the web element
   * @param optional true to return None if not found; false to throw error
   */
  def getLocatorBinding(name: String, optional: Boolean): Option[LocatorBinding] = {
    locatorBindingResolver.getBinding(name, optional)
  }

  /**
    * Add a list of error attachments to the given step including the current
    * screenshot and all current error attachments.
    *
    * @param failure the failed status
    */
  override def addErrorAttachments(step: Step, failure: Failed): Step = {
    (if (failure.isTechError) {
      super.addErrorAttachments(step, failure)
    } else {
      step
    }) tap { _ =>
      if (!failure.isLicenseError) {
        captureScreenshot(true)
      }
    }
  }

    /**
    * Binds the given element and value to a given action (element/action=value)
    * and then waits for any bound post conditions to be satisfied.
    *
    * @param element the element to bind the value to
    * @param action the action to bind the value to
    * @param value the value to bind
    */
  def bindAndWait(element: String, action: String, value: String): Unit = {
    scopes.set(s"$element/$action", value)

    // sleep if wait time is configured for this action
    scopes.getOpt(s"$element/$action/wait") foreach { secs =>
      logger.info(s"Waiting for $secs second(s) (post-$action wait)")
      Thread.sleep(secs.toLong * 1000)
    }

    // wait for javascript post condition if one is configured for this action
    scopes.getOpt(s"$element/$action/condition") foreach { condition =>
      val jsCondition = JSCondition(condition, false, WebSettings.`gwen.web.wait.seconds`, this)
      logger.info(s"waiting until ${jsCondition.name} (post-$action condition)")
      waitUntil(s"waiting for true return from JS conditioon: ${jsCondition.name}") {
        jsCondition.evaluate()
      }
    }
  }

  /**
    * Gets the actual value of an attribute and compares it with an expected value or condition.
    *
    * @param name the name of the attribute being compared
    * @param expected the expected value, regex, xpath, or json path
    * @param actual the actual value of the element
    * @param operator the comparison operator
    * @param negate true to negate the result
    * @param nameSuffix optional name suffix
    * @param message optional error message to use
    * @return true if the actual value matches the expected value
    */
  def compare(name: String, expected: String, actual: () => String, operator: ComparisonOperator, negate: Boolean, nameSuffix: Option[String], message: Option[String]): Unit = {
    var result = false
    var error: Option[String] = None
    var actualValue = actual()
    var polled = false
    try {
      waitUntil(s"waiting for $name to ${if(negate) "not " else ""}$operator '$expected'") {
        if (polled) {
          actualValue = actual()
        }
        polled = true
        result = if (actualValue != null) {
          super.compare(name, expected, actualValue, operator, negate) match {
            case Success(condition) => condition
            case Failure(e) =>
              error = Some(e.getMessage)
              false
          }
        } else false
        result
      }
    } catch {
      case _: WaitTimeoutException => result = false
    }
    error match {
      case Some(msg) =>
        Errors.assertWithError(assertion = false, message, msg)
      case None =>
        if (!polled) {
          result = super.compare(name, expected, actualValue, operator, negate).getOrElse(result)
        }
        val binding = Try(
          getLocatorBinding(name.substring(0, name.length - nameSuffix.map(_.length).getOrElse(0)), optional = true) getOrElse {
            getBinding(name)
          }
        ).map(_.displayName).getOrElse(name)
        Errors.assertWithError(result, message, s"Expected $binding to ${if(negate) "not " else ""}$operator ${if (expected.isEmpty()) "blank" else s"'$expected'"}${if (operator == ComparisonOperator.be && actualValue == expected) "" else s" but got '$actualValue'"}")
    }

  }

  /**
    * Invokes a function that performs an operation on the current web driver
    * session and conditionally captures the current screenshot if the specified
    * takeScreenShot is true.
    *
    * @param function the function to perform
    * @param takeScreenShot true to take screenshot after performing the function
    */
  def withWebDriver[T](function: WebDriver => T)(implicit takeScreenShot: Boolean = false): Option[T] = {
    evaluate(None.asInstanceOf[Option[T]]) {
      driverManager.withWebDriver { driver =>
        Option(function(driver)) tap { _ =>
          if (takeScreenShot) {
            captureScreenshot(false)
          }
        }
      }
    }
  }

  /**
    * Gets a cached web element.
    *
    * @param element the name of the web element
    * @return the cached web element
    */
  def getCachedWebElement(element: String): Option[WebElement] = topScope.getObject(element) match {
    case Some(we: WebElement) =>
      highlightElement(we)
      Some(we)
    case _ => None
  }

  /**
    * Locates a web element and highlights it.
    *
    * @param binding the locator binding
    */
  def locateAndHighlight(binding: LocatorBinding): Unit = {
    withDriverAndElement(binding, s"trying to locate ${binding.displayName}") { (driver, webElement) =>
      createActions(driver).moveToElement(webElement).perform()
    }
  }

  /**
    * Locates a web element and performs an operation on it.
    *
    * @param binding the locator binding
    * @param reason a description of what action is being performed
    * @param operation the operation to perform on the element
    */
  private def withWebElement[T](binding: LocatorBinding, reason: String)(operation: WebElement => T): Option[T] =
    evaluate(None.asInstanceOf[Option[T]]) {
      val selector = binding.selectors.head
      val wHandle = selector.relative.flatMap(_ => withWebDriver(_.getWindowHandle))
      try {
        var result: Option[Try[T]] = None
        val start = System.nanoTime()
        try {
          var lapsed = 0L
          waitUntil(reason) {
            try {
              val webElement = binding.resolve()
              tryMoveTo(webElement)
              if (!selector.isContainer) {
                highlightElement(webElement)
              }
              val res = operation(webElement)
              result = Some(Success(res))
              true
            } catch {
              case e: Throwable =>
                lapsed = Duration.fromNanos(System.nanoTime() - start).toSeconds
                if (e.isInstanceOf[InvalidElementStateException] || e.isInstanceOf[NoSuchElementException] || e.isInstanceOf[NotFoundOrInteractableException]) {
                  if (lapsed >= binding.timeoutSeconds) {
                    result =  if (e.isInstanceOf[WebElementNotFoundException]) {
                      Some(Failure(e))
                    } else {
                      Some(Try(elementNotInteractableError(binding, e)))
                    }
                    true
                  } else {
                    result = Some(Failure(e))
                    false
                  }
                } else {
                  result = Some(Failure(e))
                  false
                }
            }
          }
        } catch {
          case _: WaitTimeoutException if result.exists(_.isFailure) =>
            waitTimeoutError(WebSettings.`gwen.web.wait.seconds`, reason, result.get.failed.get)
        }
        result.map {
          case Success(res) =>
            res tap { _ =>
              if (WebSettings.`gwen.web.capture.screenshots.enabled`) {
                captureScreenshot(false)
              }
            }
          case Failure(e) =>
            throw e
        }
      } finally {
        wHandle foreach { handle =>
          withWebDriver { driver =>
            driver.switchTo().window(handle)
          }
        }
      }
    }

  def tryMoveTo(webElement: WebElement): Unit = {
    if (WebSettings.`gwen.web.implicit.element.moveTo` || (!webElement.isDisplayed && !isInViewport(webElement))) {
      withWebDriver { driver =>
        createActions(driver).moveToElement(webElement).perform()
      }
    }
  }

  /** Captures the current screenshot and adds it to the attachments list. */
  def captureScreenshot(unconditional: Boolean, name: String = "Screenshot"): Option[File] = {
    evaluate(Option(new File("$[dryRun:screenshotFile]"))) {
      Try(
        driverManager.withWebDriver { driver =>
          Thread.sleep(150) // give browser time to render
          driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)
        }
      ) match { 
        case Success(screenshot) =>
          val keep = unconditional || WebSettings.`gwen.web.capture.screenshots.duplicates` || lastScreenshotSize.fold(true) { _ != screenshot.length}
          if (keep) {
            if (!WebSettings.`gwen.web.capture.screenshots.duplicates`) lastScreenshotSize = Some(screenshot.length())
            addAttachment(name, screenshot)
            Some(screenshot)
          } else {
            None
          }
        case Failure(_) => None
      }
    }
  }

  /** Captures an element screenshot and adds it to the attachments list. */
  def captureElementScreenshot(binding: LocatorBinding, name: String = "Element Screenshot"): Option[File] = {
    evaluate(Option(new File("$[dryRun:elementScreenshotFile]"))) {
      withWebElement(binding, s"trying to capture element screenshot of ${binding.displayName}") { webElement =>
        Thread.sleep(150) // give element time to render
        webElement.getScreenshotAs(OutputType.FILE) tap { elementshot =>
          addAttachment(name, elementshot)
        }
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
  def executeJS(javascript: String, params: Any*)(implicit takeScreenShot: Boolean = false): Any = {
    withWebDriver { webDriver =>
      try {
        webDriver.asInstanceOf[JavascriptExecutor].executeScript(javascript, params.map(_.asInstanceOf[AnyRef])*) tap { result =>
          if (takeScreenShot && WebSettings.`gwen.web.capture.screenshots.enabled`) {
            captureScreenshot(false)
          }
          logger.debug(s"Evaluated javascript: $javascript, result='$result'")
          if (result.isInstanceOf[Boolean] && result.asInstanceOf[Boolean]) {
            Thread.sleep(150) // observed volatile results for booleans without wait
          }
        }
      } catch {
        case e: Throwable => javaScriptError(javascript, e)
      }
    } getOrElse {
      if (options.dryRun) s"$$[${BindingType.javascript}:$javascript]"
      else null  //js returned null
    }
  }

  /**
    * Waits for a given condition to be true. Errors on time out
    * after "gwen.web.wait.seconds" (default is 10 seconds)
    *
    * @param reason a description of what is being waited on
    * @param condition the boolean condition to wait for (until true)
    */
  def waitUntil(reason: String)(condition: => Boolean): Unit = {
    waitUntil(WebSettings.`gwen.web.wait.seconds`, reason) { condition }
  }

  /**
    * Waits until a given condition is ready for an optional number of seconds.
    * Errors on given timeout out seconds.
    *
    * @param timeoutSecs optional number of seconds to wait before timing out
    * @param reason a description of what is being waited on
    * @param condition the boolean condition to wait for (until true)
    */
  def waitUntil(timeoutSecs: Option[Long], reason: String)(condition: => Boolean): Unit = {
    timeoutSecs match {
      case (Some(secs)) => waitUntil(secs, reason) { condition }
      case _ => waitUntil(reason) { condition }
    }
  }

  /**
    * Waits until a given condition is ready for a given number of seconds.
    * Errors on given timeout out seconds.
    *
    * @param timeoutSecs the number of seconds to wait before timing out
    * @param reason a description of what is being waited on
    * @param condition the boolean condition to wait for (until true)
    */
  override def waitUntil(timeoutSecs: Long, reason: String)(condition: => Boolean): Unit = {
    waitUntil(None, Some(timeoutSecs), reason)(condition)
  }

  /**
    * Waits until a given condition is ready for a given number of seconds using a given polling delay.
    * Errors on given timeout out seconds.
    *
    * @param delayMsecs the polling delay (milliseconds)
    * @param timeoutSecs the number of seconds to wait before timing out
    * @param reason a description of what is being waited on
    * @param condition the boolean condition to wait for (until true)
    */
  def waitUntil(delayMsecs: Option[Long], timeoutSecs: Option[Long], reason: String)(condition: => Boolean): Unit = {
    val timeout = timeoutSecs.getOrElse(WebSettings.`gwen.web.wait.seconds`)
    try {
      withWebDriver { webDriver =>
        delayMsecs match {
          case Some(delay) =>
            new FluentWait(webDriver)
              .withTimeout(java.time.Duration.ofSeconds(timeout))
              .pollingEvery(java.time.Duration.ofMillis(delay))
              .until { driver => condition }
          case _ =>
            new FluentWait(webDriver)
              .withTimeout(java.time.Duration.ofSeconds(timeout))
              .until { driver => condition }
        }
      }
    } catch {
      case e: TimeoutException =>
        waitTimeoutError(timeout, reason, e)
    }
  }

  def waitUntil[T](reason: String, condition: ExpectedCondition[T]): Unit = {
    val timeout = WebSettings.`gwen.web.wait.seconds`
    try {
      withWebDriver { webDriver =>
        new FluentWait(webDriver)
          .withTimeout(java.time.Duration.ofSeconds(timeout))
          .until(condition)
      }
    } catch {
      case e: TimeoutException =>
        waitTimeoutError(timeout, reason, e)
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
  def highlightElement(element: WebElement): Unit = {
    perform {
      val msecs = WebSettings`gwen.web.throttle.msecs`; // need semi-colon (compiler bug?)
      if (msecs > 0) {
        val style = WebSettings.`gwen.web.highlight.style`
        val origStyle = executeJS(s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } original_style = element.getAttribute('style'); element.setAttribute('style', original_style + '; $style'); return original_style;", element)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
        try {
          if (!WebSettings.`gwen.web.capture.screenshots.highlighting` || !WebSettings.`gwen.web.capture.screenshots.enabled`) {
            Thread.sleep(msecs)
          }
        } finally {
          executeJS(s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } element.setAttribute('style', '$origStyle');", element)(false)
        }
      }
    }
  }

  /**
    * Checks the current state of an element.
    *
    * @param binding the locator binding of the element
    * @param state the state to check
    * @param negate whether or not to negate the check
    * @param message optional assertion error message
    */
  def checkElementState(binding: LocatorBinding, state: ElementState, negate: Boolean, message: Option[String]): Unit = {
    perform {
      val result = isElementState(binding, state, negate)
      Errors.assertWithError(result, message, s"${binding.displayName} should${if(negate) " not" else ""} be $state")
    }
  }

  /**
    * Checks the current state of an element.
    *
    * @param binding the locator binding of the element
    * @param state the state to check
    * @param negate whether or not to negate the check
    */
  def isElementState(binding: LocatorBinding, state: ElementState, negate: Boolean): Boolean = {
    var result = false
    perform {
      // use fast binding selector if checking for element absence so we don't have to wait for timeout
      val parsedBinding = state match {
        case ElementState.displayed if negate =>
          binding.withFastTimeout
        case ElementState.hidden if !negate =>
          binding.withFastTimeout
        case _ =>
          binding
      }
      try {  
        withWebElement(parsedBinding, s"waiting for ${binding.displayName} to${if (negate) " not" else ""} be $state") { webElement =>
          result = state match {
            case ElementState.displayed => 
              if (!negate) 
                isDisplayed(webElement)
              else 
                !isDisplayed(webElement)
            case ElementState.hidden =>
              if (!negate) !isDisplayed(webElement)
              else isDisplayed(webElement)
            case ElementState.checked =>
              if (!negate) webElement.isSelected
              else !webElement.isSelected
            case ElementState.ticked =>
              if (!negate) webElement.isSelected
              else !webElement.isSelected
            case ElementState.unchecked =>
              if (!negate) !webElement.isSelected
              else webElement.isSelected
            case ElementState.unticked =>
              if (!negate) !webElement.isSelected
              else webElement.isSelected
            case ElementState.enabled =>
              if (!negate) webElement.isEnabled
              else!webElement.isEnabled
            case ElementState.disabled =>
              if (!negate) !webElement.isEnabled
              else webElement.isEnabled
          }
        }
      } catch {
        case e @ (_ :  NoSuchElementException | _ : NotFoundOrInteractableException | _ : WaitTimeoutException) =>
          if (state == ElementState.displayed) result = negate
          else if (state == ElementState.hidden) result = !negate
          else throw e
      }
    }
    result
  }

  /**
    * Waits the state of an element.
    *
    * @param binding the locator binding of the element
    * @param state the state to wait for
    * @param negate whether or not to negate the check
    */
  def waitForElementState(binding: LocatorBinding, state: ElementState, negate: Boolean): Unit =
    waitUntil(s"waiting for ${binding.displayName} to${if (negate) " not" else""} be $state") {
      isElementState(binding, state, negate)
    }

  /** Gets the title of the current page in the browser.*/
  def getTitle: String =
    withWebDriver { driver =>
      driver.getTitle tap { title =>
        bindAndWait("page", "title", title)
      }
    }.getOrElse("$[dryRun:title]")

  /**
    * Sends a value to a web element.
    *
    * @param binding the locator binding
    * @param value the value to send
    * @param clickFirst true to click field first (if element is a text field)
    * @param clearFirst true to clear field first (if element is a text field)
    * @param sendEnterKey true to send the Enter key after sending the value
    */
  def sendValue(binding: LocatorBinding, value: String, clearFirst: Boolean, clickFirst: Boolean, sendEnterKey: Boolean): Unit = {
    val element = binding.name
    withDriverAndElement(binding, s"trying to send value to $element") { (driver, webElement) =>
      createActions(driver)
      if (clickFirst) {
        click(webElement)
      }
      if (clearFirst) {
        webElement.clear()
      }
      SensitiveData.withValue(value) { plainValue =>
        if ("file" == webElement.getAttribute("type")) {
          createActions(driver).moveToElement(webElement).perform()
          webElement.sendKeys(plainValue)
        } else {
        createActions(driver).moveToElement(webElement).sendKeys(plainValue).perform()
        }
      }
      bindAndWait(element, ElementAction.`type`.toString, value)
      if (sendEnterKey) {
        createActions(driver).sendKeys(webElement, Keys.RETURN).perform()
        bindAndWait(element, ElementAction.enter.toString, "true")
      }
    }
  }

  private [eval] def createSelect(webElement: WebElement): Select = new Select(webElement)

  /**
    * Selects a value in a dropdown (select control) by visible text.
    *
    * @param binding the locator binding
    * @param value the value to select
    */
  def selectByVisibleText(binding: LocatorBinding, value: String): Unit = {
    withWebElement(binding, s"trying to select option in ${binding.displayName} by visible text") { webElement =>
      logger.debug(s"Selecting '$value' in $binding by text")
      createSelect(webElement).selectByVisibleText(value)
      bindAndWait(binding.name, ElementAction.select.toString, value)
    }
  }

  /**
    * Selects a value in a dropdown (select control) by value.
    *
    * @param binding the locator binding
    * @param value the value to select
    */
  def selectByValue(binding: LocatorBinding, value: String): Unit = {
    withWebElement(binding, s"trying to select option in ${binding.displayName} by value") { webElement =>
      logger.debug(s"Selecting '$value' in $binding by value")
      createSelect(webElement).selectByValue(value)
      bindAndWait(binding.name, ElementAction.select.toString, value)
    }
  }

  /**
    * Selects a value in a dropdown (select control) by index.
    *
    * @param binding the locator binding
    * @param index the index to select (first index is 0)
    */
  def selectByIndex(binding: LocatorBinding, index: Int): Unit = {
    withWebElement(binding, s"trying to select option in ${binding.displayName} at index $index") { webElement =>
      logger.debug(s"Selecting option in $binding by index: $index")
      val select = createSelect(webElement)
      select.selectByIndex(index)
      bindAndWait(binding.name, ElementAction.select.toString, select.getOptions.get(index).getText)
    }
  }

  /**
    * Deselects a value in a dropdown (select control) by visible text.
    *
    * @param binding the locator binding
    * @param value the value to select
    */
  def deselectByVisibleText(binding: LocatorBinding, value: String): Unit = {
    withWebElement(binding, s"trying to deselect option in ${binding.displayName} by visible text") { webElement =>
      logger.debug(s"Deselecting '$value' in $binding by text")
      createSelect(webElement).deselectByVisibleText(value)
      bindAndWait(binding.name, ElementAction.deselect.toString, value)
    }
  }

  /**
    * Deselects a value in a dropdown (select control) by value.
    *
    * @param binding the locator binding
    * @param value the value to select
    */
  def deselectByValue(binding: LocatorBinding, value: String): Unit = {
    withWebElement(binding, s"trying to deselect option in ${binding.displayName} by value") { webElement =>
      logger.debug(s"Deselecting '$value' in $binding by value")
      createSelect(webElement).deselectByValue(value)
      bindAndWait(binding.name, ElementAction.deselect.toString, value)
    }
  }

  /**
    * Deselects a value in a dropdown (select control) by index.
    *
    * @param binding the locator binding
    * @param index the index to select (first index is 0)
    */
  def deselectByIndex(binding: LocatorBinding, index: Int): Unit = {
    withWebElement(binding, s"trying to deselect option in ${binding.displayName} at index $index") { webElement =>
      logger.debug(s"Deselecting option in $binding by index: $index")
      val select = createSelect(webElement)
      select.deselectByIndex(index)
      bindAndWait(binding.name, ElementAction.deselect.toString, select.getOptions.get(index).getText)
    }
  }

  private [web] def createActions(driver: WebDriver): Actions = new Actions(driver)

  def performAction(action: ElementAction, binding: LocatorBinding): Unit = {
    val actionBinding = scopes.getOpt(JSBinding.key(s"${binding.name}/action/$action"))
    actionBinding match {
      case Some(javascript) =>
        performScriptAction(action, javascript, binding, s"trying to $action ${binding.displayName}")
      case None =>
        withDriverAndElement(binding, s"trying to $action ${binding.displayName}") { (driver, webElement) =>
          if (action != ElementAction.`move to`) {
            moveToAndCapture(driver, webElement)
          }
          action match {
            case ElementAction.click =>
              click(webElement)
            case ElementAction.`right click` =>
              createActions(driver).contextClick(webElement).perform()
            case ElementAction.`double click` =>
              createActions(driver).doubleClick(webElement).perform()
            case ElementAction.`move to` =>
              moveToAndCapture(driver, webElement)
            case ElementAction.submit => 
              webElement.submit()
            case ElementAction.check | ElementAction.tick =>
              clickCheckbox(webElement, None, true)
            case ElementAction.uncheck | ElementAction.untick =>
              clickCheckbox(webElement, None, false)
            case ElementAction.clear =>
              webElement.clear()
            case _ => WebErrors.invalidActionError(action)
          }
        }
        bindAndWait(binding.name, action.toString, "true")
    }
  }

  private def clickCheckbox(webElement: WebElement, contextElement: Option[WebElement], selected: Boolean): Unit = {
    contextElement match {
      case None =>
        if (webElement.isSelected != selected) {
          Try(webElement.click()) match {
            case Failure(e) =>
              jsClick(webElement)
              if (webElement.isSelected != selected) webElement.sendKeys(Keys.SPACE)
              if (webElement.isSelected != selected) throw e
            case _ =>
              if (webElement.isSelected != selected) jsClick(webElement)
              if (webElement.isSelected != selected) webElement.sendKeys(Keys.SPACE)
          }
        }
      case Some(ctxElement) =>
        if (webElement.isSelected != selected) perform(webElement, ctxElement) { _.click() }
        if (webElement.isSelected != selected) jsClick(webElement)
        if (webElement.isSelected != selected) perform(webElement, ctxElement) { _.sendKeys(Keys.SPACE) }
    }
  }

  private def click(webElement: WebElement): Unit = {
    Try(webElement.click()) match {
      case Failure(e) =>
        Try(jsClick(webElement)) match {
          case Failure(e2) => throw e
          case _ =>
        }
      case _ =>
    }
  }

  private def jsClick(webElement: WebElement): Unit = {
    executeJS("(function(element){element.click();})(arguments[0]);", webElement)
  }

  def moveToAndCapture(driver: WebDriver, webElement: WebElement): Unit = {
    createActions(driver).moveToElement(webElement).perform()
    if (WebSettings.`gwen.web.capture.screenshots.enabled`) {
      captureScreenshot(false)
    }
  }

  def dragAndDrop(sourceBinding: LocatorBinding, targetBinding: LocatorBinding): Unit = {
    withWebDriver { driver =>
      withWebElement(sourceBinding, s"trying to drag $sourceBinding to $targetBinding") { source =>
        withWebElement(targetBinding, s"trying to drag $sourceBinding to $targetBinding") { target =>
          createActions(driver).clickAndHold(source)
            .moveToElement(target)
            .release(target)
            .build().perform()
        }
      }
    }
  }

  def holdAndClick(modifierKeys: Array[String], clickAction: ElementAction, binding: LocatorBinding): Unit = {
    val keys = modifierKeys.map(_.trim).map(key => Try(Keys.valueOf(key.toUpperCase)).getOrElse(unsupportedModifierKeyError(key)))
    withDriverAndElement(binding, s"trying to $clickAction ${binding.displayName}") { (driver, webElement) =>
      moveToAndCapture(driver, webElement)
      var actions = createActions(driver)
      keys.foreach { key => actions = actions.keyDown(key) }
      actions = clickAction match {
        case ElementAction.click => actions.click(webElement)
        case ElementAction.`right click` => actions.contextClick(webElement)
        case ElementAction.`double click` => actions.doubleClick(webElement)
        case _ => WebErrors.invalidClickActionError(clickAction)
      }
      keys.reverse.foreach { key => actions = actions.keyUp(key) }
      actions.build().perform()
    }
    bindAndWait(binding.name, clickAction.toString, "true")
  }

  def sendKeys(keysToSend: Array[String]): Unit = {
    sendKeys(None, keysToSend)
  }

  def sendKeys(binding: LocatorBinding, keysToSend: Array[String]): Unit = {
    sendKeys(Some(binding), keysToSend)
  }

  def sendKeys(elementBindingOpt: Option[LocatorBinding], keysToSend: Array[String]): Unit = {
    val keys =  keysToSend.map(_.trim).map(key => Try(Keys.valueOf(key.toUpperCase)).getOrElse(key))
    elementBindingOpt match {
      case Some(binding) =>
        withDriverAndElement(binding, s"trying to send key(s) to ${binding.displayName}") { (driver, webElement) =>
          if (keys.size > 1) {
            webElement.sendKeys(Keys.chord(keys: _*))
          } else {
            var actions = createActions(driver).moveToElement(webElement)
            keys.foreach { key => actions = actions.sendKeys(webElement, key) }
            actions.build().perform()
          }
        }
      case None =>
        withWebDriver { driver =>
          var actions = createActions(driver)
          if (keys.size > 1) {
            actions = actions.sendKeys(Keys.chord(keys: _*))
          } else {
            keys.foreach { key => actions = actions.sendKeys(key) }
          }
          actions.build().perform()
        }
    }
  }

  private def withDriverAndElement(binding: LocatorBinding, reason: String)(doActions: (WebDriver, WebElement) => Unit): Unit = {
    withWebDriver { driver =>
      withWebElement(binding, reason) { webElement =>
        if (WebSettings.`gwen.web.implicit.element.focus`) {
          executeJS("(function(element){element.focus();})(arguments[0]);", webElement)
        }
        doActions(driver, webElement)
      }
    }
  }

  private def performScriptAction(action: ElementAction, javascript: String, binding: LocatorBinding, reason: String): Unit = {
    withDriverAndElement(binding, reason) { (driver, webElement) =>
      if (action != ElementAction.`move to`) {
        moveToAndCapture(driver, webElement)
      }
      executeJS(s"(function(element) { $javascript })(arguments[0])", webElement)
      bindAndWait(binding.name, action.toString, "true")
    }
  }

  /**
    * Performs and action on a web element in the context of another element.
    *
    * @param action description of the action
    * @param element the name of the element to perform the action on
    * @param context the name of the context element binding to find the element in
    */
  def performActionInContext(action: ElementAction, element: String, context: String): Unit = {
    try {
        val contextBinding = getLocatorBinding(context)
        val binding = getLocatorBinding(element)
        performActionIn(action, binding, contextBinding)
      } catch {
        case e1: LocatorBindingException =>
          try {
            val binding = getLocatorBinding(s"$element of $context")
            performAction(action, binding)
          } catch {
            case e2: LocatorBindingException =>
              throw new LocatorBindingException(s"${e1.getMessage}. ${e2.getMessage}.")
          }
      }
  }

  private def performActionIn(action: ElementAction, binding: LocatorBinding, contextBinding: LocatorBinding): Unit = {
    val reason = s"trying to $action ${binding.displayName}"
    withWebElement(contextBinding, reason) { contextElement =>
      withWebElement(binding, reason) { webElement =>
        action match {
          case ElementAction.click => perform(webElement, contextElement) { _.click() }
          case ElementAction.`right click` => perform(webElement, contextElement) { _.contextClick() }
          case ElementAction.`double click` => perform(webElement, contextElement) { _.doubleClick() }
          case ElementAction.check | ElementAction.tick =>
            clickCheckbox(webElement, Some(contextElement), true)
          case ElementAction.uncheck | ElementAction.untick =>
            clickCheckbox(webElement, Some(contextElement), false)
          case ElementAction.`move to` => perform(webElement, contextElement) { action => action }
          case _ => WebErrors.invalidContextActionError(action)
        }
        bindAndWait(binding.name, action.toString, "true")
      }
    }
  }

  private def perform(webElement: WebElement, contextElement: WebElement)(buildAction: Actions => Actions): Unit = {
    withWebDriver { driver =>
      val moveTo = createActions(driver).moveToElement(contextElement).moveToElement(webElement)
      buildAction(moveTo).build().perform()
      if (WebSettings.`gwen.web.capture.screenshots.enabled`) {
        captureScreenshot(false)
      }
    }
  }

  /**
    * Waits for text to appear in the given web element.
    *
    * @param binding the locator binding
    */
  def waitForText(binding: LocatorBinding): Boolean =
    getElementText(binding).map(_.length()).getOrElse {
      scopes.set(TextBinding.key(binding.name), BindingType.text.toString)
      0
    } > 0

  /**
   * Scrolls an element into view.
   *
   * @param binding the locator binding
   * @param scrollTo scroll element into view, options are: top or bottom
   */
  def scrollIntoView(binding: LocatorBinding, scrollTo: ScrollTo): Unit = {
    withWebElement(binding, s"trying to scroll to $scrollTo of ${binding.displayName}") { scrollIntoView(_, scrollTo) }
  }

  /**
   * Scrolls the given web element into view.
   *
   * @param webElement the web element to scroll to
   * @param scrollTo scroll element into view, options are: top or bottom
   * @param offset offset to scroll by (default is zero)
   */
  def scrollIntoView(webElement: WebElement, scrollTo: ScrollTo, offset: Int = 0): Unit = {
    executeJS(s"var elem = arguments[0]; if (typeof elem !== 'undefined' && elem != null) { elem.scrollIntoView(${scrollTo == ScrollTo.top});${if (offset != 0) s" window.scroll(0, window.scrollY + $offset);" else ""}}", webElement)
  }

  /**
   * Scrolls to the top of bottom of the current page.
   *
   * @param scrollTo top or bottom
   */
  def scrollPage(scrollTo: ScrollTo): Unit = {
    scrollTo.match {
      case ScrollTo.top => executeJS(s"window.scrollTo(0, 0)")
      case _ => executeJS(s"window.scrollTo(0, document.body.scrollHeight || document.documentElement.scrollHeight);")
    }
  }

  /**
    * Resizes the browser window to the given dimensions.
    *
    * @param width the width
    * @param height the height
    */
  def resizeWindow(width: Int, height: Int): Unit = {
    withWebDriver { driver =>
      logger.info(s"Resizing browser window to width $width and height $height")
      driver.manage().window().setSize(new Dimension(width, height))
    }
  }

  /**
    * Maximizes the browser window.
    */
  def maximizeWindow(): Unit = {
    withWebDriver { driver =>
      logger.info("Maximising browser window")
      driver.manage().window().maximize()
    }
  }

  def captureCurrentUrl: String = {
    withWebDriver { driver =>
      driver.getCurrentUrl
    } getOrElse {
      "$[dryRun:currentUrl]"
    }
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
    *
    * @param binding the locator binding
    */
  def getElementText(binding: LocatorBinding): Option[String] =
    withWebElement(binding, s"trying to get ${binding.displayName} text") { webElement =>
      (Option(webElement.getText) match {
        case None | Some("") =>
          Option(webElement.getAttribute("text")) match {
            case None | Some("") =>
              Option(webElement.getAttribute("value")) match {
                case None | Some("") =>
                  val value = executeJS("return (function(element){return element.innerText || element.textContent || ''})(arguments[0]);", webElement).asInstanceOf[String]
                  if (value != null) value else ""
                case Some(value) => value
              }
            case Some(value) => value
          }
        case Some(value) => value
      }) tap { text =>
        bindAndWait(binding.name, BindingType.text.toString, text)
      }
    } tap { value =>
      logger.debug(s"getElementText($binding)='$value'")
    }

  /**
    * Gets the selected text of a dropdown web element on the current page.
    * If a value is found, its value is bound to the current page
    * scope as `name/selectedText`.
    *
    * @param name the web element name
    */
  private def getSelectedElementText(name: String): Option[String] = {
    val binding = getLocatorBinding(name)
    withWebElement(binding, s"trying to get selected text of ${binding.displayName}") { webElement =>
      (getElementSelectionByJS(webElement, DropdownSelection.text) match {
        case None =>
          Try(createSelect(webElement)) map { select =>
            Option(select.getAllSelectedOptions.asScala.map(_.getText()).mkString(",")) match {
              case None | Some("") =>
                select.getAllSelectedOptions.asScala.map(_.getAttribute("text")).mkString(",")
              case Some(value) => value
            }
          } getOrElse null
        case Some(value) => value
      }) tap { text =>
        bindAndWait(binding.name, "selectedText", text)
      }
    } tap { value =>
      logger.debug(s"getSelectedElementText($binding)='$value'")
    }
  }

   /**
    * Gets the selected value of a dropdown web element on the current page.
    * If a value is found, its value is bound to the current page
    * scope as `name/selectedValue`.
    *
    * @param name the web element name
    */
  private def getSelectedElementValue(name: String): Option[String] = {
    val binding = getLocatorBinding(name)
    withWebElement(binding, s"trying to get selected value of ${binding.displayName}") { webElement =>
      getElementSelectionByJS(webElement, DropdownSelection.value) match {
        case None =>
          Try(createSelect(webElement)) map { select =>
            select.getAllSelectedOptions.asScala.map(_.getAttribute("value")).mkString(",") tap { value =>
              bindAndWait(binding.name, "selectedValue", value)
            }
          } getOrElse null
        case Some(value) => value
      }
    } tap { value =>
      logger.debug(s"getSelectedElementValue($binding)='$value'")
    }
  }

  private def getElementSelectionByJS(webElement: WebElement, by: DropdownSelection): Option[String] = {
    Option(executeJS(s"""return (function(select){try{var byText=${by == DropdownSelection.text};var result='';var options=select && select.options;if(!!options){var opt;for(var i=0,iLen=options.length;i<iLen;i++){opt=options[i];if(opt.selected){if(result.length>0){result=result+',';}if(byText){result=result+opt.text;}else{result=result+opt.value;}}}return result;}else{return null;}}catch(e){return null;}})(arguments[0])""", webElement).asInstanceOf[String])
  }

  /**
   * Gets an element's selected value(s).
   *
   * @param name the name of the element
   * @param selection `text` to get selected option text, `value` to get
   *        selected option value
   * @return the selected value or a comma seprated string containing all
   * the selected values if multiple values are selected.
   */
  def getElementSelection(name: String, selection: DropdownSelection): Option[String] = {
    if (selection == DropdownSelection.text) {
      getSelectedElementText(name)
    } else {
      getSelectedElementValue(name)
    }
  }

  /**
    * Switches the web driver session
    *
    * @param session the name of the session to switch to
    */
  def switchToSession(session: String): Unit = {
    perform {
      driverManager.switchToSession(session)
    }
  }

  /**
    * Starts and switches to a new tab or window.
    *
    * @param winType tab or window
    */
  def switchToNewWindow(winType: WindowType): Unit = {
    perform {
      driverManager.switchToNewWindow(winType)
    }
  }

  /**
    * Starts a new session if there isn't one or stays in the current one.
    */
  def newOrCurrentSession(): Unit = {
    perform {
      driverManager.newOrCurrentSession()
    }
  }

  /** Gets the number of open sesions. */
  def noOfSessions(): Int = driverManager.noOfSessions()

  /** Gets the number of open windows. */
  def noOfWindows(): Int = driverManager.noOfWindows()

  /**
    * Switches to the first child window if one was just opened.
    */
  def switchToChild(): Unit = {
    switchToWindow(1)
  }

  /**
    * Switches to a tab or window occurrence.
    *
    * @param occurrence the tag or window occurrence to switch to (primary is 0, first opened is occurrence 1, 2nd is 2, ..)
    */
  def switchToWindow(occurrence: Int): Unit = {
    waitUntil(s"trying to switch to tab/window occurrence $occurrence") {
      driverManager.windows().lift(occurrence).nonEmpty
    }
    driverManager.switchToWindow(occurrence)
  }

  /**
    * Closes the last child window.
    */
  def closeChild(): Unit = {
    perform {
      driverManager.closeChild()
    }
  }

  /**
    * Closes the tab or window occurrence.
    *
    * @param occurrence the tag or window occurrence to close (primary is 0, first opened is occurrence 1, 2nd is 2, ..)
    */
  def closeWindow(occurrence: Int): Unit = {
    perform {
      driverManager.closeWindow(occurrence)
    }
  }

  /** Switches to the parent window. */
  def switchToParent(): Unit = {
    perform {
      driverManager.switchToParent()
    }
  }

  /** Switches to the top window / first frame */
  def switchToDefaultContent(): Unit = {
    perform {
      driverManager.switchToDefaultContent()
    }
  }

  /** Refreshes the current page.*/
  def refreshPage(): Unit = {
    withWebDriver { driver =>
      driver.navigate().refresh()
    }
  }

  /**
    * Handles an alert (pop-up)
    *
    * @param accept true to accept; false to dismiss
    */
  def handleAlert(accept: Boolean): Unit = {
    withWebDriver { driver =>
      waitUntil("waiting for alert popup", ExpectedConditions.alertIsPresent())
      if (accept) {
        driver.switchTo().alert().accept()
      } else {
        driver.switchTo().alert().dismiss()
      }
    }
  }

  /**
    * Navigates the browser the given URL
    *
    * @param url the URL to navigate to
    */
  def navigateTo(url: String): Unit = {
    withWebDriver { driver =>
      SensitiveData.withValue(url) { u =>
        driver.get(u)
      }
    } (WebSettings.`gwen.web.capture.screenshots.enabled`)
  }

  /**
    * Gets the message of an alert of confirmation popup.
    *
    * @return the alert or confirmation message
    */
  def getPopupMessage: String = {
    withWebDriver { driver =>
      waitUntil("waiting for alert popup", ExpectedConditions.alertIsPresent())
      driver.switchTo().alert().getText
    } getOrElse "$[dryRun:popupMessage]"
  }

  /** Checks if an element is displayed. */
  def isDisplayed(webElement: WebElement): Boolean = {
    if (!isInViewport(webElement)) {
      Try(scrollIntoView(webElement, ScrollTo.top, -100))
      isDisplayedAndInViewport(webElement)
    } else {
      webElement.isDisplayed
    }
  }

  private def isDisplayedAndInViewport(webElement: WebElement): Boolean = {
    webElement.isDisplayed && isInViewport(webElement)
  }

  /** Checks if an element is not in the view port. */
  private def isInViewport(webElement: WebElement): Boolean = {
    executeJS("return (function(elem){var b=elem.getBoundingClientRect(); return b.top>=0 && b.left>=0 && b.bottom<=(window.innerHeight || document.documentElement.clientHeight) && b.right<=(window.innerWidth || document.documentElement.clientWidth);})(arguments[0])", webElement).asInstanceOf[Boolean]
  }

}
