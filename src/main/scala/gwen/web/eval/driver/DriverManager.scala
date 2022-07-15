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

package gwen.web.eval.driver

import gwen.core._
import gwen.core.state.SensitiveData

import gwen.web.eval.WebBrowser
import gwen.web.eval.WebSettings
import gwen.web.eval.WebErrors
import gwen.web.eval.driver.event.WebSessionEventDispatcher
import gwen.web.eval.driver.event.WebSessionEventListener

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.util.chaining._

import com.typesafe.scalalogging.LazyLogging
import io.github.bonigarcia.wdm.WebDriverManager
import org.openqa.selenium.{Dimension, Capabilities, MutableCapabilities, WebDriver, WindowType}
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeDriverService
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.chromium.ChromiumOptions
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxOptions, FirefoxProfile}
import org.openqa.selenium.ie.{InternetExplorerDriver, InternetExplorerOptions}
import org.openqa.selenium.remote.HttpCommandExecutor
import org.openqa.selenium.remote.LocalFileDetector
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.safari.{SafariDriver, SafariOptions}

import java.io.File
import java.net.URL
import java.{time => jt}
import java.util.concurrent.TimeUnit
import java.util.concurrent.Semaphore

/** Driver manager companion. */
object DriverManager extends LazyLogging {

  /** Semaphore to limit number of permitted web drivers to max threads setting. */
  private lazy val totalPermits = GwenSettings.`gwen.parallel.maxThreads`
  private lazy val driverPermits = new Semaphore(totalPermits, true)
  
  def acquireDriverPermit(): Unit = driverPermits.acquire()
  def releaseDriverPermit(): Unit = {
    if (driverPermits.availablePermits() < totalPermits) {
      driverPermits.release()
    }
  }

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

  private val eventDispatcher = new WebSessionEventDispatcher()

