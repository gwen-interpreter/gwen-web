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

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

import org.openqa.selenium.{Dimension, MutableCapabilities, WebDriver}
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions, FirefoxProfile}
import org.openqa.selenium.ie.{InternetExplorerDriver, InternetExplorerOptions}
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.HttpCommandExecutor
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.safari.{SafariDriver, SafariOptions}
import com.typesafe.scalalogging.LazyLogging
import gwen.Predefs.Kestrel
import gwen.errors._
import gwen.web.errors._
import io.github.bonigarcia.wdm.WebDriverManager

import collection.JavaConverters._
import scala.collection.mutable

/**
  * Provides and manages access to the web drivers.
  */
class DriverManager extends LazyLogging {

  // put drivers downloaded by WebDriverManager in ~.gwen/wdm dir by default
  if (sys.props.get("wdm.targetPath").isEmpty) {
    sys.props += (("wdm.targetPath", new File(new File(System.getProperty("user.home")), ".gwen/wdm").getAbsolutePath))
  }
    
  /** Map of web driver instances (keyed by name). */
  private[web] val drivers: mutable.Map[String, WebDriver] = mutable.Map()

  /** The current web browser session. */
  private var session = "primary"

  /** Current stack of windows (per session). */
  private val windows = mutable.Map[String, List[String]]()
    
  /** Provides private access to the web driver */
  private def webDriver: WebDriver = drivers.getOrElse(session, {
      loadWebDriver tap { driver =>
        drivers += (session -> driver)
        addSessionWindow(driver.getWindowHandle)
      }
    })

  /** Resets the driver manager. */
  def reset() {
    session = "primary"
    windows.clear()
  }

  /** Quits all browsers and closes the web drivers (if any have loaded). */
  def quit() {
    drivers.keys.foreach(quit)
  }

  /** Quits a named browser and associated web driver instance. */
  def quit(name: String) {
    drivers.get(name) foreach { driver =>
      logger.info(s"Closing browser session${ if(name == "primary") "" else s": $name"}")
      driver.quit()
      drivers.remove(name)
      windows.remove(name)
    }
    session = "primary"
  }

   /**
    * Invokes a function that performs an operation on the current web driver.
    *
    * @param f the function to perform
    */
  def withWebDriver[T](f: WebDriver => T): T = f(webDriver)
  
  /** Loads the selenium webdriver. */
  private[web] def loadWebDriver: WebDriver = withGlobalSettings {
    (WebSettings.`gwen.web.remote.url` match {
      case Some(addr) =>
        remoteDriver(addr)
      case None =>
        val driverName = WebSettings.`gwen.web.browser`.toLowerCase
        localDriver(driverName)
    }) tap { driver =>
      WebSettings.`gwen.web.browser.size` foreach { case (width, height) =>
        logger.info(s"Resizing browser window to width $width and height $height")
        driver.manage().window().setSize(new Dimension(width, height))
      }
    }
  }
  
  private def remoteDriver(addr: String): WebDriver = {
    val capSettings = WebSettings.`gwen.web.capabilities`
    val browser = capSettings.get("browserName").orElse(capSettings.get("browser")).orElse(capSettings.get("device")).getOrElse(WebSettings.`gwen.web.browser`)
    val capabilities = new DesiredCapabilities(
      browser.trim.toLowerCase match {
        case "firefox" => firefoxOptions()
        case "chrome" => chromeOptions()
        case "ie" | "internet explorer" => ieOptions()
        case "safari" => safariOptions()
        case _ =>
          new MutableCapabilities() tap { caps =>
            setDesiredCapabilities(caps)
          }
      })
    logger.info(s"Starting remote $browser session${ if(session == "primary") "" else s": $session"}")
    remote(addr, capabilities)
  }
  
  /**
    * Gets the local web driver for the given name.
    * 
    *  @param driverName the name of the driver to get
    *  @throws gwen.web.errors.UnsupportedWebDriverException if the given
    *          web driver name is unsupported 
    */
  private def localDriver(driverName: String): WebDriver = {
    logger.info(s"Starting $driverName browser session${ if(session == "primary") "" else s": $session"}")
    driverName match {
      case "firefox" => firefox()
      case "ie" => ie()
      case "chrome" => chrome()
      case "safari" => safari()
      case _ => unsupportedWebDriverError(driverName)
    }
  }
  
