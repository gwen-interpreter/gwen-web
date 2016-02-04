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

import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.openqa.selenium.By
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar
import gwen.eval.ScopedDataStack
import gwen.eval.GwenOptions
import org.openqa.selenium.WebDriver.TargetLocator

class WebElementLocatorTest extends FlatSpec with Matchers with MockitoSugar with WebElementLocator {

  val mockWebDriver = mock[FirefoxDriver]
  val mockWebElement = mock[WebElement]
  val mockContainerElement = mock[WebElement]
  val mockIFrameElement = mock[WebElement]
  val mockFrameElement = mock[WebElement]
  val mockTargetLocator = mock[TargetLocator]
  val mockWebDriverOptions = mock[WebDriver.Options]
  val mockWebDriverTimeouts = mock[WebDriver.Timeouts]
  
  "Attempt to locate non existent element" should "throw no such element error" in {
    
    val env = newEnv
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(By.id("mname"))).thenReturn(null)
    
    val e = intercept[NoSuchElementException] {
      locate(env, LocatorBinding("middleName", "id", "mname", None))
    }
    e.getMessage should be ("Web element not found: middleName")
  }
  
  "Attempt to locate existing element by id" should "return the element" in {
    shouldFindWebElement("id", "uname", By.id("uname"))
  }
  
  "Attempt to locate existing element by name" should "return the element" in {
    shouldFindWebElement("name", "uname", By.name("uname"))
  }
  
  "Attempt to locate existing element by tag name" should "return the element" in {
    shouldFindWebElement("tag name", "input", By.tagName("input"))
  }
  
  "Attempt to locate existing element by css selector" should "return the element" in {
    shouldFindWebElement("css selector", ":focus", By.cssSelector(":focus"))
  }
  
  "Attempt to locate existing element by xpath" should "return the element" in {
    shouldFindWebElement("xpath", "//input[name='uname']", By.xpath("//input[name='uname']"))
  }
  
  "Attempt to locate existing element by class name" should "return the element" in {
    shouldFindWebElement("class name", ".userinput", By.className(".userinput"))
  }
  
  "Attempt to locate existing element by link text" should "return the element" in {
    shouldFindWebElement("link text", "User name", By.linkText("User name"))
  }
  
  "Attempt to locate existing element by partial link text" should "return the element" in {
    shouldFindWebElement("partial link text", "User", By.partialLinkText("User"))
  }
  
  "Attempt to locate existing element by javascript" should "return the element" in {
    
    val locator = "javascript"
    val lookup = "document.getElementById('username')"
    val env = newEnv
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(s"return $lookup")
    when(mockWebElement.isDisplayed()).thenReturn(true);
    
    locate(env, LocatorBinding("username", locator, lookup, None)) should be (mockWebElement)
    
    verify(mockWebDriver, times(1)).executeScript(s"return $lookup")

  }
  
  "Timeout on locating element by javascript" should "throw error" in {
    
    val locator = "javascript"
    val lookup = "document.getElementById('username')"
    val env = newEnv
    
    val timeoutError = new TimeoutException();
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doThrow(timeoutError).when(mockWebDriver).executeScript(s"return $lookup")
    
    val e = intercept[TimeoutOnWaitException] {
      locate(env, new LocatorBinding("username", locator, lookup, None))
    }
    e.getMessage should be ("Timed out waiting.")
    
    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }
  
  "Timeout on locating optional element by javascript" should "return None" in {
    
    val locator = "javascript"
    val lookup = "document.getElementById('username')"
    val env = newEnv
    
    val timeoutError = new TimeoutException();
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doThrow(timeoutError).when(mockWebDriver).executeScript(s"return $lookup")
    
    verify(mockWebDriver, atLeastOnce()).executeScript(s"return $lookup")

  }
  
  private def shouldFindWebElement(locator: String, lookup: String, by: By) {
    
    val env = newEnv
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(by)).thenReturn(mockWebElement)
    when(mockWebElement.isDisplayed()).thenReturn(true)
    
    when(mockWebDriver.findElement(By.id("container"))).thenReturn(mockContainerElement)
    when(mockContainerElement.getTagName).thenReturn("div")
    when(mockContainerElement.findElement(by)).thenReturn(mockWebElement)
    env.scopes.set("container/locator", "id")
    env.scopes.set("container/locator/id", "container")
    
    when(mockWebDriver.findElement(By.id("iframe"))).thenReturn(mockIFrameElement)
    when(mockIFrameElement.getTagName).thenReturn("iframe")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator);
    when(mockTargetLocator.frame(mockIFrameElement)).thenReturn(mockWebDriver)
    env.scopes.set("iframe/locator", "id")
    env.scopes.set("iframe/locator/id", "iframe")
    
    when(mockWebDriver.findElement(By.id("frame"))).thenReturn(mockFrameElement)
    when(mockFrameElement.getTagName).thenReturn("frame")
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator);
    when(mockTargetLocator.frame(mockFrameElement)).thenReturn(mockWebDriver)
    env.scopes.set("frame/locator", "id")
    env.scopes.set("frame/locator/id", "frame")

    locate(env, LocatorBinding("username", locator, lookup, None)) should be (mockWebElement)
    locate(env, LocatorBinding("username", locator, lookup, Some("container"))) should be (mockWebElement)
    locate(env, LocatorBinding("username", locator, lookup, Some("iframe"))) should be (mockWebElement)
    locate(env, LocatorBinding("username", locator, lookup, Some("frame"))) should be (mockWebElement)
    
    verify(mockWebDriver, times(3)).findElement(by)
    
  }
  
  "Attempt to locate element with unsupported locator" should "throw unsuported locator error" in {
    val env = newEnv
    env.scopes.addScope("login").set("username/id", "unknown").set("username/id/unknown", "funkyness")
    var e = intercept[LocatorBindingException] {
      locate(env, LocatorBinding("username", "unknown", "funkiness", None))
    }
    e.getMessage should be ("Could not locate username: unsupported locator: unknown")
  }
  
  private def newEnv = new WebEnvContext(GwenOptions(), new ScopedDataStack()) {
    override def withWebDriver[T](f: WebDriver => T)(implicit takeScreenShot: Boolean = false): T = f(mockWebDriver)
  }
  
  private def shouldFailWithLocatorBindingError(element: String, env: WebEnvContext, expectedMsg: String) {
    
  }
  
}