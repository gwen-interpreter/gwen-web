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
import org.mockito.Mockito.verify
import org.openqa.selenium.WebDriver
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.mock.MockitoSugar

import gwen.eval.ScopedDataStack

class WebEnvContextTest extends FlatSpec with Matchers with MockitoSugar {
  
  val mockWebDriverOptions = mock[WebDriver.Options]
  val mockWebDriverTimeouts = mock[WebDriver.Timeouts]
  
  "New web env context" should "have 'feature' scope" in {
    val mockWebBrowser = mock[WebBrowser]
    val env = newEnv(mockWebBrowser)
    env.scopes.current.scope should be ("feature")
  }
  
  "Bound scope attribute" should "be recreated after reset" in {
    val mockWebBrowser = mock[WebBrowser]
    val env = newEnv(mockWebBrowser)
    env.scopes.addScope("login")
    env.scopes.set("username", "Gwen")
    env.scopes.get("username") should be ("Gwen")
    env.reset
    env.scopes.current.scope should be ("feature")
    env.scopes.getOpt("username") should be (None)
  }
  
  "json on new env context" should "be empty" in {
    val mockWebBrowser = mock[WebBrowser]
    val env = newEnv(mockWebBrowser)
    env.json.toString should be ("""{"scopes":[{"scope":"feature","atts":[]}]}""")
  }
  
  "Bound scope attribute" should "show up in JSON string" in {
    val mockWebBrowser = mock[WebBrowser]
    val env = newEnv(mockWebBrowser)
    env.scopes.addScope("login")
    env.scopes.set("username", "Gwen")
    env.scopes.get("username") should be ("Gwen")
    env.json.toString should be ("""{"scopes":[{"scope":"feature","atts":[]},{"scope":"login","atts":[{"username":"Gwen"}]}]}""")
                                      
  }
  
  "Resetting new web env context after referencing webdriver" should "quit the browser" in {
    val mockWebBrowser = mock[WebBrowser]
    val env = newEnv(mockWebBrowser)
    env.webDriver
    env.reset()
    verify(mockWebBrowser).quit()
  }
  
  def newEnv(browser: WebBrowser) = {
   new WebEnvContext(new ScopedDataStack()) {
     override def webDriver: WebDriver = mock[WebDriver]
     override def close() { browser.quit() }
   }
  }
  
}