  private def firefoxOptions() : FirefoxOptions = {
    val firefoxProfile = new FirefoxProfile() tap { profile =>
      WebSettings.`gwen.web.firefox.prefs` foreach { case (name, value) =>
        try {
          logger.info(s"Setting firefox preference: $name=$value")
          profile.setPreference(name, Integer.valueOf(value.trim))
        } catch {
          case _: Throwable =>
            if (value.matches("(true|false)")) profile.setPreference(name, java.lang.Boolean.valueOf(value.trim))
            else profile.setPreference(name, value)
        }
      }
      WebSettings.`gwen.web.useragent` foreach { agent =>
        logger.info(s"Setting firefox preference: general.useragent.override=$agent")
        profile.setPreference("general.useragent.override", agent)
      }
      if (WebSettings.`gwen.web.authorize.plugins`) {
        logger.info("Setting firefox preference: security.enable_java=true")
        profile.setPreference("security.enable_java", true)
        logger.info("Setting firefox preference: plugin.state.java=2")
        profile.setPreference("plugin.state.java", 2)
      }
      WebSettings.`gwen.web.accept.untrusted.certs` tap { _ =>
        logger.info("Setting firefox option: setAcceptUntrustedCertificates(true)")
        profile.setAcceptUntrustedCertificates(true)
        logger.info("Setting firefox option: setAssumeUntrustedCertificateIssuer(false)")
        profile.setAssumeUntrustedCertificateIssuer(false)
      }
      if (WebSettings.`gwen.web.suppress.images`) {
        logger.info("Setting firefox preference: permissions.default.image=2")
        profile.setPreference("permissions.default.image", 2)
      }
    }
    new FirefoxOptions()
      .setProfile(firefoxProfile) tap { options =>
      if (WebSettings.`gwen.web.browser.headless`) {
        logger.info(s"Setting firefox argument: -headless")
        options.addArguments("-headless")
      }
      setDesiredCapabilities(options)
    }
  }
  
  private def chromeOptions() : ChromeOptions = new ChromeOptions() tap { options =>
    WebSettings.`gwen.web.useragent` foreach { agent =>
      logger.info(s"Setting chrome argument: --user-agent=$agent")
      options.addArguments(s"--user-agent=$agent")
    }
    if (WebSettings.`gwen.web.authorize.plugins`) {
      logger.info("Setting chrome argument: --always-authorize-plugins")
      options.addArguments("--always-authorize-plugins") 
    }
    options.addArguments("--test-type")
    if (WebSettings.`gwen.web.accept.untrusted.certs`) {
      logger.info("Setting chrome argument: --ignore-certificate-errors")
      options.addArguments("--ignore-certificate-errors")
    }
    WebSettings.`gwen.web.chrome.args` foreach { arg =>
      logger.info(s"Setting chrome argument: $arg")
      options.addArguments(arg)
    }
    if (WebSettings.`gwen.web.browser.headless`) {
      logger.info("Setting chrome argument: headless")
      options.addArguments("headless")
    }
    val prefs = new java.util.HashMap[String, Object]()
    WebSettings.`gwen.web.chrome.prefs` foreach { case (name, value) =>
      logger.info(s"Setting chrome preference: $name=$value")
      try {
        prefs.put(name, Integer.valueOf(value.trim))
      } catch {
        case _: Throwable =>
          if (value.matches("(true|false)")) prefs.put(name, java.lang.Boolean.valueOf(value.trim))
          else prefs.put(name, value)
      }
    }
    if (!prefs.isEmpty) {
      options.setExperimentalOption("prefs", prefs)
    }
    WebSettings.`gwen.web.chrome.extensions` tap { extensions =>
      if (extensions.nonEmpty) {
        logger.info(s"Loading chrome extension${if (extensions.size > 1) "s" else ""}: ${extensions.mkString(",")}")
        options.addExtensions(extensions:_*)
      }
    }
    val mobileSettings = WebSettings.`gwen.web.chrome.mobile`
    if (mobileSettings.nonEmpty) {
      val mobileEmulation = new java.util.HashMap[String, Object]()
      mobileSettings.get("deviceName").fold({
        val deviceMetrics = new java.util.HashMap[String, Object]()
        mobileSettings foreach { case (name, value) =>
          name match {
            case "width" | "height" => deviceMetrics.put(name, java.lang.Integer.valueOf(value.trim))
            case "pixelRatio" => deviceMetrics.put(name, java.lang.Double.valueOf(value.trim))
            case "touch" => deviceMetrics.put(name, java.lang.Boolean.valueOf(value.trim))
            case _ => mobileEmulation.put(name, value)
          }
        }
        mobileEmulation.put("deviceMetrics", deviceMetrics)
      }) { deviceName: String =>
        mobileEmulation.put("deviceName", deviceName)
      }
      logger.info(s"Chrome mobile emulation options: $mobileEmulation")
      options.setExperimentalOption("mobileEmulation", mobileEmulation)
    }
    setDesiredCapabilities(options)
  }

  private def ieOptions(): InternetExplorerOptions = new InternetExplorerOptions() tap { options =>
    setDesiredCapabilities(options)
  }

  private def safariOptions(): SafariOptions = new SafariOptions() tap { options =>
    setDesiredCapabilities(options)
  }

