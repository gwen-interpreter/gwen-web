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
    env.scopes.set("<url>", "url")
    env.scopes.set("<condition>/javascript", "condition")
        
    val interpreter = new WebInterpreter
    withSetting("<name>", "name") {
      env.dsl map { dsl =>
        dsl.replace("<position>", "1").replace("<duration>", "2")
      } foreach { dsl => 
        interpreter.evaluate(Step(StepKeyword.Given, dsl), env) 
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