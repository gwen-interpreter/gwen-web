/*
 * Copyright 2015-2021 Brady Wood, Branko Juric
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

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.util.chaining._

import com.microsoft.edge.seleniumtools.{EdgeDriver, EdgeOptions}
import com.typesafe.scalalogging.LazyLogging
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.{Dimension, MutableCapabilities, WebDriver}
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions, FirefoxProfile}
import org.openqa.selenium.ie.{InternetExplorerDriver, InternetExplorerOptions}
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.remote.HttpCommandExecutor
import org.openqa.selenium.remote.LocalFileDetector
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.safari.{SafariDriver, SafariOptions}

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.Semaphore

/** Driver manager companion. */
object DriverManager {

  /** Semaphore to limit number of permitted web drivers to max threads setting. */
  lazy val DriverPermit = new Semaphore(GwenSettings.`gwen.parallel.maxThreads`, true)

}

/**
  * Provides and manages access to the web drivers.
  */
class DriverManager() extends LazyLogging {

  // put drivers downloaded by WebDriverManager in ~.gwen/wdm dir by default
  if (sys.props.get("wdm.targetPath").isEmpty) {
    sys.props += (("wdm.targetPath", new File(new File(System.getProperty("user.home")), ".gwen/wdm").getAbsolutePath))
  }

  /** The current web browser session. */
  private var session = "primary"

  /** Map of web driver instances (keyed by name). */
  private [eval] val drivers: mutable.Map[String, WebDriver] = mutable.Map()

  /** Provides private access to the web driver */
  private def webDriver: WebDriver = drivers.getOrElse(session, {
      loadWebDriver tap { driver =>
        drivers += (session -> driver)
      }
    })

  /** Resets the driver manager. */
  def reset(): Unit = {
    session = "primary"
  }

  /** Quits all browsers and closes the web drivers (if any have loaded). */
  def quit(): Unit = {
    drivers.keys.foreach(quit)
  }

