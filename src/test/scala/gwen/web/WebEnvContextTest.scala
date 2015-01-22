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

import java.util.concurrent.TimeUnit

import org.mockito.Matchers.anyLong
import org.mockito.Matchers.same
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.when
import org.openqa.selenium.WebDriver
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar

import gwen.eval.ScopedDataStack

class WebEnvContextTest extends FlatSpec with Matchers with MockitoSugar {
  
  val mockWebDriverOptions = mock[WebDriver.Options]
  val mockWebDriverTimeouts = mock[WebDriver.Timeouts]
  
  "New web env context" should "have 'feature' scope" in {
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)
    env.scopes.current.scope should be ("feature")
  }
  
  "Bound scope attribute" should "be recreated after reset" in {
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)
    env.scopes.addScope("login")
    env.scopes.set("username", "Gwen")
    env.scopes.get("username") should be ("Gwen")
    env.reset
    env.scopes.current.scope should be ("feature")
    env.scopes.getOpt("username") should be (None)
  }
  
  "json on new env context" should "be empty" in {
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)
    env.json.toString should be ("""{"scopes":[{"scope":"feature","atts":[]}]}""")
  }
  
  "Bound scope attribute" should "show up in JSON string" in {
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)
    env.scopes.addScope("login")
    env.scopes.set("username", "Gwen")
    env.scopes.get("username") should be ("Gwen")
    env.json.toString should be ("""{"scopes":[{"scope":"feature","atts":[]},{"scope":"login","atts":[{"username":"Gwen"}]}]}""")
                                      
  }
  
  "Closing new web env context without referencing webdriver" should "not quit web driver" in {
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)
    env.close()
    verify(mockWebDriver, never()).quit()
  }
  
  "Closing new web env context after referencing webdriver" should "quit web driver" in {
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)
    env.webDriver
    env.close()
    verify(mockWebDriver).quit()
  }
  
  "Resetting new web env context without referencing webdriver" should "not quit web driver" in {
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)
    env.reset()
    verify(mockWebDriver, never()).quit()
  }
  
  "Resetting new web env context after referencing webdriver" should "quit web driver" in {
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)
    env.webDriver
    env.reset()
    verify(mockWebDriver).quit()
  }
  
  "Referencing webdriver" should "should close only once after creation" in {
	
    val mockWebDriver = mock[WebDriver]
    val env = newEnv(mockWebDriver)

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    
    // reference 1st time to force creation
    env.webDriver
    
    // calling close multiple times should call quit only once
    env.close()
    env.close()
    verify(mockWebDriver, times(1)).quit()
  }
  
  def newEnv(driver: WebDriver) = new WebEnvContext("Firefox", new ScopedDataStack()) {
    override private[web] def loadWebDriver(driverName: String): WebDriver = driver
  }
  
}