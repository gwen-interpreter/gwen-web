/*
 * Copyright 2021 Branko Juric, Brady Wood
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

import gwen.web.eval.WebEngine
import gwen.GwenInterpreter
import gwen.core.FileIO

import java.io.File

import scala.util.chaining._

/**
  * The main gwen-web interpreter.
  */
object GwenWebInterpreter extends GwenInterpreter(new WebEngine()) {

  /**
    * Initialises a workspace directory.
    *
    * @param dir the directory to initialise
    */
  override def initWorkspace(dir: File): Unit = {
    
    super.initWorkspace(dir)
    
    new File(dir, "browsers") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/browsers/chrome.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/browsers/edge.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/browsers/firefox.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/browsers/ie.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/browsers/safari.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/browsers/README.txt", dir)
    }

    new File(dir, "env") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/env/local.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/env/README.txt", dir)
    }

    new File(dir, "features") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/features/README.txt", dir)
    }

    new File(dir, "samples/floodio") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/floodio/FloodIO.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/floodio/FloodIO.meta", dir)
    }

    new File(dir, "samples/google") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/google/Google.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/google/Google.meta", dir)
    }

    new File(dir, "samples/i18n") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/i18n/Google.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/i18n/Google.meta", dir)
    }

    new File(dir, "samples/set-test") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/se-test/SeleniumTestPage.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/se-test/SeleniumTestPage.meta", dir)
    }

    new File(dir, "samples/todo/feature-level") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/feature-level/Todo.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/feature-level/Todo.meta", dir)
    }

    new File(dir, "samples/todo/homepage-example") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/homepage-example/Todo.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/homepage-example/Todo.meta", dir)
    }

    new File(dir, "samples/todo/lenient-rules") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/lenient-rules/Todo.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/lenient-rules/Todo.meta", dir)
    }

    new File(dir, "samples/todo/scenario-level") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/scenario-level/Todo.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/scenario-level/Todo.meta", dir)
    }

    new File(dir, "samples/todo/single-scenario") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/single-scenario/Todo.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/single-scenario/Todo.meta", dir)
    }

    new File(dir, "samples/todo/strict-rules") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/strict-rules/Todo.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/todo/strict-rules/Todo.meta", dir)
    }

    new File(dir, "samples/") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/samples/README.txt", dir)
    }

    FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/gitignore", dir, Some(".gitignore"))
    FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/gwen.properties", dir)
    FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/log4j.properties", dir)
    FileIO.copyClasspathTextResourceToFile("/gwen-web/workspace/README.txt", dir)

  }

}
