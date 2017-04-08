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

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import gwen.Predefs.Kestrel
import com.typesafe.scalalogging.LazyLogging
import scala.collection.JavaConverters._

/**
  * Locates web elements using the selenium web driver.
  *
  * @author Branko Juric, Brady Wood
  */
trait WebElementLocator extends LazyLogging {

  /**
    * Locates a bound web element.
    *
    *  @param env the web environment context
    *  @param elementBinding the web element locator binding
    *  @return the found element (errors if not found)
    */
  private[web] def locate(env: WebEnvContext, elementBinding: LocatorBinding): WebElement = {
    logger.debug(s"Locating ${elementBinding.element}")
    findElementByLocator(env, elementBinding) match {
      case Some(webElement) => webElement
      case None => throw new NoSuchElementException(s"Web element not found: ${elementBinding.element}")
    }
  }

  /**
    * Locates a collection of web elements.
    *
    *  @param env the web environment context
    *  @param elementBinding the web element collection binding
    *  @return a list of elements or an empty list if none found
    */
  private[web] def locateAll(env: WebEnvContext, elementBinding: LocatorBinding): List[WebElement] = {
    logger.debug(s"Locating all ${elementBinding.element}")
    findAllElementsByLocator(env, elementBinding)
  }

  /** Finds an element by the given locator expression. */
  private def findElementByLocator(env: WebEnvContext, elementBinding: LocatorBinding): Option[WebElement] = {
    val lookup = elementBinding.lookup
    val locator = elementBinding.locator
    (locator match {
      case "id" => getElement(env, By.id(lookup), elementBinding)
      case "name" => getElement(env, By.name(lookup), elementBinding)
      case "tag name" => getElement(env, By.tagName(lookup), elementBinding)
      case "css selector" => getElement(env, By.cssSelector(lookup), elementBinding)
      case "xpath" => getElement(env, By.xpath(lookup), elementBinding)
      case "class name" => getElement(env, By.className(lookup), elementBinding)
      case "link text" => getElement(env, By.linkText(lookup), elementBinding)
      case "partial link text" => getElement(env, By.partialLinkText(lookup), elementBinding)
      case "javascript" => getElementByJavaScript(env, s"$lookup")
      case _ => throw new LocatorBindingException(elementBinding.element, s"unsupported locator: $locator")
    }) tap { optWebElement =>
      optWebElement foreach { webElement =>
        if (!webElement.isDisplayed) {
          env.scrollIntoView(webElement, ScrollTo.top)
        }
        env.highlight(webElement)
      }
    }
  }

  /**
    * Gets a web element using the given by locator.
    *
    * @param env the web environment context
    * @param by the by locator
    */
  private def getElement(env: WebEnvContext, by: By, elementBinding: LocatorBinding): Option[WebElement] = Option {
    env.withWebDriver { driver =>
      val handle = driver.getWindowHandle
      try {
        elementBinding.container.fold(driver.findElement(by)) { containerName =>
          getContainerElement(env, env.getLocatorBinding(containerName)) match {
            case Some(containerElem) => containerElem.findElement(by)
            case _ => driver.findElement(by)
          }
        }
      } catch {
        case e: Throwable =>
          driver.switchTo().window(handle)
          throw e
      }
    }
  }

  /**
    * Gets container web element using the given by locator.
    *
    * @param env the web environment context
    * @param containerBinding the container binding
    */
  private def getContainerElement(env: WebEnvContext, containerBinding: LocatorBinding): Option[WebElement] = {
    val container = findElementByLocator(env, containerBinding)
    container flatMap { containerElem =>
      containerElem.getTagName match {
        case "iframe" | "frame" =>
          env.withWebDriver(_.switchTo().frame(containerElem))
          None
        case _ =>
          container
      }
    }
  }

  /**
    * Gets a web element by the given javascript expression. If the web element is not
    * visible in the browser, then the element is brought into view by scrolling to it.
    *
    * @param env the web environment context
    * @param javascript the javascript expression for returning the element
    */
  private def getElementByJavaScript(env: WebEnvContext, javascript: String): Option[WebElement] = {
    var element: Option[WebElement] = None
    env.waitUntil {
      element = env.executeScript(s"return $javascript") match {
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
  private def findAllElementsByLocator(env: WebEnvContext, elementBinding: LocatorBinding): List[WebElement] = {
    val lookup = elementBinding.lookup
    val locator = elementBinding.locator
    (locator match {
      case "id" => getAllElements(env, By.id(lookup), elementBinding)
      case "name" => getAllElements(env, By.name(lookup), elementBinding)
      case "tag name" => getAllElements(env, By.tagName(lookup), elementBinding)
      case "css selector" => getAllElements(env, By.cssSelector(lookup), elementBinding)
      case "xpath" => getAllElements(env, By.xpath(lookup), elementBinding)
      case "class name" => getAllElements(env, By.className(lookup), elementBinding)
      case "link text" => getAllElements(env, By.linkText(lookup), elementBinding)
      case "partial link text" => getAllElements(env, By.partialLinkText(lookup), elementBinding)
      case "javascript" => getAllElementsByJavaScript(env, s"$lookup")
      case _ => throw new LocatorBindingException(elementBinding.element, s"unsupported locator: $locator")
    }) tap { webElements =>
      webElements.headOption foreach { webElement =>
        if (!webElement.isDisplayed) {
          env.scrollIntoView(webElement, ScrollTo.top)
        }
        env.highlight(webElement)
      }
    }
  }

  /**
    * Gets all web elements using the given by locator.
    *
    * @param env the web environment context
    * @param by the by locator
    */
  private def getAllElements(env: WebEnvContext, by: By, elementBinding: LocatorBinding): List[WebElement] = {
    env.withWebDriver { driver =>
      val handle = driver.getWindowHandle
      try {
        Option(elementBinding.container.fold(driver.findElements(by)) { containerName =>
          getContainerElement(env, env.getLocatorBinding(containerName)) match {
            case Some(containerElem) => containerElem.findElements(by)
            case _ => driver.findElements(by)
          }
        }).map(_.asScala.toList).getOrElse(Nil)
      } catch {
        case e: Throwable =>
          driver.switchTo().window(handle)
          throw e
      }
    }
  }

  /**
    * Gets all web elements by the given javascript expression. If the first web element is not
    * visible in the browser, then the element is brought into view by scrolling to it.
    *
    * @param env the web environment context
    * @param javascript the javascript expression for returning the element
    */
  private def getAllElementsByJavaScript(env: WebEnvContext, javascript: String): List[WebElement] = {
    var elements: List[WebElement] = Nil
    env.waitUntil {
      elements = env.executeScript(s"return $javascript") match {
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
  *  Captures a web element locator binding.
  *
  *  @param element the web element name
  *  @param locator the locator type
  *  @param lookup the lookup string
  *  @param container optional parent container name
  */
case class LocatorBinding(element: String, locator: String, lookup: String, container: Option[String])

/** Thrown when a web element cannot be located. */
class LocatorBindingException(element: String, causeMsg: String) extends RuntimeException(s"Could not locate $element: $causeMsg")