  /** Quits a named browser and associated web driver instance. */
  def quit(name: String): Unit = {
    drivers.get(name) foreach { driver =>
      try {
        logger.info(s"Closing browser session${ if(name == "primary") "" else s": $name"}")
        driver.quit()
        drivers.remove(name)
      } finally {
        DriverManager.DriverPermit.release()
      }
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
  private [eval] def loadWebDriver: WebDriver = withGlobalSettings {
    DriverManager.DriverPermit.acquire()
    try {
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
    } catch {
      case e: Throwable =>
        DriverManager.DriverPermit.release()
        throw e
    }
  }

  private def remoteDriver(addr: String): WebDriver = {
    val capSettings = WebSettings.`gwen.web.capabilities`
    val browser = capSettings.get("browserName").orElse(capSettings.get("browser")).orElse(capSettings.get("device")).getOrElse(WebSettings.`gwen.web.browser`)
    val capabilities = new DesiredCapabilities(
      browser.trim.toLowerCase match {
        case "firefox" => firefoxOptions()
        case "chrome" => chromeOptions()
        case "ie" => ieOptions()
        case "edge" => edgeOptions()
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
    *  @throws gwen.web.WebErrors.UnsupportedWebDriverException if the given
    *          web driver name is unsupported
    */
  private def localDriver(driverName: String): WebDriver = {
    logger.info(s"Starting $driverName browser session${ if(session == "primary") "" else s": $session"}")
    driverName match {
      case "firefox" => firefox()
      case "ie" => ie()
      case "edge" => edge()
      case "chrome" => chrome()
      case "safari" => safari()
      case _ => WebErrors.unsupportedWebDriverError(driverName)
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
        WebSettings.`gwen.web.firefox.path` foreach { path =>
          logger.info(s"Setting firefox path: $path")
          options.setBinary(path)
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
    options.addArguments("--enable-automation")
    if (WebSettings.`gwen.web.accept.untrusted.certs`) {
      logger.info("Setting chrome argument: --ignore-certificate-errors")
      options.addArguments("--ignore-certificate-errors")
    }
    WebSettings.`gwen.web.chrome.path` foreach { path =>
      logger.info(s"Setting chrome path: $path")
      options.setBinary(path)
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
      }) { (deviceName: String) =>
        mobileEmulation.put("deviceName", deviceName)
      }
      logger.info(s"Chrome mobile emulation options: $mobileEmulation")
      options.setExperimentalOption("mobileEmulation", mobileEmulation)
    }
    setDesiredCapabilities(options)
  }

  private def edgeOptions() : EdgeOptions = new EdgeOptions() tap { options =>
    WebSettings.`gwen.web.useragent` foreach { agent =>
      logger.info(s"Setting edge argument: --user-agent=$agent")
      options.addArguments(s"--user-agent=$agent")
    }
    if (WebSettings.`gwen.web.authorize.plugins`) {
      logger.info("Setting edge argument: --always-authorize-plugins")
      options.addArguments("--always-authorize-plugins")
    }
    options.addArguments("--enable-automation")
    if (WebSettings.`gwen.web.accept.untrusted.certs`) {
      logger.info("Setting edge argument: --ignore-certificate-errors")
      options.addArguments("--ignore-certificate-errors")
    }
    WebSettings.`gwen.web.edge.path` foreach { path =>
      logger.info(s"Setting edge path: $path")
      options.setBinary(path)
    }
    WebSettings.`gwen.web.edge.args` foreach { arg =>
      logger.info(s"Setting edge argument: $arg")
      options.addArguments(arg)
    }
    if (WebSettings.`gwen.web.browser.headless`) {
      logger.info("Setting edge argument: headless")
      options.addArguments("headless")
    }
    val prefs = new java.util.HashMap[String, Object]()
    WebSettings.`gwen.web.edge.prefs` foreach { case (name, value) =>
      logger.info(s"Setting edge preference: $name=$value")
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
    WebSettings.`gwen.web.edge.extensions` tap { extensions =>
      if (extensions.nonEmpty) {
        logger.info(s"Loading edge extension${if (extensions.size > 1) "s" else ""}: ${extensions.mkString(",")}")
        options.addExtensions(extensions:_*)
      }
    }
    val mobileSettings = WebSettings.`gwen.web.edge.mobile`
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
      }) { (deviceName: String) =>
        mobileEmulation.put("deviceName", deviceName)
      }
      logger.info(s"Edge mobile emulation options: $mobileEmulation")
      options.setExperimentalOption("mobileEmulation", mobileEmulation)
    }
    setDesiredCapabilities(options)
  }

  private def ieOptions(): InternetExplorerOptions = new InternetExplorerOptions() tap { options =>
    setDefaultCapability("requireWindowFocus", true, options)
    setDefaultCapability("nativeEvents", false, options);
    setDefaultCapability("unexpectedAlertBehaviour", "accept", options);
    setDefaultCapability("ignoreProtectedModeSettings", true, options);
    setDefaultCapability("disable-popup-blocking", true, options);
    setDefaultCapability("enablePersistentHover", true, options);
  }

  private def safariOptions(): SafariOptions = new SafariOptions() tap { options =>
    setDesiredCapabilities(options)
  }

  private def setDesiredCapabilities(capabilities: MutableCapabilities): MutableCapabilities = {
    capabilities tap { caps =>
      WebSettings.`gwen.web.capabilities` foreach { case (name, value) =>
        setCapabilty(name, value, caps);
      }
      setDefaultCapability(CapabilityType.ACCEPT_SSL_CERTS, WebSettings.`gwen.web.accept.untrusted.certs`, caps)
      setDefaultCapability("javascriptEnabled", true, caps)
    }
  }

  private def setDefaultCapability(name: String, value: Any, capabilities: MutableCapabilities): Unit = {
    if (capabilities.getCapability(name) == null) {
      setCapabilty(name, value, capabilities)
    }
  }

  private def setCapabilty(name: String, value: Any, capabilities: MutableCapabilities): Unit = {
    def strValue = String.valueOf(value).trim
    try {
      logger.info(s"Setting web capability: $name=$strValue")
      capabilities.setCapability(name, Integer.valueOf(strValue))
    } catch {
      case _: Throwable =>
        if (strValue.matches("(true|false)")) capabilities.setCapability(name, java.lang.Boolean.valueOf(strValue))
        else capabilities.setCapability(name, strValue)
    }
  }

  private [eval] def chrome(): WebDriver = {
    if (WebSettings.`webdriver.chrome.driver`.isEmpty) {
      WebDriverManager.chromedriver().setup()
    }
    new ChromeDriver(chromeOptions())
  }

  private [eval] def firefox(): WebDriver = {
    if (WebSettings.`webdriver.gecko.driver`.isEmpty) {
      WebDriverManager.firefoxdriver().setup()
    }
    new FirefoxDriver(firefoxOptions())
  }

  private [eval] def ie(): WebDriver = {
    if (WebSettings.`webdriver.ie.driver`.isEmpty) {
      WebDriverManager.iedriver().setup()
    }
    new InternetExplorerDriver(ieOptions())
  }

  private [eval] def edge(): WebDriver = {
    if (WebSettings.`webdriver.edge.driver`.isEmpty) {
      WebDriverManager.edgedriver().setup()
    }
    new EdgeDriver(edgeOptions())
  }

  private [eval] def safari(): WebDriver = {
    new SafariDriver(safariOptions())
  }

  private [eval] def remote(hubUrl: String, capabilities: DesiredCapabilities): WebDriver =
    new RemoteWebDriver(new HttpCommandExecutor(new URL(hubUrl)), capabilities) tap { driver =>
      if (WebSettings`gwen.web.remote.localFileDetector`) {
        driver.setFileDetector(new LocalFileDetector())
      }
    }

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
    * Switches to a tab or child window occurance.
    *
    * @param occurrence the tag or window occurrence to switch to (first opened is occurrence 1, 2nd is 2, ..)
    */
  def switchToChild(occurrence: Int): Unit = {
    windows().lift(occurrence) map { child =>
      switchToWindow(child, isChild = true)
    } getOrElse {
      WebErrors.noSuchWindowError(s"Cannot switch to child window $occurrence: no such occurrence")
    }
  }

  /** Switches the driver to another tab or window.
    *
    * @param handle the handle of the window to switch to
    * @param isChild true if switching to a child window; false for parent window
    */
  private def switchToWindow(handle: String, isChild: Boolean): Unit = {
    logger.info(s"Switching to ${if (isChild) "child" else "parent"} window ($handle)")
    drivers.get(session).fold(WebErrors.noSuchWindowError("Cannot switch to window: no windows currently open")) { _.switchTo.window(handle) }
  }

  def closeChild(): Unit = {
    windows() match {
      case parent::children =>
        val child = children.last
        webDriver.switchTo.window(child)
        logger.info(s"Closing child window ($child)")
        webDriver.close()
        switchToParent()
      case _ =>
        WebErrors.noSuchWindowError("Cannot close child window: currently at root window which has no child")
    }
  }

  def closeChild(occurrence: Int): Unit = {
    windows().lift(occurrence) map { child =>
      switchToWindow(child, isChild = true)
      logger.info(s"Closing child window at occurrence $occurrence ($child)")
      webDriver.close()
      switchToParent()
    } getOrElse {
      WebErrors.noSuchWindowError(s"Cannot close child window $occurrence: no such occurrence")
    }
  }

  /** Switches to the parent window. */
  def switchToParent(): Unit = {
    windows() match {
      case parent::_ =>
        switchToWindow(parent, isChild = false)
      case _ =>
        logger.warn("Bypassing switch to parent window: no child windows open")
    }
  }

  /** Switches to the top window / first frame */
  def switchToDefaultContent(): Unit = {
    webDriver.switchTo().defaultContent()
  }

  /**
    * Switches the web driver session
    *
    * @param session the name of the session to switch to
    */
  def switchToSession(session: String): Unit = {
    logger.info(s"Switching to browser session: $session")
    this.session = session
    webDriver
  }

  /** Gets the number of open sesions. */
  def noOfSessions(): Int = drivers.size

  /** Gets the number of open windows. */
  def noOfWindows(): Int = windows().size

  /**
    * Starts a new session if there isn't one or stays in the current one.
    */
  def newOrCurrentSession(): Unit = {
    if (noOfSessions() == 0) {
      switchToSession("primary")
    }
  }

  def windows(): List[String] = withWebDriver(_.getWindowHandles.asScala.toList)

}
