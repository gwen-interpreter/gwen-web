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
    * Initialises a Gwen working directory.
    *
    * @param dir the directory to initialise
    */
  override def initWorkingDir(dir: File): Unit = {
    
    super.initWorkingDir(dir)

    new File(dir, "browsers") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/browsers/chrome.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/browsers/edge.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/browsers/firefox.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/browsers/ie.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/browsers/safari.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/browsers/README.md", dir)
    }

    new File(dir, "env") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/env/dev.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/env/test.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/env/README.md", dir)
    }

    new File(dir, "features") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/features/README.md", dir)
    }

    new File(dir, "samples/floodio") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/samples/floodio/FloodIO.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/init/samples/floodio/FloodIO.meta", dir)
    }

    new File(dir, "samples/google") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/samples/google/Google.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/init/samples/google/Google.meta", dir)
    }

    new File(dir, "samples/i18n") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/samples/i18n/Google_fr.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/init/samples/i18n/Google_fr.meta", dir)
    }

    new File(dir, "samples/todo") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/samples/todo/Todo.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/init/samples/todo/Todo.meta", dir)
    }

    new File(dir, "samples/") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/samples/README.md", dir)
    }

    FileIO.copyClasspathTextResourceToFile("/init/README.md", dir)
    if (!new File("gwen.conf").exists()) {
      FileIO.copyClasspathTextResourceToFile("/init/gwen.conf", targetDir = new File("."))
    } 

    println(
      s"""!  ./
          !   |  gwen.conf
          !   +--/${dir.getPath}
          !      |  README.md
          !      +--/browsers
          !      |     chrome.conf
          !      |     edge.conf
          !      |     firefox.conf
          !      |     safari.conf
          !      |     ie.conf
          !      |     README.md
          !      +--/env
          !      |     dev.conf
          !      |     test.conf
          !      |     README.md
          !      +--/features
          !      |     README.md
          !      +--/samples
          !
          !""".stripMargin('!')
    )

  }

}
