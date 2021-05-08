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

package gwen.web.eval

import gwen.core.Errors
import gwen.core.GwenOptions
import gwen.core.eval.EvalEnvironment
import gwen.core.eval.binding.JavaScriptBinding
import gwen.web._
import gwen.web.WebErrors._
import gwen.web.eval.binding._

import org.mockito.Mockito._
import org.openqa.selenium._
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Matchers.any
import org.mockito.Matchers.anyVararg
import org.mockito.Matchers.same
import org.openqa.selenium.interactions.{Action, Actions}
import org.openqa.selenium.support.ui.Select

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.Duration

import java.io.File

class WebContextTest extends BaseTest with Matchers with MockitoSugar with BeforeAndAfterEach {

  private var env: EvalEnvironment = _
  private var ctx: WebContext = _
  private var driverManager: DriverManager = _
  private var mockWebDriver: MockWebDriver = _
  private var mockLocator: WebElementLocator = _

  override def beforeEach(): Unit = {
    mockWebDriver = mock[MockWebDriver]
    mockLocator = mock[WebElementLocator]
    env = spy(new EvalEnvironment())
    driverManager = spy(new MockDriverManager(mockWebDriver))
    ctx = spy(new WebContext(GwenOptions(), env, driverManager))
    doNothing().when(ctx).tryMoveTo(any[WebElement])
    doReturn(mockLocator).when(ctx).locator
  }

  "WebContext.reset" should "reset driver manager" in {
    doNothing().when(driverManager).reset()
    ctx.reset()
    verify(driverManager).reset()
  }

  "WebContext.close" should "quit driver manager" in {
    doNothing().when(driverManager).quit()
    ctx.close()
    verify(driverManager).quit()
  }

  "WebContext.close(name)" should "quit driver manager of given name" in {
    doNothing().when(driverManager).quit("name")
    ctx.close("name")
    verify(driverManager).quit("name")
  }

  "WebContext.withWebDriver" should "call user function" in {
    ctx.withWebDriver { driver =>
      driver.getCurrentUrl
    }
    verify(mockWebDriver).getCurrentUrl
  }

