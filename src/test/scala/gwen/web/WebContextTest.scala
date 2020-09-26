/*
 * Copyright 2014-2018 Brady Wood, Branko Juric
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

import gwen.eval.GwenOptions
import org.mockito.Mockito._
import org.openqa.selenium._
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Matchers.any
import org.mockito.Matchers.anyVararg
import org.mockito.Matchers.same
import gwen.web.Errors.{LocatorBindingException, UnsupportedModifierKeyException, WaitTimeoutException}
import org.openqa.selenium.interactions.{Action, Actions}
import org.openqa.selenium.support.ui.Select

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.Duration

class WebContextTest extends BaseTest with Matchers with MockitoSugar with BeforeAndAfterEach {

  private var driverManager: DriverManager = _
  private var envContext: WebEnvContext = _
  private var webContext: WebContext = _
  private var mockWebDriver: MockWebDriver = _

  override def beforeEach(): Unit = {
    mockWebDriver = mock[MockWebDriver]
    driverManager = spy(new MockDriverManager(mockWebDriver))
    envContext = spy(new WebEnvContext(GwenOptions()))
    webContext = spy(new WebContext(envContext, driverManager))
    doNothing().when(webContext).tryMoveTo(any[WebElement])
  }

  "WebContext.reset" should "reset driver manager" in {
    doNothing().when(driverManager).reset()
    webContext.reset()
    verify(driverManager).reset()
  }

  "WebContext.close" should "quit driver manager" in {
    doNothing().when(driverManager).quit()
    webContext.close()
    verify(driverManager).quit()
  }

  "WebContext.close(name)" should "quit driver manager of given name" in {
    doNothing().when(driverManager).quit("name")
    webContext.close("name")
    verify(driverManager).quit("name")
  }

  "WebContext.withWebDriver" should "call user function" in {
    webContext.withWebDriver { driver =>
      driver.getCurrentUrl
    }
    verify(mockWebDriver).getCurrentUrl
  }

  "WebContext.getLocatorBinding" should "get binding from web context" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(envContext).getLocatorBinding("name")
    webContext.getLocatorBinding("name") should be (mockBinding)
  }

  "WebContext.getCachedWebElement" should "return element when it is in the cache" in  {
    val mockElement = mock[WebElement]
    envContext.topScope.pushObject("name", mockElement)
    doNothing().when(webContext).highlightElement(mockElement)
    webContext.getCachedWebElement("name") should be (Some(mockElement))
  }

  "WebContext.getCachedWebElement" should "return None when element is not in cache" in  {
    webContext.getCachedWebElement("name") should be (None)
  }

  "WebContext.captureScreenshot" should "add attachment to env context" in {
    val mockScreenshot = File.createTempFile("screenshot", "png")
    mockScreenshot.deleteOnExit()
    when(mockWebDriver.getScreenshotAs(OutputType.FILE)).thenReturn(mockScreenshot)
    webContext.captureScreenshot(true)
    verify(envContext).addAttachment(same("Screenshot"), any[String], same(null))
  }

  "WebContext.executeJS with arg" should "invoke javascript with arg" in {
    val script = "(function(arg){return arg;})(arg)"
    val param = "true"
    webContext.executeJS(script, param)
    verify(mockWebDriver).executeScript(same(script), anyVararg())
  }

  "WebContext.executeJS without args" should "invoke javascript without args" in {
    val script = "(function(){return true;})()"
    webContext.executeJS(script)
    verify(mockWebDriver).executeScript(script)
  }

  "WebContext.waitUntil" should "return when predicate returns true" in {
    webContext.waitUntil(10) { true }
  }

  "WebContext.waitUntil" should "timeout when predicate returns false" in {
    intercept[WaitTimeoutException] {
      webContext.waitUntil(1) { false }
    }
  }

  "WebContext.highlightElement" should "call javascript to highlight element" in {
    val mockElement = mock[WebElement]
    val hStyle = WebSettings.`gwen.web.highlight.style`
    val oStyle = ""
    val js1 = s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } original_style = element.getAttribute('style'); element.setAttribute('style', original_style + '; $hStyle'); return original_style;"
    val js2 = s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } element.setAttribute('style', '$oStyle');"
    doReturn(oStyle).when(webContext).executeJS(js1, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
    webContext.highlightElement(mockElement)
    val msecs = WebSettings`gwen.web.throttle.msecs`;
    if (msecs > 0) {
      verify(webContext).executeJS(js1, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
      verify(webContext).executeJS(js2, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
    } else {
      verify(webContext, never()).executeJS(js1, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
      verify(webContext, never()).executeJS(js2, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
    }
  }

  "WebContext.checkElementState" should "return when the state matches 'displayed'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(true).when(webContext).isDisplayed(mockElement)
    webContext.checkElementState(elemBinding, "displayed", negate = false)
    webContext.waitForElementState(elemBinding, "displayed", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'hidden'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(false).when(webContext).isDisplayed(mockElement)
    webContext.checkElementState(elemBinding, "hidden", negate = false)
    webContext.waitForElementState(elemBinding, "hidden", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'checked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true)
    webContext.checkElementState(elemBinding, "checked", negate = false)
    webContext.waitForElementState(elemBinding, "checked", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'ticked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true)
    webContext.checkElementState(elemBinding, "ticked", negate = false)
    webContext.waitForElementState(elemBinding, "ticked", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'unchecked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false)
    webContext.checkElementState(elemBinding, "unchecked", negate = false)
    webContext.waitForElementState(elemBinding, "unchecked", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'unticked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false)
    webContext.checkElementState(elemBinding, "unticked", negate = false)
    webContext.waitForElementState(elemBinding, "unticked", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'enabled'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isEnabled).thenReturn(true)
    webContext.checkElementState(elemBinding, "enabled", negate = false)
    webContext.waitForElementState(elemBinding, "enabled", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'disabled'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isEnabled).thenReturn(false)
    webContext.checkElementState(elemBinding, "disabled", negate = false)
    webContext.waitForElementState(elemBinding, "disabled", negate = false)
  }

  "WebContext.checkElementState" should "return when the state should not match 'displayed'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(false).when(webContext).isDisplayed(mockElement)
    webContext.checkElementState(elemBinding, "displayed", negate = true)
    webContext.waitForElementState(elemBinding, "displayed", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'hidden'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(true).when(webContext).isDisplayed(mockElement)
    webContext.checkElementState(elemBinding, "hidden", negate = true)
    webContext.waitForElementState(elemBinding, "hidden", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'checked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false)
    webContext.checkElementState(elemBinding, "checked", negate = true)
    webContext.waitForElementState(elemBinding, "checked", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'ticked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false)
    webContext.checkElementState(elemBinding, "ticked", negate = true)
    webContext.waitForElementState(elemBinding, "ticked", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'unchecked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true)
    webContext.checkElementState(elemBinding, "unchecked", negate = true)
    webContext.waitForElementState(elemBinding, "unchecked", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'unticked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true)
    webContext.checkElementState(elemBinding, "unticked", negate = true)
    envContext.scopes.allEntries.isEmpty should be (true)
    webContext.waitForElementState(elemBinding, "unticked", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'enabled'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isEnabled).thenReturn(false)
    webContext.checkElementState(elemBinding, "enabled", negate = true)
    webContext.waitForElementState(elemBinding, "enabled", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'disabled'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isEnabled).thenReturn(true)
    webContext.checkElementState(elemBinding, "disabled", negate = true)
    webContext.waitForElementState(elemBinding, "disabled", negate = true)
  }

  "WebContext.checkElementState" should "fail when the state does not match 'displayed'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(false).when(webContext).isDisplayed(mockElement)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "displayed", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "displayed", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'hidden'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(true).when(webContext).isDisplayed(mockElement)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "hidden", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "hidden", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'checked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "checked", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "checked", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'ticked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "ticked", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "ticked", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'unchecked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "unchecked", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "unchecked", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'unticked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "unticked", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "unticked", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'enabled'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isEnabled).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "enabled", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "enabled", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'disabled'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isEnabled).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "disabled", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "disabled", negate = false)
      }
    }
  }
  
  "WebContext.checkElementState" should "fail when the state should not but does match 'displayed'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(true).when(webContext).isDisplayed(mockElement)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "displayed", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "displayed", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'hidden'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(false).when(webContext).isDisplayed(mockElement)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "hidden", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "hidden", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'checked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
       webContext.checkElementState(elemBinding, "checked", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "checked", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'ticked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "ticked", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "ticked", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'unchecked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "unchecked", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "unchecked", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'unticked'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "unticked", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "unticked", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'enabled'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isEnabled).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "enabled", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "enabled", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'disabled'" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isEnabled).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "disabled", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "disabled", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "return when the state should not match 'displayed' and there is no such element" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    doThrow(new NoSuchElementException("no id")).when(webContext).locate(elemBinding)
    webContext.checkElementState(elemBinding, "displayed", negate = true)
    webContext.waitForElementState(elemBinding, "displayed", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should match 'hidden' and there is no such element" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    doThrow(new NoSuchElementException("no id")).when(webContext).locate(elemBinding)
    webContext.checkElementState(elemBinding, "hidden", negate = false)
    webContext.waitForElementState(elemBinding, "hidden", negate = false)
  }

  "WebContext.checkElementState" should "fail when the state should match 'displayed' and there is no such element" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    doThrow(new NoSuchElementException("no id")).when(webContext).locate(elemBinding)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "displayed", negate = false)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "displayed", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not match 'hidden' and there is no such element" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, Some(Duration.Zero), None)
    doThrow(new NoSuchElementException("no id")).when(webContext).locate(elemBinding)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        webContext.checkElementState(elemBinding, "hidden", negate = true)
      }
      intercept[WaitTimeoutException] {
        webContext.waitForElementState(elemBinding, "hidden", negate = true)
      }
    }
  }

  "WebContext.getTitle" should "return the title" in {
    when(mockWebDriver.getTitle).thenReturn("Gwen")
    webContext.getTitle should be ("Gwen")
    envContext.scopes.get("page/title") should be ("Gwen")
  }

  "WebContext.sendValue without enter" should "send value to element" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).moveToElement(mockElement)
    doReturn(mockActions).when(mockActions).sendKeys("Gwen")
    webContext.sendValue(elemBinding, "Gwen", clickFirst = false, clearFirst = false, sendEnterKey = false)
    verify(mockElement, never()).clear()
    envContext.scopes.getOpt("name/clear") should be (None)
    verify(mockActions).perform()
    envContext.scopes.get("name/type") should be ("Gwen")
    verify(mockElement, never()).sendKeys(Keys.RETURN)
    envContext.scopes.getOpt("name/enter") should be (None)
  }

  "WebContext.sendValue with clear and without enter" should "clear field and send value to element" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).moveToElement(mockElement)
    doReturn(mockActions).when(mockActions).sendKeys("Gwen")
    webContext.sendValue(elemBinding, "Gwen", clickFirst = false, clearFirst = true, sendEnterKey = false)
    verify(mockElement).clear()
    verify(mockActions).perform()
    envContext.scopes.get("name/type") should be ("Gwen")
    verify(mockElement, never()).sendKeys(Keys.RETURN)
    envContext.scopes.getOpt("name/enter") should be (None)
  }

  "WebContext.sendValue with click, clear and without enter" should "clear field and send value to element" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).moveToElement(mockElement)
    doReturn(mockActions).when(mockActions).sendKeys("Gwen")
    webContext.sendValue(elemBinding, "Gwen", clickFirst = true, clearFirst = true, sendEnterKey = false)
    verify(mockElement).click()
    verify(mockElement).clear()
    verify(mockActions).perform()
    envContext.scopes.get("name/type") should be ("Gwen")
    verify(mockElement, never()).sendKeys(Keys.RETURN)
    envContext.scopes.getOpt("name/enter") should be (None)
  }

  "WebContext.sendValue with send enter" should "send value to element" in {
    val elemBinding = LocatorBinding("name", "id", "name", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).moveToElement(mockElement)
    doReturn(mockActions).when(mockActions).sendKeys("Gwen")
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.RETURN)
    webContext.sendValue(elemBinding, "Gwen", clickFirst = false, clearFirst = false, sendEnterKey = true)
    verify(mockElement, never()).clear()
    verify(mockActions, times(2)).perform()
    envContext.scopes.getOpt("name/clear") should be (None)
    envContext.scopes.get("name/type") should be ("Gwen")
    envContext.scopes.get("name/enter") should be ("true")
  }

  "WebContext.sendValue with clear and send enter" should "clear and send value to element" in {
    withSetting("gwen.web.sendKeys.clearFirst", "true") {
      val elemBinding = LocatorBinding("name", "id", "name", None, None)
      val mockElement = mock[WebElement]
      val mockActions = mock[Actions]
      doReturn(mockElement).when(webContext).locate(elemBinding)
      doReturn(mockActions).when(webContext).createActions(mockWebDriver)
      doReturn(mockActions).when(mockActions).moveToElement(mockElement)
      doReturn(mockActions).when(mockActions).sendKeys("Gwen")
      doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.RETURN)
      webContext.sendValue(elemBinding, "Gwen", clickFirst = false, clearFirst = true, sendEnterKey = true)
      verify(mockElement).clear()
      verify(mockActions, times(2)).perform()
      envContext.scopes.get("name/type") should be ("Gwen")
      envContext.scopes.get("name/enter") should be ("true")
    }
  }

  "WebContext.sendValue with click, clear and send enter" should "clear and send value to element" in {
    withSetting("gwen.web.sendKeys.clearFirst", "true") {
      val elemBinding = LocatorBinding("name", "id", "name", None, None)
      val mockElement = mock[WebElement]
      val mockActions = mock[Actions]
      doReturn(mockElement).when(webContext).locate(elemBinding)
      doReturn(mockActions).when(webContext).createActions(mockWebDriver)
      doReturn(mockActions).when(mockActions).moveToElement(mockElement)
      doReturn(mockActions).when(mockActions).sendKeys("Gwen")
      doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.RETURN)
      webContext.sendValue(elemBinding, "Gwen", clickFirst = true, clearFirst = true, sendEnterKey = true)
      verify(mockElement).click()
      verify(mockElement).clear()
      verify(mockActions, times(2)).perform()
      envContext.scopes.get("name/enter") should be ("true")
    }
  }

  "WebContext.selectByVisibleText" should "select value provided" in {
    val elemBinding = LocatorBinding("names", "id", "names", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    webContext.selectByVisibleText(elemBinding, "Gwen")
    verify(mockSelect).selectByVisibleText("Gwen")
    envContext.scopes.get("names/select") should be ("Gwen")
  }

  "WebContext.selectByValue" should "select value provided" in {
    val elemBinding = LocatorBinding("names", "id", "names", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    webContext.selectByValue(elemBinding, "G")
    verify(mockSelect).selectByValue("G")
    envContext.scopes.get("names/select") should be ("G")
  }

  "WebContext.selectByIndex" should "select value at index provided" in {
    val elemBinding = LocatorBinding("names", "id", "names", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOption = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    when(mockSelect.getOptions).thenReturn(List(mockOption).asJava)
    when(mockOption.getText).thenReturn("Gwen")
    webContext.selectByIndex(elemBinding, 0)
    verify(mockSelect).selectByIndex(0)
    envContext.scopes.get("names/select") should be ("Gwen")
  }

  "WebContext.deselectByVisibleText" should "deselect value provided" in {
    val elemBinding = LocatorBinding("names", "id", "names", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    webContext.deselectByVisibleText(elemBinding, "Gwen")
    verify(mockSelect).deselectByVisibleText("Gwen")
    envContext.scopes.get("names/deselect") should be ("Gwen")
  }

  "WebContext.deselectByValue" should "deselect value provided" in {
    val elemBinding = LocatorBinding("names", "id", "names", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    webContext.deselectByValue(elemBinding, "G")
    verify(mockSelect).deselectByValue("G")
    envContext.scopes.get("names/deselect") should be ("G")
  }

  "WebContext.deselectByIndex" should "deselect value at index provided" in {
    val elemBinding = LocatorBinding("names", "id", "names", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOption = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    when(mockSelect.getOptions).thenReturn(List(mockOption).asJava)
    when(mockOption.getText).thenReturn("Gwen")
    webContext.deselectByIndex(elemBinding, 0)
    verify(mockSelect).deselectByIndex(0)
    envContext.scopes.get("names/deselect") should be ("Gwen")
  }

  "WebContext.performAction" should "click element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    webContext.performAction("click", elemBinding)
    verify(mockElement).click()
    envContext.scopes.get("element/click") should be ("true")
  }

  "WebContext.performAction" should "right click element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.contextClick(mockElement)).thenReturn(mockActions)
    webContext.performAction("right click", elemBinding)
    verify(mockActions, times(2)).perform()
    envContext.scopes.get("element/right click") should be ("true")
  }

  "WebContext.performAction" should "double click element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.doubleClick(mockElement)).thenReturn(mockActions)
    webContext.performAction("double click", elemBinding)
    verify(mockActions, times(2)).perform()
    envContext.scopes.get("element/double click") should be ("true")
  }

  "WebContext.performAction" should "move to element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("move to", elemBinding)
    verify(mockActions).perform()
    envContext.scopes.get("element/move to") should be ("true")
  }

  "WebContext.performAction" should "submit element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    webContext.performAction("submit", elemBinding)
    verify(mockElement).submit()
    envContext.scopes.get("element/submit") should be ("true")
  }

  "WebContext.performAction" should "check element by sending space char" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("check", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    envContext.scopes.get("element/check") should be ("true")
  }

  "WebContext.performAction" should "check element by clicking it" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockElement.isSelected).thenReturn(false, true)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("check", elemBinding)
    verify(mockActions).perform()
    verify(mockElement).click()
    envContext.scopes.get("element/check") should be ("true")
  }

  "WebContext.performAction" should "not check element that is already checked" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true, true)
    webContext.performAction("check", elemBinding)
    verify(mockElement, never()).click()
    envContext.scopes.get("element/check") should be ("true")
  }

  "WebContext.performAction" should "tick element by sending space char" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("tick", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    envContext.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performAction" should "tick element by clicking it" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("tick", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    envContext.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performAction" should "not tick element that is already ticked" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(true, true)
    webContext.performAction("tick", elemBinding)
    verify(mockElement, never()).click()
    envContext.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performAction" should "uncheck element by sending space char" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("uncheck", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    envContext.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performAction" should "uncheck element by clicking it" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockElement.isSelected).thenReturn(true, false)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("uncheck", elemBinding)
    verify(mockActions).perform()
    verify(mockElement).click()
    envContext.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performAction" should "not uncheck element that is already unchecked" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false, false)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("uncheck", elemBinding)
    verify(mockActions).perform()
    verify(mockElement, never()).sendKeys(Keys.SPACE)
    verify(mockElement, never()).click()
    envContext.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performAction" should "untick element by sending space char" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("untick", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    envContext.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performAction" should "untick element by clicking it" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    doReturn(false).when(mockElement).isSelected
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performAction("untick", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    envContext.scopes.get("element/untick") should be ("true")
  }

   "WebContext.performAction" should "not untick element that is already unticked" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.isSelected).thenReturn(false, false)
    webContext.performAction("untick", elemBinding)
    verify(mockElement, never()).sendKeys(Keys.SPACE)
    verify(mockElement, never()).click()
    envContext.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performAction" should "clear element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    webContext.performAction("clear", elemBinding)
    verify(mockElement).clear()
    envContext.scopes.get("element/clear") should be ("true")
  }

  "WebContext.performAction" should "perform javascript action on element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    envContext.scopes.set(s"element/action/click/javascript", "element.click()")
    webContext.performAction("click", elemBinding)
    verify(mockElement, never()).clear()
    verify(webContext).executeJS("(function(element) { element.click() })(arguments[0])", mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
    envContext.scopes.get("element/click") should be ("true")
  }

  "WebContext.dragAndDrop" should "drag source to target" in {
    val sourceBinding = LocatorBinding("source", "id", "source", None, None)
    val targetBinding = LocatorBinding("target", "id", "target", None, None)
    val sourceElement = mock[WebElement]
    val targetElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(sourceElement).when(webContext).locate(sourceBinding)
    doReturn(targetElement).when(webContext).locate(targetBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.dragAndDrop(sourceElement, targetElement)).thenReturn(mockActions)
    webContext.dragAndDrop(sourceBinding, targetBinding)
    verify(mockActions).perform()
  }

  "WebContext.holdAndClick" should "should command-shift click element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val modifierKeys = Array("COMMAND", "SHIFT")
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.SHIFT)).thenReturn(mockActions)
    when(mockActions.click(mockElement)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.SHIFT)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.holdAndClick(modifierKeys, "click", elemBinding)
    verify(mockAction).perform()
    envContext.scopes.get("element/click") should be ("true")
  }

  "WebContext.holdAndClick" should "should command double click element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val modifierKeys = Array("command")
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.doubleClick(mockElement)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.holdAndClick(modifierKeys, "double click", elemBinding)
    verify(mockAction).perform()
    envContext.scopes.get("element/double click") should be ("true")
  }

  "WebContext.holdAndClick" should "should command-shift right click element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val modifierKeys = Array("Command", "Shift")
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.SHIFT)).thenReturn(mockActions)
    when(mockActions.contextClick(mockElement)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.SHIFT)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.holdAndClick(modifierKeys, "right click", elemBinding)
    verify(mockAction).perform()
    envContext.scopes.get("element/right click") should be ("true")
  }

  "WebContext.holdAndClick" should "should error when given unknown modifier key" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val modifierKeys = Array("Unknown")
    intercept[UnsupportedModifierKeyException] {
      webContext.holdAndClick(modifierKeys, "click", elemBinding)
    }
  }

  "WebContext.sendKeys" should "" in {
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    val keys = Array("Command", "C")
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.sendKeys(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.sendKeys("C")).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.sendKeys(keys)
    verify(mockAction).perform()
  }

  "WebContext.sendKeys to element" should "" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    val keys = Array("Command", "C")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.sendKeys(mockElement, Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.sendKeys(mockElement, "C")).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.sendKeys(elemBinding, keys)
    verify(mockAction).perform()
  }

  "WebContext.performActionInContext" should "click element in context of another element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("click", "element", "context")
    verify(mockAction).perform()
    envContext.scopes.get("element/click") should be ("true")
  }

  "WebContext.performActionInContext" should "right click element in context of another element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.contextClick()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("right click", "element", "context")
    verify(mockAction).perform()
    envContext.scopes.get("element/right click") should be ("true")
  }

  "WebContext.performActionInContext" should "double click element in context of another element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.doubleClick()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("double click", "element", "context")
    verify(mockAction).perform()
    envContext.scopes.get("element/double click") should be ("true")
  }

  "WebContext.performActionInContext" should "check element in context of another element by sending a space char" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("check", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(2)).perform()
    envContext.scopes.get("element/check") should be ("true")
  }

  "WebContext.performActionInContext" should "check element in context of another element by clicking it" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("check", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(1)).perform()
    envContext.scopes.get("element/check") should be ("true")
  }

  "WebContext.performActionInContext" should "not check element in context of another element when it is already checked" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("check", "element", "context")
    verify(mockAction, never).perform()
    envContext.scopes.get("element/check") should be ("true")
  }

  "WebContext.performActionInContext" should "tick element in context of another element by sending a space char" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("tick", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(2)).perform()
    envContext.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performActionInContext" should "tick element in context of another element by clicking it" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("tick", "element", "context")
    verify(mockAction, times(1)).perform()
    envContext.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performActionInContext" should "not tick element in context of another element when it is already ticked" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("tick", "element", "context")
    verify(mockAction, never).perform()
    envContext.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performActionInContext" should "uncheck element in context of another element by sending a space char" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("uncheck", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(2)).perform()
    envContext.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performActionInContext" should "uncheck element in context of another element by clicking it" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("uncheck", "element", "context")
    verify(mockAction, times(2)).perform()
    envContext.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performActionInContext" should "not uncheck element in context of another element when it is already unchecked" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("uncheck", "element", "context")
    verify(mockAction, never).perform()
    envContext.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performActionInContext" should "untick element in context of another element by sending a space char" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("untick", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(2)).perform()
    envContext.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performActionInContext" should "untick element in context of another element by clicking it" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("untick", "element", "context")
    verify(mockAction, times(2)).perform()
    envContext.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performActionInContext" should "not untick element in context of another element when it is already unticked" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("untick", "element", "context")
    verify(mockAction, never).perform()
    envContext.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performActionInContext" should "move to element in context of another element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val ctxBinding = LocatorBinding("context", "id", "ctx", None, None)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(ctxBinding).when(envContext).getLocatorBinding("context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockContext).when(webContext).locate(ctxBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    webContext.performActionInContext("move to", "element", "context")
    verify(mockAction).perform()
    envContext.scopes.get("element/move to") should be ("true")
  }

  "WebContext.performActionInContext" should "fall back to non context action when element name contains 'of' literal" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doThrow(new LocatorBindingException("element not bound")).when(envContext).getLocatorBinding("element")
    doReturn(elemBinding).when(envContext).getLocatorBinding("element of context")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockActions).when(webContext).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    webContext.performActionInContext("move to", "element", "context")
    verify(mockActions).perform()
    envContext.scopes.get("element/move to") should be ("true")
  }

  "WebContext.performActionInContext" should "fail when elmenet cannot be located" in {
    doThrow(new LocatorBindingException("element not bound")).when(envContext).getLocatorBinding("element")
    doThrow(new LocatorBindingException("element not bound")).when(envContext).getLocatorBinding("element of context")
    intercept[LocatorBindingException] {
      webContext.performActionInContext("move to", "element", "context")
    }
  }

  "WebContext.waitForText" should "should return true when text is present" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    doReturn(Some("text")).when(webContext).getElementText(elemBinding)
    webContext.waitForText(elemBinding) should be (true)
  }

  "WebContext.waitForText" should "should return false when text is absent" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    doReturn(None).when(webContext).getElementText(elemBinding)
    webContext.waitForText(elemBinding) should be (false)
  }

  "WebContext.waitForText" should "should return false when text is blank" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    doReturn(Some("")).when(webContext).getElementText(elemBinding)
    webContext.waitForText(elemBinding) should be (false)
  }

  "WebContext.scrollIntoView" should "scroll to top of element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(null).when(webContext).executeJS(s"var elem = arguments[0]; if (typeof elem !== 'undefined' && elem != null) { elem.scrollIntoView(true); }", mockElement)(takeScreenShot = false)
    webContext.scrollIntoView(elemBinding, ScrollTo.top)
  }

  "WebContext.scrollIntoView" should "scroll to bottom of element" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(null).when(webContext).executeJS(s"var elem = arguments[0]; if (typeof elem !== 'undefined' && elem != null) { elem.scrollIntoView(false); }", mockElement)(takeScreenShot = false)
    webContext.scrollIntoView(elemBinding, ScrollTo.bottom)
  }

  "WebContext.resizeWindow" should "should resize the window" in {
    val mockDriverOptions = mock[WebDriver.Options]
    val mockWindow = mock[WebDriver.Window]
    when(mockWebDriver.manage).thenReturn(mockDriverOptions)
    when(mockDriverOptions.window).thenReturn(mockWindow)
    webContext.resizeWindow(300, 400)
    verify(mockWindow).setSize(new Dimension(300, 400))
  }

  "WebContext.maximizeWindow" should "maximize the window" in {
    val mockDriverOptions = mock[WebDriver.Options]
    val mockWindow = mock[WebDriver.Window]
    when(mockWebDriver.manage).thenReturn(mockDriverOptions)
    when(mockDriverOptions.window).thenReturn(mockWindow)
    webContext.maximizeWindow()
    verify(mockWindow).maximize()
  }

  "webContext.captureCurrentUrl" should "capture url in default attribute" in {
    when(mockWebDriver.getCurrentUrl).thenReturn("http://site.com")
    webContext.captureCurrentUrl(None)
    envContext.topScope.get("the current URL") should be ("http://site.com")
  }

  "webContext.captureCurrentUrl" should "capture url in provided attribute" in {
    when(mockWebDriver.getCurrentUrl).thenReturn("http://site.com")
    webContext.captureCurrentUrl(Some("my URL"))
    envContext.topScope.get("my URL") should be ("http://site.com")
  }

  "WebContext.getElementText" should "return blank on element with null text value, null text attribute, null value attribute, and null JS value" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.getText).thenReturn(null)
    when(mockElement.getAttribute("text")).thenReturn(null)
    when(mockElement.getAttribute("value")).thenReturn(null)
    doReturn(null).when(webContext).executeJS("(function(element){return element.innerText || element.textContent || ''})(arguments[0]);", mockElement)(takeScreenShot = false)
    webContext.getElementText(elemBinding) should be (Some(""))
    envContext.scopes.get("element/text") should be ("")
  }

  "WebContext.getElementText" should "return blank on element with blank text value, blank text attribute, blank value attribute, and blank JS value" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.getText).thenReturn("")
    when(mockElement.getAttribute("text")).thenReturn("")
    when(mockElement.getAttribute("value")).thenReturn("")
    doReturn("").when(webContext).executeJS("return (function(element){return element.innerText || element.textContent || ''})(arguments[0]);", mockElement)(takeScreenShot = false)
    webContext.getElementText(elemBinding) should be (Some(""))
    envContext.scopes.get("element/text") should be ("")
  }

  "WebContext.getElementText" should "return JS value on element with null text value, null text attribute, null value attribute, and non null JS value" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.getText).thenReturn(null)
    when(mockElement.getAttribute("text")).thenReturn(null)
    when(mockElement.getAttribute("value")).thenReturn(null)
    doReturn("JSValue").when(webContext).executeJS("return (function(element){return element.innerText || element.textContent || ''})(arguments[0]);", mockElement)(takeScreenShot = false)
    webContext.getElementText(elemBinding) should be (Some("JSValue"))
    envContext.scopes.get("element/text") should be ("JSValue")
  }

  "WebContext.getElementText" should "return value attribute on element with null text value, null text attribute, and non null value attribute" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.getText).thenReturn(null)
    when(mockElement.getAttribute("text")).thenReturn(null)
    when(mockElement.getAttribute("value")).thenReturn("valueAttr")
    webContext.getElementText(elemBinding) should be (Some("valueAttr"))
    envContext.scopes.get("element/text") should be ("valueAttr")
  }

  "WebContext.getElementText" should "return text attribute on element with null text value, and non null text attribute" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.getText).thenReturn(null)
    when(mockElement.getAttribute("text")).thenReturn("textAttr")
    webContext.getElementText(elemBinding) should be (Some("textAttr"))
    envContext.scopes.get("element/text") should be ("textAttr")
  }

  "WebContext.getElementText" should "return text on element with non null text value" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    when(mockElement.getText).thenReturn("text")
    webContext.getElementText(elemBinding) should be (Some("text"))
    envContext.scopes.get("element/text") should be ("text")
  }

  "WebContext.getElementSelection by text" should "return blank when no options are selected" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement]().asJava, List[WebElement]().asJava)
    webContext.getElementSelection("element", "text") should be (Some(""))
    envContext.scopes.get("element/selectedText") should be ("")
  }

  "WebContext.getElementSelection by text" should "return text attribute of selected options" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOptionElement1 = mock[WebElement]
    val mockOptionElement2 = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement]().asJava, List[WebElement](mockOptionElement1, mockOptionElement2).asJava)
    when(mockOptionElement1.getAttribute("text")).thenReturn("one")
    when(mockOptionElement2.getAttribute("text")).thenReturn("two")
    webContext.getElementSelection("element", "text") should be (Some("one,two"))
    envContext.scopes.get("element/selectedText") should be ("one,two")
  }

  "WebContext.getElementSelection by text" should "return text value of selected options" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOptionElement1 = mock[WebElement]
    val mockOptionElement2 = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement](mockOptionElement1, mockOptionElement2).asJava)
    when(mockOptionElement1.getText).thenReturn("one")
    when(mockOptionElement2.getText).thenReturn("two")
    webContext.getElementSelection("element", "text") should be (Some("one,two"))
    envContext.scopes.get("element/selectedText") should be ("one,two")
  }

  "WebContext.getElementSelection by value" should "return blank when no options are selected" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement]().asJava)
    webContext.getElementSelection("element", "value") should be (Some(""))
    envContext.scopes.get("element/selectedValue") should be ("")
  }

  "WebContext.getElementSelection by value" should "return value attribute of selected options" in {
    val elemBinding = LocatorBinding("element", "id", "elem", None, None)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOptionElement1 = mock[WebElement]
    val mockOptionElement2 = mock[WebElement]
    doReturn(elemBinding).when(envContext).getLocatorBinding("element")
    doReturn(mockElement).when(webContext).locate(elemBinding)
    doReturn(mockSelect).when(webContext).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement](mockOptionElement1, mockOptionElement2).asJava)
    when(mockOptionElement1.getAttribute("value")).thenReturn("one")
    when(mockOptionElement2.getAttribute("value")).thenReturn("two")
    webContext.getElementSelection("element", "value") should be (Some("one,two"))
    envContext.scopes.get("element/selectedValue") should be ("one,two")
  }

  "WebContext.switchToSession" should "switch to given session" in {
    doNothing().when(driverManager).switchToSession("my session")
    webContext.switchToSession("my session")
    verify(driverManager).switchToSession("my session")
  }

  "WebContext.switchToChild" should "switch to child window" in {
    doNothing().when(driverManager).switchToChild(mockWebDriver)
    webContext.switchToChild()
    verify(driverManager).switchToChild(mockWebDriver)
  }

  "WebContext.closeChild" should "close the child window" in {
    doNothing().when(driverManager).closeChild()
    webContext.closeChild()
    verify(driverManager).closeChild()
  }

  "WebContext.switchToParent" should "should stay on parent when child is closed" in {
    doNothing().when(driverManager).switchToParent(true)
    webContext.switchToParent(true)
    verify(driverManager).switchToParent(true)
  }

  "WebContext.switchToParent" should "switch to parent when child is open" in {
    doNothing().when(driverManager).switchToParent(false)
    webContext.switchToParent(false)
    verify(driverManager).switchToParent(false)
  }

  "WebContext.switchToDefaultContent" should "switch to default content" in {
    doNothing().when(driverManager).switchToDefaultContent()
    webContext.switchToDefaultContent()
    verify(driverManager).switchToDefaultContent()
  }

  "WebContext.refreshPage" should "refresh the current page" in {
    val mockNavigation = mock[WebDriver.Navigation]
    when(mockWebDriver.navigate()).thenReturn(mockNavigation)
    webContext.refreshPage()
    verify(mockNavigation).refresh()
  }

  "WebContext.handleAlert" should "handle accept alert" in {
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    val mockAlert = mock[Alert]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.alert()).thenReturn(mockAlert)
    webContext.handleAlert(true)
    verify(mockAlert).accept()
  }

  "WebContext.handleAlert" should "handle dismiss alert" in {
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    val mockAlert = mock[Alert]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.alert()).thenReturn(mockAlert)
    webContext.handleAlert(false)
    verify(mockAlert).dismiss()
  }

  "WebContext.navigateTo" should "navigate to given URL" in {
    webContext.navigateTo("http://site.com")
    verify(mockWebDriver).get("http://site.com")
  }

  "WebContext.getPopupMessage" should "return the message" in {
    val mockTargetLocaotr = mock[WebDriver.TargetLocator]
    val mockAlert = mock[Alert]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocaotr)
    when(mockTargetLocaotr.alert()).thenReturn(mockAlert)
    when(mockAlert.getText).thenReturn("popup message")
    webContext.getPopupMessage should be ("popup message")
  }

}

abstract class MockWebDriver extends WebDriver with TakesScreenshot with JavascriptExecutor

class MockDriverManager(webDriver: MockWebDriver) extends DriverManager {
  override private[web] def loadWebDriver: WebDriver = webDriver
}