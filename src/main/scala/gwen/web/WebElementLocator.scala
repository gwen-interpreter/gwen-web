/*
 * Copyright 2014-2015 Branko Juric, Brady Wood
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

import java.util.ArrayList

import org.openqa.selenium.By
import org.openqa.selenium.WebDriverException
import org.openqa.selenium.WebElement

import com.typesafe.scalalogging.slf4j.LazyLogging

import gwen.Predefs.Kestrel

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
    *  @param element the name of the element to locate
    *  @return the found element (errors if not found)
    */
  def locate(env: WebEnvContext, element: String): WebElement = 
    locateElement(env, element) match {
      case Some(webElement) => webElement
      case None => throw new NoSuchElementException(s"Web element not found: ${element}")
    }
  
  /**
   * Locates a bound web element.
   * 
   * @param env the web environment context
   * @param element the name of the element to locate
   * @return Some(element) if found, None otherwise
   */
  def locateOpt(env: WebEnvContext, element: String): Option[WebElement] = try {
    locateElement(env, element)
  } catch {
    case e: TimeoutOnWaitException => None
  }
  
  /**
   * Locates a bound web element.
   * 
   * @param env the web environment context
   * @param element the name of the element to locate
   * @return Some(element) if found, None otherwise
   */
  private def locateElement(env: WebEnvContext, element: String): Option[WebElement] = {
    val locatorBinding = s"$element/locator";
    env.scopes.getOpt(locatorBinding) match {
      case Some(locator) =>
        val expressionBinding = env.interpolate(s"$element/locator/$locator")(env.getBoundValue)
        env.scopes.getOpt(expressionBinding) match {
            case Some(expression) =>
              val expr = env.interpolate(expression)(env.getBoundValue)
              logger.info(s"Locating $element")
              try {
                findElementByLocator(env, element, locator, expr)
              } catch {
                case e: WebDriverException =>
                  // attempt to locate one more time on web driver exception
                  findElementByLocator(env, element, locator, expr)
              }
            case None => throw new LocatorBindingException(element, s"locator expression binding not bound: ${expressionBinding}")
          }
      case None => throw new LocatorBindingException(element, s"locator type binding not found: ${locatorBinding}")
    }
  }
  
  /** Finds an element by the given locator expression. */
  private def findElementByLocator(env: WebEnvContext, element: String, locator: String, expression: String): Option[WebElement] = {
    (locator match {
      case "id" => getElement(env, element, By.id(expression))
      case "name" => getElement(env, element, By.name(expression))
      case "tag name" => getElement(env, element, By.tagName(expression))
      case "css selector" => getElement(env, element, By.cssSelector(expression))
      case "xpath" => getElement(env, element, By.xpath(expression))
      case "class name" => getElement(env, element, By.className(expression))
      case "link text" => getElement(env, element, By.linkText(expression))
      case "partial link text" => getElement(env, element, By.partialLinkText(expression))
      case "javascript" => getElementByJavaScript(env, element, s"$expression")
      case _ => throw new LocatorBindingException(element, s"unsupported locator: ${locator}")
    }) tap { optWebElement =>
      optWebElement foreach { webElement =>
        if (!webElement.isDisplayed()) {
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
  private def getElement(env: WebEnvContext, element: String, by: By): Option[WebElement] = 
    Option(env.withWebDriver({ _.findElement(by) })(false))
    
  /**
    * Gets a web element by the given javascript expression. If the web element is not 
    * visible in the browser, then the element is brought into view by scrolling to it.
    * 
    * @param env the web environment context
    * @param element the element 
    * @param javascipt the javascript expression for returning the element
    */
  private def getElementByJavaScript(env: WebEnvContext, element: String, javascript: String): Option[WebElement] = {
    var elem: Option[WebElement] = None
    env.waitUntil {
      elem = env.executeScript(s"return $javascript") match {
        case elems: ArrayList[_] => 
          if (!elems.isEmpty()) Option(elems.get(0).asInstanceOf[WebElement])
          else None
        case elem => Option(elem) match {
          case Some(elem) => Option(elem.asInstanceOf[WebElement])
          case None => None
        }
      }
      elem.isDefined
    }
    elem
  }
  
}

/** Thrown when a web element cannot be located. */
class LocatorBindingException(element: String, causeMsg: String) extends Exception(s"Could not locate ${element}: ${causeMsg}")