  "WebContext.getLocatorBinding" should "get binding from web context" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("name")
    ctx.getLocatorBinding("name") should be (mockBinding)
  }

  "WebContext.getCachedWebElement" should "return element when it is in the cache" in  {
    val mockElement = mock[WebElement]
    env.topScope.pushObject("name", mockElement)
    doNothing().when(ctx).highlightElement(mockElement)
    ctx.getCachedWebElement("name") should be (Some(mockElement))
  }

  "WebContext.getCachedWebElement" should "return None when element is not in cache" in  {
    ctx.getCachedWebElement("name") should be (None)
  }

  "WebContext.captureScreenshot" should "add attachment to env context" in {
    val mockScreenshot = File.createTempFile("screenshot", "png")
    mockScreenshot.deleteOnExit()
    when(mockWebDriver.getScreenshotAs(OutputType.FILE)).thenReturn(mockScreenshot)
    ctx.captureScreenshot(true)
    verify(env).addAttachment(same("Screenshot"), any[String], same(null))
  }

  "WebContext.executeJS with arg" should "invoke javascript with arg" in {
    val script = "(function(arg){return arg;})(arg)"
    val param = "true"
    ctx.executeJS(script, param)
    verify(mockWebDriver).executeScript(same(script), anyVararg())
  }

  "WebContext.executeJS without args" should "invoke javascript without args" in {
    val script = "(function(){return true;})()"
    ctx.executeJS(script)
    verify(mockWebDriver).executeScript(script)
  }

  "WebContext.waitUntil" should "return when predicate returns true" in {
    ctx.waitUntil(10, "waiting for predicate") { true }
  }

  "WebContext.waitUntil" should "timeout when predicate returns false" in {
    intercept[Errors.WaitTimeoutException] {
      ctx.waitUntil(1, "waiting for predicate") { false }
    }
  }

  "WebContext.highlightElement" should "call javascript to highlight element" in {
    val mockElement = mock[WebElement]
    val hStyle = WebSettings.`gwen.web.highlight.style`
    val oStyle = ""
    val js1 = s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } original_style = element.getAttribute('style'); element.setAttribute('style', original_style + '; $hStyle'); return original_style;"
    val js2 = s"element = arguments[0]; type = element.getAttribute('type'); if (('radio' == type || 'checkbox' == type) && element.parentElement.getElementsByTagName('input').length == 1) { element = element.parentElement; } element.setAttribute('style', '$oStyle');"
    doReturn(oStyle).when(ctx).executeJS(js1, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
    ctx.highlightElement(mockElement)
    val msecs = WebSettings`gwen.web.throttle.msecs`;
    if (msecs > 0) {
      verify(ctx).executeJS(js1, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
      verify(ctx).executeJS(js2, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
    } else {
      verify(ctx, never()).executeJS(js1, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
      verify(ctx, never()).executeJS(js2, mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
    }
  }

  "WebContext.checkElementState" should "return when the state matches 'displayed'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(true).when(ctx).isDisplayed(mockElement)
    ctx.checkElementState(elemBinding, "displayed", negate = false)
    ctx.waitForElementState(elemBinding, "displayed", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'hidden'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(false).when(ctx).isDisplayed(mockElement)
    ctx.checkElementState(elemBinding, "hidden", negate = false)
    ctx.waitForElementState(elemBinding, "hidden", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'checked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true)
    ctx.checkElementState(elemBinding, "checked", negate = false)
    ctx.waitForElementState(elemBinding, "checked", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'ticked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true)
    ctx.checkElementState(elemBinding, "ticked", negate = false)
    ctx.waitForElementState(elemBinding, "ticked", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'unchecked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false)
    ctx.checkElementState(elemBinding, "unchecked", negate = false)
    ctx.waitForElementState(elemBinding, "unchecked", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'unticked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false)
    ctx.checkElementState(elemBinding, "unticked", negate = false)
    ctx.waitForElementState(elemBinding, "unticked", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'enabled'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isEnabled).thenReturn(true)
    ctx.checkElementState(elemBinding, "enabled", negate = false)
    ctx.waitForElementState(elemBinding, "enabled", negate = false)
  }

  "WebContext.checkElementState" should "return when the state matches 'disabled'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isEnabled).thenReturn(false)
    ctx.checkElementState(elemBinding, "disabled", negate = false)
    ctx.waitForElementState(elemBinding, "disabled", negate = false)
  }

  "WebContext.checkElementState" should "return when the state should not match 'displayed'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(false).when(ctx).isDisplayed(mockElement)
    ctx.checkElementState(elemBinding, "displayed", negate = true)
    ctx.waitForElementState(elemBinding, "displayed", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'hidden'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(true).when(ctx).isDisplayed(mockElement)
    ctx.checkElementState(elemBinding, "hidden", negate = true)
    ctx.waitForElementState(elemBinding, "hidden", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'checked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false)
    ctx.checkElementState(elemBinding, "checked", negate = true)
    ctx.waitForElementState(elemBinding, "checked", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'ticked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false)
    ctx.checkElementState(elemBinding, "ticked", negate = true)
    ctx.waitForElementState(elemBinding, "ticked", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'unchecked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true)
    ctx.checkElementState(elemBinding, "unchecked", negate = true)
    ctx.waitForElementState(elemBinding, "unchecked", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'unticked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true)
    ctx.checkElementState(elemBinding, "unticked", negate = true)
    env.scopes.allEntries.isEmpty should be (true)
    ctx.waitForElementState(elemBinding, "unticked", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'enabled'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isEnabled).thenReturn(false)
    ctx.checkElementState(elemBinding, "enabled", negate = true)
    ctx.waitForElementState(elemBinding, "enabled", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should not match 'disabled'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isEnabled).thenReturn(true)
    ctx.checkElementState(elemBinding, "disabled", negate = true)
    ctx.waitForElementState(elemBinding, "disabled", negate = true)
  }

  "WebContext.checkElementState" should "fail when the state does not match 'displayed'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(false).when(ctx).isDisplayed(mockElement)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "displayed", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "displayed", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'hidden'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(true).when(ctx).isDisplayed(mockElement)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "hidden", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "hidden", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'checked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "checked", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "checked", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'ticked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "ticked", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "ticked", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'unchecked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "unchecked", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "unchecked", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'unticked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "unticked", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "unticked", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'enabled'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isEnabled).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "enabled", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "enabled", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state does not match 'disabled'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isEnabled).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "disabled", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "disabled", negate = false)
      }
    }
  }
  
  "WebContext.checkElementState" should "fail when the state should not but does match 'displayed'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(true).when(ctx).isDisplayed(mockElement)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "displayed", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "displayed", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'hidden'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(false).when(ctx).isDisplayed(mockElement)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "hidden", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "hidden", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'checked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
       ctx.checkElementState(elemBinding, "checked", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "checked", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'ticked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "ticked", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "ticked", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'unchecked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "unchecked", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "unchecked", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'unticked'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "unticked", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "unticked", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'enabled'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isEnabled).thenReturn(true)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "enabled", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "enabled", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not but does match 'disabled'" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isEnabled).thenReturn(false)
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "disabled", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "disabled", negate = true)
      }
    }
  }

  "WebContext.checkElementState" should "return when the state should not match 'displayed' and there is no such element" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    doThrow(new NoSuchElementException("no id")).when(mockLocator).locate(any[LocatorBinding])
    ctx.checkElementState(elemBinding, "displayed", negate = true)
    ctx.waitForElementState(elemBinding, "displayed", negate = true)
  }

  "WebContext.checkElementState" should "return when the state should match 'hidden' and there is no such element" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    doThrow(new NoSuchElementException("no id")).when(mockLocator).locate(any[LocatorBinding])
    ctx.checkElementState(elemBinding, "hidden", negate = false)
    ctx.waitForElementState(elemBinding, "hidden", negate = false)
  }

  "WebContext.checkElementState" should "fail when the state should match 'displayed' and there is no such element" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    doThrow(new NoSuchElementException("no id")).when(mockLocator).locate(any[LocatorBinding])
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "displayed", negate = false)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "displayed", negate = false)
      }
    }
  }

  "WebContext.checkElementState" should "fail when the state should not match 'hidden' and there is no such element" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, Some(Duration.Zero), None, ctx)
    doThrow(new NoSuchElementException("no id")).when(mockLocator).locate(any[LocatorBinding])
    withSetting("gwen.web.wait.seconds", "1") {
      intercept[AssertionError] {
        ctx.checkElementState(elemBinding, "hidden", negate = true)
      }
      intercept[Errors.WaitTimeoutException] {
        ctx.waitForElementState(elemBinding, "hidden", negate = true)
      }
    }
  }

  "WebContext.getTitle" should "return the title" in {
    when(mockWebDriver.getTitle).thenReturn("Gwen")
    ctx.getTitle should be ("Gwen")
    env.scopes.get("page/title") should be ("Gwen")
  }

  "WebContext.sendValue without enter" should "send value to element" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).moveToElement(mockElement)
    doReturn(mockActions).when(mockActions).sendKeys("Gwen")
    ctx.sendValue(elemBinding, "Gwen", clickFirst = false, clearFirst = false, sendEnterKey = false)
    verify(mockElement, never()).clear()
    env.scopes.getOpt("name/clear") should be (None)
    verify(mockActions).perform()
    env.scopes.get("name/type") should be ("Gwen")
    verify(mockElement, never()).sendKeys(Keys.RETURN)
    env.scopes.getOpt("name/enter") should be (None)
  }

  "WebContext.sendValue with clear and without enter" should "clear field and send value to element" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).moveToElement(mockElement)
    doReturn(mockActions).when(mockActions).sendKeys("Gwen")
    ctx.sendValue(elemBinding, "Gwen", clickFirst = false, clearFirst = true, sendEnterKey = false)
    verify(mockElement).clear()
    verify(mockActions).perform()
    env.scopes.get("name/type") should be ("Gwen")
    verify(mockElement, never()).sendKeys(Keys.RETURN)
    env.scopes.getOpt("name/enter") should be (None)
  }

  "WebContext.sendValue with click, clear and without enter" should "clear field and send value to element" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).moveToElement(mockElement)
    doReturn(mockActions).when(mockActions).sendKeys("Gwen")
    ctx.sendValue(elemBinding, "Gwen", clickFirst = true, clearFirst = true, sendEnterKey = false)
    verify(mockElement).click()
    verify(mockElement).clear()
    verify(mockActions).perform()
    env.scopes.get("name/type") should be ("Gwen")
    verify(mockElement, never()).sendKeys(Keys.RETURN)
    env.scopes.getOpt("name/enter") should be (None)
  }

  "WebContext.sendValue with send enter" should "send value to element" in {
    val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).moveToElement(mockElement)
    doReturn(mockActions).when(mockActions).sendKeys("Gwen")
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.RETURN)
    ctx.sendValue(elemBinding, "Gwen", clickFirst = false, clearFirst = false, sendEnterKey = true)
    verify(mockElement, never()).clear()
    verify(mockActions, times(2)).perform()
    env.scopes.getOpt("name/clear") should be (None)
    env.scopes.get("name/type") should be ("Gwen")
    env.scopes.get("name/enter") should be ("true")
  }

  "WebContext.sendValue with clear and send enter" should "clear and send value to element" in {
    withSetting("gwen.web.sendKeys.clearFirst", "true") {
      val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, None, ctx)
      val mockElement = mock[WebElement]
      val mockActions = mock[Actions]
      doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
      doReturn(mockActions).when(ctx).createActions(mockWebDriver)
      doReturn(mockActions).when(mockActions).moveToElement(mockElement)
      doReturn(mockActions).when(mockActions).sendKeys("Gwen")
      doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.RETURN)
      ctx.sendValue(elemBinding, "Gwen", clickFirst = false, clearFirst = true, sendEnterKey = true)
      verify(mockElement).clear()
      verify(mockActions, times(2)).perform()
      env.scopes.get("name/type") should be ("Gwen")
      env.scopes.get("name/enter") should be ("true")
    }
  }

  "WebContext.sendValue with click, clear and send enter" should "clear and send value to element" in {
    withSetting("gwen.web.sendKeys.clearFirst", "true") {
      val elemBinding = LocatorBinding("name", SelectorType.id, "name", None, None, ctx)
      val mockElement = mock[WebElement]
      val mockActions = mock[Actions]
      doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
      doReturn(mockActions).when(ctx).createActions(mockWebDriver)
      doReturn(mockActions).when(mockActions).moveToElement(mockElement)
      doReturn(mockActions).when(mockActions).sendKeys("Gwen")
      doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.RETURN)
      ctx.sendValue(elemBinding, "Gwen", clickFirst = true, clearFirst = true, sendEnterKey = true)
      verify(mockElement).click()
      verify(mockElement).clear()
      verify(mockActions, times(2)).perform()
      env.scopes.get("name/enter") should be ("true")
    }
  }

  "WebContext.selectByVisibleText" should "select value provided" in {
    val elemBinding = LocatorBinding("names", SelectorType.id, "names", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    ctx.selectByVisibleText(elemBinding, "Gwen")
    verify(mockSelect).selectByVisibleText("Gwen")
    env.scopes.get("names/select") should be ("Gwen")
  }

  "WebContext.selectByValue" should "select value provided" in {
    val elemBinding = LocatorBinding("names", SelectorType.id, "names", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    ctx.selectByValue(elemBinding, "G")
    verify(mockSelect).selectByValue("G")
    env.scopes.get("names/select") should be ("G")
  }

  "WebContext.selectByIndex" should "select value at index provided" in {
    val elemBinding = LocatorBinding("names", SelectorType.id, "names", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOption = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    when(mockSelect.getOptions).thenReturn(List(mockOption).asJava)
    when(mockOption.getText).thenReturn("Gwen")
    ctx.selectByIndex(elemBinding, 0)
    verify(mockSelect).selectByIndex(0)
    env.scopes.get("names/select") should be ("Gwen")
  }

  "WebContext.deselectByVisibleText" should "deselect value provided" in {
    val elemBinding = LocatorBinding("names", SelectorType.id, "names", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    ctx.deselectByVisibleText(elemBinding, "Gwen")
    verify(mockSelect).deselectByVisibleText("Gwen")
    env.scopes.get("names/deselect") should be ("Gwen")
  }

  "WebContext.deselectByValue" should "deselect value provided" in {
    val elemBinding = LocatorBinding("names", SelectorType.id, "names", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    ctx.deselectByValue(elemBinding, "G")
    verify(mockSelect).deselectByValue("G")
    env.scopes.get("names/deselect") should be ("G")
  }

  "WebContext.deselectByIndex" should "deselect value at index provided" in {
    val elemBinding = LocatorBinding("names", SelectorType.id, "names", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOption = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    when(mockSelect.getOptions).thenReturn(List(mockOption).asJava)
    when(mockOption.getText).thenReturn("Gwen")
    ctx.deselectByIndex(elemBinding, 0)
    verify(mockSelect).deselectByIndex(0)
    env.scopes.get("names/deselect") should be ("Gwen")
  }

  "WebContext.performAction" should "click element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    ctx.performAction("click", elemBinding)
    verify(mockElement).click()
    env.scopes.get("element/click") should be ("true")
  }

  "WebContext.performAction" should "right click element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.contextClick(mockElement)).thenReturn(mockActions)
    ctx.performAction("right click", elemBinding)
    verify(mockActions, times(2)).perform()
    env.scopes.get("element/right click") should be ("true")
  }

  "WebContext.performAction" should "double click element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.doubleClick(mockElement)).thenReturn(mockActions)
    ctx.performAction("double click", elemBinding)
    verify(mockActions, times(2)).perform()
    env.scopes.get("element/double click") should be ("true")
  }

  "WebContext.performAction" should "move to element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("move to", elemBinding)
    verify(mockActions).perform()
    env.scopes.get("element/move to") should be ("true")
  }

  "WebContext.performAction" should "submit element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    ctx.performAction("submit", elemBinding)
    verify(mockElement).submit()
    env.scopes.get("element/submit") should be ("true")
  }

  "WebContext.performAction" should "check element by sending space char" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("check", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    env.scopes.get("element/check") should be ("true")
  }

  "WebContext.performAction" should "check element by clicking it" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockElement.isSelected).thenReturn(false, true)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("check", elemBinding)
    verify(mockActions).perform()
    verify(mockElement).click()
    env.scopes.get("element/check") should be ("true")
  }

  "WebContext.performAction" should "not check element that is already checked" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true, true)
    ctx.performAction("check", elemBinding)
    verify(mockElement, never()).click()
    env.scopes.get("element/check") should be ("true")
  }

  "WebContext.performAction" should "tick element by sending space char" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("tick", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    env.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performAction" should "tick element by clicking it" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("tick", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    env.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performAction" should "not tick element that is already ticked" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(true, true)
    ctx.performAction("tick", elemBinding)
    verify(mockElement, never()).click()
    env.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performAction" should "uncheck element by sending space char" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("uncheck", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    env.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performAction" should "uncheck element by clicking it" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockElement.isSelected).thenReturn(true, false)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("uncheck", elemBinding)
    verify(mockActions).perform()
    verify(mockElement).click()
    env.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performAction" should "not uncheck element that is already unchecked" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false, false)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("uncheck", elemBinding)
    verify(mockActions).perform()
    verify(mockElement, never()).sendKeys(Keys.SPACE)
    verify(mockElement, never()).click()
    env.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performAction" should "untick element by sending space char" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("untick", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    env.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performAction" should "untick element by clicking it" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    doReturn(mockActions).when(mockActions).sendKeys(mockElement, Keys.SPACE)
    doReturn(false).when(mockElement).isSelected
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performAction("untick", elemBinding)
    verify(mockActions, times(2)).perform()
    verify(mockElement).click()
    env.scopes.get("element/untick") should be ("true")
  }

   "WebContext.performAction" should "not untick element that is already unticked" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.isSelected).thenReturn(false, false)
    ctx.performAction("untick", elemBinding)
    verify(mockElement, never()).sendKeys(Keys.SPACE)
    verify(mockElement, never()).click()
    env.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performAction" should "clear element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    ctx.performAction("clear", elemBinding)
    verify(mockElement).clear()
    env.scopes.get("element/clear") should be ("true")
  }

  "WebContext.performAction" should "perform javascript action on element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    env.scopes.set(JavaScriptBinding.key("element/action/click"), "element.click()")
    ctx.performAction("click", elemBinding)
    verify(mockElement, never()).clear()
    verify(ctx).executeJS("(function(element) { element.click() })(arguments[0])", mockElement)(WebSettings.`gwen.web.capture.screenshots.highlighting`)
    env.scopes.get("element/click") should be ("true")
  }

  "WebContext.dragAndDrop" should "drag source to target" in {
    val sourceBinding = LocatorBinding("source", SelectorType.id, "source", None, None, ctx)
    val targetBinding = LocatorBinding("target", SelectorType.id, "target", None, None, ctx)
    val sourceElement = mock[WebElement]
    val targetElement = mock[WebElement]
    val mockActions = mock[Actions]
    doReturn(sourceElement).when(mockLocator).locate(sourceBinding)
    doReturn(targetElement).when(mockLocator).locate(targetBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.dragAndDrop(sourceElement, targetElement)).thenReturn(mockActions)
    ctx.dragAndDrop(sourceBinding, targetBinding)
    verify(mockActions).perform()
  }

  "WebContext.holdAndClick" should "command-shift click element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val modifierKeys = Array("COMMAND", "SHIFT")
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.SHIFT)).thenReturn(mockActions)
    when(mockActions.click(mockElement)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.SHIFT)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.holdAndClick(modifierKeys, "click", elemBinding)
    verify(mockAction).perform()
    env.scopes.get("element/click") should be ("true")
  }

  "WebContext.holdAndClick" should "command double click element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val modifierKeys = Array("command")
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.doubleClick(mockElement)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.holdAndClick(modifierKeys, "double click", elemBinding)
    verify(mockAction).perform()
    env.scopes.get("element/double click") should be ("true")
  }

  "WebContext.holdAndClick" should "command-shift right click element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val modifierKeys = Array("Command", "Shift")
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.keyDown(Keys.SHIFT)).thenReturn(mockActions)
    when(mockActions.contextClick(mockElement)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.SHIFT)).thenReturn(mockActions)
    when(mockActions.keyUp(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.holdAndClick(modifierKeys, "right click", elemBinding)
    verify(mockAction).perform()
    env.scopes.get("element/right click") should be ("true")
  }

  "WebContext.holdAndClick" should "error when given unknown modifier key" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val modifierKeys = Array("Unknown")
    intercept[UnsupportedModifierKeyException] {
      ctx.holdAndClick(modifierKeys, "click", elemBinding)
    }
  }

  "WebContext.sendKeys" should "" in {
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    val keys = Array("Command", "C")
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.sendKeys(Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.sendKeys("C")).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.sendKeys(keys)
    verify(mockAction).perform()
  }

  "WebContext.sendKeys to element" should "" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    val keys = Array("Command", "C")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.sendKeys(mockElement, Keys.COMMAND)).thenReturn(mockActions)
    when(mockActions.sendKeys(mockElement, "C")).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.sendKeys(elemBinding, keys)
    verify(mockAction).perform()
  }

  "WebContext.performActionInContext" should "click element in context of another element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("click", "element", "context")
    verify(mockAction).perform()
    env.scopes.get("element/click") should be ("true")
  }

  "WebContext.performActionInContext" should "right click element in context of another element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.contextClick()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("right click", "element", "context")
    verify(mockAction).perform()
    env.scopes.get("element/right click") should be ("true")
  }

  "WebContext.performActionInContext" should "double click element in context of another element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.doubleClick()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("double click", "element", "context")
    verify(mockAction).perform()
    env.scopes.get("element/double click") should be ("true")
  }

  "WebContext.performActionInContext" should "check element in context of another element by sending a space char" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("check", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(2)).perform()
    env.scopes.get("element/check") should be ("true")
  }

  "WebContext.performActionInContext" should "check element in context of another element by clicking it" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("check", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(1)).perform()
    env.scopes.get("element/check") should be ("true")
  }

  "WebContext.performActionInContext" should "not check element in context of another element when it is already checked" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("check", "element", "context")
    verify(mockAction, never).perform()
    env.scopes.get("element/check") should be ("true")
  }

  "WebContext.performActionInContext" should "tick element in context of another element by sending a space char" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("tick", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(2)).perform()
    env.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performActionInContext" should "tick element in context of another element by clicking it" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("tick", "element", "context")
    verify(mockAction, times(1)).perform()
    env.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performActionInContext" should "not tick element in context of another element when it is already ticked" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("tick", "element", "context")
    verify(mockAction, never).perform()
    env.scopes.get("element/tick") should be ("true")
  }

  "WebContext.performActionInContext" should "uncheck element in context of another element by sending a space char" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("uncheck", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(2)).perform()
    env.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performActionInContext" should "uncheck element in context of another element by clicking it" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("uncheck", "element", "context")
    verify(mockAction, times(2)).perform()
    env.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performActionInContext" should "not uncheck element in context of another element when it is already unchecked" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("uncheck", "element", "context")
    verify(mockAction, never).perform()
    env.scopes.get("element/uncheck") should be ("true")
  }

  "WebContext.performActionInContext" should "untick element in context of another element by sending a space char" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("untick", "element", "context")
    verify(mockActions).click()
    verify(mockAction, times(2)).perform()
    env.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performActionInContext" should "untick element in context of another element by clicking it" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(true, true)
    when(mockActions.click()).thenReturn(mockActions)
    when(mockActions.sendKeys(Keys.SPACE)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("untick", "element", "context")
    verify(mockAction, times(2)).perform()
    env.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performActionInContext" should "not untick element in context of another element when it is already unticked" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockElement.isSelected).thenReturn(false, false)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("untick", "element", "context")
    verify(mockAction, never).perform()
    env.scopes.get("element/untick") should be ("true")
  }

  "WebContext.performActionInContext" should "move to element in context of another element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val ctxBinding = LocatorBinding("context", SelectorType.id, "ctx", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockContext = mock[WebElement]
    val mockActions = mock[Actions]
    val mockAction = mock[Action]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(ctxBinding).when(ctx).getLocatorBinding("context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockContext).when(mockLocator).locate(ctxBinding)
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockContext)).thenReturn(mockActions)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    when(mockActions.build()).thenReturn(mockAction)
    ctx.performActionInContext("move to", "element", "context")
    verify(mockAction).perform()
    env.scopes.get("element/move to") should be ("true")
  }

  "WebContext.performActionInContext" should "fall back to non context action when element name contains 'of' literal" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockActions = mock[Actions]
    doThrow(new LocatorBindingException("element not bound")).when(ctx).getLocatorBinding("element")
    doReturn(elemBinding).when(ctx).getLocatorBinding("element of context")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockActions).when(ctx).createActions(mockWebDriver)
    when(mockActions.moveToElement(mockElement)).thenReturn(mockActions)
    ctx.performActionInContext("move to", "element", "context")
    verify(mockActions).perform()
    env.scopes.get("element/move to") should be ("true")
  }

  "WebContext.performActionInContext" should "fail when elmenet cannot be located" in {
    doThrow(new LocatorBindingException("element not bound")).when(ctx).getLocatorBinding("element")
    doThrow(new LocatorBindingException("element not bound")).when(ctx).getLocatorBinding("element of context")
    intercept[LocatorBindingException] {
      ctx.performActionInContext("move to", "element", "context")
    }
  }

  "WebContext.waitForText" should "return true when text is present" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    doReturn(Some("text")).when(ctx).getElementText(elemBinding)
    ctx.waitForText(elemBinding) should be (true)
  }

  "WebContext.waitForText" should "return false when text is absent" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    doReturn(None).when(ctx).getElementText(elemBinding)
    ctx.waitForText(elemBinding) should be (false)
  }

  "WebContext.waitForText" should "return false when text is blank" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    doReturn(Some("")).when(ctx).getElementText(elemBinding)
    ctx.waitForText(elemBinding) should be (false)
  }

  "WebContext.scrollIntoView" should "scroll to top of element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(null).when(ctx).executeJS(s"var elem = arguments[0]; if (typeof elem !== 'undefined' && elem != null) { elem.scrollIntoView(true); }", mockElement)(takeScreenShot = false)
    ctx.scrollIntoView(elemBinding, ScrollTo.top)
  }

  "WebContext.scrollIntoView" should "scroll to bottom of element" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(null).when(ctx).executeJS(s"var elem = arguments[0]; if (typeof elem !== 'undefined' && elem != null) { elem.scrollIntoView(false); }", mockElement)(takeScreenShot = false)
    ctx.scrollIntoView(elemBinding, ScrollTo.bottom)
  }

  "WebContext.resizeWindow" should "resize the window" in {
    val mockDriverOptions = mock[WebDriver.Options]
    val mockWindow = mock[WebDriver.Window]
    when(mockWebDriver.manage).thenReturn(mockDriverOptions)
    when(mockDriverOptions.window).thenReturn(mockWindow)
    ctx.resizeWindow(300, 400)
    verify(mockWindow).setSize(new Dimension(300, 400))
  }

  "WebContext.maximizeWindow" should "maximize the window" in {
    val mockDriverOptions = mock[WebDriver.Options]
    val mockWindow = mock[WebDriver.Window]
    when(mockWebDriver.manage).thenReturn(mockDriverOptions)
    when(mockDriverOptions.window).thenReturn(mockWindow)
    ctx.maximizeWindow()
    verify(mockWindow).maximize()
  }

  "ctx.captureCurrentUrl" should "capture url in default attribute" in {
    when(mockWebDriver.getCurrentUrl).thenReturn("http://site.com")
    ctx.captureCurrentUrl(None)
    env.topScope.get("the current URL") should be ("http://site.com")
  }

  "ctx.captureCurrentUrl" should "capture url in provided attribute" in {
    when(mockWebDriver.getCurrentUrl).thenReturn("http://site.com")
    ctx.captureCurrentUrl(Some("my URL"))
    env.topScope.get("my URL") should be ("http://site.com")
  }

  "WebContext.getElementText" should "return blank on element with null text value, null text attribute, null value attribute, and null JS value" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.getText).thenReturn(null)
    when(mockElement.getAttribute("text")).thenReturn(null)
    when(mockElement.getAttribute("value")).thenReturn(null)
    doReturn(null).when(ctx).executeJS("(function(element){return element.innerText || element.textContent || ''})(arguments[0]);", mockElement)(takeScreenShot = false)
    ctx.getElementText(elemBinding) should be (Some(""))
    env.scopes.get("element/text") should be ("")
  }

  "WebContext.getElementText" should "return blank on element with blank text value, blank text attribute, blank value attribute, and blank JS value" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.getText).thenReturn("")
    when(mockElement.getAttribute("text")).thenReturn("")
    when(mockElement.getAttribute("value")).thenReturn("")
    doReturn("").when(ctx).executeJS("return (function(element){return element.innerText || element.textContent || ''})(arguments[0]);", mockElement)(takeScreenShot = false)
    ctx.getElementText(elemBinding) should be (Some(""))
    env.scopes.get("element/text") should be ("")
  }

  "WebContext.getElementText" should "return JS value on element with null text value, null text attribute, null value attribute, and non null JS value" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.getText).thenReturn(null)
    when(mockElement.getAttribute("text")).thenReturn(null)
    when(mockElement.getAttribute("value")).thenReturn(null)
    doReturn("JSValue").when(ctx).executeJS("return (function(element){return element.innerText || element.textContent || ''})(arguments[0]);", mockElement)(takeScreenShot = false)
    ctx.getElementText(elemBinding) should be (Some("JSValue"))
    env.scopes.get("element/text") should be ("JSValue")
  }

  "WebContext.getElementText" should "return value attribute on element with null text value, null text attribute, and non null value attribute" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.getText).thenReturn(null)
    when(mockElement.getAttribute("text")).thenReturn(null)
    when(mockElement.getAttribute("value")).thenReturn("valueAttr")
    ctx.getElementText(elemBinding) should be (Some("valueAttr"))
    env.scopes.get("element/text") should be ("valueAttr")
  }

  "WebContext.getElementText" should "return text attribute on element with null text value, and non null text attribute" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.getText).thenReturn(null)
    when(mockElement.getAttribute("text")).thenReturn("textAttr")
    ctx.getElementText(elemBinding) should be (Some("textAttr"))
    env.scopes.get("element/text") should be ("textAttr")
  }

  "WebContext.getElementText" should "return text on element with non null text value" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    when(mockElement.getText).thenReturn("text")
    ctx.getElementText(elemBinding) should be (Some("text"))
    env.scopes.get("element/text") should be ("text")
  }

  "WebContext.getElementSelection by text" should "return blank when no options are selected" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement]().asJava, List[WebElement]().asJava)
    ctx.getElementSelection("element", "text") should be (Some(""))
    env.scopes.get("element/selectedText") should be ("")
  }

  "WebContext.getElementSelection by text" should "return text attribute of selected options" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOptionElement1 = mock[WebElement]
    val mockOptionElement2 = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement]().asJava, List[WebElement](mockOptionElement1, mockOptionElement2).asJava)
    when(mockOptionElement1.getAttribute("text")).thenReturn("one")
    when(mockOptionElement2.getAttribute("text")).thenReturn("two")
    ctx.getElementSelection("element", "text") should be (Some("one,two"))
    env.scopes.get("element/selectedText") should be ("one,two")
  }

  "WebContext.getElementSelection by text" should "return text value of selected options" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOptionElement1 = mock[WebElement]
    val mockOptionElement2 = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement](mockOptionElement1, mockOptionElement2).asJava)
    when(mockOptionElement1.getText).thenReturn("one")
    when(mockOptionElement2.getText).thenReturn("two")
    ctx.getElementSelection("element", "text") should be (Some("one,two"))
    env.scopes.get("element/selectedText") should be ("one,two")
  }

  "WebContext.getElementSelection by value" should "return blank when no options are selected" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement]().asJava)
    ctx.getElementSelection("element", "value") should be (Some(""))
    env.scopes.get("element/selectedValue") should be ("")
  }

  "WebContext.getElementSelection by value" should "return value attribute of selected options" in {
    val elemBinding = LocatorBinding("element", SelectorType.id, "elem", None, None, ctx)
    val mockElement = mock[WebElement]
    val mockSelect = mock[Select]
    val mockOptionElement1 = mock[WebElement]
    val mockOptionElement2 = mock[WebElement]
    doReturn(elemBinding).when(ctx).getLocatorBinding("element")
    doReturn(mockElement).when(mockLocator).locate(any[LocatorBinding])
    doReturn(mockSelect).when(ctx).createSelect(mockElement)
    when(mockSelect.getAllSelectedOptions).thenReturn(List[WebElement](mockOptionElement1, mockOptionElement2).asJava)
    when(mockOptionElement1.getAttribute("value")).thenReturn("one")
    when(mockOptionElement2.getAttribute("value")).thenReturn("two")
    ctx.getElementSelection("element", "value") should be (Some("one,two"))
    env.scopes.get("element/selectedValue") should be ("one,two")
  }

  "WebContext.switchToSession" should "switch to given session" in {
    doNothing().when(driverManager).switchToSession("my session")
    ctx.switchToSession("my session")
    verify(driverManager).switchToSession("my session")
  }

  "WebContext.switchToChild" should "switch to child window" in {
    doReturn(Set("parent", "child").asJava).when(mockWebDriver).getWindowHandles()
    doNothing().when(driverManager).switchToChild(1)
    ctx.switchToChild()
    verify(driverManager).switchToChild(1)
  }

  "WebContext.closeChild" should "close the child window" in {
    doNothing().when(driverManager).closeChild()
    ctx.closeChild()
    verify(driverManager).closeChild()
  }

  "WebContext.switchToParent" should "switch to parent" in {
    doNothing().when(driverManager).switchToParent()
    ctx.switchToParent()
    verify(driverManager).switchToParent()
  }

  "WebContext.switchToDefaultContent" should "switch to default content" in {
    doNothing().when(driverManager).switchToDefaultContent()
    ctx.switchToDefaultContent()
    verify(driverManager).switchToDefaultContent()
  }

  "WebContext.refreshPage" should "refresh the current page" in {
    val mockNavigation = mock[WebDriver.Navigation]
    when(mockWebDriver.navigate()).thenReturn(mockNavigation)
    ctx.refreshPage()
    verify(mockNavigation).refresh()
  }

  "WebContext.handleAlert" should "handle accept alert" in {
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    val mockAlert = mock[Alert]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.alert()).thenReturn(mockAlert)
    ctx.handleAlert(true)
    verify(mockAlert).accept()
  }

  "WebContext.handleAlert" should "handle dismiss alert" in {
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    val mockAlert = mock[Alert]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockTargetLocator.alert()).thenReturn(mockAlert)
    ctx.handleAlert(false)
    verify(mockAlert).dismiss()
  }

  "WebContext.navigateTo" should "navigate to given URL" in {
    ctx.navigateTo("http://site.com")
    verify(mockWebDriver).get("http://site.com")
  }

  "WebContext.getPopupMessage" should "return the message" in {
    val mockTargetLocaotr = mock[WebDriver.TargetLocator]
    val mockAlert = mock[Alert]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocaotr)
    when(mockTargetLocaotr.alert()).thenReturn(mockAlert)
    when(mockAlert.getText).thenReturn("popup message")
    ctx.getPopupMessage should be ("popup message")
  }

}

class MockDriverManager(mockWebDriver: WebDriver) extends DriverManager {
  override private [eval] def loadWebDriver: WebDriver = mockWebDriver
}

abstract class MockWebDriver extends WebDriver with TakesScreenshot with JavascriptExecutor
