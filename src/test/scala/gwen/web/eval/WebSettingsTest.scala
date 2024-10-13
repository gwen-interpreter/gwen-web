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
import gwen.core.GwenOptions
import gwen.core.GwenSettings
import gwen.core.LaunchSettings
import gwen.core.Settings
import gwen.core.behavior.BehaviorMode
import gwen.core.behavior.FeatureMode
import gwen.core.node.NodeType
import gwen.core.report.ReportFormat
import gwen.core.report.results.ResultField
import gwen.core.state.StateLevel
import gwen.core.status.StatusKeyword

import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import java.io.File
import java.util.logging.Level

class WebSettingsTest extends BaseTest with Matchers with MockitoSugar {

  "Default Gwen core settings" should "should load" in {
    Settings.exclusively {
      GwenSettings.`gwen.assertion.mode` should be (AssertionMode.hard)
      GwenSettings.`gwen.associative.meta` should be (true)
      GwenSettings.`gwen.auto.bind.tableData.outline.examples` should be (true)
      GwenSettings.`gwen.auto.discover.data.csv` should be (true)
      GwenSettings.`gwen.auto.discover.data.json` should be (false)
      GwenSettings.`gwen.auto.discover.meta` should be (true)
      GwenSettings.`gwen.auto.trim.data.csv` should be (false)
      GwenSettings.`gwen.auto.trim.data.json` should be (false)
      GwenSettings.`gwen.behavior.rules` should be (BehaviorMode.lenient)
      GwenSettings.`gwen.feature.dialect` should be ("en")
      GwenSettings.`gwen.feature.failfast.enabled` should be (true)
      GwenSettings.`gwen.feature.failfast.exit` should be (false)
      GwenSettings.`gwen.feature.mode` should be (FeatureMode.imperative)
      GwenSettings.`gwen.mask.char` should be ('*')
      GwenSettings.`gwen.baseDir`.getPath should be (".")
      GwenSettings.`gwen.outDir`.getPath should be ("target")
      GwenSettings.`gwen.parallel.maxThreads` should be (GwenSettings.availableProcessors)
      GwenSettings.`gwen.rampup.interval.seconds` should be (None)
      GwenSettings.`gwen.report.overwrite` should be (false)
      GwenSettings.`gwen.report.attach.functions` should be (true)
      GwenSettings.`gwen.report.stepDef.indent.pixels` should be (20)
      GwenSettings.`gwen.report.suppress.meta` should be (true)
      GwenSettings.`gwen.report.slideshow.create` should be (false)
      GwenSettings.`gwen.report.slideshow.framespersecond` should be (4)
      GwenSettings.`gwen.state.level` should be (StateLevel.feature)
      GwenSettings.`gwen.console.log.colors` should be (true)
      GwenSettings.`gwen.console.log.depth` should be (1)
      GwenSettings.`gwen.console.log.stepDefs` should be (true)
      GwenSettings.`gwen.console.repl.autoSuggestions` should be (true)
      GwenSettings.`gwen.console.repl.tabCompletion` should be (true)
      GwenSettings.`gwen.video.dir`.getPath should be ("output/.video")
      GwenSettings.`gwen.video.timeoutSecs` should be (10)
      GwenSettings.`gwen.dryRun.limit.tableData.outline.examples.records` should be (Integer.MAX_VALUE)
      GwenSettings.`gwen.error.messages.inline.locators` should be (false)
      GwenSettings.`gwen.logLevel.deprecations` should be (Level.WARNING)
    }
  }

