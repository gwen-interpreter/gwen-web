/*
 * Copyright 2014-2021 Brady Wood, Branko Juric
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
import gwen.web.eval.driver.DriverManager

import gwen.core.GwenOptions
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step
import gwen.core.node.gherkin.table.FlatTable
import gwen.core.node.gherkin.StepKeyword
import gwen.core.node.gherkin.table.TableType
import gwen.core.state.EnvState
import gwen.core.status._

import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class WebDslTest extends BaseTest with Matchers with MockitoSugar {

  val parent = mock[GwenNode]

  "gwen-web.dsl" should "pass --dry-run test" in {
    
    val options = new GwenOptions(dryRun = true)
    
    val envState = EnvState()
    envState.scopes.set("<element>/locator", "id")
    envState.scopes.set("<element>/locator/id", "id")
    envState.scopes.set("<sourceElement>/locator", "id")
    envState.scopes.set("<sourceElement>/locator/id", "id")
    envState.scopes.set("<targetElement>/locator", "id")
    envState.scopes.set("<targetElement>/locator/id", "id")
    envState.scopes.set("<reference>", "reference")
    envState.scopes.set("<expressionRef>", "expression")
    envState.scopes.set("<predicate>/javascript", "true")
    envState.scopes.set("<javascriptRef>/javascript", "identity")
    envState.scopes.set("identity", "arguments[0]")
    envState.scopes.set("<argument>", "arg0")
    envState.scopes.set("<arguments>", "arg0")
    envState.scopes.set("<optionRef>", "option")
    envState.scopes.set("<dropdown>/locator", "id")
    envState.scopes.set("<dropdown>/locator/id", "id")
    envState.scopes.set("<url>", "url")
    envState.scopes.set("<condition>/javascript", "condition")
    envState.scopes.set("<container>/locator", "id")
    envState.scopes.set("<container>/locator/id", "id")
    envState.scopes.set("<otherElement>/locator", "id")
    envState.scopes.set("<otherElement>/locator/id", "id")
    envState.scopes.set("<context>/locator", "id")
    envState.scopes.set("<context>/locator/id", "id")
    envState.scopes.set("<frame>/locator", "id")
    envState.scopes.set("<frame>/locator/id", "id")
    envState.scopes.set("<filepath>/file", "file.txt")
    envState.scopes.set("<filepathRef> file", "file.txt")
    envState.scopes.set("<csvFilepathRef> file", "src/test/features-data/TodoItems0.csv")
    envState.scopes.set("<jsonFilepathRef> file", "src/test/features-data/TodoItems0.json")
    envState.scopes.set("<elements>/locator", "css selector")
    envState.scopes.set("<elements>/locator/css selector", "expression")
    envState.scopes.set("<textRef>", "source")
    envState.scopes.set("<textRef1>", "source")
    envState.scopes.set("<textRef2>", "source")
    envState.scopes.set("<arrayRef>", """ [ "source "] """)
    envState.scopes.set("<xmlRef>", "xml")
    envState.scopes.set("<jsonRef>", "json")
    envState.scopes.set("<encoding>", "utf8")
    envState.scopes.topScope.pushObject("table", new FlatTable(TableType.horizontal, List(List("1", "2")), List("a", "b")))

    val engine = new WebEngine()
    val ctx = new WebContext(options, envState, mock[DriverManager])
    withSetting("<name>", "name") {
      withSetting("gwen.db.<dbName>.driver", "jdbcDriver") {
        withSetting("gwen.db.<dbName>.url", "jdbcUrl") {
          ctx.dsl map { dsl =>
            dsl
              .replace("<position>", "1")
              .replace("<occurrence>", "1")
              .replace("<duration>", "2")
              .replace("<w>", "375")
              .replace("<h>", "667")
              .replace("<modifiers>", "Command+Shift")
              .replace("<keys>", "Command,Shift,T")
              .replace("for each <element>", "for each <entry>")
              .replace("<filepath>", "../gwen/src/test/features/templates/json/StaticMultiLineTemplate.json")
              .replace("<csvFilepath>", "src/test/features-data/TodoItems0.csv")
              .replace("<jsonFilepath>", "src/test/features-data/TodoItems0.json")
              .replace("<name> setting", "gwen.web setting")
              .replace("<name> property", "gwen.web property")
              .replace("<index>", "1")
              .replace("<count>", "2")
              .replace("<pixels>", "100")
              .replace("<percentage>", "80")
              .replace("<filepathRef file>", "<filepathRef> file")
          } foreach { dsl =>
            val iStep = Step(None, StepKeyword.Given.toString, dsl.replaceAll("<step>", """a is "b""""), Nil, None, Nil, None, Pending, Nil, Nil, Nil, None, Nil)
            engine.evaluateStep(parent, iStep, ctx).evalStatus match {
              case Failed(_, error) => 
                error.printStackTrace
                fail(iStep.toString, error)
              case evalStatus => evalStatus.keyword should not be (StatusKeyword.Failed)
            }
          }
        }
      }
    }
  }
  
}
