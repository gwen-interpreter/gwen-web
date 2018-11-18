/*
 * Copyright 2014-2017 Branko Juric, Brady Wood
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

import java.util
import java.util.concurrent.TimeUnit

import org.openqa.selenium.{By, NoSuchElementException, WebElement}
import gwen.web.errors.{WaitTimeoutException, locatorBindingError, noSuchElementError}
import gwen.Predefs.Kestrel
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.text.StringEscapeUtils

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

/**
  * Locates web elements using the selenium web driver.
  *
  * @author Branko Juric, Brady Wood
  */
trait WebElementLocator extends LazyLogging {
  webContext: WebContext =>

  /**
    * Locates a bound web element.
    *
    *  @param elementBinding the web element locator binding
    *  @return the found element (or an error if not found)
    */
  private[web] def locate(elementBinding: LocatorBinding): WebElement = {
    val elementName = elementBinding.element
    val locators = elementBinding.locators
    var result: Try[WebElement] = Failure(new NoSuchElementException(s"Could not locate $elementName by ${locators.mkString(" or ")}"))
    if (locators.size == 1) {
      val locator = locators.head
      try {
        findElementByLocator(elementName, locator) foreach { webElement =>
          result = Success(webElement)
        }
      } catch {
        case _: org.openqa.selenium.NoSuchElementException =>
      }
    } else {
      // multiple locators: return first one that resolves to an element
      withWebDriver { driver =>
        // override implicit wait for each locator to configured throttle time (or 200 msecs if that is zero)
        val wait = WebSettings.`gwen.web.throttle.msecs`
        driver.manage().timeouts().implicitlyWait(if (wait > 0) wait else 200, TimeUnit.MILLISECONDS)
        try {
          // keep trying all locators until one of them resolves or timeout happens
          try {
            waitUntil(s"locating element by ${locators.mkString(" or ")}") {
              val iter = locators.iterator.flatMap(loc => Try(findElementByLocator(elementName, loc)).getOrElse(None))
              if (iter.hasNext) result = Success(iter.next)
              result.isSuccess
            }
          } catch {
            case _: WaitTimeoutException =>
          }
        } finally {
          // restore implicit waits
          driver.manage().timeouts().implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)
        }
      }
    }
    result match {
      case Success(webElement) => webElement
      case Failure(e) => throw e
    }
  }

  /**
    * Locates a collection of web elements.
    *
    * @param elementBinding the web element collection locator binding
    * @return a list of elements or an empty list if none found
    */
  private[web] def locateAll(elementBinding: LocatorBinding): List[WebElement] = {
    val elementName = elementBinding.element
    logger.debug(s"Locating all $elementName")
    findAllElementsByLocator(elementName, elementBinding.locators.head)
  }

  /** Finds an element by the given locator expression. */
  private def findElementByLocator(elementName: String, locator: Locator): Option[WebElement] = {
    locator.index match {
      case Some(index) if index > 0 =>
        val elements = findAllElementsByLocator(elementName, locator)
        if (index < elements.size) {
          Option(elements(index))
        } else {
          noSuchElementError(s"No such element at index $index for element named '$elementName', locator: $locator")
        }
      case _ =>
        val locatorType = locator.locatorType
        val expression = locator.expression
        logger.debug(s"Locating $elementName by $locator")
        try {
          // override implicit wait for locator if overridden
          locator.timeout foreach { _ =>
            val wait = locator.timeoutMilliseconds
            withWebDriver { driver =>
              driver.manage().timeouts().implicitlyWait(if (wait > 0) wait else 200, TimeUnit.MILLISECONDS)
            }
          }
          (locatorType match {
            case "id" => getElement(By.id(expression), locator)
            case "name" => getElement(By.name(expression), locator)
            case "tag name" => getElement(By.tagName(expression), locator)
            case "css selector" => getElement(By.cssSelector(expression), locator)
            case "xpath" => getElement(By.xpath(expression), locator)
            case "class name" => getElement(By.className(expression), locator)
            case "link text" => getElement(By.linkText(expression), locator)
            case "partial link text" => getElement(By.partialLinkText(expression), locator)
            case "javascript" => getElementByJavaScript(s"$expression", locator)
            case "cache" => webContext.getCachedWebElement(elementName)
            case _ => locatorBindingError(elementName, s"unsupported locator: $locator")
          }) tap { optWebElement =>
            optWebElement foreach { webElement =>
              if (!webElement.isDisplayed) {
                webContext.scrollIntoView(webElement, ScrollTo.top)
              }
              if (!locator.isContainer) {
                webContext.highlightElement(webElement)
              }
            }
          }
        } finally {
          // restore default implicit wait if overriden
          locator.timeout foreach { _ =>
            withWebDriver { driver =>
              driver.manage().timeouts().implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)
            }
          }
        }
    }
  }

  /**
    * Gets a web element using the given by locator.
    *
    * @param by the by locator
    * @param locator the locator binding
    */
  private def getElement(by: By, locator: Locator): Option[WebElement] =
    webContext.withWebDriver { driver =>
      val handle = driver.getWindowHandle
      try {
        locator.container.fold(driver.findElement(by)) { containerName =>
          val binding = webContext.getLocatorBinding(containerName)
          val containerBinding =
            LocatorBinding(
              binding.element,
              binding.locators.map(loc =>
                Locator(loc.locatorType, loc.expression, loc.container, isContainer = true, loc.timeout, loc.index))
              )
          getContainerElement(containerBinding) match {
            case Some(containerElem) =>
              containerElem.findElement(by)
            case _ =>
              driver.findElement(by)
          }
        }
      } catch {
        case e: Throwable =>
          driver.switchTo().window(handle)
          throw e
      }
    }

  /**
    * Gets container web element using the given by locator.
    *
    * @param containerBinding the container binding
    */
  private def getContainerElement(containerBinding: LocatorBinding): Option[WebElement] = {
    val container = locate(containerBinding)
    container.getTagName match {
      case "iframe" | "frame" =>
        webContext.withWebDriver(_.switchTo().frame(container))
        None
      case _ =>
        Some(container)
    }
  }

  /**
    * Gets a web element by the given javascript expression. If the web element is not
    * visible in the browser, then the element is brought into view by scrolling to it.
    *
    * @param javascript the javascript expression for returning the element
    * @param locator the locator binding
    *
    */
  private def getElementByJavaScript(javascript: String, locator: Locator): Option[WebElement] = {
    var element: Option[WebElement] = None
    webContext.waitUntil(s"locating element by javascript: $javascript", locator.timeoutSeconds) {
      val result = locator.container.fold(webContext.executeJS(s"return $javascript")) { containerName =>
        getContainerElement(webContext.getLocatorBinding(containerName)) match {
            case Some(containerElem) =>
              webContext.executeJS(s"return (function(containerElem) { return $javascript })(arguments[0])", containerElem)
            case _ =>
              webContext.executeJS(s"return $javascript")
          }
      }
      element = result match {
        case elems: util.ArrayList[_] =>
          if (!elems.isEmpty) Option(elems.get(0).asInstanceOf[WebElement])
          else None
        case elem => Option(elem) match {
          case Some(e) => Option(e.asInstanceOf[WebElement])
          case None => None
        }
      }
      element.isDefined
    }
    element
  }

  /** Finds all elements bound by the given locator expression. */
  private def findAllElementsByLocator(elementName: String, locator: Locator): List[WebElement] = {
    val expression = locator.expression
    val locatorType = locator.locatorType
    try {
      // override implicit wait for locator if overridden
      locator.timeout foreach { _ =>
        val wait = locator.timeoutMilliseconds
        withWebDriver { driver =>
          driver.manage().timeouts().implicitlyWait(if (wait > 0) wait else 200, TimeUnit.MILLISECONDS)
        }
      }
      (locatorType match {
        case "id" => getAllElements(By.id(expression), locator)
        case "name" => getAllElements(By.name(expression), locator)
        case "tag name" => getAllElements(By.tagName(expression), locator)
        case "css selector" => getAllElements(By.cssSelector(expression), locator)
        case "xpath" => getAllElements(By.xpath(expression), locator)
        case "class name" => getAllElements(By.className(expression), locator)
        case "link text" => getAllElements(By.linkText(expression), locator)
        case "partial link text" => getAllElements(By.partialLinkText(expression), locator)
        case "javascript" => getAllElementsByJavaScript(s"$expression", locator)
        case _ => locatorBindingError(elementName, s"unsupported locator: $locator")
      }) tap { webElements =>
        webElements.headOption foreach { webElement =>
          if (!webElement.isDisplayed) {
            webContext.scrollIntoView(webElement, ScrollTo.top)
          }
          if (!locator.isContainer) {
            webContext.highlightElement(webElement)
          }
        }
      }
    } finally {
      // restore default implicit wait if overriden
      locator.timeout foreach { _ =>
        withWebDriver { driver =>
          driver.manage().timeouts().implicitlyWait(WebSettings.`gwen.web.locator.wait.seconds`, TimeUnit.SECONDS)
        }
      }
    }
  }

  /**
    * Gets all web elements using the given by locator.
    *
    * @param by the by locator
    */
  private def getAllElements(by: By, locator: Locator): List[WebElement] =
    webContext.withWebDriver { driver =>
      val handle = driver.getWindowHandle
      try {
        Option(locator.container.fold(driver.findElements(by)) { containerName =>
          getContainerElement(webContext.getLocatorBinding(containerName)) match {
            case Some(containerElem) => containerElem.findElements(by)
            case _ => driver.findElements(by)
          }
        }).map(_.asScala.toList).getOrElse(Nil)
      } catch {
        case e: Throwable =>
          driver.switchTo().window(handle)
          throw e
      }
    } getOrElse Nil

  /**
    * Gets all web elements by the given javascript expression. If the first web element is not
    * visible in the browser, then the element is brought into view by scrolling to it.
    *
    * @param javascript the javascript expression for returning the element
    */
  private def getAllElementsByJavaScript(javascript: String, locator: Locator): List[WebElement] = {
    var elements: List[WebElement] = Nil
    webContext.waitUntil(s"locating elements by javascript: $javascript", locator.timeoutSeconds) {
      elements = webContext.executeJS(s"return $javascript") match {
        case elems: util.ArrayList[_] =>
          elems.asScala.toList.map(_.asInstanceOf[WebElement])
        case elem => Option(elem) match {
          case Some(e) => List(e.asInstanceOf[WebElement])
          case None => Nil
        }
      }
      elements.nonEmpty
    }
    elements
  }

}

