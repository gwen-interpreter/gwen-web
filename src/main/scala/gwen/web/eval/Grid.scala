/*
 * Copyright 2025 Branko Juric, Brady Wood
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
package gwen.web.eval

import gwen.core.Settings
import gwen.core.GwenSettings

import java.io.File

enum Grid:
  case selenium, selenoid
  private def enableVideoKeys: List[String] = {
    if (this == selenium) List(
      "gwen.web.capabilities.se:recordVideo",
      "gwen.web.capability.se:recordVideo"
    ) else List(
      "gwen.web.capabilities.selenoid:options.enableVideo",
      "gwen.web.capability.selenoid:options.enableVideo"
    )
  }
  def enableVideoKey: String = enableVideoKeys(0)
  def videoEnabled: Boolean = enableVideoKeys.flatMap(key => Settings.getBooleanOpt(key)).headOption.getOrElse(false)
  def videoFile(sessionId: String): File = {
    if (this == selenium) {
      val dir = new File(new File(GwenSettings.`gwen.outDir`, ".assets"), sessionId)
      val filename = Settings.getOpt("gwen.web.capabilities.se:name").orElse(Settings.getOpt("gwen.web.capability.se:name")).getOrElse("video")
      new File(dir, s"$filename.mp4")
    } else {
      new File(GwenSettings.`gwen.video.dir`, s"$sessionId.mp4")
    }
  }

object Grid {
  def impl: Option[Grid] = sys.env.get("SELENIUM_HUB") map { hub =>
    if (hub == Grid.selenoid.toString) Grid.selenoid else Grid.selenium
  }
}
