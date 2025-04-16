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
import gwen.web.eval.driver.DriverManager

import gwen.core.GwenOptions
import gwen.core.node.GwenNode
import gwen.core.node.gherkin.Step
import gwen.core.node.gherkin.table.DataTable
import gwen.core.node.gherkin.StepKeyword
import gwen.core.node.gherkin.table.TableOrientation
import gwen.core.state.EnvState
import gwen.core.status._

import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class WebDslTest extends BaseTest with Matchers with MockitoSugar {

  val parent = mock[GwenNode]

  "gwen-web.dsl" should "pass --dry-run test" in {
    
    val options = new GwenOptions(dryRun = true)
    
    val envState = EnvState()
    envState.topScope.set("<element>/locator", "id")
    envState.topScope.set("<element>/locator/id", "id")
    envState.topScope.set("<sourceElement>/locator", "id")
    envState.topScope.set("<sourceElement>/locator/id", "id")
    envState.topScope.set("<targetElement>/locator", "id")
    envState.topScope.set("<targetElement>/locator/id", "id")
    envState.topScope.set("<reference>", "reference")
    envState.topScope.set("<expressionRef>", "expression")
    envState.topScope.set("<predicate>/javascript", "true")
    envState.topScope.set("<javascriptRef>/javascript", "identity")
    envState.topScope.set("identity", "arguments[0]")
    envState.topScope.set("<argument>", "arg0")
    envState.topScope.set("<arguments>", "arg0")
    envState.topScope.set("<optionRef>", "option")
    envState.topScope.set("<dropdown>/locator", "id")
    envState.topScope.set("<dropdown>/locator/id", "id")
    envState.topScope.set("<url>", "url")
    envState.topScope.set("<condition>/javascript", "condition")
    envState.topScope.set("<container>/locator", "id")
    envState.topScope.set("<container>/locator/id", "id")
    envState.topScope.set("<otherElement>/locator", "id")
    envState.topScope.set("<otherElement>/locator/id", "id")
    envState.topScope.set("<context>/locator", "id")
    envState.topScope.set("<context>/locator/id", "id")
    envState.topScope.set("<frame>/locator", "id")
    envState.topScope.set("<frame>/locator/id", "id")
    envState.topScope.set("<filepath>/file", "file.txt")
    envState.topScope.set("<filepathRef> file", "file.txt")
    envState.topScope.set("<csvFilepathRef> file", "src/test/features-data/TodoItems0.csv")
    envState.topScope.set("<jsonFilepathRef> file", "src/test/features-data/TodoItems0.json")
    envState.topScope.set("<elements>/locator", "css selector")
    envState.topScope.set("<elements>/locator/css selector", "expression")
    envState.topScope.set("<textRef>", "source")
    envState.topScope.set("<textRef1>", "source")
    envState.topScope.set("<textRef2>", "source")
    envState.topScope.set("<arrayRef>", """ [ "source "] """)
    envState.topScope.set("<xmlRef>", "xml")
    envState.topScope.set("<jsonRef>", "json")
    envState.topScope.set("<encoding>", "utf8")
    envState.topScope.pushObject("table", new DataTable(TableOrientation.horizontal, List(List("1", "2")), List("a", "b")))

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
              .replace("<w>", "1920")
              .replace("<h>", "1080")
              .replace("<x>", "0")
              .replace("<y>", "0")
              .replace("<modifiers>", "Command+Shift")
              .replace("<keys>", "Command,Shift,T")
              .replace("for each <element>", "for each <entry>")
              .replace("<filepath>", "src/test/features/templates/json/StaticMultiLineTemplate.json")
              .replace("<csvFilepath>", "src/test/features-data/TodoItems0.csv")
              .replace("<jsonFilepath>", "src/test/features-data/TodoItems0.json")
              .replace("<name> setting", "gwen.web setting")
              .replace("<name> property", "gwen.web property")
              .replace("<index>", "1")
              .replace("<count>", "2")
              .replace("<pixels>", "100")
              .replace("<percentage>", "80")
              .replace("<filepathRef file>", "<filepathRef> file")
              .replace("<resultFileId>", "test")
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