/**
  *  Captures a web element locator binding. A binding can have one or more locators.
  *
  *  @param element the web element name
  *  @param locators the locators
  */
case class LocatorBinding(element: String, locators: List[Locator]) {
  /** Gets the javascript equivalent of this locator binding (used as fallback on stale element reference). */
  def jsEquivalent: LocatorBinding = {
    val jsLocators = locators.map { loc =>
      val isListLocator = element.endsWith("/list")
      val jsExpression = loc.locatorType match {
        case "id" =>
          if (!isListLocator) s"document.getElementById('${loc.expression}')"
          else s"document.querySelectorAll('#${loc.expression}')"
        case "css selector" =>
          s"document.querySelector${if (isListLocator) "All" else ""}('${StringEscapeUtils.escapeEcmaScript(loc.expression)}')"
        case "xpath" =>
          s"document.evaluate('${StringEscapeUtils.escapeEcmaScript(loc.expression)}', document, null, XPathResult.${if (isListLocator) "ORDERED_NODE_ITERATOR_TYPE" else "FIRST_ORDERED_NODE_TYPE"}, null)${if (isListLocator) "" else ".singleNodeValue"}"
        case "name" =>
          s"document.getElementsByName('${loc.expression}')${if (isListLocator) "" else "[0]"}"
        case "class name" =>
          s"document.getElementsByClassName('${loc.expression}')${if (isListLocator) "" else "[0]"}"
        case "tag name" =>
          s"document.getElementsByTagName('${loc.expression}')${if (isListLocator) "" else "[0]"}"
        case "link text" =>
          s"""document.evaluate('//a[text()="${StringEscapeUtils.escapeEcmaScript(loc.expression)}"]', document, null, XPathResult.${if (isListLocator) "ORDERED_NODE_ITERATOR_TYPE" else "FIRST_ORDERED_NODE_TYPE"}, null)${if (isListLocator) "" else ".singleNodeValue"}"""
        case "partial link text" =>
          s"""document.evaluate('//a[contains(text(), "${StringEscapeUtils.escapeEcmaScript(loc.expression)}")]', document, null, XPathResult.${if (isListLocator) "ORDERED_NODE_ITERATOR_TYPE" else "FIRST_ORDERED_NODE_TYPE"}, null)${if (isListLocator) "" else ".singleNodeValue"}"""
        case _ => loc.expression
      }
      Locator("javascript", jsExpression, loc.container, loc.timeout, loc.index)
    }
    LocatorBinding(element, jsLocators)
  }
}

