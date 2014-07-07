/*
 * Copyright 2014 Brady Wood, Branko Juric
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

import java.util.concurrent.TimeUnit
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.safari.SafariDriver
import gwen.Predefs.Kestrel
import gwen.eval.EnvContext
import gwen.gwenSetting
import gwen.eval.ScopedData
import org.openqa.selenium.firefox.FirefoxProfile

/**
 * Defines the web environment context. This includes the configured selenium web
 * driver instance and the user and page scopes.
 *
 *  @author Branko Juric
 */
class WebEnvContext(val driverName: String) extends EnvContext {

  /**
   * Provides access to the user scopes.
   */
  def featureScopes = dataScope(ScopedData.GlobalScopeName)

  /**
   * Provides access to the page scopes.
   */
  def pageScopes = dataScope("page")

  /**
   * Selenium web driver (lazily loaded).
   */
  private var isWebDriverSet = false
  lazy val webDriver: WebDriver = {
    loadWebDriver(driverName) tap { webdriver =>
      isWebDriverSet = true;
      val wait = gwenSetting.get("gwen.web.wait.seconds").toLong
      webdriver.manage().timeouts().implicitlyWait(wait, TimeUnit.SECONDS)
    }
  }

  /**
   * Gets the selenium webdriver.
   *
   * @param driverName
   * 			the name of the driver to get
   */
  private[web] def loadWebDriver(driverName: String): WebDriver = driverName match {
    case "Firefox" => { 
      val profile = new FirefoxProfile();
      gwenSetting.getOpt("general.useragent.override").foreach {  profile.setPreference("general.useragent.override", _ ) }
      new FirefoxDriver(profile);
    }
    case "IE" => new InternetExplorerDriver
    case "Chrome" => new ChromeDriver
    case "Safari" => new SafariDriver
    case _ => sys.error(s"Unsupported webdriver: $driverName")
  }

  /**
   * Closes this context and the web driver (if it has loaded).  Once closed,
   * this context should not be used again.
   */
  override def close() {
    if (isWebDriverSet) {
      webDriver.quit()
      isWebDriverSet = false
    }
  }

}
