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
import gwen.model._
import gwen.model.gherkin.Step
import gwen.web._

import org.scalatest.Matchers
import org.scalatestplus.mockito.MockitoSugar
import gwen.eval.EvalEnvironment

class WebDslTest extends BaseTest with Matchers with MockitoSugar {

  val parent = mock[Identifiable]

  "gwen-web.dsl" should "pass --dry-run test" in {
    
    val options = new GwenOptions(dryRun = true)
    
    val env = new EvalEnvironment()
    env.scopes.set("<element>/locator", "id")
    env.scopes.set("<element>/locator/id", "id")
    env.scopes.set("<reference>", "reference")
    env.scopes.set("<dropdown>/locator", "id")
    env.scopes.set("<dropdown>/locator/id", "id")
    env.scopes.set("<url>", "url")
    env.scopes.set("<condition>/javascript", "condition")
    env.scopes.set("<container>/locator", "id")
    env.scopes.set("<container>/locator/id", "id")
    env.scopes.set("<context>/locator", "id")
    env.scopes.set("<context>/locator/id", "id")
    env.scopes.set("<filepath>/file", "file.txt")
    env.scopes.set("<elements>/locator", "css selector")
    env.scopes.set("<elements>/locator/css selector", "expression")
    env.scopes.set("<source>", "source")
    env.topScope.pushObject("table", new FlatTable(List(List("1", "2")), List("a", "b")))

    val engine = new WebEngine()
    val ctx = new WebContext(options, env, mock[DriverManager])
    withSetting("<name>", "name") {
      withSetting("gwen.db.<dbName>.driver", "jdbcDriver") {
        withSetting("gwen.db.<dbName>.url", "jdbcUrl") {
          env.dsl map { dsl =>
            dsl
              .replace("<position>", "1")
              .replace("<occurrence>", "1")
              .replace("<duration>", "2")
              .replace("<delayPeriod>", "20")
              .replace("<timeoutPeriod>", "3000000")
              .replace("<w>", "375")
              .replace("<h>", "667")
              .replace("<modifiers>", "Command+Shift")
              .replace("<keys>", "Command,Shift,T")
              .replace("for each <element>", "for each <entry>")
              .replace("<filepath>", "../gwen/features/sample/templates/json/StaticMultiLineTemplate.json")
              .replace("<name> setting", "gwen.web setting")
              .replace("<name> property", "gwen.web property")
              .replace("<index>", "1")
              .replace("<count>", "2")
          } foreach { dsl =>
            val iStep = Step(None, StepKeyword.Given.toString, dsl.replaceAll("<step>", """a is "b""""), Nil, None, Nil, None, Pending)
            engine.evaluateStep(parent, iStep, ctx).evalStatus match {
              case Failed(_, error) => fail(error)
              case evalStatus => evalStatus.status should not be (StatusKeyword.Failed)
            }
          }
        }
      }
    }
  }
  
}