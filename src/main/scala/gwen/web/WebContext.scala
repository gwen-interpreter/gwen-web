/*
 * Copyright 2015-2020 Brady Wood, Branko Juric
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

import gwen._
import gwen.Errors.javaScriptError
import gwen.Sensitive
import gwen.web.Errors._

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

import com.applitools.eyes.{MatchLevel, RectangleSize}
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.io.FileUtils
import org.openqa.selenium._
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.Select

import java.io.File

/**
  * The web context. All web driver interactions happen here (and will do nothing when --dry-run is enabled).
  */
class WebContext(env: WebEnvContext, driverManager: DriverManager) extends WebElementLocator with LazyLogging {

  /** Last captured screenshot file size. */
  private var lastScreenshotSize: Option[Long] = None

  /** Applitools Eyes context. */
  private var eyesContext: Option[EyesContext] = None

  /** Resets the driver context. */
  def reset(): Unit = {
    driverManager.reset()
    lastScreenshotSize = None
    eyesContext.foreach(_.close())
    eyesContext = None
  }

  /** Closes all browsers and associated web drivers (if any have loaded). */
  def close(): Unit = {
    env.perform {
      eyesContext.foreach(_.close())
      driverManager.quit()
    }
  }

  /** Closes a named browser and associated web driver. */
  def close(name: String): Unit = {
    env.perform {
      driverManager.quit(name)
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
    env.evaluate(None.asInstanceOf[Option[T]]) {
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
    * Gets a web element binding.
    *
    * @param element the name of the web element
    */
  def getLocatorBinding(element: String): LocatorBinding = env.getLocatorBinding(element)

  /**
    * Gets a cached web element.
    *
    * @param element the name of the web element
    * @return the cached web element
    */
  def getCachedWebElement(element: String): Option[WebElement] = env.topScope.getObject(element) match {
    case Some(we: WebElement) =>
      highlightElement(we)
      Some(we)
    case _ => None
  }

  /**
    * Locates a web element and highlights it.
    *
    * @param elementBinding the web element locator binding
    */
  def locateAndHighlight(elementBinding: LocatorBinding): Unit = {
    withDriverAndElement(elementBinding, s"trying to locate $elementBinding") { (driver, webElement) =>
      createActions(driver).moveToElement(webElement).perform() 
    }
  }

  /**
    * Locates a web element and performs an operation on it.
    *
    * @param elementBinding the web element locator binding
    * @param reason a description of what action is being performed
    * @param operation the operation to perform on the element
    */
  private def withWebElement[T](elementBinding: LocatorBinding, reason: String)(operation: WebElement => T): Option[T] =
    env.evaluate(None.asInstanceOf[Option[T]]) {
      val locator = elementBinding.locators.head
      val wHandle = locator.container.flatMap(_ => withWebDriver(_.getWindowHandle))
      try {
        var result: Option[Try[T]] = None
        val start = System.nanoTime()
        try {
          var lapsed = 0L
          waitUntil(reason) {
            try {
              val webElement = locate(elementBinding)
              tryMoveTo(webElement)
              if (!locator.isContainer) {
                highlightElement(webElement)
              }
              val res = operation(webElement)
              result = Some(Success(res))
              true
            } catch {
              case e: Throwable =>
                lapsed = Duration.fromNanos(System.nanoTime() - start).toSeconds
                if (e.isInstanceOf[InvalidElementStateException] || e.isInstanceOf[NoSuchElementException] || e.isInstanceOf[NotFoundOrInteractableException]) {
                  if (lapsed >= elementBinding.timeoutSeconds) {
                    result =  if (e.isInstanceOf[WebElementNotFoundException]) {
                      Some(Failure(e))
                    } else {
                      Some(Try(elementNotInteractableError(elementBinding, e)))
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
              if (WebSettings.`gwen.web.capture.screenshots`) {
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
    if (!webElement.isDisplayed && !isInViewport(webElement)) {
      withWebDriver { driver => 
        createActions(driver).moveToElement(webElement).perform()
      }
    }
  }

  /** Captures and the current screenshot and adds it to the attachments list. */
  def captureScreenshot(unconditional: Boolean, name: String = "Screenshot"): Option[File] = {
    env.evaluate(Option(new File("$[dryRun:screenshotFile]"))) {
      val screenshot = driverManager.withWebDriver { driver =>
        Thread.sleep(150) // give browser time to render
        driver.asInstanceOf[TakesScreenshot].getScreenshotAs(OutputType.FILE)
      }
      val keep = unconditional || WebSettings.`gwen.web.capture.screenshots.duplicates` || lastScreenshotSize.fold(true) { _ != screenshot.length}
      if (keep) {
        if (!WebSettings.`gwen.web.capture.screenshots.duplicates`) lastScreenshotSize = Some(screenshot.length())
        val (n, file) = env.addAttachment(name, screenshot.getName.substring(screenshot.getName.lastIndexOf('.') + 1), null)
        FileUtils.copyFile(screenshot, file)
        Some(file)
      } else {
        None
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
  def executeJS(javascript: String, params: Any*)(implicit takeScreenShot: Boolean = false): Any =
    withWebDriver { webDriver =>
      try {
        webDriver.asInstanceOf[JavascriptExecutor].executeScript(javascript, params.map(_.asInstanceOf[AnyRef]) : _*) tap { result =>
          if (takeScreenShot && WebSettings.`gwen.web.capture.screenshots`) {
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
      if (env.isDryRun) s"$$[javascript:$javascript]"
      else null  //js returned null
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
    * Waits until a given condition is ready for a given number of seconds.
    * Errors on given timeout out seconds.
    *
    * @param timeoutSecs the number of seconds to wait before timing out
    * @param reason a description of what is being waited on
    * @param condition the boolean condition to wait for (until true)
    */
  def waitUntil(timeoutSecs: Long, reason: String)(condition: => Boolean): Unit = {
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
    env.perform {
      val msecs = WebSettings`gwen.web.throttle.msecs`; // need semi-colon (compiler bug?)
      if (msecs > 0) {
        val style = WebSettings.`gwen.web.highlight.style`
        val origStyle = executeJS(s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } original_style = element.getAttribute('style'); element.setAttribute('style', original_style + '; $style'); return original_style;", element)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
        try {
          if (!WebSettings.`gwen.web.capture.screenshots.highlighting` || !WebSettings.`gwen.web.capture.screenshots`) {
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
    * @param elementBinding the locator binding of the element
    * @param state the state to check
    * @param negate whether or not to negate the check
    */
  def checkElementState(elementBinding: LocatorBinding, state: String, negate: Boolean): Unit = {
    env.perform {
      val result = isElementState(elementBinding.jsEquivalent, state, negate)
      assert(result, s"$elementBinding should${if(negate) " not" else ""} be $state")
    }
  }

  /**
    * Checks the current state of an element.
    *
    * @param elementBinding the locator binding of the element
    * @param state the state to check
    * @param negate whether or not to negate the check
    */
  private def isElementState(elementBinding: LocatorBinding, state: String, negate: Boolean): Boolean = {
    var result = false
    env.perform {
      // use fast locator if checking for element absence so we don't have to wait for timeout to lapse
      val parsedBinding = state match {
        case "displayed" if negate =>
          elementBinding.withFastTimeout
        case "hidden" if !negate =>
          elementBinding.withFastTimeout
        case _ =>
          elementBinding
      }
      try {
        withWebElement(parsedBinding, s"waiting for $parsedBinding to${if (negate) " not" else ""} be $state") { webElement =>
          result = state match {
            case "displayed" if !negate => isDisplayed(webElement)
            case "displayed" if negate => !isDisplayed(webElement)
            case "hidden" if !negate => !isDisplayed(webElement)
            case "hidden" if negate => isDisplayed(webElement)
            case "checked" | "ticked" if !negate => webElement.isSelected
            case "checked" | "ticked" if negate => !webElement.isSelected
            case "unchecked" | "unticked" if !negate => !webElement.isSelected
            case "unchecked" | "unticked" if negate => webElement.isSelected
            case "enabled" if !negate => webElement.isEnabled
            case "enabled" if negate => !webElement.isEnabled
            case "disabled" if !negate => !webElement.isEnabled
            case "disabled" if negate => webElement.isEnabled
          }
        }
      } catch {
        case e @ (_ :  NoSuchElementException | _ : NotFoundOrInteractableException | _ : WaitTimeoutException) =>
          if (state == "displayed") result = negate
          else if (state == "hidden") result = !negate
          else throw e
      }
    }
    result
  }

  /**
    * Waits the state of an element.
    *
    * @param elementBinding the locator binding of the element
    * @param state the state to wait for
    * @param negate whether or not to negate the check
    */
  def waitForElementState(elementBinding: LocatorBinding, state: String, negate: Boolean): Unit =
    waitUntil(s"waiting for $elementBinding to${if (negate) " not" else""} be $state") {
      isElementState(elementBinding, state, negate)
    }

  /** Gets the title of the current page in the browser.*/
  def getTitle: String =
    withWebDriver { driver =>
      driver.getTitle tap { title =>
        env.bindAndWait("page", "title", title)
      }
    }.getOrElse("$[dryRun:title]")

  /**
    * Sends a value to a web element.
    *
    * @param elementBinding the web element locator binding
    * @param value the value to send
    * @param clickFirst true to click field first (if element is a text field)
    * @param clearFirst true to clear field first (if element is a text field)
    * @param sendEnterKey true to send the Enter key after sending the value
    */
  def sendValue(elementBinding: LocatorBinding, value: String, clearFirst: Boolean, clickFirst: Boolean, sendEnterKey: Boolean): Unit = {
    val element = elementBinding.name
    withDriverAndElement(elementBinding, s"trying to send value to $element") { (driver, webElement) =>
      createActions(driver)
      if (clickFirst) {
        webElement.click()
      }
      if (clearFirst) {
        webElement.clear()
      }
      Sensitive.withValue(value) { plainValue =>
        if ("file" == webElement.getAttribute("type")) {
          createActions(driver).moveToElement(webElement).perform()
          webElement.sendKeys(plainValue)
        } else {
        createActions(driver).moveToElement(webElement).sendKeys(plainValue).perform()
        }
      }
      env.bindAndWait(element, "type", value)
      if (sendEnterKey) {
        createActions(driver).sendKeys(webElement, Keys.RETURN).perform()
        env.bindAndWait(element, "enter", "true")
      }
    }
  }

  private[web] def createSelect(webElement: WebElement): Select = new Select(webElement)

  /**
    * Selects a value in a dropdown (select control) by visible text.
    *
    * @param elementBinding the web element locator binding
    * @param value the value to select
    */
  def selectByVisibleText(elementBinding: LocatorBinding, value: String): Unit = {
    withWebElement(elementBinding, s"trying to select option in $elementBinding by visible text") { webElement =>
      logger.debug(s"Selecting '$value' in ${elementBinding.name} by text")
      createSelect(webElement).selectByVisibleText(value)
      env.bindAndWait(elementBinding.name, "select", value)
    }
  }

  /**
    * Selects a value in a dropdown (select control) by value.
    *
    * @param elementBinding the web element locator binding
    * @param value the value to select
    */
  def selectByValue(elementBinding: LocatorBinding, value: String): Unit = {
    withWebElement(elementBinding, s"trying to select option in $elementBinding by value") { webElement =>
      logger.debug(s"Selecting '$value' in ${elementBinding.name} by value")
      createSelect(webElement).selectByValue(value)
      env.bindAndWait(elementBinding.name, "select", value)
    }
  }

  /**
    * Selects a value in a dropdown (select control) by index.
    *
    * @param elementBinding the web element locator binding
    * @param index the index to select (first index is 0)
    */
  def selectByIndex(elementBinding: LocatorBinding, index: Int): Unit = {
    withWebElement(elementBinding, s"trying to select option in $elementBinding at index $index") { webElement =>
      logger.debug(s"Selecting option in ${elementBinding.name} by index: $index")
      val select = createSelect(webElement)
      select.selectByIndex(index)
      env.bindAndWait(elementBinding.name, "select", select.getOptions.get(index).getText)
    }
  }

  /**
    * Deselects a value in a dropdown (select control) by visible text.
    *
    * @param elementBinding the web element locator binding
    * @param value the value to select
    */
  def deselectByVisibleText(elementBinding: LocatorBinding, value: String): Unit = {
    withWebElement(elementBinding, s"trying to deselect option in $elementBinding by visible text") { webElement =>
      logger.debug(s"Deselecting '$value' in ${elementBinding.name} by text")
      createSelect(webElement).deselectByVisibleText(value)
      env.bindAndWait(elementBinding.name, "deselect", value)
    }
  }

  /**
    * Deselects a value in a dropdown (select control) by value.
    *
    * @param elementBinding the web element locator binding
    * @param value the value to select
    */
  def deselectByValue(elementBinding: LocatorBinding, value: String): Unit = {
    withWebElement(elementBinding, s"trying to deselect option in $elementBinding by value") { webElement =>
      logger.debug(s"Deselecting '$value' in ${elementBinding.name} by value")
      createSelect(webElement).deselectByValue(value)
      env.bindAndWait(elementBinding.name, "deselect", value)
    }
  }

  /**
    * Deselects a value in a dropdown (select control) by index.
    *
    * @param elementBinding the web element locator binding
    * @param index the index to select (first index is 0)
    */
  def deselectByIndex(elementBinding: LocatorBinding, index: Int): Unit = {
    withWebElement(elementBinding, s"trying to deselect option in $elementBinding at index $index") { webElement =>
      logger.debug(s"Deselecting option in ${elementBinding.name} by index: $index")
      val select = createSelect(webElement)
      select.deselectByIndex(index)
      env.bindAndWait(elementBinding.name, "deselect", select.getOptions.get(index).getText)
    }
  }

  private [web] def createActions(driver: WebDriver): Actions = new Actions(driver)

  def performAction(action: String, elementBinding: LocatorBinding): Unit = {
    val actionBinding = env.scopes.getOpt(s"${elementBinding.name}/action/$action/javascript")
    actionBinding match {
      case Some(javascript) =>
        performScriptAction(action, javascript, elementBinding, s"trying to $action $elementBinding")
      case None =>
        withDriverAndElement(elementBinding, s"trying to $action $elementBinding") { (driver, webElement) =>
          if (action != "move to") {
            moveToAndCapture(driver, webElement)
          }
          action match {
            case "click" =>
            webElement.click()
            case "right click" =>
              createActions(driver).contextClick(webElement).perform()
            case "double click" =>
              createActions(driver).doubleClick(webElement).perform()
            case "move to" =>
              moveToAndCapture(driver, webElement)
            case "submit" => webElement.submit()
            case "check" | "tick" =>
              if (!webElement.isSelected) webElement.click()
              if (!webElement.isSelected) 
                createActions(driver).sendKeys(webElement, Keys.SPACE).perform()
            case "uncheck" | "untick" =>
              if (webElement.isSelected) webElement.click()
              if (webElement.isSelected) 
                createActions(driver).sendKeys(webElement, Keys.SPACE).perform()
            case "clear" =>
              webElement.clear()
          }
        }
        env.bindAndWait(elementBinding.name, action, "true")
    }
  }

  def moveToAndCapture(driver: WebDriver, webElement: WebElement): Unit = {
    createActions(driver).moveToElement(webElement).perform()
    if (WebSettings.`gwen.web.capture.screenshots`) {
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

  def holdAndClick(modifierKeys: Array[String], clickAction: String, elementBinding: LocatorBinding): Unit = {
    val keys = modifierKeys.map(_.trim).map(key => Try(Keys.valueOf(key.toUpperCase)).getOrElse(unsupportedModifierKeyError(key)))
    withDriverAndElement(elementBinding, s"trying to $clickAction $elementBinding") { (driver, webElement) =>
      moveToAndCapture(driver, webElement)
      var actions = createActions(driver)
      keys.foreach { key => actions = actions.keyDown(key) }
      actions = clickAction match {
        case "click" => actions.click(webElement)
        case "right click" => actions.contextClick(webElement)
        case "double click" => actions.doubleClick(webElement)
      }
      keys.reverse.foreach { key => actions = actions.keyUp(key) }
      actions.build().perform()
    }
    env.bindAndWait(elementBinding.name, clickAction, "true")
  }

  def sendKeys(keysToSend: Array[String]): Unit = {
    sendKeys(None, keysToSend)
  }

  def sendKeys(elementBinding: LocatorBinding, keysToSend: Array[String]): Unit = {
    sendKeys(Some(elementBinding), keysToSend)
  }

  def sendKeys(elementBindingOpt: Option[LocatorBinding], keysToSend: Array[String]): Unit = {
    val keys =  keysToSend.map(_.trim).map(key => Try(Keys.valueOf(key.toUpperCase)).getOrElse(key))
    elementBindingOpt match {
      case Some(elementBinding) =>
        withDriverAndElement(elementBinding, s"trying to send keys to $elementBinding") { (driver, webElement) =>
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

  private def withDriverAndElement(elementBinding: LocatorBinding, reason: String)(doActions: (WebDriver, WebElement) => Unit): Unit = {
    withWebDriver { driver =>
      withWebElement(elementBinding, reason) { webElement =>
        if (WebSettings.`gwen.web.implicit.element.focus`) {
          executeJS("(function(element){element.focus();})(arguments[0]);", webElement)
        }
        doActions(driver, webElement)
      }
    }
  }

  private def performScriptAction(action: String, javascript: String, elementBinding: LocatorBinding, reason: String): Unit = {
    withDriverAndElement(elementBinding, reason) { (driver, webElement) =>
      if (action != "move to") {
        moveToAndCapture(driver, webElement)
      }
      executeJS(s"(function(element) { $javascript })(arguments[0])", webElement)
      env.bindAndWait(elementBinding.name, action, "true")
    }
  }

  /**
    * Performs and action on a web element in the context of another element.
    *
    * @param action description of the action
    * @param element the name of the element to perform the action on
    * @param context the name of the context element binding to find the element in
    */
  def performActionInContext(action: String, element: String, context: String): Unit = {
    try {
        val contextBinding = env.getLocatorBinding(context)
        val elementBinding = env.getLocatorBinding(element)
        performActionIn(action, elementBinding, contextBinding)
      } catch {
        case e1: LocatorBindingException =>
          try {
            val elementBinding = env.getLocatorBinding(s"$element of $context")
            performAction(action, elementBinding)
          } catch {
            case e2: LocatorBindingException =>
              throw new LocatorBindingException(s"${e1.getMessage}. ${e2.getMessage}.")
          }
      }
  }

  private def performActionIn(action: String, elementBinding: LocatorBinding, contextBinding: LocatorBinding): Unit = {
    def perform(webElement: WebElement, contextElement: WebElement)(buildAction: Actions => Actions): Unit = {
      withWebDriver { driver =>
        val moveTo = createActions(driver).moveToElement(contextElement).moveToElement(webElement)
        buildAction(moveTo).build().perform()
        if (WebSettings.`gwen.web.capture.screenshots`) {
          captureScreenshot(false)
        }
      }
    }
    val reason = s"trying to $action $elementBinding"
    withWebElement(contextBinding, reason) { contextElement =>
      withWebElement(elementBinding, reason) { webElement =>
        action match {
          case "click" => perform(webElement, contextElement) { _.click() }
          case "right click" => perform(webElement, contextElement) { _.contextClick() }
          case "double click" => perform(webElement, contextElement) { _.doubleClick() }
          case "check" | "tick" =>
            if (!webElement.isSelected) perform(webElement, contextElement) { _.click() }
            if (!webElement.isSelected) perform(webElement, contextElement) { _.sendKeys(Keys.SPACE) }
          case "uncheck" | "untick" =>
            if (webElement.isSelected) perform(webElement, contextElement) { _.click() }
            if (webElement.isSelected) perform(webElement, contextElement) { _.sendKeys(Keys.SPACE) }
          case "move to" => perform(webElement, contextElement) { action => action }
        }
        env.bindAndWait(elementBinding.name, action, "true")
      }
    }
  }

  /**
    * Waits for text to appear in the given web element.
    *
    * @param elementBinding the web element locator binding
    */
  def waitForText(elementBinding: LocatorBinding): Boolean =
    getElementText(elementBinding).map(_.length()).getOrElse {
      env.scopes.set(s"${elementBinding.name}/text", "text")
      0
    } > 0

  /**
   * Scrolls an element into view.
   *
   * @param elementBinding the web element locator binding
   * @param scrollTo scroll element into view, options are: top or bottom
   */
  def scrollIntoView(elementBinding: LocatorBinding, scrollTo: ScrollTo.Value): Unit = {
    withWebElement(elementBinding, s"trying to scroll to $scrollTo of $elementBinding") { scrollIntoView(_, scrollTo) }
  }

  /**
   * Scrolls the given web element into view.
   *
   * @param webElement the web element to scroll to
   * @param scrollTo scroll element into view, options are: top or bottom
   */
  def scrollIntoView(webElement: WebElement, scrollTo: ScrollTo.Value): Unit = {
    executeJS(s"var elem = arguments[0]; if (typeof elem !== 'undefined' && elem != null) { elem.scrollIntoView(${scrollTo == ScrollTo.top}); }", webElement)
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

  def captureCurrentUrl(asName: Option[String]): Unit = {
    val name = asName.getOrElse("the current URL")
    env.topScope.set(name, withWebDriver { driver =>
      driver.getCurrentUrl tap { content =>
        env.addAttachment(name, "txt", content)
      }
    }.getOrElse("$[currentUrl]"))
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
    * @param elementBinding the web element locator binding
    */
  def getElementText(elementBinding: LocatorBinding): Option[String] =
    withWebElement(elementBinding, s"trying to get text of $elementBinding") { webElement =>
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
        env.bindAndWait(elementBinding.name, "text", text)
      }
    } tap { value =>
      logger.debug(s"getElementText(${elementBinding.name})='$value'")
    }

  /**
    * Gets the selected text of a dropdown web element on the current page.
    * If a value is found, its value is bound to the current page
    * scope as `name/selectedText`.
    *
    * @param name the web element name
    */
  private def getSelectedElementText(name: String): Option[String] = {
    val elementBinding = env.getLocatorBinding(name)
    withWebElement(elementBinding, s"trying to get selected text of $elementBinding") { webElement =>
      (getElementSelectionByJS(webElement, byText = true) match {
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
        env.bindAndWait(elementBinding.name, "selectedText", text)
      }
    } tap { value =>
      logger.debug(s"getSelectedElementText(${elementBinding.name})='$value'")
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
    val elementBinding = env.getLocatorBinding(name)
    withWebElement(elementBinding, s"trying to get selected value of $elementBinding") { webElement =>
      getElementSelectionByJS(webElement, byText = false) match {
        case None =>
          Try(createSelect(webElement)) map { select =>
            select.getAllSelectedOptions.asScala.map(_.getAttribute("value")).mkString(",") tap { value =>
              env.bindAndWait(elementBinding.name, "selectedValue", value)
            }
          } getOrElse null
        case Some(value) => value
      }
    } tap { value =>
      logger.debug(s"getSelectedElementValue(${elementBinding.name})='$value'")
    }
  }

  private def getElementSelectionByJS(webElement: WebElement, byText: Boolean): Option[String] = {
    Option(executeJS(s"""return (function(select){try{var byText=$byText;var result='';var options=select && select.options;if(!!options){var opt;for(var i=0,iLen=options.length;i<iLen;i++){opt=options[i];if(opt.selected){if(result.length>0){result=result+',';}if(byText){result=result+opt.text;}else{result=result+opt.value;}}}return result;}else{return null;}}catch(e){return null;}})(arguments[0])""", webElement).asInstanceOf[String])
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
  def getElementSelection(name: String, selection: String): Option[String] = {
    selection.trim match {
      case "text" => getSelectedElementText(name)
      case _ => getSelectedElementValue(name)
    }
  }

  /**
    * Switches the web driver session
    *
    * @param session the name of the session to switch to
    */
  def switchToSession(session: String): Unit = {
    env.perform {
      driverManager.switchToSession(session)
    }
  }

  /**
    * Starts a new session if there isn't one or stays in the current one.
    */
  def newOrCurrentSession(): Unit = {
    env.perform {
      driverManager.newOrCurrentSession()
    }
  }

  /** Gets the number of open sesions. */
  def noOfSessions(): Int = driverManager.noOfSessions()

  /** Gets the number of open windows. */
  def noOfWindows(): Int = driverManager.noOfWindows()

  /**
    * Switches to the child window if one was just opened.
    */
  def switchToChild(): Unit = {
    switchToChild(1)
  }

  /**
    * Switches to a tab or child window ocurrance.
    * 
    * @param occurrence the tag or window occurrence to switch to (first opened is occurrence 1, 2nd is 2, ..)
    */
  def switchToChild(occurrence: Int): Unit = {
    waitUntil(s"trying to switch to child tab/window occurrence $occurrence") { 
      driverManager.windows().lift(occurrence).nonEmpty
    }
    driverManager.switchToChild(occurrence)
  }

  /**
    * Closes the last child window.
    */
  def closeChild(): Unit = {
    env.perform {
      driverManager.closeChild()
    }
  }

  /**
    * Closes the tab or child window occurrence.
    * 
    * @param occurrence the tag or window occurrence to close (first opened is occurrence 1, 2nd is 2, ..)
    */
  def closeChild(occurrence: Int): Unit = {
    env.perform {
      driverManager.closeChild(occurrence)
    }
  }

  /** Switches to the parent window. */
  def switchToParent(): Unit = {
    env.perform {
      driverManager.switchToParent()
    }
  }

  /** Switches to the top window / first frame */
  def switchToDefaultContent(): Unit = {
    env.perform {
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
      driver.get(url)
    } (WebSettings.`gwen.web.capture.screenshots`)
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
    webElement.isDisplayed && isInViewport(webElement)
  }

  /** Checks if an element is not in the view port. */
  def isInViewport(webElement: WebElement): Boolean = {
    executeJS("return (function(elem){var b=elem.getBoundingClientRect(); return b.top>=0 && b.left>=0 && b.bottom<=(window.innerHeight || document.documentElement.clientHeight) && b.right<=(window.innerWidth || document.documentElement.clientWidth);})(arguments[0])", webElement).asInstanceOf[Boolean]
  }

  /**
    * Performs the given function on the eyes context.
    *
    * @param f the function to perform
    * @return the result of the function
    */
  private def withEyesContext[T](f: EyesContext => T): Option[T] = {
    env.evaluate(None.asInstanceOf[Option[T]]) {
      if (eyesContext.isEmpty) {
        eyesContext = Some(new EyesContext(env))
      }
      eyesContext.map(f)
    }
  }

  /**
    * Starts a visual text.
    *
    * @param testName the name of the visual test
    * @param viewportSize optional viewport size
    */
  def startVisualTest(testName: String, viewportSize: Option[RectangleSize]): Unit =
    withEyesContext { context =>
      withWebDriver { driver =>
        context.open(driver, testName, viewportSize)
      }
    }

  /**
    * Performs a visual checkpoint of the contents in the current browser window.
    *
    * @param checkpoint the checkpoint name
    * @param fullPage true to capture full page, false otherwise
    * @param matchLevel optional match Level
    */
  def checkVisual(checkpoint: String, fullPage: Boolean, matchLevel: Option[MatchLevel]): Unit =
    withEyesContext { context =>
      Thread.sleep(150) // give browser time to render
      context.check(checkpoint, fullPage, matchLevel)
    }

  /**
    * Performs a visual check of all checkpoints in current eyes session.
    */
  def asertVisuals(): Unit = {
    withEyesContext { context =>
      context.results() tap { results =>
        env.addAttachment("AppliTools dashboard", "url", results.getUrl)
        try {
          assert(results.isNew || results.isPassed, s"Expected visual check to pass but status was: ${results.getStatus}")
        } catch {
          case e: AssertionError => visualAssertionError(e.getMessage)
        }
      }
    }
  }

}