  private def setDesiredCapabilities(capabilities: MutableCapabilities) {
    WebSettings.`gwen.web.capabilities` foreach { case (name, value) =>
      try {
        logger.info(s"Setting web capability: $name=$value")
        capabilities.setCapability(name, Integer.valueOf(value.trim))
      } catch {
        case _: Throwable =>
          if (value.matches("(true|false)")) capabilities.setCapability(name, java.lang.Boolean.valueOf(value.trim))
          else capabilities.setCapability(name, value)
      }
    }
    logger.info(s"Setting web capability: ${CapabilityType.ACCEPT_SSL_CERTS}=${WebSettings.`gwen.web.accept.untrusted.certs`}")
    capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, WebSettings.`gwen.web.accept.untrusted.certs`)
    logger.info(s"Setting web capability: javascriptEnabled=true")
    capabilities.setCapability("javascriptEnabled", true)
  }
  
  private[web] def chrome(): WebDriver = {
    if (WebSettings.`webdriver.chrome.driver`.isEmpty) { 
      WebDriverManager.chromedriver().setup()
    }
    new ChromeDriver(chromeOptions())
  }
  
  private[web] def firefox(): WebDriver = {
    if (WebSettings.`webdriver.gecko.driver`.isEmpty) {
      WebDriverManager.firefoxdriver().setup()
    }
    new FirefoxDriver(firefoxOptions())
  }
  
  private[web] def ie(): WebDriver = {
    if (WebSettings.`webdriver.ie.driver`.isEmpty) {
      WebDriverManager.iedriver().setup()
    }
    new InternetExplorerDriver(ieOptions())
  }
  
  private[web] def safari(): WebDriver = {
    new SafariDriver(safariOptions())
  }
  
  private[web] def remote(hubUrl: String, capabilities: DesiredCapabilities): WebDriver =
    new RemoteWebDriver(new HttpCommandExecutor(new URL(hubUrl)), capabilities)
  
  private def withGlobalSettings(driver: WebDriver): WebDriver = {
    logger.info(s"Implicit wait (default locator timeout) = ${WebSettings.`gwen.web.locator.wait.seconds`} second(s)")
    driver.manage().timeouts().implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)
    if (WebSettings.`gwen.web.maximize`) {
      logger.info(s"Attempting to maximize window")
      try {
        driver.manage().window().maximize()
      } catch {
        case _: Throwable =>
          logger.warn(s"Maximizing window not supported on current platform, attempting to go full screen instead")
          try {
            driver.manage().window().fullscreen()
          } catch {
            case _: Throwable =>
              logger.warn(s"Could not maximise or go full screen on current platform")
          }
      }
    }
    driver
  }

  /**
    * Switches to the child window if one was just opened.
    */
  private[web] def switchToChild(driver: WebDriver) {
    val sWindows = sessionWindows()
    val children = driver.getWindowHandles.asScala.filter(window => !sWindows.contains(window)).toList match {
      case Nil if sWindows.size > 1 => sWindows.init
      case cs => cs
    }
    if (children.size == 1) {
      switchToWindow(children.head, isChild = true)
    } else if (children.size > 1) {
      ambiguousCaseError(s"Cannot determine which child window to switch to: ${children.size} were detected but only one is supported")
    } else {
      noSuchWindowError("Cannot switch to child window: no child window was found")
    }
  }

  /** Switches the driver to another window.
    *
    * @param handle the handle of the window to switch to
    * @param isChild true if switching to a child window; false for parent window
    */
  private def switchToWindow(handle: String, isChild: Boolean) {
    logger.info(s"Switching to ${if (isChild) "child" else "parent"} window ($handle)")
    drivers.get(session).fold(noSuchWindowError("Cannot switch to window: no windows currently open")) { _.switchTo.window(handle) }
    pushWindow(handle)
  }

  private[web] def closeChild() {
    val sWindows = sessionWindows()
    if (sWindows.size > 1) {
      val child = popSessionWindow()
      webDriver.switchTo.window(child)
      logger.info(s"Closing child window ($child)")
      webDriver.close()
      switchToParent(true)
    } else {
      noSuchWindowError("Cannot close child window: currently at root window which has no child")
    }
  }

  /** Switches to the parent window. */
  private[web] def switchToParent(childClosed: Boolean) {
    val sWindows = sessionWindows()
    if (sWindows.nonEmpty) {
      val child = popSessionWindow()
      val target = if (sWindows.nonEmpty) sWindows.head else child
      switchToWindow(target, isChild = false)
      if (!childClosed) { pushWindow(child) }
    } else {
      logger.warn("Bypassing switch to parent window: no child window currently open")
    }
  }

  /** Switches to the top window / first frame */
  private[web] def switchToDefaultContent(): Unit = webDriver.switchTo().defaultContent()

  private[web] def pushWindow(window: String) {
    val sWindows = sessionWindows()
    if (sWindows.isEmpty || sWindows.head != window) {
      addSessionWindow(window)
    }
  }

  /**
    * Switches the web driver session
    *
    * @param session the name of the session to switch to
    */
  def switchToSession(session: String) {
    logger.info(s"Switching to browser session: $session")
    this.session = session
    webDriver
  }

  private def sessionWindows(): List[String] = {
    if (!windows.contains(session)) {
      windows += (session -> Nil)
    }
    windows(session)
  }

  private def addSessionWindow(window: String): Unit = {
    if (window != null) {
      windows += (session -> (window :: sessionWindows()))
    }
  }

  private def popSessionWindow(): String = {
    val sWindows = sessionWindows()
    windows += (session -> sWindows.tail)
    sWindows.head
  }
  
}
