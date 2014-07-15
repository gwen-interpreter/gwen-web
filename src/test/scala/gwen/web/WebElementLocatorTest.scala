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

import org.mockito.Mockito._
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.firefox.FirefoxDriver
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec

class WebElementLocatorTest extends FlatSpec with ShouldMatchers with MockitoSugar with WebElementLocator {

  val mockWebDriver = mock[FirefoxDriver]
  val mockWebElement = mock[WebElement]
  val mockWebDriverOptions = mock[WebDriver.Options]
  val mockWebDriverTimeouts = mock[WebDriver.Timeouts]
  
  "Attempt to locate unbound element "should "throw locator binding not found error" in {
    shouldFailWithLocatorBindingError("username", newEnv, "Could not locate web element: username, locator binding not found: username/locator")
  }
  
  "Attempt to locate element with unbound locator" should "throw locator not found error" in {
    val env = newEnv
    env.pageScopes.addScope("login").set("username/locator", "id")
    shouldFailWithLocatorBindingError("username", env, "Could not locate web element: username, locator not bound: username/locator/id")
  }
  
  "Attempt to locate element with unsupported locator" should "throw unsuported locator error" in {
    val env = newEnv
    env.pageScopes.addScope("login").set("username/locator", "unknown").set("username/locator/unknown", "funkyness")
    shouldFailWithLocatorBindingError("username", env, "Could not locate web element: username, unsupported locator: unknown")
  }
  
  "Attempt to locate non existent element" should "throw no such element error" in {
    
    val env = newEnv
    env.pageScopes.addScope("login").set("middleName/locator", "id").set("middleName/locator/id", "mname")
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(By.id("mname"))).thenReturn(null)
    
    val e = intercept[NoSuchElementException] {
      locate(env, "middleName")
    }
    e.getMessage should be ("Web element not found: middleName")
  }
  
  "Attempt to locate non existent element by Opt" should "return None" in {
    
    val env = newEnv
    env.pageScopes.addScope("login").set("middleName/locator", "id").set("middleName/locator/id", "mname")
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(By.id("mname"))).thenReturn(null)
    
    locateOpt(env, "middleName") match {
      case None => { } //expected
      case Some(webElement) => fail("Expected None but got Some(webElement)")
    }
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
    val locatorValue = "return document.getElementById('username');"
    val env = newEnv
    env.pageScopes.addScope("login").set("username/locator", locator).set(s"username/locator/${locator}", locatorValue)
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    doReturn(mockWebElement).when(mockWebDriver).executeScript(locatorValue)
    when(mockWebElement.isDisplayed()).thenReturn(true);
    
    locate(env, "username") should be (mockWebElement)
    
    locateOpt(env, "username") match {
      case Some(elem) => 
        elem should be (mockWebElement)
      case None =>
      	fail("Excpected Some(webElement) but got None")
    }
    
    verify(mockWebDriver, times(2)).executeScript(locatorValue)
	
  }
  
  private def shouldFailWithLocatorBindingError(element: String, env: WebEnvContext, expectedMsg: String) {
    
    var e = intercept[LocatorBindingException] {
      locate(env, element)
    }
    e.getMessage should be (expectedMsg)
    
    e = intercept[LocatorBindingException] {
      locateOpt(env, element)
    }
    e.getMessage should be (expectedMsg)
  }
  
  private def shouldFindWebElement(locator: String, locatorValue: String, by: By) {
    
    val env = newEnv
    env.pageScopes.addScope("login").set("username/locator", locator).set(s"username/locator/${locator}", locatorValue)
    
    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    when(mockWebDriver.findElement(by)).thenReturn(mockWebElement)
    when(mockWebElement.isDisplayed()).thenReturn(true)

    locate(env, "username") should be (mockWebElement)
    
    locateOpt(env, "username") match {
      case Some(elem) => 
        elem should be (mockWebElement)
      case None => 
      	fail("Excpected Some(webElement) but got None")
    }
    
    verify(mockWebDriver, times(2)).findElement(by)
    
  }
  
  private def newEnv = new WebEnvContext("Firefox") {
    override private[web] def loadWebDriver(driverName: String): WebDriver = mockWebDriver
  }
  
}