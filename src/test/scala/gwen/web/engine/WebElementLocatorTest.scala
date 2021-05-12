/*
 * Copyright 2014-2021 Brady Wood, Branko Juric
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

package gwen.web.engine

import gwen.web._
import gwen.web.engine.WebErrors._
import gwen.web.engine.binding._

import gwen.core.Errors
import gwen.core.GwenOptions
import gwen.core.Settings
import gwen.core.model.state.EnvState

import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.mockito.Mockito.verifyZeroInteractions
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.openqa.selenium.WebDriver.{Options, TargetLocator, Timeouts}
import org.mockito.Matchers.{anyVararg, same}

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.Duration

import java.util
import java.util.concurrent.TimeUnit

class WebElementLocatorTest extends BaseTest with Matchers with MockitoSugar with BeforeAndAfterEach {

  // disable implicit js locators for unit test
  Settings.set("gwen.web.implicit.js.locators", "false")

  private var mockWebElement: WebElement = _
  private var mockWebElements: List[WebElement] = _
  private var mockContainerElement: WebElement = _
  private var mockIFrameElement: WebElement = _
  private var mockFrameElement: WebElement = _
  private var mockTargetLocator: TargetLocator = _
  private var mockWebDriverOptions: Options = _
  private var mockWebDriverTimeouts: Timeouts = _

  override def beforeEach(): Unit = {
    mockWebElement = mock[WebElement]
    mockWebElements = List(mock[WebElement], mock[WebElement])
    mockContainerElement = mock[WebElement]
    mockIFrameElement = mock[WebElement]
    mockFrameElement = mock[WebElement]
    mockTargetLocator = mock[TargetLocator]
    mockWebDriverOptions = mock[WebDriver.Options]
    mockWebDriverTimeouts = mock[WebDriver.Timeouts]
  }

  "Attempt to locate non existent element" should "throw no element not found error" in {

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(By.id("mname"))).thenReturn(null)
    
    val e = intercept[WebElementNotFoundException] {
      try {
        locator.locate(LocatorBinding("middleName", SelectorType.id, "mname", None, None, ctx))
      } catch {
        case e: Throwable =>
          e.printStackTrace()
          throw e
      }
    }
    e.getMessage should startWith ("Could not locate element: middleName")
  }
  
  "Attempt to locate existing element by id" should "return the element" in {
    shouldFindWebElement(SelectorType.id, "uname", By.id("uname"), None, None)
  }

  "Attempt to locate existing element by id at index 0" should "return the element" in {
    shouldFindWebElement(SelectorType.id, "uname", By.id("uname"), None, Some(0))
  }

  "Attempt to locate existing element by id with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.id, "uname", By.id("uname"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by id at index 1 with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.id, "uname", By.id("uname"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by id with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.id, "uname", By.id("uname"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by id at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.id, "uname", By.id("uname"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by name" should "return the element" in {
    shouldFindWebElement(SelectorType.name, "uname", By.name("uname"), None, None)
  }

  "Attempt to locate existing element by name at index 0" should "return the element" in {
    shouldFindWebElement(SelectorType.name, "uname", By.name("uname"), None, Some(0))
  }

  "Attempt to locate existing element by name with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.name, "uname", By.name("uname"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by name at index 1 with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.name, "uname", By.name("uname"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by name with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.name, "uname", By.name("uname"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by name at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.name, "uname", By.name("uname"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by tag name" should "return the element" in {
    shouldFindWebElement(SelectorType.`tag name`, "input", By.tagName("input"), None, None)
  }

  "Attempt to locate existing element by tag name at index 0" should "return the element" in {
    shouldFindWebElement(SelectorType.`tag name`, "input", By.tagName("input"), None, Some(0))
  }

  "Attempt to locate existing element by tag name with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`tag name`, "input", By.tagName("input"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by tag name at index 1 with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`tag name`, "input", By.tagName("input"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by tag name with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`tag name`, "input", By.tagName("input"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by tag name at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`tag name`, "input", By.tagName("input"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by css selector" should "return the element" in {
    shouldFindWebElement(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), None, None)
  }

  "Attempt to locate existing element by css selector at index 0" should "return the element" in {
    shouldFindWebElement(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), None, Some(0))
  }

  "Attempt to locate existing element by css selector with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by css selector at index 1 with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by css selector with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by css selector at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by xpath" should "return the element" in {
    shouldFindWebElement(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), None, None)
  }

  "Attempt to locate existing element by xpath at index 0" should "return the element" in {
    shouldFindWebElement(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), None, Some(0))
  }

  "Attempt to locate existing element by xpath with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by xpath at index 1 with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by xpath with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by xpath at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by class name" should "return the element" in {
    shouldFindWebElement(SelectorType.`class name`, ".userinput", By.className(".userinput"), None, None)
  }

  "Attempt to locate existing element by class name at index 0" should "return the element" in {
    shouldFindWebElement(SelectorType.`class name`, ".userinput", By.className(".userinput"), None, Some(0))
  }

  "Attempt to locate existing element by class name with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`class name`, ".userinput", By.className(".userinput"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by class name at index 1 with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`class name`, ".userinput", By.className(".userinput"), Some(Duration.Zero), Some(1))
  }
  
  "Attempt to locate existing element by link text with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`link text`, "User name", By.linkText("User name"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by link text at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`link text`, "User name", By.linkText("User name"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by partial link text" should "return the element" in {
    shouldFindWebElement(SelectorType.`partial link text`, "User", By.partialLinkText("User"), None, None)
  }

  "Attempt to locate existing element by partial link text at index 0" should "return the element" in {
    shouldFindWebElement(SelectorType.`partial link text`, "User", By.partialLinkText("User"), None, Some(0))
  }

  "Attempt to locate existing element by partial link text with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`partial link text`, "User", By.partialLinkText("User"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by partial link text at index 1 with no wait" should "return the element" in {
    shouldFindWebElement(SelectorType.`partial link text`, "User", By.partialLinkText("User"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by partial link text with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`partial link text`, "User", By.partialLinkText("User"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by partial link text at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement(SelectorType.`partial link text`, "User", By.partialLinkText("User"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by javascript" should "return the element" in {
    
    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)
    
    locator.locate(LocatorBinding("username", selectorType, lookup, None, None, ctx)) should be (mockWebElement)
    
    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verifyZeroInteractions(mockWebDriverTimeouts)

  }

  "Attempt to locate existing element by javascript at index 0" should "return the element" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(0), ctx)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verifyZeroInteractions(mockWebDriverTimeouts)

  }

  "Attempt to locate existing element by javascript at index 1" should "return the element" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementsByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    val elements = new util.ArrayList[WebElement]()
    elements.add(mockWebElement)
    elements.add(mockWebElement)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)

    doReturn(elements).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(1), ctx)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verifyZeroInteractions(mockWebDriverTimeouts)

  }

  "Attempt to locate existing element by javascript with no wait" should "return the element" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(Duration.Zero), None, ctx)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(200L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript at index 0 with no wait" should "return the element" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(Duration.Zero), Some(0), ctx)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(200L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript at index 1 with no wait" should "return the element" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    val elements = new util.ArrayList[WebElement]()
    elements.add(mockWebElement)
    elements.add(mockWebElement)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(elements).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(Duration.Zero), Some(1), ctx)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(200L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript with 2 second timeout" should "return the element" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(Duration.create(2, TimeUnit.SECONDS)), None, ctx)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(2000L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript at index 0 with 2 second timeout" should "return the element" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(Duration.create(2, TimeUnit.SECONDS)), Some(0), ctx)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(2000L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript at index 1 with 2 second timeout" should "return the element" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    val elements = new util.ArrayList[WebElement]()
    elements.add(mockWebElement)
    elements.add(mockWebElement)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(elements).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(Duration.create(2, TimeUnit.SECONDS)), Some(1), ctx)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(2000L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }
  
  "Timeout on locating element by javascript" should "throw not found error" in {
    
    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(null).when(mockWebDriver).executeScript(same(s"return $lookup"), anyVararg())
    
    intercept[WebElementNotFoundException] {
      locator.locate(LocatorBinding("username", selectorType, lookup, None, None, ctx))
    }
    
    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }

  "Timeout on locating element by javascriptat index 0" should "throw not found error" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(null).when(mockWebDriver).executeScript(same(s"return $lookup"), anyVararg())

    intercept[WebElementNotFoundException] {
      locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(0), ctx))
    }

    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }

  "Timeout on locating element by javascriptat index 1" should "throw error" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(null).when(mockWebDriver).executeScript(same(s"return $lookup"), anyVararg())

    intercept[Errors.WaitTimeoutException] {
      locator.locate(LocatorBinding("username", selectorType, lookup, None, Some(1), ctx))
    }

    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }
  
  private def shouldFindWebElement(selectorType: SelectorType.Value, lookup: String, by: By, timeout: Option[Duration], index: Option[Int]): Unit = {

    val envState = newEnvState
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    index match {
      case Some(idx) if idx > 0 =>
        when(mockWebDriver.findElements(by)).thenReturn(List(mockWebElement, mockWebElement, mockWebElement).asJava)
      case _ =>
        when(mockWebDriver.findElement(by)).thenReturn(mockWebElement)
    }

    when(mockWebElement.isDisplayed).thenReturn(true)

    when(mockWebDriver.findElement(By.id("container"))).thenReturn(mockContainerElement)
    when(mockContainerElement.getTagName).thenReturn("div")
    index match {
      case Some(idx) if idx > 0 =>
        when(mockContainerElement.findElements(by)).thenReturn(List(mockWebElement, mockWebElement, mockWebElement).asJava)
      case _ =>
        when(mockContainerElement.findElement(by)).thenReturn(mockWebElement)
    }
    envState.scopes.set("container/locator", SelectorType.id.toString)
    envState.scopes.set("container/locator/id", "container")
    
    when(mockWebDriver.findElement(By.id("iframe"))).thenReturn(mockIFrameElement)
    when(mockIFrameElement.getTagName).thenReturn("iframe")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.frame(mockIFrameElement)).thenReturn(mockWebDriver)
    envState.scopes.set("iframe/locator", SelectorType.id.toString)
    envState.scopes.set("iframe/locator/id", "iframe")
    
    when(mockWebDriver.findElement(By.id("frame"))).thenReturn(mockFrameElement)
    when(mockFrameElement.getTagName).thenReturn("frame")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.frame(mockFrameElement)).thenReturn(mockWebDriver)
    envState.scopes.set("frame/locator", SelectorType.id.toString)
    envState.scopes.set("frame/locator/id", "frame")

    locator.locate(LocatorBinding("username", selectorType, lookup, None, timeout, index, ctx)) should be (mockWebElement)
    locator.locate(LocatorBinding("username", selectorType, lookup, Some(new LocatorBinding("container", List(Selector(SelectorType.id, "container")), ctx)), timeout, index, ctx)) should be (mockWebElement)
    locator.locate(LocatorBinding("username", selectorType, lookup, Some(new LocatorBinding("iframe", List(Selector(SelectorType.id, "iframe")), ctx)), timeout, index, ctx)) should be (mockWebElement)
    locator.locate(LocatorBinding("username", selectorType, lookup, Some(new LocatorBinding("frame", List(Selector(SelectorType.id, "frame")), ctx)), timeout, index, ctx)) should be (mockWebElement)

    index match {
      case Some(idx) if idx > 0 =>
        verify(mockWebDriver, times(3)).findElements(by)
      case _ =>
        verify(mockWebDriver, times(3)).findElement(by)
    }

    timeout.foreach { t =>
      val expectedTimeout = if(t == Duration.Zero) 200L else t.toMillis
      verify(mockWebDriverTimeouts, times(4)).implicitlyWait(expectedTimeout, TimeUnit.MILLISECONDS)
      verify(mockWebDriverTimeouts, times(4)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)
    }
    if (timeout.isEmpty) {
      verifyZeroInteractions(mockWebDriverTimeouts)
    }
    
  }

  "Attempt to locate all non existent elements" should "return an empty list when empty array is returned" in {

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElements(By.cssSelector(".mname"))).thenReturn(new java.util.ArrayList[WebElement]())

    locator.locateAll(LocatorBinding("middleNames", SelectorType.`css selector`, ".mname", None, Some(0), ctx)) should be (Nil)
  }

  "Attempt to locate all non existent elements" should "return an empty list when null is returned" in {

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElements(By.cssSelector(".mname"))).thenReturn(null)

    locator.locateAll(LocatorBinding("middleNames", SelectorType.`css selector`, ".mname", None, Some(1), ctx)) should be (Nil)
  }

  "Attempt to locate existing elements by id" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.id, "uname", By.id("uname"), None)
  }

  "Attempt to locate existing elements by id with no wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.id, "uname", By.id("uname"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by id with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.id, "uname", By.id("uname"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by name" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.name, "uname", By.name("uname"), None)
  }

  "Attempt to locate existing elements by name with no wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.name, "uname", By.name("uname"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by name with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.name, "uname", By.name("uname"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by tag name" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`tag name`, "input", By.tagName("input"), None)
  }

  "Attempt to locate existing elements by tag name with no wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`tag name`, "input", By.tagName("input"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by tag name with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`tag name`, "input", By.tagName("input"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by css selector" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), None)
  }

  "Attempt to locate existing elements by css selector with no wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by css selector with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`css selector`, ":focus", By.cssSelector(":focus"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by xpath" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), None)
  }

  "Attempt to locate existing elements by xpath with no wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by xpath with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`xpath`, "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by class name" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`class name`, ".userinput", By.className(".userinput"), None)
  }

  "Attempt to locate existing elements by class name with no wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`class name`, ".userinput", By.className(".userinput"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by class name with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`class name`, ".userinput", By.className(".userinput"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by link text" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`link text`, "User name", By.linkText("User name"), None)
  }

  "Attempt to locate existing elements by link text with no wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`link text`, "User name", By.linkText("User name"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by link text with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`link text`, "User name", By.linkText("User name"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by partial link text" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`partial link text`, "User", By.partialLinkText("User"), None)
  }

  "Attempt to locate existing elements by partial link text with no wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`partial link text`, "User", By.partialLinkText("User"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by partial link text with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements(SelectorType.`partial link text`, "User", By.partialLinkText("User"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by javascript" should "return the elements" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementsByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    val mockWebElementsArrayList = new java.util.ArrayList[WebElement]()
    mockWebElementsArrayList.add(mockWebElements(0))
    mockWebElementsArrayList.add(mockWebElements(1))

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElementsArrayList).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locateAll(LocatorBinding("username", selectorType, lookup, None, None, ctx)) should be (mockWebElements)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verifyZeroInteractions(mockWebDriverTimeouts)

  }

  "Attempt to locate existing elements by javascript with no wait" should "return the elements" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementsByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    val mockWebElementsArrayList = new java.util.ArrayList[WebElement]()
    mockWebElementsArrayList.add(mockWebElements(0))
    mockWebElementsArrayList.add(mockWebElements(1))

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElementsArrayList).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locateAll(LocatorBinding("username", selectorType, lookup, None, Some(Duration.Zero), None, ctx)) should be (mockWebElements)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(200L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing elements by javascript with 2 second timeout" should "return the elements" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementsByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    val mockWebElementsArrayList = new java.util.ArrayList[WebElement]()
    mockWebElementsArrayList.add(mockWebElements(0))
    mockWebElementsArrayList.add(mockWebElements(1))

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElementsArrayList).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locateAll(LocatorBinding("username", selectorType, lookup, None, Some(Duration.create(2, TimeUnit.SECONDS)), None, ctx)) should be (mockWebElements)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(2000L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Timeout on locating elements by javascript" should "throw error" in {

    val selectorType = SelectorType.javascript
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(null).when(mockWebDriver).executeScript(same(s"return $lookup"), anyVararg())

    intercept[WebElementNotFoundException] {
      locator.locate(LocatorBinding("username", selectorType, lookup, None, None, ctx))
    }

    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }

  private def shouldFindAllWebElements(selectorType: SelectorType.Value, lookup: String, by: By, timeout: Option[Duration]): Unit = {

    val envState = newEnvState
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)

    val mockWebElementsJava = new java.util.ArrayList[WebElement]()
    mockWebElementsJava.add(mockWebElements(0))
    mockWebElementsJava.add(mockWebElements(1))

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElements(by)).thenReturn(mockWebElementsJava)
    when(mockWebElement.isDisplayed).thenReturn(true)

    when(mockWebDriver.findElement(By.id("container"))).thenReturn(mockContainerElement)
    when(mockContainerElement.getTagName).thenReturn("div")
    when(mockContainerElement.findElements(by)).thenReturn(mockWebElementsJava)
    envState.scopes.set("container/locator", SelectorType.id.toString)
    envState.scopes.set("container/locator/id", "container")

    when(mockWebDriver.findElement(By.id("iframe"))).thenReturn(mockIFrameElement)
    when(mockIFrameElement.getTagName).thenReturn("iframe")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.frame(mockIFrameElement)).thenReturn(mockWebDriver)
    envState.scopes.set("iframe/locator", SelectorType.id.toString)
    envState.scopes.set("iframe/locator/id", "iframe")

    when(mockWebDriver.findElement(By.id("frame"))).thenReturn(mockFrameElement)
    when(mockFrameElement.getTagName).thenReturn("frame")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.frame(mockFrameElement)).thenReturn(mockWebDriver)
    envState.scopes.set("frame/locator", SelectorType.id.toString)
    envState.scopes.set("frame/locator/id", "frame")

    locator.locateAll(LocatorBinding("username", selectorType, lookup, None, timeout, None, ctx)) should be (mockWebElements)
    locator.locateAll(LocatorBinding("username", selectorType, lookup, Some(new LocatorBinding("container", List(Selector(SelectorType.id, "container")), ctx)), timeout, None, ctx)) should be (mockWebElements)
    locator.locateAll(LocatorBinding("username", selectorType, lookup, Some(new LocatorBinding("iframe", List(Selector(SelectorType.id, "iframe")), ctx)), timeout, None, ctx)) should be (mockWebElements)
    locator.locateAll(LocatorBinding("username", selectorType, lookup, Some(new LocatorBinding("frame", List(Selector(SelectorType.id, "frame")), ctx)), timeout, None, ctx)) should be (mockWebElements)

    verify(mockWebDriver, times(3)).findElements(by)

    timeout.foreach { t =>
      val expectedTimeout = if(t == Duration.Zero) 200L else t.toMillis
      verify(mockWebDriverTimeouts, times(4)).implicitlyWait(expectedTimeout, TimeUnit.MILLISECONDS)
      verify(mockWebDriverTimeouts, times(4)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)
    }
    if (timeout.isEmpty) {
      verifyZeroInteractions(mockWebDriverTimeouts)
    }

  }

  "Attempt to locate existing element by three locators" should "return the element by first one" in {

    val selectors = List(
      Selector(SelectorType.id, "uname"),
      Selector(SelectorType.name, "username"),
      Selector(SelectorType.`class name`, ".usrname")
    )
    
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)
    val binding = new LocatorBinding("username", selectors, ctx)


    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(By.id("uname"))).thenReturn(mockWebElement)
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(binding) should be (mockWebElement)

    verify(mockWebDriver, times(1)).findElement(By.id("uname"))
    verify(mockWebDriver, times(0)).findElement(By.name("username"))
    verify(mockWebDriver, times(0)).findElement(By.className(".usrname"))

  }

  "Attempt to locate existing element by three locators" should "return the element by second one" in {

    val selectors = List(
      Selector(SelectorType.id, "uname"),
      Selector(SelectorType.name, "username"),
      Selector(SelectorType.`class name`, ".usrname")
    )

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)
    val binding = new LocatorBinding("username", selectors, ctx)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(By.id("uname"))).thenReturn(null)
    when(mockWebDriver.findElement(By.name("username"))).thenReturn(mockWebElement)
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(binding) should be (mockWebElement)

    verify(mockWebDriver, times(1)).findElement(By.id("uname"))
    verify(mockWebDriver, times(1)).findElement(By.name("username"))
    verify(mockWebDriver, times(0)).findElement(By.className(".usrname"))

  }

  "Attempt to locate existing element by three locators" should "return the element by third one" in {

    val selectors = List(
      Selector(SelectorType.id, "uname"),
      Selector(SelectorType.name, "username"),
      Selector(SelectorType.`class name`, ".usrname")
    )

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)
    val locator = newLocator(ctx)
    val binding = new LocatorBinding("username", selectors, ctx)


    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(By.id("uname"))).thenReturn(null)
    when(mockWebDriver.findElement(By.name("username"))).thenReturn(null)
    when(mockWebDriver.findElement(By.className(".usrname"))).thenReturn(mockWebElement)
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(binding) should be (mockWebElement)

    verify(mockWebDriver, times(1)).findElement(By.id("uname"))
    verify(mockWebDriver, times(1)).findElement(By.name("username"))
    verify(mockWebDriver, times(1)).findElement(By.className(".usrname"))

  }

  "Single element locator bindings with JS equivalents" should "resolve" in {

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)

    val container = Some(new LocatorBinding("container", List(Selector(SelectorType.id, "container")), ctx))

    LocatorBinding("user name", SelectorType.id, "username", None, None, ctx).jsEquivalent.toString should be (
      """user name [locator: javascript=document.getElementById('username')]"""
    )

    LocatorBinding("user name", SelectorType.id, "username", container, None, ctx).jsEquivalent.toString should be (
      """user name [locator: javascript=document.getElementById('username') in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name", SelectorType.name, "username", None, None, ctx).jsEquivalent.toString should be (
      """user name [locator: javascript=document.getElementsByName('username')[0]]"""
    )
    
    LocatorBinding("user name", SelectorType.name, "username", container, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.getElementsByName('username')[0] in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name", SelectorType.`class name`, "username", None, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.getElementsByClassName('username')[0]]"""
    )
    
    LocatorBinding("user name", SelectorType.`class name`, "username", container, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.getElementsByClassName('username')[0] in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name", SelectorType.`css selector`, ".username", None, None, ctx).jsEquivalent.toString should be (
     "user name [locator: javascript=document.querySelector('.username')]"
    )
    
    LocatorBinding("user name", SelectorType.`css selector`, ".username", container, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.querySelector('.username') in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name", SelectorType.`tag name`, "input", None, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.getElementsByTagName('input')[0]]"""
    )
    
    LocatorBinding("user name", SelectorType.`tag name`, "input", container, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.getElementsByTagName('input')[0] in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name", SelectorType.`xpath`, "//html/body/div/input", None, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.evaluate('\/\/html\/body\/div\/input', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue]"""
    )
    
    LocatorBinding("user link", SelectorType.`link text`, "user", None, None, ctx).jsEquivalent.toString should be (
     """user link [locator: javascript=document.evaluate('//a[text()="user"]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue]"""
    )
    
    LocatorBinding("user link", SelectorType.`link text`, "user", container, None, ctx).jsEquivalent.toString should be (
     """user link [locator: javascript=document.evaluate('//a[text()="user"]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue in container [locator: id=container]]"""
    )
    
    LocatorBinding("user link", SelectorType.`partial link text`, "user", None, None, ctx).jsEquivalent.toString should be (
     """user link [locator: javascript=document.evaluate('//a[contains(text(), "user")]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue]"""
    )
    
    LocatorBinding("user link", SelectorType.`partial link text`, "user", container, None, ctx).jsEquivalent.toString should be (
     """user link [locator: javascript=document.evaluate('//a[contains(text(), "user")]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name", SelectorType.javascript, "document.getElementById('user')", None, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.getElementById('user')]"""
    )
    
    LocatorBinding("user name", SelectorType.javascript, "document.getElementById('user')", container, None, ctx).jsEquivalent.toString should be (
     """user name [locator: javascript=document.getElementById('user') in container [locator: id=container]]"""
    )

  }

  "Multi element locator bindings with JS equivalents" should "resolve" in {

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val ctx = newCtx(None, mockWebDriver)

    val container = Some(new LocatorBinding("container", List(Selector(SelectorType.id, "container")), ctx))

    LocatorBinding("user name/list", SelectorType.id, "username", None, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.querySelectorAll('#username')]"""
    )
    
    LocatorBinding("user name/list", SelectorType.id, "username", container, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.querySelectorAll('#username') in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name/list", SelectorType.name, "username", None, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.getElementsByName('username')]"""
    )
    
    LocatorBinding("user name/list", SelectorType.name, "username", container, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.getElementsByName('username') in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name/list", SelectorType.`class name`, "username", None, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.getElementsByClassName('username')]"""
    )
    
    LocatorBinding("user name/list", SelectorType.`class name`, "username", container, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.getElementsByClassName('username') in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name/list", SelectorType.`css selector`, ".username", None, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.querySelectorAll('.username')]"""
    )
    
    LocatorBinding("user name/list", SelectorType.`css selector`, ".username", container, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.querySelectorAll('.username') in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name/list", SelectorType.`tag name`, "input", None, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.getElementsByTagName('input')]"""
    )
    
    LocatorBinding("user name/list", SelectorType.`tag name`, "input", container, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.getElementsByTagName('input') in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name/list", SelectorType.`xpath`, "//html/body/div/input", None, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.evaluate('\/\/html\/body\/div\/input', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null)]"""
    )
    
    LocatorBinding("user link/list", SelectorType.`link text`, "user", None, None, ctx).jsEquivalent.toString should be (
     """user link/list [locator: javascript=document.evaluate('//a[text()="user"]', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null)]"""
    )
    
    LocatorBinding("user link/list", SelectorType.`link text`, "user", container, None, ctx).jsEquivalent.toString should be (
     """user link/list [locator: javascript=document.evaluate('//a[text()="user"]', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null) in container [locator: id=container]]"""
    )
    
    LocatorBinding("user link/list", SelectorType.`partial link text`, "user", None, None, ctx).jsEquivalent.toString should be (
     """user link/list [locator: javascript=document.evaluate('//a[contains(text(), "user")]', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null)]"""
    )
    
    LocatorBinding("user link/list", SelectorType.`partial link text`, "user", container, None, ctx).jsEquivalent.toString should be (
     """user link/list [locator: javascript=document.evaluate('//a[contains(text(), "user")]', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null) in container [locator: id=container]]"""
    )
    
    LocatorBinding("user name/list", SelectorType.javascript, "document.getElementsByName('user')", None, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.getElementsByName('user')]"""
    )
    
    LocatorBinding("user name/list", SelectorType.javascript, "document.getElementsByName('user')", container, None, ctx).jsEquivalent.toString should be (
     """user name/list [locator: javascript=document.getElementsByName('user') in container [locator: id=container]]"""
    )

  }

  private def newLocator(ctx: WebContext): WebElementLocator = {
    new WebElementLocator(ctx)
  }

  private def newCtx(envState: Option[EnvState], mockWebDriver: WebDriver): WebContext = {
    val driverManager = new DriverManager() {
      override private [engine] def loadWebDriver: WebDriver = mockWebDriver
    }
    new WebContext(GwenOptions(), envState.getOrElse(newEnvState), driverManager)
  }

  private def newEnvState: EnvState = EnvState()
  
}