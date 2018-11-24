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

package gwen.web

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import gwen.dsl._
import gwen.eval.GwenOptions
import gwen.eval.ScopedDataStack
import gwen.Settings

class WebDslTest extends FlatSpec with Matchers {

  "gwen-web.dsl" should "pass --dry-run test" in {
    
    val options = new GwenOptions(dryRun = true)
    
    val env = new WebEnvContext(options, new ScopedDataStack())
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
    env.featureScope.pushObject("table", new FlatTable(List(List("1", "2")), List("a", "b")))

    val interpreter = new WebInterpreter
    withSetting("<name>", "name") {
      withSetting("gwen.db.<dbName>.driver", "jdbcDriver") {
        withSetting("gwen.db.<dbName>.url", "jdbcUrl") {
          env.dsl map { dsl =>
            dsl
              .replace("<position>", "1")
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
          } foreach { dsl =>
            interpreter.evaluateStep(Step(StepKeyword.Given, dsl.replaceAll("<step>", """a is "b"""")), env).evalStatus match {
              case Failed(_, error) => fail(error)
              case evalStatus => evalStatus.status should be (StatusKeyword.Passed)
            }
          }
        }
      }
    }
  }
  
  private def withSetting[T](name: String, value: String)(f: => T):T = {
    Settings.synchronized {
      val original = Settings.getOpt(name)
      try {
        Settings.set(name, value)
        f
      } finally {
        original.fold(Settings.clear(name)) { v =>
          Settings.set(name, v)
        }
      }
    }
  }
  
}