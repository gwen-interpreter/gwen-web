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
import gwen.core._

import java.io.File

import scala.io.Source
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
      FileIO.copyClasspathTextResourceToFile("/init/browsers/remote.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/browsers/README.md", dir)
    }

    new File(dir, "env") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/env/local.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/env/dev.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/env/test.conf", dir)
      FileIO.copyClasspathTextResourceToFile("/init/env/README.md", dir)
    }

    new File(dir, "features") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/features/README.md", dir)
    }

    new File(dir, "meta") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/meta/README.md", dir)
    }

    new File(dir, "output") tap { dir =>
      FileIO.copyClasspathTextResourceToFile("/init/output/README.md", dir)
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
    FileIO.copyClasspathTextResourceToFile("/init/gitignore", dir, Some(".gitignore"))
    if (!new File("gwen.conf").exists()) {
      val res = Source.fromInputStream(getClass.getResourceAsStream("/init/gwen.conf"))
      val conf = try res.mkString.replace("${gwen.dir}", dir.getPath) finally res.close()
      new File("gwen.conf").writeText(conf)
    }

    println(
      s"""!  ./                        # Your project root
          !   |  gwen.conf             # Common/default Gwen settings
          !   +--/${dir.getPath}
          !      |  .gitignore         # Git ignore file
          !      |  README.md
          !      +--/browsers          # Browser settings
          !      |     chrome.conf     # - default is chrome
          !      |     edge.conf
          !      |     firefox.conf
          !      |     safari.conf
          !      |     ie.conf
          !      |     remote.conf     # Remote web driver settings
          !      |     README.md
          !      +--/env               # Environment settings
          !      |     local.conf      # - default is local
          !      |     dev.conf
          !      |     test.conf
          !      |     README.md
          !      +--/features          # Features and associative meta
          !      |     README.md
          !      +--/meta              # Common/reusable meta
          !      |     README.md
          !      +--/output            # Output directory
          !      +--/samples           # Sample features
          !
          !""".stripMargin('!')
    )

  }

}
