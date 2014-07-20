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
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar

class WebEnvContextTest extends FlatSpec with ShouldMatchers with MockitoSugar {
  
  val mockWebDriver = mock[WebDriver]
  val mockWebDriverOptions = mock[WebDriver.Options]
  val mockWebDriverTimeouts = mock[WebDriver.Timeouts]

  "New web env context" should "have global feature scope" in {
    val env = newEnv
    env.featureScopes.current.get.scope should be ("feature")
    env.featureScopes.current.get.name should be ("global") 
  }
  
  "New web env context" should "have 'global' page scope" in {
    val env = newEnv
    env.pageScopes.current.get.name should be ("global")
  }
  
  "Bound feature scope attribute" should "be removed after reset" in {
    val env = newEnv
    env.featureScopes.set("firstName", "Gwen")
    env.featureScopes.get("firstName") should be ("Gwen")
    env.reset
    env.featureScopes.getOpt("firstName") should be (None)
    env.featureScopes.current.get.scope should be ("feature")
    env.featureScopes.current.get.name should be ("global")
  }
  
  "Bound page scope attribute" should "be recreated after reset" in {
    val env = newEnv
    env.pageScopes.addScope("login")
    env.pageScopes.set("username", "Gwen")
    env.pageScopes.get("username") should be ("Gwen")
    env.reset
    env.pageScopes.current.get.name should be ("global")
    env.pageScopes.getOpt("username") should be (None)
  }
  
  "toJson on new env context" should "be empty" in {
    val env = newEnv
    env.toJson.toString should be ("""{"data":[]}""")
  }
  
  "Bound feature scope attribute" should "show up in JSON string" in {
    val env = newEnv
    env.featureScopes.set("firstName", "Gwen")
    env.featureScopes.get("firstName") should be ("Gwen")
    env.toJson.toString should be ("""{"data":[{"feature":[{"scope":"global","atts":[{"firstName":"Gwen"}]}]}]}""")
  }
  
  "Bound page scope attribute" should "show up in JSON string" in {
    val env = newEnv
    env.pageScopes.addScope("login")
    env.pageScopes.set("username", "Gwen")
    env.pageScopes.get("username") should be ("Gwen")
    env.toJson.toString should be ("""{"data":[{"page":[{"scope":"global","atts":[]},{"scope":"login","atts":[{"username":"Gwen"}]}]}]}""")
  }
  
  "Closing new web env context without referencing webdriver" should "not close web driver" in {
    val env = newEnv
    env.close()
    verify(mockWebDriver, never()).close()
  }
  
  "Referencing webdriver" should "initialise with implicit wait and close correctly" in {
	
    val env = newEnv

    when(mockWebDriver.manage()).thenReturn(mockWebDriverOptions)
    when(mockWebDriverOptions.timeouts()).thenReturn(mockWebDriverTimeouts)
    
    // reference 1t time should setup implicit wait time
    env.webDriver
    verify(mockWebDriverTimeouts).implicitlyWait(anyLong, same(TimeUnit.SECONDS))
    
    // calling close multiple times should call quit only once
    env.close()
    env.close()
    verify(mockWebDriver, times(1)).quit()
  }
  
  def newEnv = new WebEnvContext("Firefox") {
    override private[web] def loadWebDriver(driverName: String): WebDriver = mockWebDriver
  }
  
}