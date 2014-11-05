/*
 * Copyright 2014 Branko Juric, Brady Wood
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
import org.openqa.selenium.WebElement

import gwen.Predefs.Kestrel


/**
 * Locates web elements using the selenium web driver.
 * 
 * @author Branko Juric, Brady Wood
 */
trait WebElementLocator {
  
  /**
   * Locates a bound web element.
   * 
   *  @param env
   *      the web environment context
   *  @param element
   *      the name of the element to locate
   *  @return 
   *      the found element (errors if not found)
   */
  def locate(env: WebEnvContext, element: String): WebElement = locateElement(env, element) match {
    case Some(webElement) => webElement
    case None => throw new NoSuchElementException(s"Web element not found: ${element}")
  }
  
  /**
   * Locates a bound web element.
   * 
   * @param env
   *      the web environment context
   * @param element
   *      the name of the element to locate
   * @return 
   *       Some(element) if found, None otherwise
   */
  def locateOpt(env: WebEnvContext, element: String): Option[WebElement] = try {
    locateElement(env, element)
  } catch {
    case e: TimeoutOnWaitException => None
  }
  
  /**
   * Locates a bound web element.
   * 
   * @param env
   *      the web environment context
   * @param element
   *      the name of the element to locate
   * @return 
   *       Some(element) if found, None otherwise
   */
  private def locateElement(env: WebEnvContext, element: String): Option[WebElement] = {
    val locatorBinding = s"$element/locator";
    env.pageScopes.getOpt(locatorBinding) match {
      case Some(locator) =>
        val expressionBinding = s"$element/locator/$locator"
        env.pageScopes.getOpt(expressionBinding) match {
            case Some(expression) => 
              locator match {
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
              }
            case None => throw new LocatorBindingException(element, s"locator expression binding not bound: ${expressionBinding}")
          }
      case None => throw new LocatorBindingException(element, s"locator type binding not found: ${locatorBinding}")
    }
  }
  
  /**
   * Gets a web element using the given by locator.
   * 
   * @param env the web environment context
   * @param by the by locator
   */
  private def getElement(env: WebEnvContext, element: String, by: By): Option[WebElement] = Option(env.webDriver.findElement(by)) map {
    moveTo(env, element, _)
  }
    
  /**
   * Gets a web element by the given javascript expression. If the web element is not 
   * visible in the browser, then the element is brought into view by scrolling to it.
   * 
   * @param env the web environment context
   * @param javascipt the javascript expression for returning the element
   */
  private def getElementByJavaScript(env: WebEnvContext, element: String, javascript: String): Option[WebElement] = {
    var elem: Option[WebElement] = None
  	env.waitUntil(s"Locating $element") {
      elem = (env.executeScript(s"return $javascript") match {
        case elems: ArrayList[_] => 
          if (!elems.isEmpty()) Option(elems.get(0).asInstanceOf[WebElement])
          else None
        case elem => Option(elem) match {
          case Some(elem) => Option(elem.asInstanceOf[WebElement])
          case None => None
        }
      }) map { webElement =>
        moveTo(env, element, webElement)
      }
      elem.isDefined
	}
  	elem
  }
  
  /**
   * Physically scrolls to the given web element so it is visible in the browser.
   * 
   * @param env the web environment context
   * @param webElement the webElement to scroll to (if not None)
   */
  private def moveTo(env: WebEnvContext, element: String, webElement: WebElement): WebElement = {
    if (!webElement.isDisplayed()) {
      env.waitUntil(s"Moving to $element") {
        env.executeScript("var elem = arguments[0]; if (typeof elem !== 'undefined' && elem != null) { elem.scrollIntoView(true); return true; } else return false;").asInstanceOf[Boolean]
      }
    }
    webElement tap { env.highlight(_) } 
  }
  
}

/**
 * Thrown when a web element cannot be located
 */
class LocatorBindingException(element: String, causeMsg: String) extends Exception(s"Could not locate ${element}: ${causeMsg}")

