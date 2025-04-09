/*
 * Copyright 2015-2025 Brady Wood, Branko Juric
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

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.Capabilities
import org.openqa.selenium.MutableCapabilities

import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.util.chaining._

import java.io.File
import java.util.ArrayList
import java.util.HashMap

/**
  * Provides access to gwen web settings defined through system properties loaded
  * from properties files.
  *
  * @author Branko Juric
  */
object WebSettings extends LazyLogging {

  /**
    * Checks all mandatory settings.
    */
  def check(): Unit = {
    `gwen.target.browser`
    `gwen.target.env`
    `gwen.web.authorize.plugins`
    `gwen.web.assertions.delayMillisecs`
    `gwen.web.assertions.maxStrikes`
    `gwen.web.browser.headless`
    `gwen.web.browser.size`
    `gwen.web.capabilities`
    `gwen.web.capture.screenshots.enabled`
    `gwen.web.capture.screenshots.duplicates`
    `gwen.web.capture.screenshots.highlighting`
    `gwen.web.chrome.args`
    `gwen.web.chrome.extensions`
    `gwen.web.chrome.mobile`
    `gwen.web.chrome.path`
    `gwen.web.chrome.prefs`
    `gwen.web.edge.args`
    `gwen.web.edge.extensions`
    `gwen.web.edge.mobile`
    `gwen.web.edge.path`
    `gwen.web.edge.prefs`
    `gwen.web.firefox.path`
    `gwen.web.firefox.prefs`
    `gwen.web.highlight.style`
    `gwen.web.implicit.element.focus`
    `gwen.web.locator.wait.seconds`
    `gwen.web.maximize`
    `gwen.web.remote.localFileDetector`
    `gwen.web.remote.url`
    `gwen.web.remote.connectTimeout.seconds`
    `gwen.web.sendKeys.clearFirst`
    `gwen.web.sendKeys.clickFirst`
    `gwen.web.session.expired.autoReplace`
    `gwen.web.suppress.images`
    `gwen.web.throttle.msecs`
    `gwen.web.useragent`
    `gwen.web.wait.seconds`

    Grid.impl.foreach(_.videoEnabled)

  }

  /** Chrome driver setting. */
  def `webdriver.chrome.driver`: Option[String] = {
    Settings.getOpt("webdriver.chrome.driver")
  }

  /** Gecko (firefox) driver setting. */
  def `webdriver.gecko.driver`: Option[String] = {
    Settings.getOpt("webdriver.gecko.driver")
  }

  /** Edge driver setting. */
  def `webdriver.edge.driver`: Option[String] = {
    Settings.getOpt("webdriver.edge.driver")
  }

  /**
    * Provides access to the `gwen.target.browser` setting used to set the target browser
    * (default value is `chrome`). Valid values include chrome, firefox, safari and edge
    */
  def `gwen.target.browser`: WebBrowser = {
    WebBrowser.parse(Settings.get("gwen.target.browser", Some("gwen.web.browser")))
  }

  /**
    * Provides access to the `gwen.target.env` setting used to set the target environment
    * (default value is `local`). Valid values include local, dev, test, staging, prod, or other user defined environment.
    */
  def `gwen.target.env`: String = {
    Settings.get("gwen.target.env")
  }

  /**
    * Provides access to the `gwen.web.useragent` setting used to set the user agent header
    * in the browser (currently only supported for firefox and chrome).
    */
  def `gwen.web.useragent`: Option[String] = {
    Settings.getOpt("gwen.web.useragent")
  }

  /**
   * If set, allows gwen-web to connect to a remote webdriver.
   */
  def `gwen.web.remote.url`: Option[String] = {
    Settings.getOpt("gwen.web.remote.url")
  }

  /**
    * Provides access to the `gwen.authorize.plugins` setting used to control whether
    * or not the browser should authorize browser plugins. (default value is `false`).
    */
  def `gwen.web.authorize.plugins`: Boolean = {
    Settings.getBoolean("gwen.web.authorize.plugins")
  }

