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

import gwen.web.init.WebProjectInitialiser
import gwen.web.eval.WebEngine

import gwen.GwenInterpreter

/**
  * The main gwen-web interpreter.
  */
object GwenWebInterpreter extends GwenInterpreter(new WebEngine()) with WebProjectInitialiser {

  override def initDefaultEnvSettings(): Unit = {
    applyEnvSettings(
      List(
        ("gwen.target.browser", "GWEN_BROWSER"), 
        ("gwen.web.browser.headless", "GWEN_HEADLESS"),
        ("gwen.web.capability.enableVideo", "GWEN_VIDEO"), 
      )
    )
    super.initDefaultEnvSettings()
  }

}