  def addWebSessionEventListener(listener: WebSessionEventListener): Unit = {
    eventDispatcher.addListener(listener)
  }

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
        eventDispatcher.sessionClosed(driver)
      } finally {
        DriverManager.releaseDriverPermit()
      }
    }
    session = "primary"
  }

   /**
    * Invokes a function that performs an operation on the current web driver.
    *
    * @param f the function to perform
    */
  def withWebDriver[T](f: WebDriver => T): T = {
      f(webDriver)
  }

  /** Loads the selenium webdriver. */
  private [eval] def loadWebDriver: WebDriver = withGlobalSettings {
    DriverManager.acquireDriverPermit()
    try {
      (WebSettings.`gwen.web.remote.url` match {
        case Some(addr) =>
          remoteDriver(addr)
        case None =>
          localDriver(WebSettings.`gwen.target.browser`)
      }) tap { driver =>
        eventDispatcher.sessionOpened(driver)
        WebSettings.`gwen.web.browser.size` foreach { case (width, height) =>
          logger.info(s"Resizing browser window to width $width and height $height")
          driver.manage().window().setSize(new Dimension(width, height))
        }
      }
    } catch {
      case e: Throwable =>
        DriverManager.releaseDriverPermit()
        throw e
    }
  }

  private def remoteDriver(addr: String): WebDriver = {
    val capSettings = WebSettings.`gwen.web.capabilities`
    val browserName = capSettings.asMap.asScala.get("browserName").orElse(capSettings.asMap.asScala.get("browser")).orElse(capSettings.asMap.asScala.get("device")).getOrElse(WebSettings.`gwen.target.browser`).toString.toLowerCase
    val browser = WebBrowser.parse(browserName)
    val options = browser match {
      case WebBrowser.firefox => firefoxOptions()
      case WebBrowser.chrome => chromeOptions()
      case WebBrowser.ie => ieOptions()
      case WebBrowser.edge => edgeOptions()
      case WebBrowser.safari => safariOptions()
    }
    logger.info(s"Starting remote $browser session${ if(session == "primary") "" else s": $session"}")
    remote(addr, options)
  }

  /**
    * Gets the local web driver for the given name.
    *
    *  @param browser the target browser
    *  @throws gwen.web.WebErrors.UnsupportedWebDriverException if the given
    *          web driver name is unsupported
    */
  private def localDriver(browser: WebBrowser): WebDriver = {
    logger.info(s"Starting $browser browser session${ if(session == "primary") "" else s": $session"}")
    browser match {
      case WebBrowser.chrome => chrome()
      case WebBrowser.edge => edge()
      case WebBrowser.firefox => firefox()
      case WebBrowser.safari => safari()
      case WebBrowser.ie => ie()
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
        setCapabilities(options)
      }
  }

  private def chromeOptions() : ChromeOptions = chromiumOptions(WebBrowser.chrome, new ChromeOptions())

  private def edgeOptions() : EdgeOptions = chromiumOptions(WebBrowser.edge, new EdgeOptions())

  private def chromiumOptions[T <: ChromiumOptions[T]](browser: WebBrowser, options: T): T = {
    WebSettings.`gwen.web.useragent` foreach { agent =>
      logger.info(s"Setting $browser argument: --user-agent=$agent")
      options.addArguments(s"--user-agent=$agent")
    }
    if (WebSettings.`gwen.web.authorize.plugins`) {
      logger.info(s"Setting $browser argument: --always-authorize-plugins")
      options.addArguments("--always-authorize-plugins")
    }
    options.addArguments("--enable-automation")
    val browserPath = browser match {
      case WebBrowser.chrome => WebSettings.`gwen.web.chrome.path`
      case _ => WebSettings.`gwen.web.edge.path`
    }
    browserPath foreach { path =>
      logger.info(s"Setting $browser path: $path")
      options.setBinary(path)
    }
    val browserArgs =  browser match {
      case WebBrowser.chrome => WebSettings.`gwen.web.chrome.args`
      case _ => WebSettings.`gwen.web.edge.args`
    }
    browserArgs foreach { arg =>
      logger.info(s"Setting $browser argument: $arg")
      SensitiveData.withValue(arg) { a =>
        options.addArguments(a)
      }
    }
    if (WebSettings.`gwen.web.browser.headless`) {
      logger.info(s"Setting $browser argument: headless")
      options.addArguments("headless")
    }
    val prefs = new java.util.HashMap[String, Object]()
    val browserPrefs = browser match {
      case WebBrowser.chrome => WebSettings.`gwen.web.chrome.prefs`
      case _ => WebSettings.`gwen.web.edge.prefs`
    }
    browserPrefs foreach { case (name, value) =>
      logger.info(s"Setting $browser preference: $name=$value")
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
    val browserExensions = browser match {
      case WebBrowser.chrome => WebSettings.`gwen.web.chrome.extensions`
      case _ => WebSettings.`gwen.web.edge.extensions`
    }
    browserExensions tap { extensions =>
      if (extensions.nonEmpty) {
        logger.info(s"Loading $browser extension${if (extensions.size > 1) "s" else ""}: ${extensions.mkString(",")}")
        options.addExtensions(extensions*)
      }
    }
    val mobileSettings = browser match {
      case WebBrowser.chrome => WebSettings.`gwen.web.chrome.mobile`
      case _ => WebSettings.`gwen.web.edge.mobile`
    }
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
      logger.info(s"$browser mobile emulation options: $mobileEmulation")
      options.setExperimentalOption("mobileEmulation", mobileEmulation)
    }
    setCapabilities(options)
    options
  }

  private def ieOptions(): InternetExplorerOptions = new InternetExplorerOptions() tap { options =>
    setDefaultCapability("requireWindowFocus", java.lang.Boolean.TRUE, options)
    setDefaultCapability("nativeEvents", java.lang.Boolean.TRUE, options);
    setDefaultCapability("unexpectedAlertBehavior", "accept", options);
    setDefaultCapability("ignoreProtectedModeSettings", java.lang.Boolean.TRUE, options);
    setDefaultCapability("disable-popup-blocking", java.lang.Boolean.TRUE, options);
    setDefaultCapability("enablePersistentHover", java.lang.Boolean.TRUE, options);
  }

  private def safariOptions(): SafariOptions = new SafariOptions() tap { options =>
    setCapabilities(options)
  }

  private def setCapabilities(capabilities: MutableCapabilities): Capabilities = {
    capabilities tap { caps =>
      WebSettings.`gwen.web.capabilities`.asMap.asScala foreach { case (name, value) =>
        logger.info(s"Setting web capability: $name=$value")
        caps.setCapability(name, value)
      }
    }
  }

  private def setDefaultCapability(name: String, value: Any, capabilities: MutableCapabilities): Unit = {
    if (capabilities.getCapability(name) == null) {
      logger.info(s"Setting web capability: $name=$value")
      capabilities.setCapability(name, value)
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

  private [eval] def remote(hubUrl: String, capabilities: Capabilities): WebDriver =
    new RemoteWebDriver(new HttpCommandExecutor(new URL(hubUrl)), capabilities) tap { driver =>
      if (WebSettings`gwen.web.remote.localFileDetector`) {
        driver.setFileDetector(new LocalFileDetector())
      }
    }

  private def withGlobalSettings(driver: WebDriver): WebDriver = {
    logger.info(s"Implicit wait (default locator timeout) = ${WebSettings.`gwen.web.locator.wait.seconds`} second(s)")
    driver.manage().timeouts().implicitlyWait(jt.Duration.ofSeconds(WebSettings.`gwen.web.locator.wait.seconds`))
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
    * Switches to a tab or window occurance.
    *
    * @param occurrence the tag or window occurrence to switch to (primary is zero, 1st opened is 1, 2nd open is 2, ..)
    */
  def switchToWindow(occurrence: Int): Unit = {
    windows().lift(occurrence) map { handle =>
      switchToWindow(handle, isChild = (occurrence > 0))
    } getOrElse {
      WebErrors.noSuchWindowError(s"Cannot switch to window $occurrence: no such occurrence")
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
      case parent::children if children.nonEmpty =>
        val child = children.last
        webDriver.switchTo.window(child)
        logger.info(s"Closing child window ($child)")
        webDriver.close()
        switchToParent()
      case _ =>
        WebErrors.noSuchWindowError("Cannot close child window: currently at root window which has no child")
    }
  }

  def closeWindow(occurrence: Int): Unit = {
    windows().lift(occurrence) map { handle =>
      switchToWindow(handle, isChild = (occurrence > 0))
      logger.info(s"Closing window occurrence $occurrence ($handle)")
      webDriver.close()
      switchToParent()
    } getOrElse {
      WebErrors.noSuchWindowError(s"Cannot close window $occurrence: no such occurrence")
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

  /**
    * Starts and switches to a new tab or window.
    *
    * @param winType tab or window
    */
  def switchToNewWindow(winType: WindowType): Unit = {
    logger.info(s"Switching to new browser ${winType.name.toLowerCase}")
    if (noOfSessions() > 0) {
      webDriver.switchTo().newWindow(winType)
    } else {
      switchToSession("primary")
    }
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

  def getSessionId: Option[String] = {
    withWebDriver { getSessionId }
  }

  def getSessionId(driver: WebDriver): Option[String] = {
    if (driver.isInstanceOf[RemoteWebDriver]) {
      Some(driver.asInstanceOf[RemoteWebDriver].getSessionId.toString)
    } else {
      None
    }
  }

}
