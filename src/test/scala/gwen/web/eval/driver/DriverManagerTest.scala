/*
 * Copyright 2015-2021 Brady Wood, Branko Juric
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

package gwen.web.eval.driver

import gwen.web._
import gwen.web.eval.WebErrors.NoSuchWindowException

import scala.util.chaining._

import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.openqa.selenium.Capabilities
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriver.Options
import org.openqa.selenium.WebDriver.Timeouts
import org.openqa.selenium.WebDriver.Window
import org.openqa.selenium.remote.RemoteWebDriver
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import org.scalatest.matchers.should.Matchers

class DriverManagerTest extends BaseTest with Matchers with MockitoSugar with BeforeAndAfterEach {

  var mockChromeDriver: WebDriver = _
  var mockFirefoxDriver: WebDriver = _
  var mockIeDriver: WebDriver = _
  var mockEdgeDriver: WebDriver = _
  var mockSafariDriver: WebDriver = _
  var mockRemoteDriver: RemoteWebDriver = _

  override def afterEach(): Unit = {
    DriverManager.releaseDriverPermit()
  }

  "Firefox setting" should "load firefox driver" in {
    withSetting("gwen.target.browser", "firefox") {
      val manager = newDriverManager()
      manager.withWebDriver { _ should be (mockFirefoxDriver) }
    }
  }

  "Chrome setting" should "load chrome driver" in {
    withSetting("gwen.target.browser", "chrome") {
      val manager = newDriverManager()
      manager.withWebDriver { _ should be (mockChromeDriver) }
    }
  }

  "IE setting" should "load IE driver" in {
    withSetting("gwen.target.browser", "ie") {
      val manager = newDriverManager()
      manager.withWebDriver { _ should be (mockIeDriver) }
    }
  }

  "Edge setting" should "load Edge driver" in {
    withSetting("gwen.target.browser", "edge") {
      val manager = newDriverManager()
      manager.withWebDriver { _ should be (mockEdgeDriver) }
    }
  }

  "Safari setting" should "load safari driver" in {
    withSetting("gwen.target.browser", "safari") {
      val manager = newDriverManager()
      manager.withWebDriver { _ should be (mockSafariDriver) }
    }
  }

  "Hub URL setting" should "load remote web driver" in {
    withSetting("gwen.web.remote.url", "http://localhost:44466/wd/hub") {
      withSetting("gwen.target.browser", "chrome") {
        val manager = newDriverManager()
        val driver = manager.withWebDriver { _.asInstanceOf[RemoteWebDriver] }
        driver should be (mockRemoteDriver)
      }
    }
  }

  "Web driver" should "quit on manager quit" in {
    val manager = newDriverManager()
    val mockWebDriver = manager.withWebDriver { webDriver => webDriver }
    manager.quit()
    verify(mockWebDriver).quit()
  }

  "Quitting manager" should "create new webdriver on subsequent access" in {
    val manager = newDriverManager()
    val webDriver1 = manager.withWebDriver { webDriver => webDriver }
    manager.quit()
    verify(webDriver1).quit()
    val webDriver2 = manager.withWebDriver { webDriver => webDriver }
    webDriver1 should not be webDriver2
  }

  "Accessing web driver without closing manager" should "return the same web driver instance" in {

    val manager = newDriverManager()
    val webDriver1 = manager.withWebDriver { webDriver => webDriver }
    val webDriver2 = manager.withWebDriver { webDriver => webDriver }
    webDriver1 should be (webDriver2)
  }

  "Quitting the manager multiple times" should "quit the web driver only once" in {

    val manager = newDriverManager()
    val mockWebDriver = manager.withWebDriver { webDriver => webDriver }

    // calling quit multiple times on manager should call quit on web driver just once
    manager.quit()
    manager.quit()
    verify(mockWebDriver, times(1)).quit()
  }

  "DriverManager.switchToChild" should "error when there is no child window" in {
    val mockWebDriver = stubDriverMock( mock[WebDriver])
    val manager = newDriverManager(mockWebDriver)
    intercept[NoSuchWindowException] {
      manager.switchToWindow(1)
    }
  }

  "DriverManager.switchToChild" should "switch to child window" in {
    val mockWebDriver = stubDriverMock( mock[WebDriver])
    val manager = newDriverManager(mockWebDriver)
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    when(mockWebDriver.getWindowHandles).thenReturn(Set("parent", "child").asJava)
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    manager.switchToWindow(1)
    verify(mockTargetLocator).window("child")
  }

  "DriverManager.switchToChild 1" should "return first child when there is more than one child window" in {
    val mockWebDriver = stubDriverMock( mock[WebDriver])
    val manager = newDriverManager(mockWebDriver)
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    when(mockWebDriver.getWindowHandles).thenReturn(Set("parent", "child1", "child2").asJava)
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    manager.switchToWindow(1)
    verify(mockTargetLocator).window("child1")
  }

  "DriverManager.switchToChild 2" should "return second child when there is more than one child window" in {
    val mockWebDriver = stubDriverMock( mock[WebDriver])
    val manager = newDriverManager(mockWebDriver)
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    when(mockWebDriver.getWindowHandles).thenReturn(Set("parent", "child1", "child2").asJava)
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    manager.switchToWindow(2)
    verify(mockTargetLocator).window("child2")
  }

  "DriverManager.closeChild" should "error when there is no root or child window" in {
    val mockWebDriver = stubDriverMock( mock[WebDriver])
    val manager = newDriverManager(mockWebDriver)
    intercept[NoSuchWindowException] {
      manager.closeChild()
    }
  }

  "DriverManager.closeChild" should "error when there is a root window but no child window" in {
    val mockWebDriver = stubDriverMock( mock[WebDriver])
    val manager = newDriverManager(mockWebDriver)
    intercept[NoSuchWindowException] {
      manager.closeChild()
    }
  }

  "DriverManager.closeChild" should "close child window" in {
    val mockWebDriver = stubDriverMock( mock[WebDriver])
    val manager = newDriverManager(mockWebDriver)
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    when(mockWebDriver.getWindowHandles).thenReturn(Set("root", "child").asJava)
    manager.closeChild()
    verify(mockTargetLocator).window("child")
    verify(mockTargetLocator).window("root")
    verify(mockWebDriver).close()
  }

  "DriverManager.switchToDefaultContent" should "" in {
    val mockWebDriver = stubDriverMock( mock[WebDriver])
    val manager = newDriverManager(mockWebDriver)
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    manager.switchToDefaultContent()
    verify(mockTargetLocator).defaultContent()
  }

  private def newDriverManager(): DriverManager ={
    new DriverManager {
      override private [eval] def chrome(): WebDriver = createMockLocalDriver() tap { driver =>
        mockChromeDriver = driver
      }
      override private [eval] def firefox(): WebDriver = createMockLocalDriver() tap { driver =>
        mockFirefoxDriver = driver
      }
      override private [eval] def ie(): WebDriver = createMockLocalDriver() tap { driver =>
        mockIeDriver = driver
      }
      override private [eval] def edge(): WebDriver = createMockLocalDriver() tap { driver =>
        mockEdgeDriver = driver
      }
      override private [eval] def safari(): WebDriver = createMockLocalDriver() tap { driver =>
        mockSafariDriver = driver
      }
      override private [eval] def remote(hubUrl: String, capabilities: Capabilities): WebDriver = {
        val mockDriver = createMockRemoteDriver()
        when(mockDriver.getCapabilities).thenReturn(capabilities)
        mockRemoteDriver = mockDriver
        mockDriver
      }
    }
  }

  private def newDriverManager(mockDriver: WebDriver): DriverManager = new DriverManager() {
    override private [eval] val drivers = mutable.Map() += ("primary" -> mockDriver)
  }
  private def createMockLocalDriver(): WebDriver = stubDriverMock(mock[WebDriver])
  private def createMockRemoteDriver(): RemoteWebDriver = stubDriverMock(mock[RemoteWebDriver])

  private def stubDriverMock[T <: WebDriver](mockDriver: T): T = {
    val mockOptions = mock[Options]
    val mockTimeouts = mock[Timeouts]
    val mockWindow = mock[Window]
    when(mockDriver.manage()).thenReturn(mockOptions)
    when(mockOptions.timeouts()).thenReturn(mockTimeouts)
    when(mockOptions.window()).thenReturn(mockWindow)
    mockDriver
  }

}
