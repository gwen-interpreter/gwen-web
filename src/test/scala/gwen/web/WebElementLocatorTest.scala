/*
 * Copyright 2014-2017 Brady Wood, Branko Juric
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

import java.util
import java.util.concurrent.TimeUnit

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
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import org.scalatest.mockito.MockitoSugar
import gwen.eval.ScopedDataStack
import gwen.eval.GwenOptions
import gwen.Settings
import gwen.web.errors.{LocatorBindingException, WaitTimeoutException}
import org.openqa.selenium.WebDriver.{Options, TargetLocator, Timeouts}
import org.openqa.selenium.NoSuchElementException
import org.mockito.Matchers.{anyVararg, same}

import collection.JavaConverters._
import scala.concurrent.duration.Duration

class WebElementLocatorTest extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

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
  private var mockDriverManager: DriverManager = _

  override def beforeEach(): Unit = {
    mockWebElement = mock[WebElement]
    mockWebElements = List(mock[WebElement], mock[WebElement])
    mockContainerElement = mock[WebElement]
    mockIFrameElement = mock[WebElement]
    mockFrameElement = mock[WebElement]
    mockTargetLocator = mock[TargetLocator]
    mockWebDriverOptions = mock[WebDriver.Options]
    mockWebDriverTimeouts = mock[WebDriver.Timeouts]
    mockDriverManager = mock[DriverManager]
  }

  "Attempt to locate non existent element" should "throw no such element error" in {

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(By.id("mname"))).thenReturn(null)
    
    val e = intercept[NoSuchElementException] {
      locator.locate(LocatorBinding("middleName", "id", "mname", None, None))
    }
    e.getMessage should startWith ("Could not locate middleName by (id: mname)")
  }
  
  "Attempt to locate existing element by id" should "return the element" in {
    shouldFindWebElement("id", "uname", By.id("uname"), None, None)
  }

  "Attempt to locate existing element by id at index 0" should "return the element" in {
    shouldFindWebElement("id", "uname", By.id("uname"), None, Some(0))
  }

  "Attempt to locate existing element by id with no wait" should "return the element" in {
    shouldFindWebElement("id", "uname", By.id("uname"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by id at index 1 with no wait" should "return the element" in {
    shouldFindWebElement("id", "uname", By.id("uname"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by id with 2 second timeout" should "return the element" in {
    shouldFindWebElement("id", "uname", By.id("uname"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by id at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement("id", "uname", By.id("uname"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by name" should "return the element" in {
    shouldFindWebElement("name", "uname", By.name("uname"), None, None)
  }

  "Attempt to locate existing element by name at index 0" should "return the element" in {
    shouldFindWebElement("name", "uname", By.name("uname"), None, Some(0))
  }

  "Attempt to locate existing element by name with no wait" should "return the element" in {
    shouldFindWebElement("name", "uname", By.name("uname"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by name at index 1 with no wait" should "return the element" in {
    shouldFindWebElement("name", "uname", By.name("uname"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by name with 2 second timeout" should "return the element" in {
    shouldFindWebElement("name", "uname", By.name("uname"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by name at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement("name", "uname", By.name("uname"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by tag name" should "return the element" in {
    shouldFindWebElement("tag name", "input", By.tagName("input"), None, None)
  }

  "Attempt to locate existing element by tag name at index 0" should "return the element" in {
    shouldFindWebElement("tag name", "input", By.tagName("input"), None, Some(0))
  }

  "Attempt to locate existing element by tag name with no wait" should "return the element" in {
    shouldFindWebElement("tag name", "input", By.tagName("input"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by tag name at index 1 with no wait" should "return the element" in {
    shouldFindWebElement("tag name", "input", By.tagName("input"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by tag name with 2 second timeout" should "return the element" in {
    shouldFindWebElement("tag name", "input", By.tagName("input"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by tag name at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement("tag name", "input", By.tagName("input"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by css selector" should "return the element" in {
    shouldFindWebElement("css selector", ":focus", By.cssSelector(":focus"), None, None)
  }

  "Attempt to locate existing element by css selector at index 0" should "return the element" in {
    shouldFindWebElement("css selector", ":focus", By.cssSelector(":focus"), None, Some(0))
  }

  "Attempt to locate existing element by css selector with no wait" should "return the element" in {
    shouldFindWebElement("css selector", ":focus", By.cssSelector(":focus"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by css selector at index 1 with no wait" should "return the element" in {
    shouldFindWebElement("css selector", ":focus", By.cssSelector(":focus"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by css selector with 2 second timeout" should "return the element" in {
    shouldFindWebElement("css selector", ":focus", By.cssSelector(":focus"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by css selector at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement("css selector", ":focus", By.cssSelector(":focus"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by xpath" should "return the element" in {
    shouldFindWebElement("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), None, None)
  }

  "Attempt to locate existing element by xpath at index 0" should "return the element" in {
    shouldFindWebElement("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), None, Some(0))
  }

  "Attempt to locate existing element by xpath with no wait" should "return the element" in {
    shouldFindWebElement("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by xpath at index 1 with no wait" should "return the element" in {
    shouldFindWebElement("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by xpath with 2 second timeout" should "return the element" in {
    shouldFindWebElement("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by xpath at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by class name" should "return the element" in {
    shouldFindWebElement("class name", ".userinput", By.className(".userinput"), None, None)
  }

  "Attempt to locate existing element by class name at index 0" should "return the element" in {
    shouldFindWebElement("class name", ".userinput", By.className(".userinput"), None, Some(0))
  }

  "Attempt to locate existing element by class name with no wait" should "return the element" in {
    shouldFindWebElement("class name", ".userinput", By.className(".userinput"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by class name at index 1 with no wait" should "return the element" in {
    shouldFindWebElement("class name", ".userinput", By.className(".userinput"), Some(Duration.Zero), Some(1))
  }
  
  "Attempt to locate existing element by link text with 2 second timeout" should "return the element" in {
    shouldFindWebElement("link text", "User name", By.linkText("User name"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by link text at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement("link text", "User name", By.linkText("User name"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by partial link text" should "return the element" in {
    shouldFindWebElement("partial link text", "User", By.partialLinkText("User"), None, None)
  }

  "Attempt to locate existing element by partial link text at index 0" should "return the element" in {
    shouldFindWebElement("partial link text", "User", By.partialLinkText("User"), None, Some(0))
  }

  "Attempt to locate existing element by partial link text with no wait" should "return the element" in {
    shouldFindWebElement("partial link text", "User", By.partialLinkText("User"), Some(Duration.Zero), None)
  }

  "Attempt to locate existing element by partial link text at index 1 with no wait" should "return the element" in {
    shouldFindWebElement("partial link text", "User", By.partialLinkText("User"), Some(Duration.Zero), Some(1))
  }

  "Attempt to locate existing element by partial link text with 2 second timeout" should "return the element" in {
    shouldFindWebElement("partial link text", "User", By.partialLinkText("User"), Some(Duration.create(2, TimeUnit.SECONDS)), None)
  }

  "Attempt to locate existing element by partial link text at index 2 with 2 second timeout" should "return the element" in {
    shouldFindWebElement("partial link text", "User", By.partialLinkText("User"), Some(Duration.create(2, TimeUnit.SECONDS)), Some(2))
  }
  
  "Attempt to locate existing element by javascript" should "return the element" in {
    
    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)
    
    locator.locate(LocatorBinding("username", locatorType, lookup, None, None)) should be (mockWebElement)
    
    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verifyZeroInteractions(mockWebDriverTimeouts)

  }

  "Attempt to locate existing element by javascript at index 0" should "return the element" in {

    val locatorType = "javascript"
    val lookup = "document.getElementByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(0))) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verifyZeroInteractions(mockWebDriverTimeouts)

  }

  "Attempt to locate existing element by javascript at index 1" should "return the element" in {

    val locatorType = "javascript"
    val lookup = "document.getElementsByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    val elements = new util.ArrayList[WebElement]()
    elements.add(mockWebElement)
    elements.add(mockWebElement)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)

    doReturn(elements).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(1))) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verifyZeroInteractions(mockWebDriverTimeouts)

  }

  "Attempt to locate existing element by javascript with no wait" should "return the element" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(Duration.Zero), None)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(200L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript at index 0 with no wait" should "return the element" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(Duration.Zero), Some(0))) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(200L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript at index 1 with no wait" should "return the element" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    val elements = new util.ArrayList[WebElement]()
    elements.add(mockWebElement)
    elements.add(mockWebElement)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(elements).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(Duration.Zero), Some(1))) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(200L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript with 2 second timeout" should "return the element" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(Duration.create(2, TimeUnit.SECONDS)), None)) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(2000L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript at index 0 with 2 second timeout" should "return the element" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(Duration.create(2, TimeUnit.SECONDS)), Some(0))) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(2000L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing element by javascript at index 1 with 2 second timeout" should "return the element" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    val elements = new util.ArrayList[WebElement]()
    elements.add(mockWebElement)
    elements.add(mockWebElement)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(elements).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(Duration.create(2, TimeUnit.SECONDS)), Some(1))) should be (mockWebElement)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(2000L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }
  
  "Timeout on locating element by javascript" should "throw error" in {
    
    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(null).when(mockWebDriver).executeScript(same(s"return $lookup"), anyVararg())
    
    intercept[WaitTimeoutException] {
      locator.locate(LocatorBinding("username", locatorType, lookup, None, None))
    }
    
    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }

  "Timeout on locating element by javascriptat index 0" should "throw error" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(null).when(mockWebDriver).executeScript(same(s"return $lookup"), anyVararg())

    intercept[WaitTimeoutException] {
      locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(0)))
    }

    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }

  "Timeout on locating element by javascriptat index 1" should "throw error" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(null).when(mockWebDriver).executeScript(same(s"return $lookup"), anyVararg())

    intercept[WaitTimeoutException] {
      locator.locate(LocatorBinding("username", locatorType, lookup, None, Some(1)))
    }

    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }
  
  private def shouldFindWebElement(locatorType: String, lookup: String, by: By, timeout: Option[Duration], index: Option[Int]) {

    val env = newEnv
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(Some(env), mockWebDriver)
    
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
    env.scopes.set("container/locator", "id")
    env.scopes.set("container/locator/id", "container")
    
    when(mockWebDriver.findElement(By.id("iframe"))).thenReturn(mockIFrameElement)
    when(mockIFrameElement.getTagName).thenReturn("iframe")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.frame(mockIFrameElement)).thenReturn(mockWebDriver)
    env.scopes.set("iframe/locator", "id")
    env.scopes.set("iframe/locator/id", "iframe")
    
    when(mockWebDriver.findElement(By.id("frame"))).thenReturn(mockFrameElement)
    when(mockFrameElement.getTagName).thenReturn("frame")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.frame(mockFrameElement)).thenReturn(mockWebDriver)
    env.scopes.set("frame/locator", "id")
    env.scopes.set("frame/locator/id", "frame")

    locator.locate(LocatorBinding("username", locatorType, lookup, None, timeout, index)) should be (mockWebElement)
    locator.locate(LocatorBinding("username", locatorType, lookup, Some("container"), timeout, index)) should be (mockWebElement)
    locator.locate(LocatorBinding("username", locatorType, lookup, Some("iframe"), timeout, index)) should be (mockWebElement)
    locator.locate(LocatorBinding("username", locatorType, lookup, Some("frame"), timeout, index)) should be (mockWebElement)

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
  
  "Attempt to locate element with unsupported locator" should "throw unsupported locator error" in {

    val env = newEnv
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(Some(env), mockWebDriver)

    env.scopes.addScope("login").set("username/id", "unknown").set("username/id/unknown", "funkyness")
    val e = intercept[LocatorBindingException] {
      locator.locate(LocatorBinding("username", "unknown", "funkiness", None, None))
    }
    e.getMessage should be ("Could not locate username: unsupported locator: (unknown: funkiness)")
  }

  "Attempt to locate all non existent elements" should "return an empty list when empty array is returned" in {

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElements(By.cssSelector(".mname"))).thenReturn(new java.util.ArrayList[WebElement]())

    locator.locateAll(LocatorBinding("middleNames", "css selector", ".mname", None, Some(0))) should be (Nil)
  }

  "Attempt to locate all non existent elements" should "return an empty list when null is returned" in {

    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElements(By.cssSelector(".mname"))).thenReturn(null)

    locator.locateAll(LocatorBinding("middleNames", "css selector", ".mname", None, Some(1))) should be (Nil)
  }

  "Attempt to locate existing elements by id" should "return the elements" in {
    shouldFindAllWebElements("id", "uname", By.id("uname"), None)
  }

  "Attempt to locate existing elements by id with no wait" should "return the elements" in {
    shouldFindAllWebElements("id", "uname", By.id("uname"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by id with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements("id", "uname", By.id("uname"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by name" should "return the elements" in {
    shouldFindAllWebElements("name", "uname", By.name("uname"), None)
  }

  "Attempt to locate existing elements by name with no wait" should "return the elements" in {
    shouldFindAllWebElements("name", "uname", By.name("uname"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by name with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements("name", "uname", By.name("uname"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by tag name" should "return the elements" in {
    shouldFindAllWebElements("tag name", "input", By.tagName("input"), None)
  }

  "Attempt to locate existing elements by tag name with no wait" should "return the elements" in {
    shouldFindAllWebElements("tag name", "input", By.tagName("input"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by tag name with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements("tag name", "input", By.tagName("input"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by css selector" should "return the elements" in {
    shouldFindAllWebElements("css selector", ":focus", By.cssSelector(":focus"), None)
  }

  "Attempt to locate existing elements by css selector with no wait" should "return the elements" in {
    shouldFindAllWebElements("css selector", ":focus", By.cssSelector(":focus"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by css selector with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements("css selector", ":focus", By.cssSelector(":focus"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by xpath" should "return the elements" in {
    shouldFindAllWebElements("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), None)
  }

  "Attempt to locate existing elements by xpath with no wait" should "return the elements" in {
    shouldFindAllWebElements("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by xpath with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by class name" should "return the elements" in {
    shouldFindAllWebElements("class name", ".userinput", By.className(".userinput"), None)
  }

  "Attempt to locate existing elements by class name with no wait" should "return the elements" in {
    shouldFindAllWebElements("class name", ".userinput", By.className(".userinput"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by class name with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements("class name", ".userinput", By.className(".userinput"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by link text" should "return the elements" in {
    shouldFindAllWebElements("link text", "User name", By.linkText("User name"), None)
  }

  "Attempt to locate existing elements by link text with no wait" should "return the elements" in {
    shouldFindAllWebElements("link text", "User name", By.linkText("User name"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by link text with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements("link text", "User name", By.linkText("User name"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by partial link text" should "return the elements" in {
    shouldFindAllWebElements("partial link text", "User", By.partialLinkText("User"), None)
  }

  "Attempt to locate existing elements by partial link text with no wait" should "return the elements" in {
    shouldFindAllWebElements("partial link text", "User", By.partialLinkText("User"), Some(Duration.Zero))
  }

  "Attempt to locate existing elements by partial link text with 2 second wait" should "return the elements" in {
    shouldFindAllWebElements("partial link text", "User", By.partialLinkText("User"), Some(Duration(2, TimeUnit.SECONDS)))
  }

  "Attempt to locate existing elements by javascript" should "return the elements" in {

    val locatorType = "javascript"
    val lookup = "document.getElementsByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    val mockWebElementsArrayList = new java.util.ArrayList[WebElement]()
    mockWebElementsArrayList.add(mockWebElements(0))
    mockWebElementsArrayList.add(mockWebElements(1))

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElementsArrayList).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locateAll(LocatorBinding("username", locatorType, lookup, None, None)) should be (mockWebElements)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verifyZeroInteractions(mockWebDriverTimeouts)

  }

  "Attempt to locate existing elements by javascript with no wait" should "return the elements" in {

    val locatorType = "javascript"
    val lookup = "document.getElementsByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    val mockWebElementsArrayList = new java.util.ArrayList[WebElement]()
    mockWebElementsArrayList.add(mockWebElements(0))
    mockWebElementsArrayList.add(mockWebElements(1))

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElementsArrayList).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locateAll(LocatorBinding("username", locatorType, lookup, None, Some(Duration.Zero), None)) should be (mockWebElements)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(200L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Attempt to locate existing elements by javascript with 2 second timeout" should "return the elements" in {

    val locatorType = "javascript"
    val lookup = "document.getElementsByName('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    val mockWebElementsArrayList = new java.util.ArrayList[WebElement]()
    mockWebElementsArrayList.add(mockWebElements(0))
    mockWebElementsArrayList.add(mockWebElements(1))

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElementsArrayList).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed).thenReturn(true)

    locator.locateAll(LocatorBinding("username", locatorType, lookup, None, Some(Duration.create(2, TimeUnit.SECONDS)), None)) should be (mockWebElements)

    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(2000L, TimeUnit.MILLISECONDS)
    verify(mockWebDriverTimeouts, times(1)).implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)

  }

  "Timeout on locating elements by javascript" should "throw error" in {

    val locatorType = "javascript"
    val lookup = "document.getElementById('username')"
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(None, mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(null).when(mockWebDriver).executeScript(same(s"return $lookup"), anyVararg())

    intercept[WaitTimeoutException] {
      locator.locate(LocatorBinding("username", locatorType, lookup, None, None))
    }

    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }

  private def shouldFindAllWebElements(locatorType: String, lookup: String, by: By, timeout: Option[Duration]) {

    val env = newEnv
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(Some(env), mockWebDriver)

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
    env.scopes.set("container/locator", "id")
    env.scopes.set("container/locator/id", "container")

    when(mockWebDriver.findElement(By.id("iframe"))).thenReturn(mockIFrameElement)
    when(mockIFrameElement.getTagName).thenReturn("iframe")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.frame(mockIFrameElement)).thenReturn(mockWebDriver)
    env.scopes.set("iframe/locator", "id")
    env.scopes.set("iframe/locator/id", "iframe")

    when(mockWebDriver.findElement(By.id("frame"))).thenReturn(mockFrameElement)
    when(mockFrameElement.getTagName).thenReturn("frame")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.frame(mockFrameElement)).thenReturn(mockWebDriver)
    env.scopes.set("frame/locator", "id")
    env.scopes.set("frame/locator/id", "frame")

    locator.locateAll(LocatorBinding("username", locatorType, lookup, None, timeout, None)) should be (mockWebElements)
    locator.locateAll(LocatorBinding("username", locatorType, lookup, Some("container"), timeout, None)) should be (mockWebElements)
    locator.locateAll(LocatorBinding("username", locatorType, lookup, Some("iframe"), timeout, None)) should be (mockWebElements)
    locator.locateAll(LocatorBinding("username", locatorType, lookup, Some("frame"), timeout, None)) should be (mockWebElements)

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

    val locators = List(
      Locator("id", "uname"),
      Locator("name", "username"),
      Locator("class name", ".usrname")
    )
    val binding = LocatorBinding("username", locators)

    val env = newEnv
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(Some(env), mockWebDriver)

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

    val locators = List(
      Locator("id", "uname"),
      Locator("name", "username"),
      Locator("class name", ".usrname")
    )
    val binding = LocatorBinding("username", locators)

    val env = newEnv
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(Some(env), mockWebDriver)

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

    val locators = List(
      Locator("id", "uname"),
      Locator("name", "username"),
      Locator("class name", ".usrname")
    )
    val binding = LocatorBinding("username", locators)

    val env = newEnv
    val mockWebDriver: FirefoxDriver = mock[FirefoxDriver]
    val locator = newLocator(Some(env), mockWebDriver)

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

    val container = Some("container")

    LocatorBinding("user name", "id", "username", None, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.getElementById('username')", None, None)
    }
    LocatorBinding("user name", "id", "username", container, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.getElementById('username')", container, None)
    }
    LocatorBinding("user name", "name", "username", None, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.getElementsByName('username')[0]", None, None)
    }
    LocatorBinding("user name", "name", "username", container, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.getElementsByName('username')[0]", container, None)
    }
    LocatorBinding("user name", "class name", "username", None, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.getElementsByClassName('username')[0]", None, None)
    }
    LocatorBinding("user name", "class name", "username", container, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.getElementsByClassName('username')[0]", container, None)
    }
    LocatorBinding("user name", "css selector", ".username", None, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.querySelector('.username')", None, None)
    }
    LocatorBinding("user name", "css selector", ".username", container, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.querySelector('.username')", container, None)
    }
    LocatorBinding("user name", "tag name", "input", None, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.getElementsByTagName('input')[0]", None, None)
    }
    LocatorBinding("user name", "tag name", "input", container, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", "document.getElementsByTagName('input')[0]", container, None)
    }
    LocatorBinding("user name", "xpath", "//html/body/div/input", None, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", """document.evaluate('\/\/html\/body\/div\/input', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue""", None, None)
    }
    LocatorBinding("user link", "link text", "user", None, None).jsEquivalent should be {
     LocatorBinding("user link", "javascript", """document.evaluate('//a[text()="user"]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue""", None, None)
    }
    LocatorBinding("user link", "link text", "user", container, None).jsEquivalent should be {
     LocatorBinding("user link", "javascript", """document.evaluate('//a[text()="user"]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue""", container, None)
    }
    LocatorBinding("user link", "partial link text", "user", None, None).jsEquivalent should be {
     LocatorBinding("user link", "javascript", """document.evaluate('//a[contains(text(), "user")]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue""", None, None)
    }
    LocatorBinding("user link", "partial link text", "user", container, None).jsEquivalent should be {
     LocatorBinding("user link", "javascript", """document.evaluate('//a[contains(text(), "user")]', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue""", container, None)
    }
    LocatorBinding("user name", "javascript", "document.getElementById('user')", None, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", """document.getElementById('user')""", None, None)
    }
    LocatorBinding("user name", "javascript", "document.getElementById('user')", container, None).jsEquivalent should be {
     LocatorBinding("user name", "javascript", """document.getElementById('user')""", container, None)
    }

  }

  "Multi element locator bindings with JS equivalents" should "resolve" in {

    val container = Some("container")

    LocatorBinding("user name/list", "id", "username", None, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.querySelectorAll('#username')", None, None)
    }
    LocatorBinding("user name/list", "id", "username", container, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.querySelectorAll('#username')", container, None)
    }
    LocatorBinding("user name/list", "name", "username", None, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.getElementsByName('username')", None, None)
    }
    LocatorBinding("user name/list", "name", "username", container, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.getElementsByName('username')", container, None)
    }
    LocatorBinding("user name/list", "class name", "username", None, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.getElementsByClassName('username')", None, None)
    }
    LocatorBinding("user name/list", "class name", "username", container, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.getElementsByClassName('username')", container, None)
    }
    LocatorBinding("user name/list", "css selector", ".username", None, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.querySelectorAll('.username')", None, None)
    }
    LocatorBinding("user name/list", "css selector", ".username", container, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.querySelectorAll('.username')", container, None)
    }
    LocatorBinding("user name/list", "tag name", "input", None, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.getElementsByTagName('input')", None, None)
    }
    LocatorBinding("user name/list", "tag name", "input", container, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", "document.getElementsByTagName('input')", container, None)
    }
    LocatorBinding("user name/list", "xpath", "//html/body/div/input", None, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", """document.evaluate('\/\/html\/body\/div\/input', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null)""", None, None)
    }
    LocatorBinding("user link/list", "link text", "user", None, None).jsEquivalent should be {
     LocatorBinding("user link/list", "javascript", """document.evaluate('//a[text()="user"]', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null)""", None, None)
    }
    LocatorBinding("user link/list", "link text", "user", container, None).jsEquivalent should be {
     LocatorBinding("user link/list", "javascript", """document.evaluate('//a[text()="user"]', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null)""", container, None)
    }
    LocatorBinding("user link/list", "partial link text", "user", None, None).jsEquivalent should be {
     LocatorBinding("user link/list", "javascript", """document.evaluate('//a[contains(text(), "user")]', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null)""", None, None)
    }
    LocatorBinding("user link/list", "partial link text", "user", container, None).jsEquivalent should be {
     LocatorBinding("user link/list", "javascript", """document.evaluate('//a[contains(text(), "user")]', document, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE, null)""", container, None)
    }
    LocatorBinding("user name/list", "javascript", "document.getElementsByName('user')", None, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", """document.getElementsByName('user')""", None, None)
    }
    LocatorBinding("user name/list", "javascript", "document.getElementsByName('user')", container, None).jsEquivalent should be {
     LocatorBinding("user name/list", "javascript", """document.getElementsByName('user')""", container, None)
    }

  }
  
  private def newLocator(env: Option[WebEnvContext], webDriver: WebDriver): WebElementLocator =
    new WebContext(env.getOrElse(newEnv), mockDriverManager) {
     override def withWebDriver[T](function: WebDriver => T)(implicit takeScreenShot: Boolean = false): Option[T] =
       Option(function(webDriver))
  }

  private def newEnv: WebEnvContext = new WebEnvContext(GwenOptions(), new ScopedDataStack())
  
}