/*
 * Copyright 2015 Brady Wood, Branko Juric
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
import org.openqa.selenium.WebDriver
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar

class DriverManagerTest extends FlatSpec with Matchers with MockitoSugar {
  
  "Web driver" should "quit on manager quit" in {
    val manager = newManager()
    val mockWebDriver = manager.webDriver
    manager.quit()
    verify(mockWebDriver).quit()
  }
  
  "Quitting manager" should "create new webdriver on subsequent access" in {
    
    val manager = newManager()
    val webDriver1 = manager.webDriver
    manager.quit()
    val webDriver2 = manager.webDriver
    webDriver1 should not be (webDriver2)
  }
  
  "Accessing web driver without closing manager" should "return the same web driver instance" in {
    
    val manager = newManager()
    val webDriver1 = manager.webDriver
    val webDriver2 = manager.webDriver
    webDriver1 should be (webDriver2)
  }
  
  "Quitting the manager multiple times" should "quit the web driver only once" in {

    val manager = newManager()
    val mockWebDriver = manager.webDriver

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
  
  private def newManager(): DriverManager = new DriverManager() {
    override private[web] def loadWebDriver: WebDriver = mock[WebDriver]
  }
  
  private def newManager(driver: WebDriver): DriverManager = new DriverManager() {
    override private[web] def loadWebDriver: WebDriver = driver
  }
  
}