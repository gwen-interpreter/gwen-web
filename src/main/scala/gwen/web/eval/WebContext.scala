/*
 * Copyright 2015-2025 Brady Wood, Branko Juric
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
import gwen.core.eval.binding.DryValueBinding
import gwen.core.eval.binding.JSBinding
import gwen.core.eval.support.BooleanCondition
import gwen.core.node.gherkin.Step
import gwen.core.state.StateLevel
import gwen.core.state.EnvState
import gwen.core.state.SensitiveData
import gwen.core.status.Failed

import scala.concurrent.duration._
import scala.collection.SeqView
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
class WebContext(options: GwenOptions, envState: EnvState, driverManager: DriverManager) extends EvalContext(options, envState) with LazyLogging with WebSessionEventListener with ImplicitWebValueKeys {

  Try(logger.info(s"GWEN_CLASSPATH = ${sys.env("GWEN_CLASSPATH")}"))
  Try(logger.info(s"SELENIUM_HOME = ${sys.env("SELENIUM_HOME")}"))

  private val locatorBindingResolver = new LocatorBindingResolver(this)
  private var lastScreenshotSize: Option[Long] = None

  driverManager.addWebSessionEventListener(this)

  override def sessionOpened(event: WebSessionEvent): Unit = { 
    driverManager.getSessionId(event.driver) foreach { sessionId =>
      Grid.impl.foreach { grid =>
        if (grid.videoEnabled) {
          addVideo(grid.videoFile(sessionId), sessionId)
        }
      }
      envState.topScope.set(`gwen.web.sessionId`, sessionId)
    }
  }

  override def sessionClosing(event: WebSessionEvent): Unit = { 
    envState.topScope.set(`gwen.web.sessionId`, null)
  }

  def webElementlocator = new WebElementLocator(this)

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
    * Executes a javascript function on the current page through the web driver.
    *
    * @param javascript the script function to execute
    * @param params optional parameters to the web driver
    */
  override def evaluateJS(javascript: String, params: List[Any]): Any = {
    evaluateJSWeb(javascript, params)
  }

  /**
    * Injects and executes a javascript function on the current page through web driver.
    *
    * @param javascript the function to apply
    * @param takeScreenShot true to take screenshot after execting the function
    */
  def executeJS(javascript: String)(implicit takeScreenShot: Boolean = false): Any = {
    evaluateJSWeb(javascript, Nil)
  }

  /**
    * Injects and applies a javascript function to a web element on the current page through web driver.
    *
    * @param javascript the function to apply
    * @param webElement the web element to apply the function to
    * @param takeScreenShot true to take screenshot after execting the function
    */
  def applyJS(javascript: String, webElement: WebElement)(implicit takeScreenShot: Boolean = false): Any = {
    evaluateJSWeb(javascript, List(webElement))
  }

  /**
    * Injects and executes a javascript function on the current page through web driver.
    *
    * @param javascript the function to execute
    * @param params optional objects to apply the function to
    * @param takeScreenShot true to take screenshot after execting the function
    */
  private def evaluateJSWeb(javascript: String, params: List[Any])(implicit takeScreenShot: Boolean = false): Any = {
    withWebDriver { webDriver =>
      try {
        SensitiveData.withValue(javascript) { js =>
          webDriver.asInstanceOf[JavascriptExecutor].executeScript(formatJSReturn(parseJS(js)), params.map(_.asInstanceOf[AnyRef])*) tap { result =>
            if (takeScreenShot && WebSettings.`gwen.web.capture.screenshots.enabled`) {
              captureScreenshot(false)
            }
            logger.debug(s"Evaluated javascript: $javascript, result='$result'")
            if (result.isInstanceOf[Boolean] && result.asInstanceOf[Boolean]) {
              Thread.sleep(150) // observed volatile results for booleans without wait
            }
          }
        }
      } catch {
        case e: GwenException => throw e
        case e: Throwable => functionError(javascript, e)
      }
    } getOrElse {
      if (options.dryRun) s"$$[${BindingType.javascript}:$javascript]"
      else null  //js returned null
    }
  }

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
  override def getBoundValue(name: String): String = getBoundValue(name.trim, None)
  def getBoundValue(name: String, timeout: Option[Duration]): String = {
    if (name == "the current URL") {
      val url = captureCurrentUrl
      topScope.set(name, url)
    }
    val locatorEntry = topScope.namedEntry(name) { _ => true } map { (n, _) => n.startsWith(LocatorKey.baseKey(name)) }
    if (locatorEntry.exists(_ == true) || locatorEntry.isEmpty) {
      getLocatorBinding(name, optional = true).map(_.withTimeout(timeout)) match {
        case Some(binding) =>
          evaluate(new DryValueBinding(binding.name, "webElementText", this).resolve()) {
            Try(getElementText(binding)) match {
              case Success(text) => 
                text.getOrElse(getWebBoundValue(name))
              case Failure(e) => throw e
            }
          }
        case _ => getWebBoundValue(name)
      }
    } else {
      getWebBoundValue(name)
    }

  }

  /**
    * Resolves a bound attribute value from the visible scope.
    *
    * @param name the name of the bound attribute to find
    */
  def getWebBoundValue(name: String): String = {
    getCachedWebElement(s"${JSBinding.key(name)}/param/webElement") map { webElement =>
      val javascript = interpolate(topScope.get(JSBinding.key(name)))
      val jsFunction = jsFunctionWrapper("element", "arguments[0]", s"return $javascript")
      Option(applyJS(jsFunction, webElement)).map(_.toString).getOrElse("")
    } getOrElse {
      try {
        super.getBoundValue(name)
      } catch {
        case e: UnboundAttributeException =>
          ElementFunctionBinding.find(name, this) match {
            case Success(binding) => binding.resolve()
            case _ => throw e
          }
      }
    }
  }

  def boundAttributeOrSelection(element: String, selection: Option[DropdownSelection]): String = boundAttributeOrSelection(element, selection, None)
  def boundAttributeOrSelection(element: String, selection: Option[DropdownSelection], timeout: Option[Duration]): String = {
    selection match {
      case None => getBoundValue(element, timeout)
      case Some(sel) =>
        try {
          getBoundValue(s"$element $sel", timeout)
        } catch {
          case _: UnboundAttributeException =>
            getElementSelection(element, sel).getOrElse(getBoundValue(element, timeout))
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
    */
  def getLocatorBindingOpt(name: String): Option[LocatorBinding] = {
    locatorBindingResolver.getBinding(name, optional = true)
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
    * Checks whether or not a name is bound to a JS expresison or web element binding
    *
    * @param name the name to check
    * @return true if is JS or web elmeent binding, false otherwise
    */
  def isWebBinding(name: String): Boolean = {
    Try(getBinding(name).isInstanceOf[JSBinding[?]]).getOrElse(false) || getLocatorBindingOpt(name).nonEmpty
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
    * Gets the actual value of an attribute and compares it with an expected value or condition.
    *
    * @param name the name of the attribute being compared
    * @param expected the expected value, regex, xpath, or json path
    * @param actual the actual value of the element
    * @param operator the comparison operator
    * @param negate true to negate the result
    * @param nameSuffix optional name suffix
    * @param timeoutSecs the number of seconds to wait before timing out
    * @param mode the assertion mode
    * @return true if the actual value matches the expected value
    */
  def compare(name: String, expected: String, actual: () => String, operator: ComparisonOperator, negate: Boolean, nameSuffix: Option[String], timeoutSecs: Option[Long], mode: AssertionMode): Unit = {
    Thread.sleep(WebSettings.`gwen.web.assertions.delayMillisecs`)
    var result = false
    var error: Option[String] = None
    var actualValue = actual()
    var polled = false
    var attempts = 0
    try {
      waitUntil(timeoutSecs, s"waiting for $name to ${if(negate) "not " else ""}$operator '$expected'") {
        if (polled) {
          Thread.sleep(WebSettings.`gwen.web.assertions.delayMillisecs`)
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
        attempts = attempts + 1
        result || maxStrikesExhausted(attempts, timeoutSecs)
      }
    } catch {
      case _: WaitTimeoutException => result = false
    }
    error match {
      case Some(msg) =>
        assertWithError(assertion = false, msg, mode)
      case None =>
        if (!polled) {
          result = super.compare(name, expected, actualValue, operator, negate).getOrElse(result)
        }
        val binding = Try(
          getLocatorBinding(name.substring(0, name.length - nameSuffix.map(_.length).getOrElse(0)), optional = true) getOrElse {
            getBinding(name)
          }
        ).map(_.displayName).getOrElse(name)
        assertWithError(
          result, 
          Assert.formatFailed(binding, expected, actualValue, negate, operator),
          mode)
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
    evaluate { 
      topScope.set(`gwen.web.sessionId`, DryValueBinding.unresolved(`gwen.web.sessionId`))
      None.asInstanceOf[Option[T]] 
    } {
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
      val timeoutSecs = binding.timeoutSeconds
      val selector = binding.selectors.head
      val wHandle = selector.relative.flatMap(_ => withWebDriver(_.getWindowHandle))
      try {
        var result: Option[Try[T]] = None
        val start = System.nanoTime()
        try {
          var lapsed = 0L
          waitUntil(timeoutSecs, reason) {
            try {
              var webElement = binding.resolve()
              tryMoveTo(webElement)
              if (selector.index.isEmpty) {
                highlightElement(webElement)
              }
              val res = operation(webElement)
              result = Some(Success(res))
              true
            } catch {
              case e: FunctionException =>
                result = Some(Failure(e))
                true
              case e: Throwable =>
                lapsed = Duration.fromNanos(System.nanoTime() - start).toSeconds
                if (e.isInstanceOf[InvalidElementStateException] || e.isInstanceOf[NoSuchElementException] || e.isInstanceOf[NotFoundOrInteractableException]) {
                  if (lapsed >= timeoutSecs) {
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
            waitTimeoutError(timeoutSecs, reason, result.get.failed.get)
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
    evaluate(Option(new File(DryValueBinding.unresolved("screenshotFile")))) {
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
            addAttachmentFile(name, screenshot)
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
    evaluate(Option(new File(DryValueBinding.unresolved("elementScreenshotFile")))) {
      withWebElement(binding, s"trying to capture element screenshot of ${binding.displayName}") { webElement =>
        Thread.sleep(150) // give element time to render
        webElement.getScreenshotAs(OutputType.FILE) tap { elementshot =>
          addAttachmentFile(name, elementshot)
        }
      }
    }
  }

  /**
    * Waits for a given condition to be true. Errors on time out
    * after default wait time.
    *
    * @param reason a description of what is being waited on
    * @param condition the boolean condition to wait for (until true)
    */
  def waitUntil(reason: String)(condition: => Boolean): Unit = {
    waitUntil(defaultWait.toSeconds, reason) { condition }
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
    * @param pollMsecs the polling interval in milliseconds
    * @param timeoutSecs the number of seconds to wait before timing out
    * @param reason a description of what is being waited on
    * @param condition the boolean condition to wait for (until true)
    */
  override def waitUntil(pollMsecs: Long, timeoutSecs: Long, reason: String)(condition: => Boolean): Unit = {
    waitUntil(Some(pollMsecs), Some(timeoutSecs), reason)(condition)
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
    val timeout = timeoutSecs.getOrElse(defaultWait.toSeconds)
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
    waitUntil(None, reason, condition)
  }

  def waitUntil[T](timeoutSecs: Option[Long], reason: String, condition: ExpectedCondition[T]): Unit = {
    val timeout = timeoutSecs.getOrElse(defaultWait.toSeconds)
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
        Try(applyJS(jsFunctionWrapper("element", "arguments[0]", s"type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } original_style = element.getAttribute('style'); element.setAttribute('style', original_style + '; $style'); return original_style;"), element)(using WebSettings.`gwen.web.capture.screenshots.highlighting`)) map { origStyle =>
          try {
            if (!WebSettings.`gwen.web.capture.screenshots.highlighting` || !WebSettings.`gwen.web.capture.screenshots.enabled`) {
              Thread.sleep(msecs)
            }
          } finally {
            applyJS(jsFunctionWrapper("element", "arguments[0]", s"type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } element.setAttribute('style', '$origStyle');"), element)(using false)
          }
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
    * @param mode the assertion mode
    */
  def checkElementState(binding: LocatorBinding, state: ElementState, negate: Boolean, mode: AssertionMode): Unit = {
    perform {
        var result = false
        var attempts = 0
        try {
          waitUntil(binding.timeoutSeconds, s"waiting for ${binding.displayName} to ${if(negate) "not " else ""}be '$state'") {
            result = isElementState(binding, state, negate)
            attempts = attempts + 1
            result || maxStrikesExhausted(attempts, binding.timeoutSecondsOpt)
          }
        } catch {
          case _: WaitTimeoutException =>
            result = false  
        }
        assertWithError(result, s"${binding.displayName} should${if(negate) " not" else ""} be $state", mode)
    }
  }

  /**
    * Checks the current state of an element.
    *
    * @param binding the locator binding of the element
    * @param state the state to check
    * @param negate whether or not to negate the check
    * @param message optional assertion error message
    * @param mode the assertion mode
    */
  def checkPopupDisplayed(waitSecs: Option[Long], negate: Boolean, mode: AssertionMode): Unit = {
    val result = Try(waitForPopup(waitSecs: Option[Long])) match {
      case Success(_) => true
      case Failure(e) => 
        if (e.isInstanceOf[WaitTimeoutException]) false
        else throw e
    }
    assertWithError(result, s"Alert/confirmation popup should${if(negate) " not" else ""} be displayed", mode)
  }

  private def maxStrikesExhausted(attempt: Int, timeoutSecs: Option[Long]): Boolean = {
    timeoutSecs filter { ts => 
      ts == defaultWait.toSeconds
    } map { ts => 
      !(attempt < WebSettings.`gwen.web.assertions.maxStrikes`)
    } getOrElse false
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
      Thread.sleep(WebSettings.`gwen.web.assertions.delayMillisecs`)
      val fastBinding = binding.withFastTimeout
      state match {
        case ElementState.displayed => 
          result = if (!negate) isDisplayed(fastBinding) else !isDisplayed(fastBinding)
        case ElementState.hidden =>
          result = if (!negate) !isDisplayed(fastBinding) else isDisplayed(fastBinding)
        case _ =>
          try {  
            withWebElement(fastBinding, s"waiting for ${binding.displayName} to${if (negate) " not" else ""} be $state") { webElement =>
              result = state match {
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
                case _ => false //never
              }
            }
          } catch {
            case e @ (_ :  NoSuchElementException | _ : NotFoundOrInteractableException | _ : WaitTimeoutException) =>
              if (state == ElementState.displayed) result = negate
              else if (state == ElementState.hidden) result = !negate
              else throw e
          }
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
    waitUntil(binding.timeoutSeconds, s"waiting for ${binding.displayName} to${if (negate) " not" else""} be $state") {
      isElementState(binding, state, negate)
    }

  /** Gets the title of the current page in the browser.*/
  def getTitle: String =
    withWebDriver { driver =>
      driver.getTitle
    }.getOrElse(DryValueBinding.unresolved("title"))

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
      waitUntilEnabled(binding, webElement)
      createActions(driver)
      if (clickFirst) {
        click(webElement)
      }
      if (clearFirst) {
        webElement.clear()
      }
      SensitiveData.withValue(value) { plainValue =>
        if ("file" == webElement.getDomAttribute("type")) {
          createActions(driver).moveToElement(webElement).perform()
          webElement.sendKeys(plainValue)
        } else {
        createActions(driver).moveToElement(webElement).sendKeys(plainValue).perform()
        }
      }
      if (sendEnterKey) {
        createActions(driver).sendKeys(webElement, Keys.RETURN).perform()
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
      waitUntilEnabled(binding, webElement)
      logger.debug(s"Selecting '$value' in $binding by text")
      createSelect(webElement).selectByVisibleText(value)
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
      waitUntilEnabled(binding, webElement)
      logger.debug(s"Selecting '$value' in $binding by value")
      createSelect(webElement).selectByValue(value)
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
      waitUntilEnabled(binding, webElement)
      logger.debug(s"Selecting option in $binding by index: $index")
      val select = createSelect(webElement)
      select.selectByIndex(index)
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
      waitUntilEnabled(binding, webElement)
      logger.debug(s"Deselecting '$value' in $binding by text")
      createSelect(webElement).deselectByVisibleText(value)
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
      waitUntilEnabled(binding, webElement)
      logger.debug(s"Deselecting '$value' in $binding by value")
      createSelect(webElement).deselectByValue(value)
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
      waitUntilEnabled(binding, webElement)
      logger.debug(s"Deselecting option in $binding by index: $index")
      val select = createSelect(webElement)
      select.deselectByIndex(index)
    }
  }

  private [web] def createActions(driver: WebDriver): Actions = new Actions(driver)

  def performAction(action: ElementAction, binding: LocatorBinding): Unit = {
    val actionBinding = topScope.getOpt(JSBinding.key(s"${binding.name}/action/$action"))
    actionBinding match {
      case Some(javascript) =>
        performScriptAction(action, javascript, binding, s"trying to $action ${binding.displayName}")
      case None =>
        withDriverAndElement(binding, s"trying to $action ${binding.displayName}") { (driver, webElement) =>
          if (action != ElementAction.`move to`) {
            moveToAndCapture(driver, webElement)
            waitUntilEnabled(binding, webElement)
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
    applyJS(jsFunctionWrapper("element", "arguments[0]", "element.click()"), webElement)
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
        waitUntilEnabled(sourceBinding, source)
        withWebElement(targetBinding, s"trying to drag $sourceBinding to $targetBinding") { target =>
          waitUntilEnabled(targetBinding, target)
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
      waitUntilEnabled(binding, webElement)
      var actions = createActions(driver)
      keys.foreach { key => actions = actions.keyDown(key) }
      actions = clickAction match {
        case ElementAction.click => 
          actions.click(webElement)
        case ElementAction.`right click` => 
          actions.contextClick(webElement)
        case ElementAction.`double click` => 
          actions.doubleClick(webElement)
        case _ => WebErrors.invalidClickActionError(clickAction)
      }
      keys.reverse.foreach { key => actions = actions.keyUp(key) }
      actions.build().perform()
    }
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
          waitUntilEnabled(binding, webElement)
          if (keys.size > 1) {
            webElement.sendKeys(Keys.chord(keys*))
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
            actions = actions.sendKeys(Keys.chord(keys*))
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
          applyJS(jsFunctionWrapper("element", "arguments[0]", "element.focus()"), webElement)
        }
        doActions(driver, webElement)
      }
    }
  }

  private def performScriptAction(action: ElementAction, javascript: String, binding: LocatorBinding, reason: String): Unit = {
    withDriverAndElement(binding, reason) { (driver, webElement) =>
      if (action != ElementAction.`move to`) {
        moveToAndCapture(driver, webElement)
        waitUntilEnabled(binding, webElement)
      }
      applyJS(jsFunctionWrapper("element", "arguments[0]", javascript), webElement)
    }
  }

  def evaluateElementFunction(javascript: String, binding: LocatorBinding): Option[String] = {
    withWebElement(binding, s"trying to apply JS function to web element: ${binding.displayName}") { webElement =>
      String.valueOf(applyJS(javascript, webElement))
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

  private def waitUntilEnabled(binding: LocatorBinding, webElement: WebElement): Unit = {
    if(!webElement.isEnabled) { 
      waitUntil(binding.timeoutSecondsOpt, s"waiting for ${binding.displayName} to be enabled") {
        webElement.isEnabled
      }
    }
  }

  private def performActionIn(action: ElementAction, binding: LocatorBinding, contextBinding: LocatorBinding): Unit = {
    val reason = s"trying to $action ${binding.displayName}"
    withWebElement(contextBinding, reason) { contextElement =>
      withWebElement(binding, reason) { webElement =>
        if (action != ElementAction.`move to`) {
          waitUntilEnabled(binding, webElement)
        }
        action match {
          case ElementAction.click => 
            perform(webElement, contextElement) { _.click() }
          case ElementAction.`right click` => 
            perform(webElement, contextElement) { _.contextClick() }
          case ElementAction.`double click` => 
            perform(webElement, contextElement) { _.doubleClick() }
          case ElementAction.check | ElementAction.tick =>
            clickCheckbox(webElement, Some(contextElement), true)
          case ElementAction.uncheck | ElementAction.untick =>
            clickCheckbox(webElement, Some(contextElement), false)
          case ElementAction.`move to` => perform(webElement, contextElement) { action => action }
          case _ => WebErrors.invalidContextActionError(action)
        }
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
    getElementText(binding).map(_.length() > 0).getOrElse(false)

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
    applyJS(jsFunctionWrapper("elem", "arguments[0]", s"if (typeof elem !== 'undefined' && elem != null) { elem.scrollIntoView(${scrollTo == ScrollTo.top});${if (offset != 0) s" window.scroll(0, window.scrollY + $offset);" else ""}}"), webElement)
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
    * Positions the browser window to the given co-ordinates.
    *
    * @param x the x co-ordinate
    * @param x the y co-ordinate
    */
  def positionWindow(x: Int, y: Int): Unit = {
    withWebDriver { driver =>
      logger.info(s"Postioning browser (x, y) position to ($x, $y)")
      driver.manage().window().setPosition(new Point(x, y))
    }
  }

  /**
    * Maximizes the browser window.
    */
  def maximizeWindow(): Unit = {
    withWebDriver { driver =>
      logger.info("Maximising browser window")
      driver.manage().window().setPosition(new Point(0, 0))
      driver.manage().window().maximize()
    }
  }

  def captureCurrentUrl: String = {
    withWebDriver { driver =>
      driver.getCurrentUrl
    } getOrElse {
      DryValueBinding.unresolved("currentUrl")
    }
  }

  /**
    * Gets the text value of a web element on the current page.
    * A search for the text is made in the following order and the first value
    * found is returned:
    *  - Web element text
    *  - Web element text attribute
    *  - Web element value attribute
    *
    * @param binding the locator binding
    */
  def getElementText(binding: LocatorBinding): Option[String] =
    withWebElement(binding, s"trying to get ${binding.displayName} text") { webElement =>
      Option(webElement.getText) match {
        case None | Some("") =>
          Option(webElement.getDomAttribute("text")) match {
            case None | Some("") =>
              Option(webElement.getDomProperty("text")) match {
                case None | Some("") =>
                  Option(webElement.getDomAttribute("value")) match {
                    case None | Some("") =>
                      Option(webElement.getDomProperty("value")) match {
                        case None | Some("") =>
                          val value = applyJS(jsFunctionWrapper("element", "arguments[0]", "return element.innerText || element.textContent || ''"), webElement).asInstanceOf[String]
                          if (value != null) value else ""
                        case Some(value) => value
                      }
                    case Some(value) => value
                  }
                case Some(value) => value
              }
            case Some(value) => value
          }
        case Some(value) => value
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
      getElementSelectionByJS(webElement, DropdownSelection.text) match {
        case None =>
          Try(createSelect(webElement)) map { select =>
            Option(select.getAllSelectedOptions.asScala.map(_.getText()).mkString(",")) match {
              case None | Some("") =>
                Option(select.getAllSelectedOptions.asScala.map(_.getDomAttribute("text")).mkString(",")) match {
                  case None | Some("") =>
                    select.getAllSelectedOptions.asScala.map(_.getDomProperty("text")).mkString(",")
                  case Some(value) => value
                }
              case Some(value) => value
            }
          } getOrElse null
        case Some(value) => value
      }
    } tap { value =>
      logger.debug(s"getSelectedElementText($binding)='$value'")
    }
  }

  def getElementAttribute(binding: LocatorBinding, name: String): String = {
    evaluate(Some(DryValueBinding.unresolved(s"WebElementAttribute"))) {
      withWebElement(binding, s"trying to get $name attribute of ${binding.displayName}") { webElement => 
        Option(webElement.getDomAttribute(name)).orElse(Option(webElement.getDomProperty(name))) getOrElse ""
      }
    } getOrElse ""
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
      waitUntilEnabled(binding, webElement)
      getElementSelectionByJS(webElement, DropdownSelection.value) match {
        case None =>
          Try(createSelect(webElement)) map { select =>
            select.getAllSelectedOptions.asScala.flatMap(s => Option(s.getDomAttribute("value")).orElse(Option(s.getDomProperty("value")))).mkString(",")
          } getOrElse null
        case Some(value) => value
      }
    } tap { value =>
      logger.debug(s"getSelectedElementValue($binding)='$value'")
    }
  }

  private def getElementSelectionByJS(webElement: WebElement, by: DropdownSelection): Option[String] = {
    Option(applyJS(jsFunctionWrapper("select", "arguments[0]", s"try{var byText=${by == DropdownSelection.text};var result='';var options=select && select.options;if(!!options){var opt;for(var i=0,iLen=options.length;i<iLen;i++){opt=options[i];if(opt.selected){if(result.length>0){result=result+',';}if(byText){result=result+opt.text;}else{result=result+opt.value;}}}return result;}else{return null;}}catch(e){return null;}"), webElement).asInstanceOf[String])
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

   /** Switches to the top window / first frame */
  def switchToFrame(binding: LocatorBinding): Unit = {
    withDriverAndElement(binding, s"trying to switch to ${binding.displayName} content (frame)") { (driver, frame) =>
      driver.switchTo().frame(frame)
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
  def handlePopup(accept: Boolean, waitSecs: Option[Long]): Unit = {
    withWebDriver { driver =>
      waitForPopup(waitSecs)
      if (accept) {
        driver.switchTo().alert().accept()
      } else {
        driver.switchTo().alert().dismiss()
      }
    }
  }

  def waitForPopup(waitSecs: Option[Long]): Unit = {
    waitUntil(waitSecs, "waiting for alert popup", ExpectedConditions.alertIsPresent())
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
        Settings.getOpt("gwen.web.capabilities.goog:loggingPrefs.performance") foreach { _ =>
          addAttachment("performance-trace", "json", driverManager.performanceTrace(u, driver))
        }
      }
    } (using WebSettings.`gwen.web.capture.screenshots.enabled`)
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
    } getOrElse DryValueBinding.unresolved("popupMessage")
  }

  /** Checks if an element is displayed. */
  def isDisplayed(binding: LocatorBinding): Boolean = {
    Try {
      withDriverAndElement(binding, s"trying to locate ${binding.displayName}") { (driver, webElement) =>
        createActions(driver).moveToElement(webElement).perform()
        if (!isInViewport(webElement)) {
          Try(scrollIntoView(webElement, ScrollTo.top, -100))
        }
      }
    } isSuccess
  }

  private def isDisplayedAndInViewport(webElement: WebElement): Boolean = {
    webElement.isDisplayed && isInViewport(webElement)
  }

  /** Checks if an element is not in the view port. */
  private def isInViewport(webElement: WebElement): Boolean = {
    applyJS(jsFunctionWrapper("elem", "arguments[0]", "var b=elem.getBoundingClientRect(); return b.top>=0 && b.left>=0 && b.bottom<=(window.innerHeight || document.documentElement.clientHeight) && b.right<=(window.innerWidth || document.documentElement.clientWidth);"), webElement).asInstanceOf[Boolean]
  }

  override def defaultWait: Duration = Duration(WebSettings.`gwen.web.wait.seconds`, SECONDS)

}