  /**
    * Provides access to the `gwen.web.assertions.delayMillisecs` setting used to control the 
    * delay time between initial and consecutive assertion attempts.
    */
  def `gwen.web.assertions.delayMillisecs`: Int = {
    val delay = Settings.getInt("gwen.web.assertions.delayMillisecs")
    if (delay < 0) {
      Errors.propertyLoadError("gwen.web.assertions.delayMillisecs", "cannot be less than 0")
    } else {
      delay
    }
  }

  /**
    * Provides access to the `gwen.web.assertions.maxStrikes` setting used to control the 
    * maximum number of times to retry failed assertions before timing out.
    */
  def `gwen.web.assertions.maxStrikes`: Int = {
    val maxStrikes = {
      val valueOpt = Settings.getOpt("gwen.web.assertions.maxStrikes")
      if (valueOpt.map(_ == "infinity").getOrElse(false)) Int.MaxValue
      else if (valueOpt.map(_ == "auto").getOrElse(false)) `gwen.web.wait.seconds`.toInt
      else Settings.getInt("gwen.web.assertions.maxStrikes")
    }
    if (maxStrikes < 1) {
      Errors.propertyLoadError("gwen.web.assertions.maxStrikes", "cannot be less than 1")
    } else {
      maxStrikes
    }
  }

  /**
    * Provides access to the `gwen.web.wait.seconds` setting used to set the implicit
    * timeout/wait time in the web driver (default is 10 seconds). This value is also used as the default for
    * `gwen.web.locator.wait.seconds`.
    */
  def `gwen.web.wait.seconds`: Long = {
    Settings.getLong("gwen.web.wait.seconds")
  }

  /**
    * Provides access to the `gwen.web.locator.wait.seconds` setting used to set the implicit
    * locator wait/timeout time in the web driver (default is `gwen.web.wait.seconds` seconds).
    */
  def `gwen.web.locator.wait.seconds`: Long = {
    Settings.getLong("gwen.web.locator.wait.seconds")
  }

  /**
    * Provides access to the `gwen.web.maximize` setting used to control whether
    * or not the web driver should maximize the browser window (default value is `false`).
    */
  def `gwen.web.maximize`: Boolean = {
    Settings.getBoolean("gwen.web.maximize") tap { enabled => 
      if (enabled) {
        Deprecation.log("Setting", "gwen.web.maximize = true", Some("gwen.web.browser.size = 1920x1080 (or desired size)"))
      }
    }
  }

  /**
    * Provides access to the `gwen.web.throttle.msecs` setting used to control the wait
    * between javascript evaluations and duration of element highlighting (default value
    * is 100 msecs).
    */
  def `gwen.web.throttle.msecs`: Long = {
    Settings.getLong("gwen.web.throttle.msecs")
  }

  /**
    * Provides access to the `gwen.web.highlight.style` setting used to control how
    * elements are highlighted (default value is `background: yellow; border: 1px solid gold;`).
    */
  def `gwen.web.highlight.style`: String = {
    Settings.get("gwen.web.highlight.style")
  }

  /**
    * Provides access to the `gwen.web.capture.screenshots.enabled` setting used to control whether
    * or not the web driver should capture screenshots for all steps (default value is `false`).
    * Note that setting this to `true` degrades performance significantly. If the setting is true,
    * then the `gwen.report.slideshow.create` setting is also implicitly set to true if it is not
    * set so that the report generator in core web component knows to generate the slideshow.
    */
  def `gwen.web.capture.screenshots.enabled`: Boolean = {
    Settings.getBoolean("gwen.web.capture.screenshots.enabled", Some("gwen.web.capture.screenshots")) tap { isSet =>
      if (isSet) Settings.set("gwen.report.slideshow.create", true.toString)
    }
  }

  /**
    * Provides access to the `gwen.web.capture.screenshots.highlighting` setting used to control whether
    * or not the web driver should capture screenshots for all steps that highlight elements on a page
    * (default value is `false`). If the setting is true, then the `gwen.report.slideshow.create` setting is
    * also implicitly set to true if it is not set so that the report generator in core web component knows
    * to generate the slideshow.
    * Note that setting this to `true` degrades performance significantly.
    */
  def `gwen.web.capture.screenshots.highlighting`: Boolean = {
    Settings.getBoolean("gwen.web.capture.screenshots.highlighting") tap { isSet =>
      if (isSet) Settings.set("gwen.report.slideshow.create", true.toString)
    }
  }

