/*
 * Copyright 2021 Brady Wood, Branko Juric
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

import gwen.web._

import gwen.core.AssertionMode
import gwen.core.GwenSettings
import gwen.core.Settings
import gwen.core.behavior.BehaviorMode
import gwen.core.behavior.FeatureMode
import gwen.core.report.rp.RPConfig.ErrorBlocks
import gwen.core.report.rp.RPConfig.ErrorReportingMode
import gwen.core.report.rp.RPConfig.StepDefFormat
import gwen.core.report.rp.RPConfig.TestCaseIdKeys
import gwen.core.report.rp.RPSettings
import gwen.core.state.StateLevel

import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class WebSettingsTest extends BaseTest with Matchers with MockitoSugar {

  "Default Gwen core settings" should "should load" in {
    Settings.exclusively {
      GwenSettings.`gwen.assertion.mode` should be (AssertionMode.hard)
      GwenSettings.`gwen.associative.meta` should be (false)
      GwenSettings.`gwen.auto.discover.data.csv` should be (true)
      GwenSettings.`gwen.auto.discover.meta` should be (true)
      GwenSettings.`gwen.behavior.rules` should be (BehaviorMode.lenient)
      GwenSettings.`gwen.feature.dialect` should be ("en")
      GwenSettings.`gwen.feature.failfast.enabled` should be (true)
      GwenSettings.`gwen.feature.failfast.exit` should be (false)
      GwenSettings.`gwen.feature.mode` should be (FeatureMode.imperative)
      GwenSettings.`gwen.mask.char` should be ('●')
      GwenSettings.`gwen.parallel.maxThreads` should be (GwenSettings.availableProcessors)
      GwenSettings.`gwen.rampup.interval.seconds` should be (None)
      GwenSettings.`gwen.report.overwrite` should be (false)
      GwenSettings.`gwen.report.suppress.meta` should be (false)
      GwenSettings.`gwen.report.slideshow.create` should be (false)
      GwenSettings.`gwen.report.slideshow.framespersecond` should be (4)
      RPSettings.`gwen.rp.debug` should be (false)
      RPSettings.`gwen.rp.heartbeat.enabled` should be (true)
      RPSettings.`gwen.rp.heartbeat.timeoutSecs` should be (3)
      RPSettings.`gwen.rp.send.annotations` should be (false)
      RPSettings.`gwen.rp.send.breadcrumbs` should be (false)
      RPSettings.`gwen.rp.send.failed.envTrace` should be (ErrorReportingMode.none)
      RPSettings.`gwen.rp.send.failed.errorBlocks` should be (ErrorBlocks.none)
      RPSettings.`gwen.rp.send.failed.errorTrace` should be (ErrorReportingMode.none)
      RPSettings.`gwen.rp.send.failed.hierarchy` should be (ErrorReportingMode.inlined)
      RPSettings.`gwen.rp.send.failed.stepDefs` should be (StepDefFormat.inlined)
      RPSettings.`gwen.rp.send.markdownBlocks` should be (true)
      RPSettings.`gwen.rp.send.meta` should be (false)
      RPSettings.`gwen.rp.send.stepDefs` should be (StepDefFormat.none)
      RPSettings.`gwen.rp.send.tags` should be (true)
      RPSettings.`gwen.rp.testCaseId.keys` should be (TestCaseIdKeys.`nodePath+params`)
      GwenSettings.`gwen.state.level` should be (StateLevel.feature)
    }
  }

  "Default Gwen web settings" should "should load" in {
    Settings.exclusively {
      WebSettings.`gwen.web.accept.untrusted.certs` should be (true)
      WebSettings.`gwen.web.authorize.plugins` should be (false)
      WebSettings.`gwen.web.browser.target` should be ("chrome")
      WebSettings.`gwen.web.browser.size` should be (None)
      WebSettings.`gwen.web.capabilities`.size should be (0)
      WebSettings.`gwen.web.capture.screenshots.enabled` should be (false)
      WebSettings.`gwen.web.capture.screenshots.duplicates` should be (false)
      WebSettings.`gwen.web.capture.screenshots.highlighting` should be (false)
      WebSettings.`gwen.web.chrome.args` should be (Nil)
      WebSettings.`gwen.web.chrome.extensions` should be (Nil)
      WebSettings.`gwen.web.chrome.mobile`.size should be (0)
      WebSettings.`gwen.web.chrome.path` should be (None)
      WebSettings.`gwen.web.chrome.prefs`.size should be (0)
      WebSettings.`gwen.web.edge.args` should be (Nil)
      WebSettings.`gwen.web.edge.extensions` should be (Nil)
      WebSettings.`gwen.web.edge.mobile`.size should be (0)
      WebSettings.`gwen.web.edge.path` should be (None)
      WebSettings.`gwen.web.edge.prefs`.size should be (0)
      WebSettings.`gwen.web.firefox.path` should be (None)
      WebSettings.`gwen.web.firefox.prefs`.size should be (0)
      WebSettings.`gwen.web.highlight.style` should be ("background: yellow; border: 2px solid gold;")
      WebSettings.`gwen.web.implicit.element.focus` should be (true)
      WebSettings.`gwen.web.implicit.js.locators` should be (false)
      WebSettings.`gwen.web.locator.wait.seconds` should be (10L)
      WebSettings.`gwen.web.maximize` should be (false)
      WebSettings.`gwen.web.remote.localFileDetector` should be (false)
      WebSettings.`gwen.web.remote.url` should be (None)
      WebSettings.`gwen.web.sendKeys.clearFirst` should be (false)
      WebSettings.`gwen.web.sendKeys.clickFirst` should be (false)
      WebSettings.`gwen.web.suppress.images` should be (false)
      WebSettings.`gwen.web.throttle.msecs` should be (100)
      WebSettings.`gwen.web.useragent` should be (None)
      WebSettings.`gwen.web.wait.seconds` should be (10L)
    }
  }

}
