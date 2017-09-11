package gwen.web

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import gwen.dsl._
import gwen.eval.GwenOptions
import gwen.eval.ScopedData
import gwen.eval.ScopedDataStack
import gwen.Settings
import gwen.UserOverrides

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
          } foreach { dsl =>
            StepKeyword.values foreach { keyword =>
              interpreter.evaluateStep(Step(keyword, dsl.replaceAll("<step>", """a is "b"""")), env).evalStatus match {
                case Failed(_, error) => fail(error)
                case evalStatus => evalStatus.status should be (StatusKeyword.Passed)
              }
            }
          }
        }
      }
    }
  }
  
  private def withSetting[T](name: String, value: String)(f: => T):T = {
    Settings.synchronized {
      try {
        sys.props += ((name, value))
        f
      } finally {
        Settings.loadAll(UserOverrides.UserProperties.toList)
      }
    }
  }
  
}