  /**
    * Provides access to the `gwen.web.suppress.images` setting used to control whether
    * or not image rendering will be suppressed in the browser (default value
    * is `false`). Currently this capability is only supported in firefox driver.
    */
  def `gwen.web.suppress.images`: Boolean = {
    Settings.getBoolean("gwen.web.suppress.images")
  }

  /**
   * Provides access to the `gwen.web.chrome.extensions` settings use to set
   * the list of Chrome web browser extensions to load (default is empty list).
   * The settings accepts a comma separated list of paths to extensions (.crx files
   * or location paths). Each extension provided is loaded into the Chrome web driver.
   */
  def `gwen.web.chrome.extensions`: List[File] = {
    Settings.getList("gwen.web.chrome.extensions").map(new File(_))
  }

  /**
   * Provides access to the `gwen.web.edge.extensions` settings use to set
   * the list of Edge web browser extensions to load (default is empty list).
   * The settings accepts a comma separated list of paths to extensions (.crx files
   * or location paths). Each extension provided is loaded into the Edge web driver.
   */
  def `gwen.web.edge.extensions`: List[File] = {
    Settings.getList("gwen.web.edge.extensions").map(new File(_))
  }

  /**
    * Provides access to the `gwen.web.capture.screenshots.duplicates` setting used to control whether
    * or not the web driver should capture or discard contiguously duplicate screenshots
    * (default value is `false` ~ to discard). If set to `false`, then a screenshot will be discarded
    * if its size in bytes matches that of the last captured screenshot.
    */
  def `gwen.web.capture.screenshots.duplicates`: Boolean = {
    Settings.getBoolean("gwen.web.capture.screenshots.duplicates")
  }

  /**
    * Provides access to the `gwen.web.chrome.path` setting used to specify the
    * path to the Chrome browser binary. If not set, chromedriver will use the
    * default system Chrome install. On macOS, this should be the actual binary,
    * not just the app (e.g., `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome`).
    */
  def `gwen.web.chrome.path`: Option[String] = {
    Settings.getOpt("gwen.web.chrome.path")
  }

  /**
    * Provides access to the `gwen.web.edge.path` setting used to specify the
    * path to the Edge browser binary. If not set, edgedriver will use the
    * default system Edge install.
    */
  def `gwen.web.edge.path`: Option[String] = {
    Settings.getOpt("gwen.web.edge.path")
  }

  /**
   * Provides access to the `gwen.web.chrome.args` setting used to set
   * the list of Chrome web driver arguments to load (default is empty list).
   * This setting merges a comma separated list of arguments set in the `gwen.web.chrome.args` property with all
   * the value of all properties that start with `gwen.web.chrome.args.`.
   * List of chrome arguments: https://peter.sh/experiments/chromium-command-line-switches
   */
  def `gwen.web.chrome.args`: List[String] = {
    Settings.getList("gwen.web.chrome.args")
  }

  /**
   * Provides access to the `gwen.web.edge.args` setting used to set
   * the list of Edge web driver arguments to load (default is empty list).
   * This setting merges a comma separated list of arguments set in the `gwen.web.edge.args` property with all
   * the value of all properties that start with `gwen.web.edge.args.`.
   * List of edge arguments: https://peter.sh/experiments/chromium-command-line-switches
   */
  def `gwen.web.edge.args`: List[String] = {
    Settings.getList("gwen.web.edge.args")
  }

  /**
   * Provides access to the chrome preference settings. This setting merges a comma separated list of preferences
   * set in the `gwen.web.chrome.prefs` property with all properties that start with `gwen.web.chrome.pref.`.
   * List of chrome prefs: https://chromium.googlesource.com/chromium/src/+/master/chrome/common/pref_names.cc
   */
  def `gwen.web.chrome.prefs`: Map[String, String] = {
    Settings.getMap("gwen.web.chrome.prefs", "gwen.web.chrome.pref")
  }

