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

import com.fasterxml.jackson.databind.ObjectMapper
import com.typesafe.scalalogging.LazyLogging

import scala.io.Source
import scala.util.Try

import java.io.File
import java.util.HashMap

enum Grid extends LazyLogging:
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
    val dirname = GwenSettings.`gwen.video.dir`
      .getPath
      .replaceAll("""\$<user.home>""", sys.props("user.home"))
      .replaceAll("""\$<gwen.web.sessionId>""", sessionId)
    val filename = {
      if (this == selenium) {
        Settings.getOpt("gwen.web.capabilities.se:name").orElse(Settings.getOpt("gwen.web.capability.se:name")).getOrElse("video")
      } else {
        sessionId
      }
    }
    new File(new File(dirname), s"$filename.mp4")
  }
  def waitFor(): Unit = {
    if (this == selenium) {
      WebSettings.`gwen.web.remote.url` foreach { remoteUrl =>
        val statusUrl = s"$remoteUrl/status"
        val mapper = new ObjectMapper()
        val isReady = () => {
          Try(
            mapper
              .readValue(Source.fromURL(statusUrl).mkString, classOf[HashMap[String, Object]])
              .get("value").asInstanceOf[HashMap[String, Object]]
              .get("ready").asInstanceOf[Boolean]
          ).getOrElse(false)
        }
        var ready = isReady()
        val timeoutSecs = WebSettings.`gwen.web.remote.connectTimeout.seconds`
        var waitSecs = timeoutSecs
        if (!ready) {
          logger.info(s"Remote url is $remoteUrl")
          println(s"Waiting for Grid")
          while(!ready && waitSecs > 0) {
            if (!ready) {
              waitSecs = waitSecs - 1
              Thread.sleep(1000)
              print(".")
            }
            ready = isReady()
          }
        }
        if (waitSecs < timeoutSecs) println(s"${timeoutSecs - waitSecs}s")
        if (ready) println(s"Grid is ready ✔\n") else WebErrors.gridWaitTimeout(timeoutSecs)
      }
    }
  }

object Grid {
  def impl: Option[Grid] = sys.env.get("SELENIUM_HUB") map { hub =>
    if (hub == Grid.selenoid.toString) Grid.selenoid else Grid.selenium
  }
}
