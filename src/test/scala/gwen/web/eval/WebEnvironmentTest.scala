/*
 * Copyright 2014-2024 Brady Wood, Branko Juric
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

import gwen.web._
import gwen.web.eval.WebErrors
import gwen.web.eval.driver.DriverManager

import gwen.core.AssertionMode
import gwen.core.GwenOptions
import gwen.core.eval.ComparisonOperator
import gwen.core.state.EnvState
import gwen.core.state.StateLevel

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers

class WebEnvironmentTest extends BaseTest with Matchers with MockitoSugar {
  
  "New web ctx context" should "have 'top' scope" in {
    val ctx = newCtx()
    ctx.topScope.isTopScope should be (true)
  }
  
  "json on new ctx context" should "be empty" in {
    val ctx = newCtx()
    ctx.topScope.asString(all = false, env = false).replace("\r", "") should be ("""scope : "feature" { }""")
  }
  
  "JavaScript binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.topScope.set("username/javascript", "$('#username').val()")
    ctx.getCachedOrBoundValue("username") should be ("$[dryValue:javascript]")
  }
  
  "XPath binding" should "resolve" in {
    val ctx = newCtx()
    ctx.topScope.set("xml", "<users><user>Gwen</user><user>Stacey</user></users>")
    ctx.topScope.set("username/xpath/source", "xml")
    ctx.topScope.set("username/xpath/targetType", "text")
    ctx.topScope.set("username/xpath/expression", "users/user")
    ctx.getCachedOrBoundValue("username") should be ("Gwen")
    ctx.topScope.set("username/xpath/expression", "users/user[2]")
    ctx.getCachedOrBoundValue("username") should be ("Stacey")
  }

  "XPath binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.topScope.set("xml", "<users><user>Gwen</user><user>Stacey</user></users>")
    ctx.topScope.set("username/xpath/source", "xml")
    ctx.topScope.set("username/xpath/targetType", "text")
    ctx.topScope.set("username/xpath/expression", "users/user")
    ctx.getCachedOrBoundValue("username") should be ("$[dryValue:xpath]")
    ctx.topScope.set("username/xpath/expression", "users/user[2]")
    ctx.getCachedOrBoundValue("username") should be ("$[dryValue:xpath]")
  }
  
  "Regex binding" should "resolve" in {
    val ctx = newCtx()
    ctx.topScope.set("url", "http://www.domain.com?param1=one&param2=two")
    ctx.topScope.set("param1/regex/source", "url")
    ctx.topScope.set("param1/regex/expression", "param1=(.+)&")
    ctx.getCachedOrBoundValue("param1") should be ("one")
    ctx.topScope.set("param2/regex/source", "url")
    ctx.topScope.set("param2/regex/expression", "param2=(.+)")
    ctx.getCachedOrBoundValue("param2") should be ("two")
  }

  "Regex binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.topScope.set("url", "http://www.domain.com?param1=one&param2=two")
    ctx.topScope.set("param1/regex/source", "url")
    ctx.topScope.set("param1/regex/expression", "param1=(.+)&")
    ctx.getCachedOrBoundValue("param1") should be ("$[dryValue:regex]")
    ctx.topScope.set("param2/regex/source", "url")
    ctx.topScope.set("param2/regex/expression", "param2=(.+)")
    ctx.getCachedOrBoundValue("param2") should be ("$[dryValue:regex]")
  }
  
  "Json path binding" should "resolve" in {
    val ctx = newCtx()
    ctx.topScope.set("ctx", """{"scopes":[{"scope":"login","atts":[{"username":"Gwen"}]}]}""")
    ctx.topScope.set("username/json path/source", "ctx")
    ctx.topScope.set("username/json path/expression", "$.scopes[0].atts[0].username")
    ctx.getCachedOrBoundValue("username") should be ("Gwen")
  }

  "Json path binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.topScope.set("ctx", """{"scopes":[{"scope":"login","atts":[{"username":"Gwen"}]}]}""")
    ctx.topScope.set("username/json path/source", "ctx")
    ctx.topScope.set("username/json path/expression", "$.scopes[0].atts[0].username")
    ctx.getCachedOrBoundValue("username") should be ("$[dryValue:json path]")
  }
  
  "Sysproc binding" should "resolve" in {
    val ctx = newCtx()
    ctx.topScope.set("hostname/sysproc", "hostname")
    ctx.getCachedOrBoundValue("hostname") should not be ("")
  }

  "Sysproc binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.topScope.set("hostname/sysproc", "local command")
    ctx.getCachedOrBoundValue("hostname") should be ("$[dryValue:sysproc]")
  }
  
  "File binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.topScope.set("xml/file", "path-to/file.xml")
    ctx.getCachedOrBoundValue("xml") should be ("$[dryValue:file]")
  }
  
  "Sql binding on dry run" should "not resolve" in {
    withSetting("gwen.db.subscribers.driver", "jdbc.driver.class") {
      withSetting("gwen.db.subscribers.url", "db:url") {
        val ctx = newCtx(true)
        ctx.topScope.set("username/sql/selectStmt", "select username from users")
        ctx.topScope.set("username/sql/dbName", "subscribers")
        ctx.getCachedOrBoundValue("username") should be ("$[dryValue:sql]")
      }
    }
  }

  "Timeout on compare" should "result in assertion error" in {
    val ctx = newCtx()
    intercept[AssertionError] {
      ctx.compare("a", "2", () => "1", ComparisonOperator.be, negate = false, None, None, None, AssertionMode.hard)
    }
  }

  "Timeout on negated compare" should "result in assertion error" in {
    val ctx = newCtx()
    intercept[AssertionError] {
      ctx.compare("a", "2", () => "2", ComparisonOperator.be, negate = true, None, None, None, AssertionMode.hard)
    }
  }

  "Attempt to locate unbound element "should "throw locator bindingerror" in {
    val ctx = newCtx()
    shouldFailWithLocatorBindingError("username", ctx, "Undefined selector for: username")
  }

  def newCtx(dry:Boolean = false): WebContext = {
    newCtx(EnvState(), dry)
  }

  def newCtx(state: EnvState): WebContext = {
    newCtx(state, false)
  }

  def newCtx(state: EnvState, dry: Boolean): WebContext = {
    new WebContext(GwenOptions(dryRun=dry), state, mock[DriverManager])
  }

  private def shouldFailWithLocatorBindingError(element: String, ctx: WebContext, expectedMsg: String): Unit = {
    val e = intercept[WebErrors.LocatorBindingException] {
      ctx.getLocatorBinding(element)
    }
    e.getMessage should be (expectedMsg)
  }
  
}