  /**
   * Provides access to the edge preference settings. This setting merges a comma separated list of preferences
   * set in the `gwen.web.edge.prefs` property with all properties that start with `gwen.web.edge.pref.`.
   * List of edge prefs: https://chromium.googlesource.com/chromium/src/+/master/chrome/common/pref_names.cc
   */
  def `gwen.web.edge.prefs`: Map[String, String] = {
    Settings.getMap("gwen.web.edge.prefs", "gwen.web.edge.pref")
  }

  /**
    * Provides access to the `gwen.web.firefox.path` setting used to specify the
    * path to the Firefox browser binary. If not set, geckodriver will use the
    * default system Firefox install. On macOS, this should be the actual binary,
    * not just the app (e.g, `/Applications/Firefox.app/Contents/MacOS/firefox`).
    */
  def `gwen.web.firefox.path`: Option[String] = {
    Settings.getOpt("gwen.web.firefox.path")
  }

  /**
   * Provides access to the firefox preference settings. This setting merges a comma separated list of preferences
   * set in the `gwen.web.firefox.prefs` property with all properties that start with `gwen.web.firefox.pref.`.
   * List of firefox prefs: https://stackoverflow.com/questions/25251583/downloading-file-to-specified-location-with-selenium-and-python
   */
  def `gwen.web.firefox.prefs`: Map[String, String] = {
    Settings.getMap("gwen.web.firefox.prefs", "gwen.web.firefox.pref")
  }

  /**
    * Provides access to the `gwen.web.browser.headless` setting used to control whether
    * or not the browser should run headless. (default value is `false`).
    */
  def `gwen.web.browser.headless`: Boolean = { 
    Settings.getBoolean("gwen.web.browser.headless")
  }

  /**
   * Provides access to the web capabilities settings. This setting merges a comma separated list of capabilities
   * set in the `gwen.web.capabilities` property with all properties that start with `gwen.web.capability.`.
   * See: https://github.com/SeleniumHQ/selenium/wiki/DesiredCapabilities
   */
  def `gwen.web.capabilities`: Capabilities = {
    new MutableCapabilities(getCapabilities(Settings.getMap("gwen.web.capabilities", "gwen.web.capability"), new HashMap[String, Any]()))
  }

  private def getCapabilities(input: Map[String, Any], caps: HashMap[String, Any]): HashMap[String, Any] = {
    input foreach { (name, value) => 
      name.split("""\.""").toList match {
        case head :: tail if tail.nonEmpty => 
          val unqualifiedName = tail.mkString(".")
          addExtensionCapability(head, unqualifiedName, value, caps)
        case _ => 
          addCapability(name, value, caps)
      }
    }
    caps
  }

  private def addExtensionCapability(key: String, name: String, value: Any, caps: HashMap[String, Any]): Unit = {
    val extCaps = Option(caps.get(key)).map(_.asInstanceOf[HashMap[String, Any]]).getOrElse(new HashMap[String, Any]())
    addCapability(name, value, extCaps)
    caps.put(key, getCapabilities(extCaps.asScala.toMap, extCaps))
    if (name.contains(".")) extCaps.remove(name)
  }

  private def addCapability(name: String, value: Any, caps: HashMap[String, Any]): Unit = {
    val strValue = String.valueOf(value).trim
    val (entryName, isListEntry) = name match {
        case r"""(.*)$entryName\.\d+""" => (entryName, true)
        case _ => (name, false)
      }
    if (isListEntry) {
      val list = Option(caps.get(entryName)).map(_.asInstanceOf[ArrayList[String]]).getOrElse(new ArrayList[String]())
      list.add(strValue)
      caps.put(entryName, list)
    } else {
      if (!caps.containsKey(entryName)) {
        try {
          caps.put(entryName, Integer.valueOf(strValue))
        } catch {
          case _: Throwable =>
            if (strValue.matches("(true|false)")) caps.put(entryName, java.lang.Boolean.valueOf(strValue))
            else caps.put(entryName, strValue)
        }
      }
    }
  }

