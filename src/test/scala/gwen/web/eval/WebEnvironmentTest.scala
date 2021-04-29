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

package gwen.web.eval

import gwen.GwenOptions
import gwen.eval.EvalEnvironment
import gwen.model.StateLevel
import gwen.web._
import gwen.web.WebErrors.LocatorBindingException

import org.mockito.Mockito.verify
import org.scalatest.Matchers
import org.scalatestplus.mockito.MockitoSugar

class WebEnvironmentTest extends BaseTest with Matchers with MockitoSugar {
  
  "New web env context" should "have 'top' scope" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.current.isTopScope should be (true)
    }
  }
  
  "Bound scope attribute" should "be recreated after reset" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.addScope("login")
      env.scopes.set("username", "Gwen")
      env.scopes.get("username") should be ("Gwen")
    }
    ctx.reset(StateLevel.feature)
    ctx.withEnv { env =>
      env.scopes.current.isTopScope should be (true)
      env.scopes.getOpt("username") should be (None)
    }
  }
  
  "json on new env context" should "be empty" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.asString.replace("\r", "") should be (
        """|{
           |  scopes { }
           |}""".stripMargin.replace("\r", ""))
    }
  }
  
  "Bound scope attribute" should "show up in JSON string" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.addScope("login")
      env.scopes.set("username", "Gwen")
      env.scopes.get("username") should be ("Gwen")
      env.scopes.asString.replace("\r", "") should be (
        """|{
           |  scopes {
           |    scope : "login" {
           |      username : "Gwen"
           |    }
           |  }
           |}""".stripMargin.replace("\r", ""))
    }
                                      
  }
  
  "Resetting eval context at feature level" should "reset environment" in {
    val mockEnv = mock[EvalEnvironment]
    val ctx = newCtx(mockEnv)
    ctx.reset(StateLevel.feature)
    verify(mockEnv).reset(StateLevel.feature)
  }

  "Resetting eval context at scenario level" should "reset environment" in {
    val mockEnv = mock[EvalEnvironment]
    val ctx = newCtx(mockEnv)
    ctx.reset(StateLevel.scenario)
    verify(mockEnv).reset(StateLevel.scenario)
  }

  "Closing eval context" should "close environment" in {
    val mockEnv = mock[EvalEnvironment]
    val ctx = newCtx(mockEnv)
    ctx.close()
    verify(mockEnv).close()
  }

  "Attribute with text binding" should "be resolved" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.set("username/text", "Gwen")
    }
    ctx.getAttribute("username") should be ("Gwen")
  }
  
  "Attribute with javascript binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.withEnv { env =>
      env.scopes.set("username/javascript", "$('#username').val()")
    }
    ctx.getAttribute("username") should be ("$[dryRun:javascript]")
  }
  
  "Attribute with xpath binding" should "resolve" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.set("xml", "<users><user>Gwen</user><user>Stacey</user></users>")
      env.scopes.set("username/xpath/source", "xml")
      env.scopes.set("username/xpath/targetType", "text")
      env.scopes.set("username/xpath/expression", "users/user")
    }
    ctx.getAttribute("username") should be ("Gwen")
    ctx.withEnv { env =>
      env.scopes.set("username/xpath/expression", "users/user[2]")
    }
    ctx.getAttribute("username") should be ("Stacey")
  }

  "Attribute with xpath binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.withEnv { env =>
      env.scopes.set("xml", "<users><user>Gwen</user><user>Stacey</user></users>")
      env.scopes.set("username/xpath/source", "xml")
      env.scopes.set("username/xpath/targetType", "text")
      env.scopes.set("username/xpath/expression", "users/user")
    }
    ctx.getAttribute("username") should be ("$[dryRun:xpath]")
    ctx.withEnv { env =>
      env.scopes.set("username/xpath/expression", "users/user[2]")
    }
    ctx.getAttribute("username") should be ("$[dryRun:xpath]")
  }
  
  "Attribute with regex binding" should "resolve" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.set("url", "http://www.domain.com?param1=one&param2=two")
      env.scopes.set("param1/regex/source", "url")
      env.scopes.set("param1/regex/expression", "param1=(.+)&")
    }
    ctx.getAttribute("param1") should be ("one")
    ctx.withEnv { env =>
      env.scopes.set("param2/regex/source", "url")
      env.scopes.set("param2/regex/expression", "param2=(.+)")
    }
    ctx.getAttribute("param2") should be ("two")
  }

  "Attribute with regex binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.withEnv { env =>
      env.scopes.set("url", "http://www.domain.com?param1=one&param2=two")
      env.scopes.set("param1/regex/source", "url")
      env.scopes.set("param1/regex/expression", "param1=(.+)&")
    }
    ctx.getAttribute("param1") should be ("$[dryRun:regex]")
    ctx.withEnv { env =>
      env.scopes.set("param2/regex/source", "url")
      env.scopes.set("param2/regex/expression", "param2=(.+)")
    }
    ctx.getAttribute("param2") should be ("$[dryRun:regex]")
  }
  
  "Attribute with json path binding" should "resolve" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.set("env", """{"scopes":[{"scope":"login","atts":[{"username":"Gwen"}]}]}""")
      env.scopes.set("username/json path/source", "env")
      env.scopes.set("username/json path/expression", "$.scopes[0].atts[0].username")
    }
    ctx.getAttribute("username") should be ("Gwen")
  }

  "Attribute with json path binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.withEnv { env =>
      env.scopes.set("env", """{"scopes":[{"scope":"login","atts":[{"username":"Gwen"}]}]}""")
      env.scopes.set("username/json path/source", "env")
      env.scopes.set("username/json path/expression", "$.scopes[0].atts[0].username")
    }
    ctx.getAttribute("username") should be ("$[dryRun:json path]")
  }
  
  "Attribute with sysproc binding" should "resolve" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.set("hostname/sysproc", "hostname")
    }
    ctx.getAttribute("hostname") should not be ("")
  }

  "Attribute with sysproc binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.withEnv { env =>
      env.scopes.set("hostname/sysproc", "local command")
    }
    ctx.getAttribute("hostname") should be ("$[dryRun:sysproc]")
  }
  
  "Attribute with file binding on dry run" should "not resolve" in {
    val ctx = newCtx(true)
    ctx.withEnv { env =>
      env.scopes.set("xml/file", "path-to/file.xml")
    }
    ctx.getAttribute("xml") should be ("$[dryRun:file]")
  }
  
  "Attribute with sql binding on dry run" should "not resolve" in {
    withSetting("gwen.db.subscribers.driver", "jdbc.driver.class") {
      withSetting("gwen.db.subscribers.url", "db:url") {
        val ctx = newCtx(true)
        ctx.withEnv { env =>
          env.scopes.set("username/sql/selectStmt", "select username from users")
          env.scopes.set("username/sql/dbName", "subscribers")
        }
        ctx.getAttribute("username") should be ("$[dryRun:sql]")
      }
    }
  }

  "Timeout on compare" should "result in assertion error" in {
    val ctx = newCtx()
    intercept[AssertionError] {
      ctx.compare("a", "2", () => "1", "be", negate = false)
    }
  }

  "Timeout on negated compare" should "result in assertion error" in {
    val ctx = newCtx()
    intercept[AssertionError] {
      ctx.compare("a", "2", () => "2", "be", negate = true)
    }
  }

  "Attempt to locate unbound element "should "throw locator bindingerror" in {
    val ctx = newCtx()
    shouldFailWithLocatorBindingError("username", ctx, "Undefined locator binding for username: username/locator")
  }

  "Attempt to locate element with unbound locator" should "throw locator binding error" in {
    val ctx = newCtx()
    ctx.withEnv { env =>
      env.scopes.addScope("login").set("username/locator", "id")
    }
    shouldFailWithLocatorBindingError("username", ctx, "Undefined locator lookup binding for username: username/locator/id")
  }

  def newCtx(dry:Boolean = false): WebContext = {
    newCtx(new EvalEnvironment(), dry)
  }

  def newCtx(env: EvalEnvironment): WebContext = {
    newCtx(env, false)
  }

  def newCtx(env: EvalEnvironment, dry: Boolean): WebContext = {
    new WebContext(GwenOptions(dryRun=dry), env, mock[DriverManager])
  }

  private def shouldFailWithLocatorBindingError(element: String, ctx: WebContext, expectedMsg: String): Unit = {
    val e = intercept[LocatorBindingException] {
      ctx.getLocatorBinding(element)
    }
    e.getMessage should be (expectedMsg)
  }
  
}