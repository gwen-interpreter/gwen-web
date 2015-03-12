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

import java.util.concurrent.TimeUnit

import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxProfile
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.safari.SafariDriver

import com.typesafe.scalalogging.slf4j.LazyLogging

import gwen.Predefs.Kestrel

/** Provides access to the web driver used to drive the browser. */
trait DriverManager extends LazyLogging {

  /** Web driver (lazily loaded). */
  private[web] var _webDriver: Option[WebDriver] = None
  
  def webDriver: WebDriver = _webDriver match {
    case None => 
      _webDriver = Some(loadWebDriver)
      _webDriver.get
    case _ => 
      _webDriver.get
  }
  
  /** Quits the browser and closes the web driver (if it has loaded). */
   def quit() {
    _webDriver foreach { _.quit() } 
    _webDriver = None
  }
  
  /** Loads the selenium webdriver. */
  private[web] def loadWebDriver: WebDriver = {
    val driverName = GwenWebSettings.`gwen.web.browser` tap { browser =>
      logger.info(s"Loading $browser web driver")
    }
    (driverName.toLowerCase() match {
      case "firefox" =>
        new FirefoxDriver(new FirefoxProfile() tap { profile =>
          GwenWebSettings.`gwen.web.useragent` foreach { 
            profile.setPreference("general.useragent.override", _)
          }
          profile.setAcceptUntrustedCertificates(true);
          if (GwenWebSettings.`gwen.web.authorize.plugins`) {
            profile.setPreference("security.enable_java", true);
            profile.setPreference("plugin.state.java", 2);
          }
        })
      case "ie" => new InternetExplorerDriver()
      case "chrome" =>
        new ChromeDriver(new ChromeOptions() tap { options =>
          GwenWebSettings.`gwen.web.useragent` foreach { 
            agent => options.addArguments(s"--user-agent=$agent") 
          }
          if (GwenWebSettings.`gwen.web.authorize.plugins`) {
            options.addArguments(s"--always-authorize-plugins") 
          }
          options.addArguments("--test-type")
        })
      case "safari" => new SafariDriver
      case _ => sys.error(s"Unsupported webdriver: $driverName")
    }) tap { driver =>
      driver.manage().timeouts().implicitlyWait(GwenWebSettings.`gwen.web.wait.seconds`, TimeUnit.SECONDS)
      if (GwenWebSettings.`gwen.web.maximize`) {
        driver.manage().window().maximize() 
      }
    }
  }
  
}