  "Default Gwen web settings" should "should load" in {
    Settings.exclusively {
      WebSettings.`gwen.target.browser` should be (WebBrowser.chrome)
      WebSettings.`gwen.target.env` should be ("test")
      WebSettings.`gwen.web.authorize.plugins` should be (false)
      WebSettings.`gwen.web.assertions.delayMillisecs` should be (200)
      WebSettings.`gwen.web.assertions.maxStrikes` should be (10)
      WebSettings.`gwen.web.browser.size` should be (None)
      WebSettings.`gwen.web.capabilities`.asMap.isEmpty should be (true)
      WebSettings.`gwen.web.capture.screenshots.enabled` should be (false)
      WebSettings.`gwen.web.capture.screenshots.duplicates` should be (false)
      WebSettings.`gwen.web.capture.screenshots.highlighting` should be (false)
      WebSettings.`gwen.web.chrome.args`.contains("--remote-allow-origins=*") should be (false)
      WebSettings.`gwen.web.chrome.extensions` should be (Nil)
      WebSettings.`gwen.web.chrome.mobile` should be (Map())
      WebSettings.`gwen.web.chrome.path` should be (None)
      WebSettings.`gwen.web.chrome.prefs` should be (Map())
      WebSettings.`gwen.web.driver.manager` should be (DriverManagerImpl.SeleniumManager)
      WebSettings.`gwen.web.edge.args` should be (Nil)
      WebSettings.`gwen.web.edge.extensions` should be (Nil)
      WebSettings.`gwen.web.edge.mobile` should be (Map())
      WebSettings.`gwen.web.edge.path` should be (None)
      WebSettings.`gwen.web.edge.prefs` should be (Map())
      WebSettings.`gwen.web.firefox.path` should be (None)
      WebSettings.`gwen.web.firefox.prefs` should be (Map())
      WebSettings.`gwen.web.highlight.style` should be ("background: yellow; border: 2px solid gold;")
      WebSettings.`gwen.web.implicit.element.focus` should be (true)
      WebSettings.`gwen.web.implicit.element.moveTo` should be (false)
      WebSettings.`gwen.web.implicit.js.locators` should be (false)
      WebSettings.`gwen.web.locator.wait.seconds` should be (10L)
      WebSettings.`gwen.web.maximize` should be (false)
      WebSettings.`gwen.web.remote.localFileDetector` should be (false)
      WebSettings.`gwen.web.remote.url` should be (None)
      WebSettings.`gwen.web.remote.sessionRetries` should be (false)
      WebSettings.`gwen.web.sendKeys.clearFirst` should be (false)
      WebSettings.`gwen.web.sendKeys.clickFirst` should be (false)
      WebSettings.`gwen.web.suppress.images` should be (false)
      WebSettings.`gwen.web.throttle.msecs` should be (100)
      WebSettings.`gwen.web.useragent` should be (None)
      WebSettings.`gwen.web.wait.seconds` should be (10L)
    }
  }

  "Standalone project init" should "override .conf defaults" in {
    Settings.exclusively {
      withSetting("gwen.initDir", ".") {
        Settings.init(
          new File("src/main/resources/init/gwen.conf"),
          new File("src/main/resources/init/browsers/chrome.conf"))
        assertInitConf(".", "target")
      }
    }
  }

  "JS project init" should "should override .conf defaults" in {
    Settings.exclusively {
      withSetting("gwen.initDir", "gwen") {
        Settings.init(
          new File("src/main/resources/init/gwen.conf"),
          new File("src/main/resources/init/browsers/chrome.conf"))
        assertInitConf("gwen", "target")
      }
    }
  }

