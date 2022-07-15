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
package gwen.web.eval

import gwen.core._

import scala.util.chaining._
import scala.util.Try

import java.io.File

enum WebBrowser:
  case chrome, edge, firefox, safari, ie

object WebBrowser {

  def findSettingsFile(settings: List[File]): Option[(File ,WebBrowser)] = {
    (settings flatMap { conf => 
      WebBrowser.values flatMap { browser => 
        if (conf.dropExtension == browser.toString) Some((conf, browser))
        else None
      } 
    }) tap {result => 
      if (result.size > 1) {
        WebErrors.multipleBrowserSettingsError(result.map(_._1))
      }
    }
  } headOption

  def parse(name: String): WebBrowser = {
    Try(WebBrowser.valueOf(name)).getOrElse(WebErrors.unsupportedWebBrowserError(name))
  }

}
