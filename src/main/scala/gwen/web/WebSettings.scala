/*
 * Copyright 2015-2019 Brady Wood, Branko Juric
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

import gwen.{Predefs, Settings}
import java.io.File

import gwen.Predefs.{FileIO, Kestrel}
import gwen.errors.invalidSettingError

import scala.util.Try

/**
  * Provides access to gwen web settings defined through system properties loaded 
  * from properties files.
  *
  * @author Branko Juric
  */
object WebSettings {

  /** Chrome driver setting. */
  def `webdriver.chrome.driver`: Option[String] = Settings.getOpt("webdriver.chrome.driver")

  /** Gecko (firefox) driver setting. */
  def `webdriver.gecko.driver`: Option[String] = Settings.getOpt("webdriver.gecko.driver")

  /** IE driver setting. */
  def `webdriver.ie.driver`: Option[String] = Settings.getOpt("webdriver.ie.driver")

  /** Edge driver setting. */
  def `webdriver.edge.driver`: Option[String] = Settings.getOpt("webdriver.edge.driver")
  
  /**
    * Provides access to the `gwen.web.browser` setting used to set the target browser 
    * (default value is `chrome`). Valid values include chrome, firefox, safari, ie, and edge
    */
  def `gwen.web.browser`: String = 
    Settings.getOpt("gwen.web.browser").getOrElse("chrome")
  
  /**
    * Provides access to the `gwen.web.useragent` setting used to set the user agent header 
    * in the browser (currently only supported for firefox and chrome).
    */
  def `gwen.web.useragent`: Option[String] = Settings.getOpt("gwen.web.useragent")
  
  /**
   * If set, allows gwen-web to connect to a remote webdriver.
   */
  def `gwen.web.remote.url`: Option[String] = Settings.getOpt("gwen.web.remote.url")
  
  /**
    * Provides access to the `gwen.authorize.plugins` setting used to control whether 
    * or not the browser should authorize browser plugins. (default value is `false`).
    */
  def `gwen.web.authorize.plugins`: Boolean = Settings.getOpt("gwen.web.authorize.plugins").getOrElse("false").toBoolean
  
  /**
    * Provides access to the `gwen.web.wait.seconds` setting used to set the implicit
    * timeout/wait time in the web driver (default is 10 seconds). This value is also used as the default for
    * `gwen.web.locator.wait.seconds`.
    */
  def `gwen.web.wait.seconds`: Long = Settings.getOpt("gwen.web.wait.seconds").getOrElse("10").toLong

  /**
    * Provides access to the `gwen.web.locator.wait.seconds` setting used to set the implicit
    * locator wait/timeout time in the web driver (default is `gwen.web.wait.seconds` seconds).
    */
  def `gwen.web.locator.wait.seconds`: Long = Settings.getOpt("gwen.web.locator.wait.seconds").map(_.toLong).getOrElse(`gwen.web.wait.seconds`)
  
  /**
    * Provides access to the `gwen.web.maximize` setting used to control whether 
    * or not the web driver should maximize the browser window (default value is `false`).
    */
  def `gwen.web.maximize`: Boolean = Settings.getOpt("gwen.web.maximize").getOrElse("false").toBoolean
  
  /**
    * Provides access to the `gwen.web.throttle.msecs` setting used to control the wait 
    * between javascript evaluations and duration of element highlighting (default value 
    * is 100 msecs).
    */
  def `gwen.web.throttle.msecs`: Long = Settings.getOpt("gwen.web.throttle.msecs").getOrElse("100").toLong
  
  /**
    * Provides access to the `gwen.web.highlight.style` setting used to control how
    * elements are highlighted (default value is `background: yellow; border: 2px solid gold;`).
    */
  def `gwen.web.highlight.style`: String = Settings.getOpt("gwen.web.highlight.style").getOrElse("background: yellow; border: 2px solid gold;")
  