  private def assertInitConf(expectedBaseDir: String, expectedOutDir: String): Unit = {

    GwenSettings.`gwen.assertion.mode` should be (AssertionMode.hard)
    GwenSettings.`gwen.associative.meta` should be (true)
    GwenSettings.`gwen.auto.bind.tableData.outline.examples` should be (true)
    GwenSettings.`gwen.auto.discover.data.csv` should be (true)
    GwenSettings.`gwen.auto.discover.data.json` should be (false)
    GwenSettings.`gwen.auto.discover.meta` should be (true)
    GwenSettings.`gwen.auto.trim.data.csv` should be (false)
    GwenSettings.`gwen.auto.trim.data.json` should be (false)
    GwenSettings.`gwen.behavior.rules` should be (BehaviorMode.lenient)
    GwenSettings.`gwen.feature.dialect` should be ("en")
    GwenSettings.`gwen.feature.failfast.enabled` should be (true)
    GwenSettings.`gwen.feature.failfast.exit` should be (false)
    GwenSettings.`gwen.feature.mode` should be (FeatureMode.imperative)
    GwenSettings.`gwen.mask.char` should be ('*')
    GwenSettings.`gwen.baseDir`.getPath should be (expectedBaseDir)
    GwenSettings.`gwen.outDir`.getPath should be (expectedOutDir)
    GwenSettings.`gwen.parallel.maxThreads` should be (GwenSettings.availableProcessors)
    GwenSettings.`gwen.rampup.interval.seconds` should be (None)
    GwenSettings.`gwen.report.overwrite` should be (false)
    GwenSettings.`gwen.report.attach.functions` should be (true)
    GwenSettings.`gwen.report.stepDef.indent.pixels` should be (20)
    GwenSettings.`gwen.report.suppress.meta` should be (true)
    GwenSettings.`gwen.report.slideshow.create` should be (false)
    GwenSettings.`gwen.report.slideshow.framespersecond` should be (4)
    GwenSettings.`gwen.state.level` should be (StateLevel.feature)
    GwenSettings.`gwen.console.log.colors` should be (true)
    GwenSettings.`gwen.console.log.depth` should be (1)
    GwenSettings.`gwen.console.log.stepDefs` should be (true)
    GwenSettings.`gwen.console.repl.autoSuggestions` should be (true)
    GwenSettings.`gwen.console.repl.tabCompletion` should be (true)
    GwenSettings.`gwen.logLevel.deprecations` should be (Level.WARNING)

    LaunchSettings.`gwen.launch.options.format` should be (List(ReportFormat.html, ReportFormat.results))

    WebSettings.`gwen.target.browser` should be (WebBrowser.chrome)
    WebSettings.`gwen.target.env` should be ("test")

    WebSettings.`gwen.web.authorize.plugins` should be (false)
    WebSettings.`gwen.web.assertions.delayMillisecs` should be (200)
    WebSettings.`gwen.web.assertions.maxStrikes` should be (10)
    WebSettings.`gwen.web.browser.size` should be (None)
    WebSettings.`gwen.web.capabilities`.asMap.isEmpty should be (true)
    WebSettings.`gwen.web.capture.screenshots.enabled` should be (false)
    WebSettings.`gwen.web.capture.screenshots.duplicates` should be (false)
    WebSettings.`gwen.web.capture.screenshots.highlighting` should be (false)
    WebSettings.`gwen.web.chrome.args`.contains("--remote-allow-origins=*") should be (false)
    WebSettings.`gwen.web.chrome.extensions` should be (Nil)
    WebSettings.`gwen.web.chrome.mobile` should be (Map())
    WebSettings.`gwen.web.chrome.path` should be (None)
    WebSettings.`gwen.web.chrome.prefs` should be (Map())
    WebSettings.`gwen.web.driver.manager` should be (DriverManagerImpl.SeleniumManager)
    WebSettings.`gwen.web.edge.args` should be (Nil)
    WebSettings.`gwen.web.edge.extensions` should be (Nil)
    WebSettings.`gwen.web.edge.mobile` should be (Map())
    WebSettings.`gwen.web.edge.path` should be (None)
    WebSettings.`gwen.web.edge.prefs` should be (Map())
    WebSettings.`gwen.web.firefox.path` should be (None)
    WebSettings.`gwen.web.firefox.prefs` should be (Map())
    WebSettings.`gwen.web.highlight.style` should be ("background: yellow; border: 2px solid gold;")
    WebSettings.`gwen.web.implicit.element.focus` should be (true)
    WebSettings.`gwen.web.implicit.element.moveTo` should be (false)
    WebSettings.`gwen.web.implicit.js.locators` should be (false)
    WebSettings.`gwen.web.locator.wait.seconds` should be (10L)
    WebSettings.`gwen.web.maximize` should be (false)
    WebSettings.`gwen.web.remote.localFileDetector` should be (false)
    WebSettings.`gwen.web.remote.url` should be (None)
    WebSettings.`gwen.web.remote.sessionRetries` should be (false)
    WebSettings.`gwen.web.sendKeys.clearFirst` should be (false)
    WebSettings.`gwen.web.sendKeys.clickFirst` should be (false)
    WebSettings.`gwen.web.suppress.images` should be (false)
    WebSettings.`gwen.web.throttle.msecs` should be (100)
    WebSettings.`gwen.web.useragent` should be (None)
    WebSettings.`gwen.web.wait.seconds` should be (10L)
  }

  "Sample migration .conf" should "should override defaults" in {
    Settings.exclusively {
      Settings.init(new File("src/test/resources/sample/settings/sample.conf"))
      assertMigrationSample()
    }
  }

  "Sample migration .json" should "should override defaults" in {
    Settings.exclusively {
      Settings.init(new File("src/test/resources/sample/settings/sample.json"))
      assertMigrationSample()
    }
  }

  "Sample migration .properties" should "should override defaults" in {
    Settings.exclusively {
      Settings.init(new File("src/test/resources/sample/settings/sample.properties"))
      assertMigrationSample()
    }
  }

