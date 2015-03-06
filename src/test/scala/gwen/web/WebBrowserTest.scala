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

import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.openqa.selenium.WebDriver
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar

class WebBrowserTest extends FlatSpec with Matchers with MockitoSugar {
  
  val mockWebDriverOptions = mock[WebDriver.Options]
  val mockWebDriverTimeouts = mock[WebDriver.Timeouts]
  
  "Web driver" should "should quit on browser quit" in {
    val browser = newBrowser()
    val mockWebDriver = browser.webDriver
    browser.quit()
    verify(mockWebDriver).quit()
  }
  
  "Quitting browser" should "create new webdriver on subsequent access" in {
    
    val browser = newBrowser()
    val webDriver1 = browser.webDriver
    browser.quit()
    val webDriver2 = browser.webDriver
    webDriver1 should not be (webDriver2)
  }
  
  "Accessing web driver without closing browser" should "return the same web driver instance" in {
    
    val browser = newBrowser()
    val webDriver1 = browser.webDriver
    val webDriver2 = browser.webDriver
    webDriver1 should be (webDriver2)
  }
  
  "Quitting the browser multiple times" should "quit the web driver only once" in {
	
    val browser = newBrowser()
    val mockWebDriver = browser.webDriver

    // calling quit multiple times on browser should call quit on web driver just once
    browser.quit()
    browser.quit()
    verify(mockWebDriver, times(1)).quit()
  }
  
  "Quitting new browser without referencing webdriver" should "not quit the web driver" in {
    val mockWebDriver = mock[WebDriver]
    val browser = newBrowser(mockWebDriver)
    browser.quit()
    verify(mockWebDriver, never()).quit()
  }
  
  private def newBrowser(): WebBrowser = new WebBrowser() {
    override private[web] def loadWebDriver: WebDriver = mock[WebDriver]
  }
  
  private def newBrowser(driver: WebDriver): WebBrowser = new WebBrowser() {
    override private[web] def loadWebDriver: WebDriver = driver
  }
  
}