  /**
    * Provides access to the `gwen.web.implicit.element.focus` setting used to determine whether or not Gwen should
    * implicitly put the focus on all located web elements. Default value is true.
    */
  def `gwen.web.implicit.element.focus`: Boolean = {
    Settings.getBoolean("gwen.web.implicit.element.focus")
  }

  /**
    * Provides access to the `gwen.web.implicit.element.moveTo` setting used to determine whether or not Gwen should
    * implicitly move to all located web elements. Default value is false.
    */
  def `gwen.web.implicit.element.moveTo`: Boolean = {
    Settings.getBooleanOpt("gwen.web.implicit.element.moveTo").getOrElse(false)
  }

  /**
    * Provides access to the `gwen.web.browser.size` setting used to set the browser window size.
    * Expects value matching `width x height (e:g 1920 x 1080 for height 1920 and width 1080).
    */
  def `gwen.web.browser.size`: Option[(Int, Int)] = {
    Settings.getOpt("gwen.web.browser.size") map { value =>
      val values = value.split('x')
      if (values != null && values.size == 2) {
        Try((values(0).trim.toInt, values(1).trim.toInt)) getOrElse {
          Errors.invalidSettingError("gwen.web.browser.size", value, "width and height must be integers")
        }
      } else {
        Errors.invalidSettingError("gwen.web.browser.size", value, "width x height expected")
      }
    }
  }

  /**
    * Provides access to the `gwen.web.sendKeys.clearFirst` setting used to control whether
    * or not Gwen will clear fields before sending keys to them. (default value is `false`).
    */
  def `gwen.web.sendKeys.clearFirst`: Boolean = {
    Settings.getBoolean("gwen.web.sendKeys.clearFirst")
  }

  /**
    * Provides access to the `gwen.web.sendKeys.clickFirst` setting used to control whether
    * or not Gwen will click fields before sending keys to them. (default value is `false`).
    */
  def `gwen.web.sendKeys.clickFirst`: Boolean = {
    Settings.getBoolean("gwen.web.sendKeys.clickFirst")
  }

  /**
    * Provides access to the `gwen.web.session.expired.autoReplace` setting used to control whether
    * or not Gwen will auto create a new session if current one has expired. (default value is `true`).
    */
  def `gwen.web.session.expired.autoReplace`: Boolean = {
    Settings.getBoolean("gwen.web.session.expired.autoReplace")
  }

  /**
   * Provides access to the chrome mobile emulation settings. This setting merges a comma separated list of values
   * set in the `gwen.web.chrome.mobile` property with all properties that start with
   * `gwen.web.chrome.mobile.`.
   */
  def `gwen.web.chrome.mobile`: Map[String, String] = {
    Settings.getMap("gwen.web.chrome.mobile", "gwen.web.chrome.mobile")
  }

  /**
   * Provides access to the edge mobile emulation settings. This setting merges a comma separated list of values
   * set in the `gwen.web.edge.mobile` property with all properties that start with
   * `gwen.web.edge.mobile.`.
   */
  def `gwen.web.edge.mobile`: Map[String, String] = {
    Settings.getMap("gwen.web.edge.mobile", "gwen.web.edge.mobile")
  }
    
  /**
   * If set, enables the local file detector on remote webdriver if `gwen.web.remote.url` is set (default is auto).
   */
  def `gwen.web.remote.localFileDetector`: Boolean = {
    `gwen.web.remote.url`.nonEmpty && 
    (Settings.getOpt("gwen.web.remote.localFileDetector").exists(_ == "auto") || Settings.getBoolean("gwen.web.remote.localFileDetector"))
  }

  /**
   * If set, enables remote webdriver session retries if `gwen.web.remote.url` is set (default is auto).
   */
  def `gwen.web.remote.sessionRetries`: Boolean = {
    val auto = Settings.getOpt("gwen.web.remote.sessionRetries").exists(_ == "auto")
    (auto && `gwen.web.remote.url`.nonEmpty) || (!auto && Settings.getBoolean("gwen.web.remote.sessionRetries"))
  }

  /**
   * The maximum time to wait for a remote server connection.
   */
  def `gwen.web.remote.connectTimeout.seconds`: Long = {
    Settings.getLong("gwen.web.remote.connectTimeout.seconds")
  }

}