  private def assertMigrationSample(): Unit = {
    GwenSettings.`gwen.assertion.mode` should be (AssertionMode.hard)
    GwenSettings.`gwen.associative.meta` should be (true)
    GwenSettings.`gwen.auto.bind.tableData.outline.examples` should be (true)
    GwenSettings.`gwen.auto.discover.data.csv` should be (true)
    GwenSettings.`gwen.auto.discover.data.json` should be (false)
    GwenSettings.`gwen.auto.discover.meta` should be (true)
    GwenSettings.`gwen.auto.trim.data.csv` should be (false)
    GwenSettings.`gwen.auto.trim.data.json` should be (false)
    GwenSettings.`gwen.behavior.rules` should be (BehaviorMode.lenient)
    GwenSettings.`gwen.feature.dialect` should be ("en")
    GwenSettings.`gwen.feature.failfast.enabled` should be (true)
    GwenSettings.`gwen.feature.failfast.exit` should be (false)
    GwenSettings.`gwen.feature.mode` should be (FeatureMode.imperative)
    GwenSettings.`gwen.mask.char` should be ('*')
    GwenSettings.`gwen.baseDir`.getPath should be (".")
    GwenSettings.`gwen.outDir`.getPath should be ("target")
    GwenSettings.`gwen.parallel.maxThreads` should be (GwenSettings.availableProcessors)
    GwenSettings.`gwen.rampup.interval.seconds` should be (None)
    GwenSettings.`gwen.report.overwrite` should be (false)
    GwenSettings.`gwen.report.attach.functions` should be (true)
    GwenSettings.`gwen.report.stepDef.indent.pixels` should be (20)
    GwenSettings.`gwen.report.suppress.meta` should be (true)
    GwenSettings.`gwen.report.slideshow.create` should be (false)
    GwenSettings.`gwen.report.slideshow.framespersecond` should be (4)
    GwenSettings.`gwen.state.level` should be (StateLevel.feature)
    GwenSettings.`gwen.console.log.colors` should be (true)
    GwenSettings.`gwen.console.log.depth` should be (1)
    GwenSettings.`gwen.console.log.stepDefs` should be (true)
    GwenSettings.`gwen.console.repl.autoSuggestions` should be (true)
    GwenSettings.`gwen.console.repl.tabCompletion` should be (true)
    GwenSettings.`gwen.logLevel.deprecations` should be (Level.WARNING)

    WebSettings.`gwen.target.browser` should be (WebBrowser.chrome)
    WebSettings.`gwen.target.env` should be ("test")

    WebSettings.`gwen.web.authorize.plugins` should be (false)
    WebSettings.`gwen.web.assertions.delayMillisecs` should be (200)
    WebSettings.`gwen.web.assertions.maxStrikes` should be (9)
    WebSettings.`gwen.web.browser.size` should be (None)
    WebSettings.`gwen.web.capabilities`.asMap.isEmpty should be (true)
    WebSettings.`gwen.web.capture.screenshots.enabled` should be (false)
    WebSettings.`gwen.web.capture.screenshots.duplicates` should be (false)
    WebSettings.`gwen.web.capture.screenshots.highlighting` should be (false)
    WebSettings.`gwen.web.chrome.args`.contains("--ignore-certificate-errors") should be (true)
    WebSettings.`gwen.web.chrome.args`.contains("--window-size=1920,1080") should be (true)
    WebSettings.`gwen.web.chrome.args`.contains("--remote-allow-origins=*") should be (false)
    Settings.get("gwen.web.chrome.args:JSONArray") should be ("""["--ignore-certificate-errors","--window-size=1920,1080"]""")
    WebSettings.`gwen.web.chrome.extensions` should be (Nil)
    WebSettings.`gwen.web.chrome.mobile` should be (Map())
    WebSettings.`gwen.web.chrome.path` should be (None)
    WebSettings.`gwen.web.chrome.prefs` should be (Map())
    WebSettings.`gwen.web.driver.manager` should be (DriverManagerImpl.SeleniumManager)
    WebSettings.`gwen.web.edge.args` should be (Nil)
    WebSettings.`gwen.web.edge.extensions` should be (Nil)
    WebSettings.`gwen.web.edge.mobile` should be (Map())
    WebSettings.`gwen.web.edge.path` should be (None)
    WebSettings.`gwen.web.edge.prefs` should be (Map())
    WebSettings.`gwen.web.firefox.path` should be (None)
    WebSettings.`gwen.web.firefox.prefs` should be (Map())
    WebSettings.`gwen.web.highlight.style` should be ("background: yellow; border: 2px solid gold;")
    WebSettings.`gwen.web.implicit.element.focus` should be (true)
    WebSettings.`gwen.web.implicit.element.moveTo` should be (false)
    WebSettings.`gwen.web.implicit.js.locators` should be (false)
    WebSettings.`gwen.web.locator.wait.seconds` should be (10L)
    WebSettings.`gwen.web.maximize` should be (false)
    WebSettings.`gwen.web.remote.localFileDetector` should be (false)
    WebSettings.`gwen.web.remote.url` should be (None)
    WebSettings.`gwen.web.remote.sessionRetries` should be (false)
    WebSettings.`gwen.web.sendKeys.clearFirst` should be (false)
    WebSettings.`gwen.web.sendKeys.clickFirst` should be (false)
    WebSettings.`gwen.web.suppress.images` should be (false)
    WebSettings.`gwen.web.throttle.msecs` should be (100)
    WebSettings.`gwen.web.useragent` should be (None)
    WebSettings.`gwen.web.wait.seconds` should be (9L)

  }