  /**
    * Provides access to the `gwen.web.capture.screenshots` setting used to control whether 
    * or not the web driver should capture screenshots for all steps (default value is `false`).
    * Note that setting this to `true` degrades performance significantly. If the setting is true,
    * then the `gwen.report.slideshow.create` setting is also implicitly set to true so that the
    * report generator in core web component knows to generate the slideshow.
    */
  def `gwen.web.capture.screenshots`: Boolean = Settings.getOpt("gwen.web.capture.screenshots").getOrElse("false").toBoolean tap { isSet =>
    if (isSet) Settings.set("gwen.report.slideshow.create", "true")
  }
  
  /**
    * Provides access to the `gwen.web.capture.screenshots.highlighting` setting used to control whether 
    * or not the web driver should capture screenshots for all steps that highlight elements on a page 
    * (default value is `false`).
    * Note that setting this to `true` degrades performance significantly.
    */
  def `gwen.web.capture.screenshots.highlighting`: Boolean = Settings.getOpt("gwen.web.capture.screenshots.highlighting").getOrElse("false").toBoolean
  
  /**
    * Provides access to the `gwen.web.accept.untrusted.certs` setting used to control whether 
    * or not the web driver should accept untrusted (self signed) SSL certificates (default value
    * is `true`).
    */
  def `gwen.web.accept.untrusted.certs`: Boolean = Settings.getOpt("gwen.web.accept.untrusted.certs").getOrElse("true").toBoolean
  
  /**
    * Provides access to the `gwen.web.suppress.images` setting used to control whether 
    * or not image rendering will be suppressed in the browser (default value
    * is `false`). Currently this capability is only supported in firefox driver.
    */
  def `gwen.web.suppress.images`: Boolean = Settings.getOpt("gwen.web.suppress.images").getOrElse("false").toBoolean

  /**
   * Provides access to the `gwen.web.chrome.extensions` settings use to set 
   * the list of Chrome web browser extensions to load (default is empty list).
   * The settings accepts a comma separated list of paths to extensions (.crx files
   * or location paths). Each extension provided is loaded into the Chrome web driver.
   */
  def `gwen.web.chrome.extensions`: List[File] = Settings.getOpt("gwen.web.chrome.extensions").map(_.split(",").toList.map(_.trim)).getOrElse(Nil).map(new File(_))
  
  /**
    * Provides access to the `gwen.web.capture.screenshots.duplicates` setting used to control whether 
    * or not the web driver should capture or discard contiguously duplicate screenshots 
    * (default value is `false` ~ to discard). If set to `false`, then a screenshot will be discarded 
    * if its size in bytes matches that of the last captured screenshot.
    */
  def `gwen.web.capture.screenshots.duplicates`: Boolean = Settings.getOpt("gwen.web.capture.screenshots.duplicates").getOrElse("false").toBoolean
  
  /**
   * Provides access to the `gwen.web.chrome.args` setting used to set
   * the list of Chrome web driver arguments to load (default is empty list).
   * This setting merges a comma separated list of arguments set in the `gwen.web.chrome.args` property with all
   * the value of all properties that start with `gwen.web.chrome.args.`.
   * List of chrome arguments: https://peter.sh/experiments/chromium-command-line-switches
   */
  def `gwen.web.chrome.args`: List[String] = Settings.findAllMulti("gwen.web.chrome.args")
  
  /**
   * Provides access to the chrome preference settings. This setting merges a comma separated list of preferences
   * set in the `gwen.web.chrome.prefs` property with all properties that start with `gwen.web.chrome.pref.`.
   * List of chrome prefs: https://chromium.googlesource.com/chromium/src/+/master/chrome/common/pref_names.cc
   */
  def `gwen.web.chrome.prefs`: Map[String, String] = Settings.findAllMulti("gwen.web.chrome.prefs", "gwen.web.chrome.pref")

