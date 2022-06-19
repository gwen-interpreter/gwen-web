/*
 * Copyright 2022 Branko Juric, Brady Wood
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

package gwen.web.init

import gwen.core._
import gwen.core.init.ProjectInitialiser

import scala.io.Source
import scala.util.chaining._

import java.io.File
import java.nio.file.Files

/**
  * Initilises a new Gwen web projecrt directory.
  */
trait WebProjectInitialiser extends ProjectInitialiser {

  /**
    * Initialises a new Gwen project.
    *
    * @param isNew true if the project is new, false otherwise
    * @param standalone true if initialising a standalone project, false otherwise
    * @param options Gwen options
    */
  override def init(isNew: Boolean, standalone: Boolean, options: GwenOptions): Unit = {
    
    val dir = options.initDir
    val filler = if (standalone) "   " else "       "

    if (isNew) {

      new File(dir, "browsers") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/browsers/chrome.conf", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/browsers/edge.conf", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/browsers/firefox.conf", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/browsers/ie.conf", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/browsers/README.md", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/browsers/remote.conf", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/browsers/safari.conf", dir, allowExists = false)
      }

      new File(dir, "env") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/env/dev.conf", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/env/local.conf", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/env/prod.conf", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/env/README.md", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/env/test.conf", dir, allowExists = false)
      }

      new File(dir, "features") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/features/README.md", dir, allowExists = false)
      }

      new File(dir, "meta") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/meta/README.md", dir, allowExists = false)
      }

      new File(dir, "samples/floodio") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/samples/floodio/FloodIO.feature", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/samples/floodio/FloodIO.meta", dir, allowExists = false)
      }
      new File(dir, "samples/google") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/samples/google/Google.feature", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/samples/google/Google.meta", dir, allowExists = false)
      }
      new File(dir, "samples/todo") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/samples/todo/Todo.feature", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/samples/todo/Todo.meta", dir, allowExists = false)
      }
      new File(dir, "samples") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/samples/README.md", dir, allowExists = false)
      }

      FileIO.copyClasspathTextResourceToFile("/init/README.md", dir, allowExists = false)
      FileIO.copyClasspathTextResourceToFile("/init/gitignore", dir, Some(".gitignore"), allowExists = false)
      val res = Source.fromInputStream(getClass.getResourceAsStream("/init/gwen.conf"))
      val conf = try res.mkString.replace("${gwen.initDir}", dir.getPath) finally res.close()
      new File("gwen.conf").writeText(conf)

      println(
        s"""|Project directory initialised
            |
            |  ./            $filler        # Current directory
            |   ├── gwen.conf$filler        # Gwen settings file${if (standalone) "" else {
        s"""|
            |   └── /${dir.getPath}""".stripMargin}}
            |$filler├── README.md
            |$filler├── .gitignore          # Git ignore file
            |$filler├── /browsers           # Browser settings
            |$filler│   ├── chrome.conf
            |$filler│   ├── edge.conf
            |$filler│   ├── firefox.conf
            |$filler│   ├── ie.conf
            |$filler│   ├── README.md
            |$filler│   ├── remote.conf     # Remote web driver settings
            |$filler│   └── safari.conf
            |$filler├── /env                # Environment settings
            |$filler│   ├── dev.conf
            |$filler│   ├── local.conf
            |$filler│   ├── prod.conf
            |$filler│   ├── README.md
            |$filler│   └── test.conf
            |$filler├── /features           # Features and associative meta
            |$filler│   └── README.md
            |$filler├── /meta               # Optional common/reusable meta
            |$filler│   └── README.md
            |$filler└── /samples            # Sample features and meta
            |
            |""".stripMargin
      )
    }
    if (options.docker) {
      FileIO.copyClasspathTextResourceToFile("/init/Dockerfile", dir, allowExists = false)
      FileIO.copyClasspathTextResourceToFile("/init/docker-compose.yml", dir, allowExists = false)
      new File(dir, "browsers") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/browsers/browsers.json", dir, allowExists = false)
        FileIO.copyClasspathTextResourceToFile("/init/browsers/selenoid.conf", dir, allowExists = false)
      }
      println(
        s"""|Docker files initialised
            |
            |  ./            $filler        # Current directory${if (standalone) "" else {
        s"""|
            |   └── /${dir.getPath}""".stripMargin}}
            |$filler├── docker-compose.yml  # Docker compose file
            |$filler├── Dockerfile          # Docker image file
            |$filler└── /browsers           # Browser settings
            |$filler    ├── browsers.json   # Selenoid browsers file
            |$filler    └── selenoid.conf   # Selenoid settings
            |
            |""".stripMargin
      )
    }
    if (options.jenkins) {
      FileIO.copyClasspathTextResourceToFile("/init/Jenkinsfile", dir, allowExists = false)
      println(
        s"""|Jenkinsfile initialised
            |
            |  ./            $filler        # Current directory${if (standalone) "" else {
        s"""|
            |   └── /${dir.getPath}""".stripMargin}}
            |$filler└── Jenkinsfile         # Jenkins pipeline file
            |
            |""".stripMargin
      )
    }

  }
  
}
