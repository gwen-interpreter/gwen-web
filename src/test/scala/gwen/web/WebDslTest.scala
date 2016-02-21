package gwen.web

import org.scalatest.FlatSpec
import gwen.dsl.Step
import gwen.dsl.StepKeyword
import gwen.eval.GwenOptions
import gwen.eval.ScopedDataStack
import gwen.Settings
import gwen.UserOverrides

class WebDslTest extends FlatSpec {

  "gwen-web.dsl" should "pass --dry-run test" in {
    
    val options = new GwenOptions(dryRun = true);
    
    val env = new WebEnvContext(options, new ScopedDataStack())
    env.scopes.set("<element>/locator", "id");
    env.scopes.set("<element>/locator/id", "id")
    env.scopes.set("<reference>", "reference")
    env.scopes.set("<dropdown>/locator", "id")
    env.scopes.set("<dropdown>/locator/id", "id")
    env.scopes.set("<url>", "url")
    env.scopes.set("<condition>/javascript", "condition")
    env.scopes.set("<container>/locator", "id");
    env.scopes.set("<container>/locator/id", "id")
    env.scopes.set("<context>/locator", "id")
    env.scopes.set("<context>/locator/id", "id")
        
    val interpreter = new WebInterpreter
    withSetting("<name>", "name") {
      env.dsl map { dsl =>
        dsl.replace("<position>", "1").replace("<duration>", "2")
      } foreach { dsl => 
        StepKeyword.values foreach { keyword =>
          interpreter.evaluate(Step(keyword, dsl.replaceAll("<step>", "I refresh the current page")), env)
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