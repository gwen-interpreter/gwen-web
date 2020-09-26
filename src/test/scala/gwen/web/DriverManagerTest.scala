/*
 * Copyright 2015-2019 Brady Wood, Branko Juric
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

import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebDriver.Options
import org.openqa.selenium.WebDriver.Timeouts
import org.openqa.selenium.WebDriver.Window
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.RemoteWebDriver
import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.mockito.MockitoSugar
import gwen.web.Errors.NoSuchWindowException
import gwen.Errors.AmbiguousCaseException
import scala.jdk.CollectionConverters._

class DriverManagerTest extends BaseTest with Matchers with MockitoSugar with BeforeAndAfterEach {
  
  val mockChromeDriver: WebDriver = createMockLocalDriver
  val mockFirefoxDriver: WebDriver = createMockLocalDriver
  val mockIeDriver: WebDriver = createMockLocalDriver
  val mockEdgeDriver: WebDriver = createMockLocalDriver
  val mockSafariDriver: WebDriver = createMockLocalDriver
  val mockRemoteDriver: RemoteWebDriver = createMockRemoteDriver
  
  override def afterEach(): Unit = {
    DriverManager.DriverPermit.release();
  }
  
  "Firefox setting" should "load firefox driver" in {
    val manager = newManager("firefox")
    manager.withWebDriver { _ should be (mockFirefoxDriver) }
  }

  "Chrome setting" should "load chrome driver" in {
    val manager = newManager("chrome")
    manager.withWebDriver { _ should be (mockChromeDriver) }
  }

  "IE setting" should "load IE driver" in {
    val manager = newManager("ie")
    manager.withWebDriver { _ should be (mockIeDriver) }
  }

  "Edge setting" should "load Edge driver" in {
    val manager = newManager("edge")
    manager.withWebDriver { _ should be (mockEdgeDriver) }
  }

  "Safari setting" should "load safari driver" in {
    val manager = newManager("safari")
    manager.withWebDriver { _ should be (mockSafariDriver) }
  }

  "Hub URL setting" should "load remote web driver" in {
    withSetting("gwen.web.remote.url", "http://localhost:44466/wd/hub") {
      val manager = newManager("chrome")
      val driver = manager.withWebDriver { _.asInstanceOf[RemoteWebDriver] }
      driver should be (mockRemoteDriver)
    }
  }

  "Web driver" should "quit on manager quit" in {
    val manager = newManager()
    val mockWebDriver = manager.withWebDriver { webDriver => webDriver }
    manager.quit()
    verify(mockWebDriver).quit()
  }

  "Quitting manager" should "create new webdriver on subsequent access" in {
    val manager = newManager()
    val webDriver1 = manager.withWebDriver { webDriver => webDriver }
    manager.quit()
    verify(webDriver1).quit()
    val webDriver2 = manager.withWebDriver { webDriver => webDriver }
    webDriver1 should not be webDriver2
  }

  "Accessing web driver without closing manager" should "return the same web driver instance" in {

    val manager = newManager()
    val webDriver1 = manager.withWebDriver { webDriver => webDriver }
    val webDriver2 = manager.withWebDriver { webDriver => webDriver }
    webDriver1 should be (webDriver2)
  }

  "Quitting the manager multiple times" should "quit the web driver only once" in {

    val manager = newManager()
    val mockWebDriver = manager.withWebDriver { webDriver => webDriver }

    // calling quit multiple times on manager should call quit on web driver just once
    manager.quit()
    manager.quit()
    verify(mockWebDriver, times(1)).quit()
  }

  "Quitting new manager without referencing webdriver" should "not quit the web driver" in {
    val mockWebDriver = mock[WebDriver]
    val manager = newManager(mockWebDriver)
    manager.quit()
    verify(mockWebDriver, never()).quit()
  }

  "DriverManager.switchToChild" should "error when there is no child window" in {
    val mockWebDriver = mock[WebDriver]
    val manager = newManager(mockWebDriver)
    intercept[NoSuchWindowException] {
      manager.switchToChild(mockWebDriver)
    }
  }

  "DriverManager.switchToChild" should "should switch to child window" in {
    val mockWebDriver = mock[WebDriver]
    val manager = newManager(mockWebDriver)
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    manager.drivers.put("primary", mockWebDriver)
    when(mockWebDriver.getWindowHandles).thenReturn(Set("child").asJava)
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    manager.switchToChild(mockWebDriver)
    verify(mockTargetLocator).window("child")
  }

  "DriverManager.switchToChild" should "should error when there is more than one child window" in {
    val mockWebDriver = mock[WebDriver]
    val manager = newManager(mockWebDriver)
    manager.drivers.put("primary", mockWebDriver)
    when(mockWebDriver.getWindowHandles).thenReturn(Set("child1", "child2").asJava)
    intercept[AmbiguousCaseException] {
      manager.switchToChild(mockWebDriver)
    }
  }

  "DriverManager.closeChild" should "error when there is no root or child window" in {
    val mockWebDriver = mock[WebDriver]
    val manager = newManager(mockWebDriver)
    intercept[NoSuchWindowException] {
      manager.closeChild()
    }
  }

  "DriverManager.closeChild" should "error when there is a root window but no child window" in {
    val mockWebDriver = mock[WebDriver]
    val manager = newManager(mockWebDriver)
    manager.pushWindow("root")
    intercept[NoSuchWindowException] {
      manager.closeChild()
    }
  }

  "DriverManager.closeChild" should "close child window" in {
    val mockWebDriver = mock[WebDriver]
    val manager = newManager(mockWebDriver)
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    manager.pushWindow("root")
    manager.pushWindow("child")
    manager.closeChild()
    verify(mockTargetLocator).window("child")
    verify(mockTargetLocator).window("root")
    verify(mockWebDriver).close()
  }

  "DriverManager.switchToDefaultContent" should "" in {
    val mockWebDriver = mock[WebDriver]
    val manager = newManager(mockWebDriver)
    val mockTargetLocator = mock[WebDriver.TargetLocator]
    when(mockWebDriver.switchTo()).thenReturn(mockTargetLocator)
    manager.switchToDefaultContent()
    verify(mockTargetLocator).defaultContent()
  }
  
  private def newManager(driverName: String): DriverManager = new DriverManager {
    override private[web] def chrome(): WebDriver = mockChromeDriver
    override private[web] def firefox(): WebDriver = mockFirefoxDriver
    override private[web] def ie(): WebDriver = mockIeDriver
    override private[web] def edge(): WebDriver = mockEdgeDriver
    override private[web] def safari(): WebDriver = mockSafariDriver
    override private[web] def remote(hubUrl: String, capabilities: DesiredCapabilities): WebDriver = {
      val mockDriver = mockRemoteDriver
      when(mockDriver.getCapabilities).thenReturn(capabilities)
      mockDriver
    }
    override private[web] def loadWebDriver: WebDriver = withSetting("gwen.web.browser", driverName) {
      super.loadWebDriver
    }
  }
  
  private def newManager(mockDriver: WebDriver): DriverManager = new  DriverManager {
    override private[web] def loadWebDriver: WebDriver = mockDriver
  }
  
  private def newManager(): DriverManager = new DriverManager {
    override private[web] def loadWebDriver: WebDriver = mock[WebDriver]
  }
  
  private def createMockLocalDriver: WebDriver = createMockDriver(mock[WebDriver])
  private def createMockRemoteDriver: RemoteWebDriver = createMockDriver(mock[RemoteWebDriver])
  
  private def createMockDriver[T <: WebDriver](mockDriver: T): T = {
    val mockOptions = mock[Options]
    val mockTimeouts = mock[Timeouts]
    val mockWindow = mock[Window]
    when(mockDriver.manage()).thenReturn(mockOptions)
    when(mockOptions.timeouts()).thenReturn(mockTimeouts)
    when(mockOptions.window()).thenReturn(mockWindow)
    mockDriver
  }
  
}