/** Locator binding factory companion. */
object LocatorBinding {
  def apply(element: String, locatorType: String, expression: String, container: Option[String], timeout: Option[Duration], index: Option[Int]): LocatorBinding =
    LocatorBinding(element, List(Locator(locatorType, expression, container, timeout, index)))
  def apply(element: String, locatorType: String, expression: String, container: Option[String], index: Option[Int]): LocatorBinding =
    LocatorBinding(element, List(Locator(locatorType, expression, container, None, index)))
  def apply(binding: LocatorBinding, locator: Locator): LocatorBinding =
    LocatorBinding(binding.element, List(locator))
}

/**
  * Captures a web locator.
  * @param locatorType the locator type
  * @param expression the locator expression
  * @param container optional parent container name
  * @param isContainer true if this is a locaotr for a container element, false otherwise
  * @param timeout optional timeout (defaults to `gwen.web.locator.wait.seconds` if not provided)
  * @param index optional index (if locator returns more than one element then index is required)
  */
case class Locator(locatorType: String, expression: String, container: Option[String], isContainer: Boolean, timeout: Option[Duration], index: Option[Int]) {
  override def toString: String =
    s"($locatorType: $expression)${container.map(c => s" in $c").getOrElse("")}${index.map(i => s" at index $i").getOrElse("")}"
  lazy val timeoutSeconds = timeout.map(_.toSeconds).getOrElse(WebSettings.`gwen.web.locator.wait.seconds`)
  lazy val timeoutMilliseconds = timeoutSeconds * 1000
}

/** Locator factory companion. */
object Locator {
  def apply(locatorType: String, expression: String, container: Option[String], timeout: Option[Duration], index: Option[Int]): Locator =
    Locator(locatorType, expression, container, isContainer = false, timeout, index)
  def apply(locatorType: String, expression: String): Locator =
    Locator(locatorType, expression, None, isContainer = false, None, None)
  def apply(locator: Locator, timeout: Option[Duration], index: Option[Int]): Locator =
    Locator(locator.locatorType, locator.expression, locator.container, timeout, index)
}
