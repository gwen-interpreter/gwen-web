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

import gwen.GwenInterpreter
import gwen.core.Settings

import gwen.web.init.WebProjectInitialiser
import gwen.web.eval.WebBrowser
import gwen.web.eval.WebEngine
import gwen.web.eval.WebSettings
import gwen.core.GwenOptions
import gwen.core.GwenSettings

import java.io.File

/**
  * The main gwen-web interpreter.
  */
object GwenWebInterpreter extends GwenInterpreter(new WebEngine()) with WebProjectInitialiser {

  private val `gwen.target.env` = "gwen.target.env"
  private val `gwen.target.browser` = "gwen.target.browser"
  
  override def initDefaultSettings(options: GwenOptions): GwenOptions = {
    applyEnvOverrides(
      List(
        (`gwen.target.env`, "GWEN_ENV"), 
        (`gwen.target.browser`, "GWEN_BROWSER"), 
        ("gwen.web.browser.headless", "GWEN_HEADLESS"),
        (WebSettings.enableVideoKey, "GWEN_VIDEO")
      )
    )
    val opts = if (!options.init) {
      val conf =  Settings.BootstrapConf
      val baseDir = sys.props.get("gwen.baseDir").getOrElse(conf.getString("gwen.baseDir"))
      val browserConf = {
        val browsersDir = new File(baseDir, "browsers")
        if (browsersDir.exists && WebBrowser.findSettingsFile(options.settingsFiles).isEmpty) {
          val targetBrowser = WebBrowser.parse(sys.props.get(`gwen.target.browser`).getOrElse(conf.getString(`gwen.target.browser`)))
          List("conf", "json", "properties") map { confType =>
            new File(s"$baseDir/browsers/$targetBrowser.$confType")
          } find { conf => 
            conf.exists
          }
        } else None
      }
      val envConf = {
        val envDir = new File(baseDir, "env")
        if (envDir.exists && options.settingsFiles.filter(_.getPath.startsWith(s"$baseDir${File.separatorChar}env${File.separatorChar}")).isEmpty) {
          val targetEnv = sys.props.get(`gwen.target.env`).getOrElse(conf.getString(`gwen.target.env`))
          List("conf", "json", "properties") map { confType =>
            new File(s"$baseDir/env/$targetEnv.$confType")
          } find { conf => 
            conf.exists
          } orElse {
            Some(new File(s"$baseDir/env/$targetEnv.conf"))
          }
        } else None
      }
      val allSettings = (options.settingsFiles ++ browserConf.toList ++ envConf.toList).distinct
      options.copy(settingsFiles = allSettings)
    } else {
      options
    }
    WebBrowser.findSettingsFile(opts.settingsFiles) foreach { (_, browser) => 
      if (sys.props.get(`gwen.target.browser`).isEmpty) {
        sys.props.put(`gwen.target.browser`, browser.toString)
      }
    }
    super.initDefaultSettings(opts)
  }

}
