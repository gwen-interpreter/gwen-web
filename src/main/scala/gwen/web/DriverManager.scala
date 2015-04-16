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
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.HttpCommandExecutor
import java.net.URL
import org.openqa.selenium.remote.CapabilityType

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
  private[web] def loadWebDriver: WebDriver = withGlobalSettings {
    val driverName = WebSettings.`gwen.web.browser`.toLowerCase
    logger.info(s"Loading $driverName web driver")
    WebSettings.`gwen.web.remote.url` match {
    	case Some(addr) => remoteDriver(driverName, addr)
      case None => localDriver(driverName)
    }
  }
  
  private def remoteDriver(driverName: String, addr: String): WebDriver = {
    val capabilities = driverName match {
      case "firefox" => 
        DesiredCapabilities.firefox tap { capabilities =>
          capabilities.setCapability(FirefoxDriver.PROFILE, firefoxProfile)
        }
      case "chrome" => DesiredCapabilities.chrome tap { capabilities =>
        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions)
        capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, WebSettings.`gwen.web.accept.untrusted.certs`);
      }
      case "ie" => ieCapabilities
    }
    capabilities.setJavascriptEnabled(true)

    //TODO
    //profile override if the useragent is set.
    //set the untrusted certificates
    //etc..
    //mock the drivers, write tests
    //capabilities.setCapability("record-video", true)
    //capabilities.setCapability("build", sys.env("TRAVIS_BUILD_NUMBER"))
    //capabilities.setCapability("tunnel-identifier", sys.env("TRAVIS_JOB_NUMBER"))
    //val username = sys.env("SAUCE_USERNAME")
    //val accessKey = sys.env("SAUCE_ACCESS_KEY")
    // val hubURL = new URL(s"http://$username:$accessKey@localhost:4445/wd/hub")
    //val hubURL = new URL(s"http://localhost:44466/wd/hub")
    remote(addr, capabilities)
  }
  
  private def localDriver(driverName: String): WebDriver = driverName match {
    case "firefox" => firefox()
    case "ie" => ie()
    case "chrome" => chrome()
    case "safari" => safari()
    case _ => sys.error(s"Unsupported webdriver: $driverName")
  }
  
  private def firefoxProfile() : FirefoxProfile = new FirefoxProfile() tap { profile =>
    WebSettings.`gwen.web.useragent` foreach { 
      profile.setPreference("general.useragent.override", _)
    }
    if (WebSettings.`gwen.web.authorize.plugins`) {
      profile.setPreference("security.enable_java", true);
      profile.setPreference("plugin.state.java", 2);
    }
    WebSettings.`gwen.web.accept.untrusted.certs` tap { acceptUntrustedCerts =>
      profile.setAcceptUntrustedCertificates(true);
      profile.setAssumeUntrustedCertificateIssuer(false);
    }
  }
  
  private def chromeOptions() : ChromeOptions = new ChromeOptions() tap { options =>
    WebSettings.`gwen.web.useragent` foreach { 
      agent => options.addArguments(s"--user-agent=$agent") 
    }
    if (WebSettings.`gwen.web.authorize.plugins`) {
      options.addArguments(s"--always-authorize-plugins") 
    }
    options.addArguments("--test-type")
    if (WebSettings.`gwen.web.accept.untrusted.certs`) {
      options.addArguments("--ignore-certificate-errors")
    }
  }
  
  private def ieCapabilities: DesiredCapabilities = new DesiredCapabilities() tap {capabilities => 
    capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);  
  }
  
  private[web] def chrome(): WebDriver = new ChromeDriver(chromeOptions)
  
  private[web] def firefox(): WebDriver = new FirefoxDriver(firefoxProfile)
  
  private[web] def ie(): WebDriver = new InternetExplorerDriver(ieCapabilities)
  
  private[web] def safari(): WebDriver = new SafariDriver()
  
  private[web] def remote(hubUrl: String, capabilities: DesiredCapabilities): WebDriver =
    new RemoteWebDriver(new HttpCommandExecutor(new URL(hubUrl)), capabilities)
  
  private def withGlobalSettings(driver: WebDriver): WebDriver = {
    driver.manage().timeouts().implicitlyWait(WebSettings.`gwen.web.wait.seconds`, TimeUnit.SECONDS)
    if (WebSettings.`gwen.web.maximize`) {
      driver.manage().window().maximize() 
    }
    driver
  }
  
}