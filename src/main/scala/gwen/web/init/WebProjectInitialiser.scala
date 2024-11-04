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
import gwen.core.init.InitOption
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
    * @param flat true if initialising in current or nested directory
    * @param options Gwen options
    */
  override def init(isNew: Boolean, flat: Boolean, options: GwenOptions): Unit = {
    
    val dir = options.initDir
    val confDir = new File(dir, "conf")
    val filler = if (flat) "   " else "       "
    val force = options.initOptions.contains(InitOption.force)
    val copyRootGitIgnore = !flat && !(new File(".gitignore").exists())
    val copyRootReadme = !(new File("README.md").exists())

    if (isNew || options.initOptions == List(InitOption.force)) {

      new File(confDir, "browsers") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/conf/browsers/chrome.conf", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/browsers/edge.conf", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/browsers/firefox.conf", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/browsers/README.md", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/browsers/safari.conf", dir, allowReplace = force)
      }

      new File(confDir, "env") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/conf/env/dev.conf", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/env/local.conf", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/env/prod.conf", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/env/README.md", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/env/staging.conf", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/env/test.conf", dir, allowReplace = force)
      }

      new File(confDir, "profiles") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/conf/profiles/samples.conf", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/profiles/README.md", dir, allowReplace = force)
      }  

      new File(dir, "features") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/features/README.md", dir, allowReplace = force)
        new File(dir, "samples") tap { dir =>
          FileIO.copyClasspathTextResourceToFile("/init/features/samples/README.md", dir, allowReplace = force)
          new File(dir, "google") tap { dir =>
            FileIO.copyClasspathTextResourceToFile("/init/features/samples/google/Google.feature", dir, allowReplace = force)
            FileIO.copyClasspathTextResourceToFile("/init/features/samples/google/Google.meta", dir, allowReplace = force)
          }
          new File(dir, "todo") tap { dir =>
            FileIO.copyClasspathTextResourceToFile("/init/features/samples/todo/Todo.feature", dir, allowReplace = force)
            FileIO.copyClasspathTextResourceToFile("/init/features/samples/todo/Todo.meta", dir, allowReplace = force)
          }
        }
      }

      new File(dir, "meta") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/meta/README.md", dir, allowReplace = force)
      }

      if(copyRootReadme) {
        FileIO.copyClasspathTextResourceToFile("/init/README.md", new File("."), allowReplace = false)
      }
      FileIO.copyClasspathTextResourceToFile("/init/gitignore", dir, Some(".gitignore"), allowReplace = force)
      copyClasspathResourceAndInject("/init/gwen.conf", dir, flat, allowReplace = force, targetFile = Some(new File("gwen.conf")), targetPath = Some(if (flat) "." else dir.getPath))
      if(copyRootGitIgnore) {
        FileIO.copyClasspathTextResourceToFile("/init/gitignore_root", new File("."), Some(".gitignore"), allowReplace = false)
      }

      println(
        s"""|Project directory initialised${if (force) " (forced)" else ""}
            |
            |  ./            $filler           # Project root${if (!copyRootGitIgnore) "" else {
        s"""|
            |   ├── .gitignore$filler          # Git ignore file""".stripMargin}}${if (!copyRootReadme) "" else {
        s"""|
            |   ├── README.md""".stripMargin}}
            |   ├── gwen.conf$filler           # Common settings${if (flat) "" else {
        s"""|
            |   └── /${dir.getPath}""".stripMargin}}${if (copyRootReadme) "" else {
        s"""|
            |$filler├── README.md""".stripMargin}}
            |$filler├── .gitignore             # Git ignore file
            |$filler├── /conf
            |$filler│   ├── /browsers          # Browser settings
            |$filler│   |   ├── chrome.conf
            |$filler│   |   ├── edge.conf
            |$filler│   |   ├── firefox.conf
            |$filler│   |   ├── README.md
            |$filler│   |   └── safari.conf
            |$filler│   ├──/env                # Environment settings
            |$filler│   |  ├── dev.conf
            |$filler│   |  ├── local.conf
            |$filler│   |  ├── prod.conf
            |$filler│   |  ├── README.md
            |$filler│   |  ├── staging.conf
            |$filler│   |  └── test.conf
            |$filler│   └──/profiles           # Profile settings
            |$filler│      ├── README.md
            |$filler│      └── samples.conf
            |$filler├── /features              # Features (and associative meta)
            |$filler│   ├── README.md
            |$filler│   └── /samples           # Samples
            |$filler└── /meta                  # Common meta
            |$filler    └── README.md
            |
            |""".stripMargin
      )
    }
    if (options.initOptions.contains(InitOption.docker)) {
      FileIO.copyClasspathTextResourceToFile("/init/docker_env", dir, Some(".env"), allowReplace = force)
      FileIO.copyClasspathTextResourceToFile("/init/Dockerfile", dir, allowReplace = force)
      copyClasspathResourceAndInject("/init/docker-compose.yml", dir, flat, allowReplace = force)
      new File(confDir, "browsers") tap { dir =>
        FileIO.copyClasspathTextResourceToFile("/init/conf/browsers/browsers.json", dir, allowReplace = force)
        FileIO.copyClasspathTextResourceToFile("/init/conf/browsers/selenoid.conf", dir, allowReplace = force)
      }
      println(
        s"""|Docker files initialised${if (force) " (forced)" else ""}
            |
            |  ./            $filler            # Project root${if (flat) "" else {
        s"""|
            |   └── /${dir.getPath}""".stripMargin}}
            |$filler├── .env                    # Docker env file
            |$filler├── docker-compose.yml      # Docker compose file
            |$filler├── Dockerfile              # Docker image file
            |$filler└── /conf
            |$filler    └── /browsers           # Browser settings
            |$filler        ├── browsers.json   # Selenoid browsers file
            |$filler        └── selenoid.conf   # Selenoid settings
            |
            |""".stripMargin
      )
    }
    if (options.initOptions.contains(InitOption.jenkins)) {
      copyClasspathResourceAndInject("/init/Jenkinsfile", dir, flat, allowReplace = force)
      println(
        s"""|Jenkinsfile initialised${if (force) " (forced)" else ""}
            |
            |  ./            $filler            # Project root${if (flat) "" else {
        s"""|
            |   └── /${dir.getPath}""".stripMargin}}
            |$filler└── Jenkinsfile             # Jenkins pipeline file
            |
            |""".stripMargin
      )
    }

  }

  private def copyClasspathResourceAndInject(resource: String, dir: File, flat: Boolean, allowReplace: Boolean = false, targetFile: Option[File] = None, targetPath: Option[String] = None ): Unit = {
    val toFile = targetFile.getOrElse(new File(dir, new File(resource).getName))
    if (!allowReplace && toFile.exists) Errors.copyResourceError(s"File alredy exists: $toFile (use --force option to replace).")
    val res = Source.fromInputStream(getClass.getResourceAsStream(resource))
    try {
      val initDir = targetPath.getOrElse(if (flat) "" else dir.getPath)
      if (!toFile.exists || allowReplace) {
        toFile.writeText(res.mkString.replace("${gwen.initDir}", initDir).replace("${slash}", if (flat) "" else "/").replace("${docker.compose.options}", if (flat) "" else s" -f $initDir/docker-compose.yml"))
      }
    } finally {
     res.close()
    }
  }

}
