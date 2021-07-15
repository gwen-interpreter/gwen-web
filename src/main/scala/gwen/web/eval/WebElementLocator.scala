/*
 * Copyright 2014-2021 Branko Juric, Brady Wood
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

import WebErrors._
import gwen.web.eval.binding._

import gwen.core.Errors.ScriptException

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.{By, NoSuchElementException, WebElement}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

import java.{time => jt}
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
  * Locates web elements using the selenium web driver.
  *
  * @author Branko Juric, Brady Wood
  */
class WebElementLocator(ctx: WebContext) extends LazyLogging {

  /**
    * Locates a bound web element.
    *
    *  @param binding the web element binding
    *  @return the found element (or an error if not found)
    */
  def locate(binding: LocatorBinding): WebElement = {
    val name = binding.name
    val selectors = binding.selectors
    var result: Try[WebElement] = Try(elementNotFoundError(binding))
    if (selectors.size == 1) {
      val selector = selectors.head
      try {
        findElementBySelector(name, selector) foreach { webElement =>
          result = Success(webElement)
        }
      } catch {
        case e @ (_ :  NoSuchElementException | _ : NotFoundOrInteractableException) =>
          result = Try(elementNotFoundError(binding, e))
      }
    } else {
      // multiple selectors: return first one that resolves to an element
      ctx.withWebDriver { driver =>
        // override implicit wait for each selector to configured throttle time (or 200 msecs if that is zero)
        val wait = WebSettings.`gwen.web.throttle.msecs`
        driver.manage().timeouts().implicitlyWait(jt.Duration.ofMillis(if (wait > 0) wait else 200))
        try {
          // return the first one that resolves
          val iter = selectors.iterator.flatMap(loc => Try(findElementBySelector(name, loc)).getOrElse(None))
          if (iter.hasNext) {
            result = Success(iter.next())
          }
        } finally {
          // restore implicit waits
          driver.manage().timeouts().implicitlyWait(jt.Duration.ofSeconds(WebSettings.`gwen.web.locator.wait.seconds`))
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
    * @param binding the web element collection binding
    * @return a list of elements or an empty list if none found
    */
  def locateAll(binding: LocatorBinding): List[WebElement] = {
    val name = binding.name
    logger.debug(s"Locating all $name")
    findAllElementsBySelector(name, binding.selectors.head)
  }

  /** Finds an element by the given name and selector. */
  private def findElementBySelector(name: String, selector: Selector): Option[WebElement] = {
    selector.index match {
      case Some(index) if index > 0 =>
        val elements = findAllElementsBySelector(name, selector)
        if (index < elements.size) {
          Option(elements(index))
        } else {
          elementNotFoundError(new LocatorBinding(name, List(selector), ctx))
        }
      case _ =>
        val selectorType = selector.selectorType
        val expression = selector.expression
        logger.debug(s"Locating $name by $selector")
        try {
          // override implicit wait for selector if overridden
          selector.timeout foreach { _ =>
            val wait = selector.timeoutMilliseconds
            ctx.withWebDriver { driver =>
              driver.manage().timeouts().implicitlyWait(jt.Duration.ofMillis(if (wait > 0) wait else 200))
            }
          }
          selectorType match {
            case SelectorType.id => getElement(By.id(expression), selector)
            case SelectorType.name => getElement(By.name(expression), selector)
            case SelectorType.`tag name` => getElement(By.tagName(expression), selector)
            case SelectorType.`css selector` => getElement(By.cssSelector(expression), selector)
            case SelectorType.xpath => getElement(By.xpath(expression), selector)
            case SelectorType.`class name` => getElement(By.className(expression), selector)
            case SelectorType.`link text` => getElement(By.linkText(expression), selector)
            case SelectorType.`partial link text` => getElement(By.partialLinkText(expression), selector)
            case SelectorType.javascript =>
              try {
                getElementByJavaScript(s"$expression", selector)
              } catch {
                case e: ScriptException =>
                  elementNotFoundError(new LocatorBinding(name, List(selector), ctx))
              }
            case SelectorType.cache => ctx.getCachedWebElement(name)
          }
        } finally {
          // restore default implicit wait if overriden
          selector.timeout foreach { _ =>
            ctx.withWebDriver { driver =>
              driver.manage().timeouts().implicitlyWait(jt.Duration.ofSeconds(WebSettings.`gwen.web.locator.wait.seconds`))
            }
          }
        }
    }
  }

  /**
    * Gets a web element using the given by selector.
    *
    * @param by the by locator
    * @param selector the selector
    */
  private def getElement(by: By, selector: Selector): Option[WebElement] =
    ctx.withWebDriver { driver =>
      val handle = driver.getWindowHandle
      try {
        selector.container.fold(driver.findElement(by)) { containerBinding =>
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
    * Gets container web element using the given binding.
    *
    * @param binding the container binding
    */
  private def getContainerElement(binding: LocatorBinding): Option[WebElement] = {
    val container = locate(binding)
    container.getTagName match {
      case "iframe" | "frame" =>
        ctx.withWebDriver(_.switchTo().frame(container))
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
    * @param selector the selector
    *
    */
  private def getElementByJavaScript(javascript: String, selector: Selector): Option[WebElement] = {
    val result = selector.container.fold(ctx.executeJS(s"return $javascript")) { containerBinding =>
      getContainerElement(containerBinding) match {
          case Some(containerElem) =>
            ctx.executeJS(s"return (function(containerElem) { return $javascript })(arguments[0])", containerElem)
          case _ =>
            ctx.executeJS(s"return $javascript")
        }
    }
    result match {
      case elems: ArrayList[_] =>
        if (!elems.isEmpty) Option(elems.get(0).asInstanceOf[WebElement])
        else None
      case elem => Option(elem) match {
        case Some(e) => Option(e.asInstanceOf[WebElement])
        case None => None
      }
    }
  }

  /** Finds all elements bound by the given name and selector. */
  private def findAllElementsBySelector(name: String, selector: Selector): List[WebElement] = {
    val expression = selector.expression
    val selectorType = selector.selectorType
    try {
      // override implicit wait for selector if overridden
      selector.timeout foreach { _ =>
        val wait = selector.timeoutMilliseconds
        ctx.withWebDriver { driver =>
          driver.manage().timeouts().implicitlyWait(jt.Duration.ofMillis(if (wait > 0) wait else 200))
        }
      }
      selectorType match {
        case SelectorType.id => getAllElements(By.id(expression), selector)
        case SelectorType.name => getAllElements(By.name(expression), selector)
        case SelectorType.`tag name` => getAllElements(By.tagName(expression), selector)
        case SelectorType.`css selector` => getAllElements(By.cssSelector(expression), selector)
        case SelectorType.xpath => getAllElements(By.xpath(expression), selector)
        case SelectorType.`class name` => getAllElements(By.className(expression), selector)
        case SelectorType.`link text` => getAllElements(By.linkText(expression), selector)
        case SelectorType.`partial link text` => getAllElements(By.partialLinkText(expression), selector)
        case SelectorType.javascript =>
          try {
            getAllElementsByJavaScript(s"$expression", selector)
          } catch {
            case e: ScriptException =>
              elementNotFoundError(new LocatorBinding(name, List(selector), ctx), e)
          }
        case _ => locatorBindingError(s"Unsupported locator defined for $name: $selector")
      }
    } finally {
      // restore default implicit wait if overriden
      selector.timeout foreach { _ =>
        ctx.withWebDriver { driver =>
          driver.manage().timeouts().implicitlyWait(jt.Duration.ofSeconds(WebSettings.`gwen.web.locator.wait.seconds`))
        }
      }
    }
  }

  /**
    * Gets all web elements using the given by selector.
    *
    * @param by the by locator
    */
  private def getAllElements(by: By, selector: Selector): List[WebElement] =
    ctx.withWebDriver { driver =>
      val handle = driver.getWindowHandle
      try {
        val elems = selector.container match {
          case None => 
            driver.findElements(by)
          case Some(binding) => 
            getContainerElement(binding) match {
              case Some(containerElem) => containerElem.findElements(by)
              case _ => driver.findElements(by)
            }
        }
        Option(elems).map(_.asScala.toList).getOrElse(Nil)
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
  private def getAllElementsByJavaScript(javascript: String, selector: Selector): List[WebElement] = {
    var elements: List[WebElement] = Nil
    ctx.waitUntil(selector.timeoutSeconds, s"trying to locate elements by javascript: $javascript") {
      elements = ctx.executeJS(s"return $javascript") match {
        case elems: ArrayList[_] =>
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
