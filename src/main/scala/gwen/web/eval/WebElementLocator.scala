/*
 * Copyright 2014-2022 Branko Juric, Brady Wood
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
import gwen.core.Errors.WaitTimeoutException

import com.typesafe.scalalogging.LazyLogging
import org.openqa.selenium.{By, NoSuchElementException, WebElement}
import org.openqa.selenium.support.locators.RelativeLocator

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
          if (selector.hasTimeoutOverride) {
            val wait = selector.timeoutMilliseconds
            ctx.withWebDriver { driver =>
              driver.manage().timeouts().implicitlyWait(jt.Duration.ofMillis(if (wait > 0) wait else 200))
            }
          }
          selectorType match {
            case SelectorType.javascript =>
              try {
                getElementByJavaScript(s"$expression", selector)
              } catch {
                case e: ScriptException =>
                  elementNotFoundError(new LocatorBinding(name, List(selector), ctx))
              }
            case SelectorType.cache => ctx.getCachedWebElement(name)
            case _ => getElement(selector)

          }
        } finally {
          // restore default implicit wait if overriden
          if (selector.hasTimeoutOverride) {
            ctx.withWebDriver { driver =>
              driver.manage().timeouts().implicitlyWait(jt.Duration.ofSeconds(Selector.DefaultTimeout.toSeconds))
            }
          }
        }
    }
  }

  private def getBySelector(selector: Selector): By = {
    val expression = selector.expression
    selector.selectorType match {
      case SelectorType.id => By.id(expression)
      case SelectorType.name => By.name(expression)
      case SelectorType.`tag name` => By.tagName(expression)
      case SelectorType.`css selector` => By.cssSelector(expression)
      case SelectorType.xpath => By.xpath(expression)
      case SelectorType.`class name` => By.className(expression)
      case SelectorType.`link text` => By.linkText(expression)
      case SelectorType.`partial link text` => By.partialLinkText(expression)
      case _ => locatorBindingError(s"No such selector: $selector")
    }
  }

  /**
    * Gets a web element using the given by selector.
    *
    * @param selector the selector
    */
  private def getElement(selector: Selector): Option[WebElement] = {
    ctx.withWebDriver { driver =>
      val handle = driver.getWindowHandle
      try {
        val by = getBySelector(selector)
        selector.relative match {
          case None =>
            driver.findElement(by)
          case Some((rSelector, rBinding, rAtMostPixels)) =>
            rSelector match {
              case RelativeSelectorType.in =>
                getContainerElement(rBinding) match {
                  case Some(containerElem) =>
                    containerElem.findElement(by)
                  case _ =>
                    driver.findElement(by)
                }
              case RelativeSelectorType.above =>
                driver.findElement(RelativeLocator.`with`(getBySelector(selector)).above(locate(rBinding)))
              case RelativeSelectorType.below =>
                driver.findElement(RelativeLocator.`with`(getBySelector(selector)).below(locate(rBinding)))
              case RelativeSelectorType.near =>
                rAtMostPixels match {
                  case None =>
                    driver.findElement(RelativeLocator.`with`(getBySelector(selector)).near(locate(rBinding)))
                  case Some(pixels) => 
                    driver.findElement(RelativeLocator.`with`(getBySelector(selector)).near(locate(rBinding), pixels))
                }
              case RelativeSelectorType.`to left of` =>
                driver.findElement(RelativeLocator.`with`(getBySelector(selector)).toLeftOf(locate(rBinding)))
              case RelativeSelectorType.`to right of` =>
                driver.findElement(RelativeLocator.`with`(getBySelector(selector)).toRightOf(locate(rBinding)))
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
    val result = selector.relative match {
      case None =>
        ctx.executeJS(s"return $javascript")
      case Some((_, rBinding, _)) =>
        getContainerElement(rBinding) match {
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
      if (selector.hasTimeoutOverride) {
        val wait = selector.timeoutMilliseconds
        ctx.withWebDriver { driver =>
          driver.manage().timeouts().implicitlyWait(jt.Duration.ofMillis(if (wait > 0) wait else 200))
        }
      }
      selectorType match {
        case SelectorType.javascript =>
          try {
            getAllElementsByJavaScript(s"$expression", selector)
          } catch {
            case e: WaitTimeoutException => Nil
          }
        case SelectorType.cache => locatorBindingError(s"Unsupported selector defined for: $name")
        case _ => getAllElements(selector)

      }
    } finally {
      // restore default implicit wait if overriden
      if (selector.hasTimeoutOverride) {
        ctx.withWebDriver { driver =>
          driver.manage().timeouts().implicitlyWait(jt.Duration.ofSeconds(Selector.DefaultTimeout.toSeconds))
        }
      }
    }
  }

  /**
    * Gets all web elements using the given by selector.
    *
    * @param selector the selector
    */
  private def getAllElements(selector: Selector): List[WebElement] = {
    ctx.withWebDriver { driver =>
      val handle = driver.getWindowHandle
      try {
        val by = getBySelector(selector)
        val elems = selector.relative match {
          case None => 
            driver.findElements(by)
          case Some((_, rBinding, _)) => 
            getContainerElement(rBinding) match {
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
  }

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
