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
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/browsers/chrome.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/browsers/edge.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/browsers/firefox.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/browsers/ie.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/browsers/safari.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/browsers/README.txt", dir)
      logger.info(s"Initalised $dir")
    }

    new File(dir, "env") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/env/local.properties", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/env/README.txt", dir)
      logger.info(s"Initalised $dir")
    }

    new File(dir, "features") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/features/README.txt", dir)
      logger.info(s"Initalised $dir")
    }

    new File(dir, "samples/floodio") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/floodio/FloodIO.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/floodio/FloodIO.meta", dir)
    }

    new File(dir, "samples/google") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/google/Google.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/google/Google.meta", dir)
    }

    new File(dir, "samples/i18n") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/i18n/Google_fr.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/i18n/Google_fr.meta", dir)
    }

    new File(dir, "samples/todo") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/todo/Todo.feature", dir)
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/todo/Todo.meta", dir)
    }

    new File(dir, "samples/") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/samples/README.txt", dir)
      logger.info(s"Initalised $dir")
    }

    FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/gitignore", dir, Some(".gitignore"))
    FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/gwen.properties", dir)
    FileIO.copyClasspathTextResourceToFile("/gwen-web/working-dir/README.txt", dir)
    logger.info(s"Initalised $dir")

  }

}