  "Results files in gwen init conf" should "resolve" in {
    Settings.exclusively {
      withSetting("gwen.initDir", "gwen") {
        Settings.init(new File("src/main/resources/init/gwen.conf"))
          val resDir = "output/reports/results"
          val resFiles = GwenSettings.`gwen.report.results.files`(GwenOptions())
          resFiles.size should be (7)
          val fPassed = resFiles.find(_.id == "feature.passed").get
          fPassed.id should be ("feature.passed")
          fPassed.file should be (new File(s"$resDir/feature-results-PASSED.csv"))
          fPassed.scope.map(_.nodeType) should be (Some(NodeType.Feature))
          fPassed.scope.flatMap(_.nodeName) should be (None)
          fPassed.status should be (Some(StatusKeyword.Passed))
          fPassed.fields should be (List(
            ResultField("EVAL_STATUS", "gwen.feature.eval.status.keyword.upperCased", false),
            ResultField("EVAL_STARTED", "gwen.feature.eval.started", false),
            ResultField("EVAL_FINISHED", "gwen.feature.eval.finished", false),
            ResultField("FEATURE_FILE", "gwen.feature.file.path", false),
            ResultField("FEATURE_NAME", "gwen.feature.displayName", false),
            ResultField("EVAL_DURATION", "gwen.feature.eval.duration", false)))
          val fFailed = resFiles.find(_.id == "feature.failed").get
          fFailed.id should be ("feature.failed")
          fFailed.file should be (new File(s"$resDir/feature-results-FAILED.csv"))
          fFailed.scope.map(_.nodeType) should be (Some(NodeType.Feature))
          fFailed.scope.flatMap(_.nodeName) should be (None)
          fFailed.status should be (Some(StatusKeyword.Failed))
          fFailed.fields should be (List(
            ResultField("EVAL_STATUS", "gwen.feature.eval.status.keyword.upperCased", false),
            ResultField("EVAL_STARTED", "gwen.feature.eval.started", false),
            ResultField("EVAL_FINISHED", "gwen.feature.eval.finished", false),
            ResultField("FEATURE_FILE", "gwen.feature.file.path", false),
            ResultField("FEATURE_NAME", "gwen.feature.displayName", false),
            ResultField("EVAL_DURATION", "gwen.feature.eval.duration", false),
            ResultField("EVAL_MESSAGE", "gwen.feature.eval.status.message", false)))
          val fAll = resFiles.find(_.id == "feature.all").get
          fAll.id should be ("feature.all")
          fAll.file should be (new File(s"$resDir/feature-results-ALL.csv"))
          fAll.scope.map(_.nodeType) should be (Some(NodeType.Feature))
          fAll.scope.flatMap(_.nodeName) should be (None)
          fAll.status should be (None)
          fAll.fields should be (List(
            ResultField("EVAL_STATUS", "gwen.feature.eval.status.keyword.upperCased", false),
            ResultField("EVAL_STARTED", "gwen.feature.eval.started", false),
            ResultField("EVAL_FINISHED", "gwen.feature.eval.finished", false),
            ResultField("FEATURE_FILE", "gwen.feature.file.path", false),
            ResultField("FEATURE_NAME", "gwen.feature.displayName", false),
            ResultField("EVAL_DURATION", "gwen.feature.eval.duration", false),
            ResultField("EVAL_MESSAGE", "gwen.feature.eval.status.message", false)))
          val sPassed = resFiles.find(_.id == "scenario.passed").get
          sPassed.id should be ("scenario.passed")
          sPassed.file should be (new File(s"$resDir/scenario-results-PASSED.csv"))
          sPassed.scope.map(_.nodeType) should be (Some(NodeType.Scenario))
          sPassed.scope.flatMap(_.nodeName) should be (None)
          sPassed.status should be (Some(StatusKeyword.Passed))
          sPassed.fields should be (List(
            ResultField("EVAL_STATUS", "gwen.scenario.eval.status.keyword.upperCased", false),
            ResultField("EVAL_STARTED", "gwen.scenario.eval.started", false),
            ResultField("EVAL_FINISHED", "gwen.scenario.eval.finished", false),
            ResultField("FEATURE_FILE", "gwen.feature.file.path", false),
            ResultField("FEATURE_NAME", "gwen.feature.displayName", false),
            ResultField("SCENARIO_NAME", "gwen.scenario.displayName", false),
            ResultField("EVAL_DURATION", "gwen.scenario.eval.duration", false)))
          val sFailed = resFiles.find(_.id == "scenario.failed").get
          sFailed.id should be ("scenario.failed")
          sFailed.file should be (new File(s"$resDir/scenario-results-FAILED.csv"))
          sFailed.scope.map(_.nodeType) should be (Some(NodeType.Scenario))
          sFailed.scope.flatMap(_.nodeName) should be (None)
          sFailed.status should be (Some(StatusKeyword.Failed))
          sFailed.fields should be (List(
            ResultField("EVAL_STATUS", "gwen.scenario.eval.status.keyword.upperCased", false),
            ResultField("EVAL_STARTED", "gwen.scenario.eval.started", false),
            ResultField("EVAL_FINISHED", "gwen.scenario.eval.finished", false),
            ResultField("FEATURE_FILE", "gwen.feature.file.path", false),
            ResultField("FEATURE_NAME", "gwen.feature.displayName", false),
            ResultField("SCENARIO_NAME", "gwen.scenario.displayName", false),
            ResultField("EVAL_DURATION", "gwen.scenario.eval.duration", false),
            ResultField("EVAL_MESSAGE", "gwen.scenario.eval.status.message", false)))
          val sAll = resFiles.find(_.id == "scenario.all").get
          sAll.id should be ("scenario.all")
          sAll.file should be (new File(s"$resDir/scenario-results-ALL.csv"))
          sAll.scope.map(_.nodeType) should be (Some(NodeType.Scenario))
          sAll.scope.flatMap(_.nodeName) should be (None)
          sAll.status should be (None)
          sAll.fields should be (List(
            ResultField("EVAL_STATUS", "gwen.scenario.eval.status.keyword.upperCased", false),
            ResultField("EVAL_STARTED", "gwen.scenario.eval.started", false),
            ResultField("EVAL_FINISHED", "gwen.scenario.eval.finished", false),
            ResultField("FEATURE_FILE", "gwen.feature.file.path", false),
            ResultField("FEATURE_NAME", "gwen.feature.displayName", false),
            ResultField("SCENARIO_NAME", "gwen.scenario.displayName", false),
            ResultField("EVAL_DURATION", "gwen.scenario.eval.duration", false),
            ResultField("EVAL_MESSAGE", "gwen.scenario.eval.status.message", false)))
          val sdAll = resFiles.find(_.id == "stepDef.all").get
          sdAll.id should be ("stepDef.all")
          sdAll.file should be (new File(s"$resDir/stepDef-results-ALL.csv"))
          sdAll.scope.map(_.nodeType) should be (Some(NodeType.StepDef))
          sdAll.scope.flatMap(_.nodeName) should be (None)
          sdAll.status should be (None)
          sdAll.fields should be (List(
            ResultField("EVAL_STATUS", "gwen.stepDef.eval.status.keyword.upperCased", false),
            ResultField("EVAL_STARTED", "gwen.stepDef.eval.started", false),
            ResultField("EVAL_FINISHED", "gwen.stepDef.eval.finished", false),
            ResultField("STEPDEF_NAME", "gwen.stepDef.displayName", false),
            ResultField("EVAL_DURATION", "gwen.stepDef.eval.duration", false),
            ResultField("EVAL_MESSAGE", "gwen.stepDef.eval.status.message", false)))
      }
    }
  }

}