  /**
   * Provides access to the firefox preference settings. This setting merges a comma separated list of preferences
   * set in the `gwen.web.firefox.prefs` property with all properties that start with `gwen.web.firefox.pref.`.
   * List of firefox prefs: https://stackoverflow.com/questions/25251583/downloading-file-to-specified-location-with-selenium-and-python
   */
  def `gwen.web.firefox.prefs`: Map[String, String] = Settings.findAllMulti("gwen.web.firefox.prefs", "gwen.web.firefox.pref")

  /**
    * Provides access to the `gwen.web.browser.headless` setting used to control whether
    * or not the browser should run headless. (default value is `false`).
    */
  def `gwen.web.browser.headless`: Boolean = Settings.getOpt("gwen.web.browser.headless").getOrElse("false").toBoolean

  /**
   * Provides access to the web capabilities settings. This setting merges a comma separated list of capabilities
   * set in the `gwen.web.capabilities` property with all properties that start with `gwen.web.capability.`.
   * See: https://github.com/SeleniumHQ/selenium/wiki/DesiredCapabilities
   */
  def `gwen.web.capabilities`: Map[String, String] = Settings.findAllMulti("gwen.web.capabilities", "gwen.web.capability")

  /**
    * Provides access to the `gwen.web.implicit.js.locators` setting used to determine whether or not Gwen should
    * implicitly convert all locator bindings to JavaScript equivalents to force all elements to be located by
    * executing javascript on the page. Default value is false.
    */
  def `gwen.web.implicit.js.locators`: Boolean = Settings.getOpt("gwen.web.implicit.js.locators").getOrElse("false").toBoolean

  /**
    * Provides access to the `gwen.web.implicit.element.focus` setting used to determine whether or not Gwen should
    * implicitly put the focus on all located web elements. Default value is true.
    */
  def `gwen.web.implicit.element.focus`: Boolean = Settings.getOpt("gwen.web.implicit.element.focus").getOrElse("true").toBoolean

  /**
    * Provides access to the `gwen.web.browser.size` setting used to set the browser window size.
    * Expects value matching `width x height (e:g 1200 x 800 for height 1200 and width 800).
    * This setting is only applicable if the gwen.web.maximize` is not set to `true`.
    */
  def `gwen.web.browser.size`: Option[(Int, Int)] = {
    if (!`gwen.web.maximize`) {
      Settings.getOpt("gwen.web.browser.size") map { value =>
        val values = value.split('x')
        if (values != null && values.size == 2) {
          Try(values(0).trim.toInt, values(1).trim.toInt) getOrElse {
            invalidSettingError("gwen.web.browser.size", value, "width and height must be integers")
          }
        } else {
          invalidSettingError("gwen.web.browser.size", value, "width x height expected")
        }
      }
    } else {
      None
    }
  }

  /**
    * Provides access to the `gwen.web.sendKeys.clearFirst` setting used to control whether
    * or not Gwen will clear fields before sending keys to them. (default value is `false`).
    */
  def `gwen.web.sendKeys.clearFirst`: Boolean = Settings.getOpt("gwen.web.sendKeys.clearFirst").getOrElse("false").toBoolean

  /**
    * Provides access to the `gwen.web.sendKeys.clickFirst` setting used to control whether
    * or not Gwen will click fields before sending keys to them. (default value is `false`).
    */
  def `gwen.web.sendKeys.clickFirst`: Boolean = Settings.getOpt("gwen.web.sendKeys.clickFirst").getOrElse("false").toBoolean

  /**
   * Provides access to the chrome mobile emulation settings. This setting merges a comma separated list of values
   * set in the `gwen.web.chrome.mobile` property with all properties that start with
   * `gwen.web.chrome.mobile.`.
   * See: https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#mobile-emulation-by-device-name
   * See: https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#mobile-emulation-by-device-metrics
   */
  def `gwen.web.chrome.mobile`: Map[String, String] =
    Settings.findAllMulti("gwen.web.chrome.mobile", "gwen.web.chrome.mobile")

}