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
import org.openqa.selenium.WebElement
import org.openqa.selenium.JavascriptExecutor

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
	 *  		the web environment context
	 *  @param element
	 *  		the name of the element to locate
	 *  @return 
	 *  		the found element (errors if not found)
	 */
	def locate(env: WebEnvContext, element: String): WebElement = locateOpt(env, element) match {
	  case Some(webElement) => webElement
	  case None => throw new NoSuchElementException(s"Web element not found: ${element}")
	}
	
	/**
	 * Locates a bound web element.
	 * 
	 * @param env
	 *  		the web environment context
	 * @param element
	 *  		the name of the element to locate
	 * @return 
	 * 			Some(element) if found, None otherwise
	 */
	def locateOpt(env: WebEnvContext, element: String): Option[WebElement] = {
	  val locatorBindingName = s"$element/locator";
	  env.pageScopes.getOpt(locatorBindingName) match {
	    case Some(locator) =>
	      val locatorName = s"$element/locator/$locator"
	      env.pageScopes.getOpt(locatorName) match {
            case Some(locatorValue) => 
              locator match {
                case "id" => getElement(env, By.id(locatorValue))
                case "name" => getElement(env, By.name(locatorValue))
                case "tag name" => getElement(env, By.tagName(locatorValue))
                case "css selector" => getElement(env, By.cssSelector(locatorValue))
                case "xpath" => getElement(env, By.xpath(locatorValue))
                case "class name" => getElement(env, By.className(locatorValue))
                case "link text" => getElement(env, By.linkText(locatorValue))
                case "partial link text" => getElement(env, By.partialLinkText(locatorValue))
      	        case "javascript" => getElementByJavaScript(env, element, s"$locatorValue")
                case _ => throw new LocatorBindingException(element, s"unsupported locator: ${locator}")
              }
            case None => throw new LocatorBindingException(element, s"locator not bound: ${locatorName}")
          }
	    case None => throw new LocatorBindingException(element, s"locator binding not found: ${locatorBindingName}")
	  }
	}
	
	private def getElement(env: WebEnvContext, by: By): Option[WebElement] = Option(env.webDriver.findElement(by))
	  
	private def getElementByJavaScript(env: WebEnvContext, element: String, javascript: String): Option[WebElement] = 
	  env.webDriver.asInstanceOf[JavascriptExecutor].executeScript(javascript) match {
	    case elem @ _ :: _ => Some(elem.asInstanceOf[WebElement])
	    case elem => Option(elem) match {
	      case Some(elem) => Some(elem.asInstanceOf[WebElement])
	      case None => None
	    }
	  }
}

class LocatorBindingException(element: String, causeMsg: String) extends Exception(s"Could not locate web element: ${element}, ${causeMsg}")