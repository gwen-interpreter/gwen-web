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

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import gwen.Predefs.Kestrel
import gwen.gwenSetting

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
  def locate(env: WebEnvContext, element: String): WebElement = locateOpt(env, element) match {
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
  def locateOpt(env: WebEnvContext, element: String): Option[WebElement] = {
    val locatorBinding = s"$element/locator";
    env.pageScopes.getOpt(locatorBinding) match {
      case Some(locator) =>
        val expressionBinding = s"$element/locator/$locator"
        env.pageScopes.getOpt(expressionBinding) match {
            case Some(expression) => 
              locator match {
                case "id" => getElement(env, By.id(expression))
                case "name" => getElement(env, By.name(expression))
                case "tag name" => getElement(env, By.tagName(expression))
                case "css selector" => getElement(env, By.cssSelector(expression))
                case "xpath" => getElement(env, By.xpath(expression))
                case "class name" => getElement(env, By.className(expression))
                case "link text" => getElement(env, By.linkText(expression))
                case "partial link text" => getElement(env, By.partialLinkText(expression))
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
  private def getElement(env: WebEnvContext, by: By): Option[WebElement] = Option(env.webDriver.findElement(by)) tap {
    moveToIfNotDisplayed(env, _)
  }
    
  /**
   * Gets a web element by the given javascript expression. If the web element is not 
   * visible in the browser, then the element is brought into view by scrolling to it.
   * 
   * @param env the web environment context
   * @param javascipt the javascript expression for returning the element
   */
  private def getElementByJavaScript(env: WebEnvContext, element: String, javascript: String): Option[WebElement] =
    (env.webDriver.asInstanceOf[JavascriptExecutor].executeScript(javascript) match {
      case elem @ _ :: _ => Some(elem.asInstanceOf[WebElement])
      case elem => Option(elem) match {
        case Some(elem) => Some(elem.asInstanceOf[WebElement])
        case None => None
      }
    }) tap {
      moveToIfNotDisplayed(env, _)
    }
  
  /**
   * Physically scrolls to the given web element so it is visible in the browser.
   * 
   * @param env the web environment context
   * @param webElement the webElement to scroll to (if not None)
   */
  private def moveToIfNotDisplayed(env: WebEnvContext, webElement: Option[WebElement]) = webElement foreach { element =>
      if (!element.isDisplayed()) {
        env.webDriver.asInstanceOf[JavascriptExecutor].executeScript("arguments[0].scrollIntoView(true);")
      }
  }
  
}

/**
 * Thrown when a web element cannot be located
 */
class LocatorBindingException(element: String, causeMsg: String) extends Exception(s"Could not locate web element: ${element}, ${causeMsg}")
