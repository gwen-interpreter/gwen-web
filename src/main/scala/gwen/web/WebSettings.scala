/*
 * Copyright 2015 Brady Wood, Branko Juric
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

import gwen.Settings

/**
  * Provides access to gwen web settings defined through system properties loaded 
  * from properties files.
  *
  * @author Branko Juric
  */
object WebSettings {
  
  /**
    * Provides access to the `gwen.web.browser` setting used to set the target browser 
    * (default value is `firefox`).
    */
  def `gwen.web.browser`: String = 
    Settings.getOpt("gwen.web.browser").getOrElse("firefox")
  
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
    * timeout/wait time in the web driver (default is 10 seconds).
    */
  def `gwen.web.wait.seconds`: Long = Settings.getOpt("gwen.web.wait.seconds").getOrElse("10").toLong
  
  /**
    * Provides access to the `gwen.web.maximize` setting used to control whether 
    * or not the web driver should maximize the browser window (default value is `false`).
    */
  def `gwen.web.maximize`: Boolean = Settings.getOpt("gwen.web.maximize").getOrElse("false").toBoolean
  
  /**
    * Provides access to the `gwen.web.throttle.msecs` setting used to control the wait 
    * between javascript evaluations and duration of element highlighting (default value 
    * is 200 msecs).
    */
  def `gwen.web.throttle.msecs`: Long = Settings.getOpt("gwen.web.throttle.msecs").getOrElse("200").toLong
  
  /**
    * Provides access to the `gwen.web.highlight.style` setting used to control how
    * elements are highlighted (default value is `background: yellow; border: 2px solid gold;`).
    */
  def `gwen.web.highlight.style`: String = Settings.getOpt("gwen.web.highlight.style").getOrElse("background: yellow; border: 2px solid gold;")
  
  /**
    * Provides access to the `gwen.web.capture.screenshots` setting used to control whether 
    * or not the web driver should capture screenshots for all steps (default value is `false`).
    * Note that setting this to `true` degrades performance significantly.
    */
  def `gwen.web.capture.screenshots`: Boolean = Settings.getOpt("gwen.web.capture.screenshots").getOrElse("false").toBoolean
  
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
}