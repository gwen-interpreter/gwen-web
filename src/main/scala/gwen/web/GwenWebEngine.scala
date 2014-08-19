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
import org.openqa.selenium.support.ui.Select

import gwen.Predefs.Kestrel
import gwen.dsl.Step
import gwen.eval.EvalEngine
import gwen.eval.GwenOptions
import gwen.gwenSetting


/**
 * A web engine that uses the Selenium web driver
 * API to automate various web operations.
 * 
 * @author Branko Juric, Brady Wood
 */
trait GwenWebEngine extends EvalEngine[WebEnvContext] with WebElementLocator {
  
  /**
   * Initialises and returns a new web environment context.
   * 
   * @param options
   * 			command line options
   */
  override def init(options: GwenOptions) = 
    new WebEnvContext(
      gwenSetting.get("gwen.web.browser") tap { webdriver =>
        logger.info(s"$webdriver web driver configured")
	  }
    )
  
 
  
  /**
   * Evaluates a given step.  This method matches the incoming step against a 
   * set of supported steps and evaluates only those that are successfully 
   * matched.
   *
   * @param step
   * 			the step to evaluate
   * @param env
   * 			the web environment context
   * @return
   * 			`Try.Success(step)` if the evaluation is successful, or
   *            `Try.Failure(error)` otherwise
   */
  override def evaluate(step: Step, env: WebEnvContext) {
    
    step.expression match {
    
      case r"""I navigate to the "?(.+?)"?$pageScope page""" =>
        env.webDriver.get(env.pageScopes.getIn(pageScope, "navigation/url"))
        env.pageScopes.addScope(pageScope)
        
      case r"""I navigate to "?(.+?)"?$$$url""" =>
        env.pageScopes.addScope(url)
        env.webDriver.get(url)
        
      case r"""I am on the "?(.+?)"?$pageScope page""" =>
        env.pageScopes.addScope(pageScope)
  
      case r""""?(.+?)"?$element can be located by "?(id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)"?$locator "?(.+?)"?$$$expression""" =>
        env.pageScopes.set(s"$element/locator", locator);
        env.pageScopes.set(s"$element/locator/$locator", expression)
        
      case r"""the page title should (be|contain)$operator my "?(.+?)"?$attribute attribute""" =>
        val actual = env.webDriver.getTitle()
        env.pageScopes.set("page/title", actual)
        doFeatureScopeAction(attribute, env) { expected => 
          compare(expected, actual, operator) 
        }
        
      case r"""the page title should (be|contain)$operator "?(.+?)"?$$$expected""" =>
        val actual = env.webDriver.getTitle()
        env.pageScopes.set("page/title", actual)
        compare(expected, actual, operator)
        
      case r"""I switch to "?(.+?)"?$element frame by source""" =>
        val actual = env.webDriver.switchTo().frame(env.webDriver.findElement(By.cssSelector("iframe[src='" + element + "']")))
        
      case r"""I switch to the default frame""" =>
        val actual = env.webDriver.switchTo().defaultContent()
        
      case r""""?(.+?)"?$element text should (be|contain)$operator my "?(.+?)"?$attribute attribute""" =>
        val webElement = locate(env, element)
        val actual = getElementText(webElement)
        env.pageScopes.set(s"$element/text", actual)
        doFeatureScopeAction(attribute, env) { expected => 
          compare(expected, actual, operator) 
        }
        
      case r"""I capture "?(.+?)"?$element text""" =>
        val webElement = locate(env, element)
        val value = getElementText(webElement)
        env.featureScopes.set(element, value)
        
      case r""""?(.+?)"?$element text should (be|contain)$operator "?(.+?)"?$$$expected""" =>
        val webElement = locate(env, element)
        val actual = getElementText(webElement)
        env.pageScopes.set(s"$element/text", actual)
        compare(expected, actual, operator)
        
      case r""""?(.+?)"?$element should be (visible|displayed|invisible|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" =>
        val webElement = locate(env, element)
        state match {
          case "visible" | "displayed" => assert(webElement.isDisplayed(), s"$element should be $state")
          case "invisible" | "hidden" => assert(!webElement.isDisplayed(), s"$element should be $state")
          case "checked" | "ticked" => assert(webElement.isSelected(), s"$element should be $state")
          case "unchecked" | "unticked" => assert(!webElement.isSelected(), s"$element should be $state")
          case "enabled" => assert(webElement.isEnabled(), s"$element should be $state")
          case "disabled" => assert(!webElement.isEnabled(), s"$element should be $state")
        }
        env.pageScopes.set(s"$element/state", state)
        
      case r"""my role is "?(.+?)"?$role""" => 
        env.featureScopes.addScope(role)
        
      case r"""my "?(.+?)"?$setting setting (?:is|will be) "?(.+?)"?$$$value""" => 
        sys.props += ((setting, value))
        
      case r"""my "?(.+?)"?$attribute attribute (?:is|will be) "?(.+?)"?$$$value""" => 
        env.featureScopes.set(attribute, value)
        
      case r"""the url will be "?(.+?)"?$$$url""" => 
        env.pageScopes.set("navigation/url", url)   
        
      //search for and use the element's text to determine wait
      case r"""I wait for ((?:[^"]).+?(?:[^"]))$element text for (.+?)$seconds second(?:s?)""" =>
        env.wait(seconds.toInt) {
          getElementText(locate(env, element)).length() > 0
        }
        
      //search for and use the element's text to determine wait
      case r"""I wait for ((?:[^"]).+?(?:[^"]))$element text""" =>
        env.wait(gwenSetting.get("gwen.web.wait.seconds").toInt) {
          getElementText(locate(env, element)).length() > 0
        }
        
      //search for and use the element's visibility/availability(on the page) to determine wait
      case r"""I wait for ((?:[^"]).+?(?:[^"]))$element for (.+?)$seconds second(?:s?)""" =>
        env.wait(seconds.toInt) {
          locateOpt(env, element).isDefined
        }
        
      //search for and use the element's visibility/availability(on the page) to determine wait
      case r"""I wait for ((?:[^"]).+?(?:[^"]))$$$element""" =>
       env.wait(gwenSetting.get("gwen.web.wait.seconds").toInt) {
         locateOpt(env, element).isDefined
       }
      
      case r"""I enter my ((?:[^"]).+?(?:[^"]))$attribute attribute in "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        val sValue = env.featureScopes.get(attribute);
        webElement.sendKeys(sValue)
        env.pageScopes.set(s"$element/text", sValue)
        
      /**
       * Will perform a lookup on the page scope retrieve the text value and place it in the specified element
       */
      case r"""I enter ((?:[^"]).+?(?:[^"]))$attribute in "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        val sValue = getElementText(locate(env, attribute))
        webElement.sendKeys(sValue)
        env.pageScopes.set(s"$element/text", sValue)
        
      /**
       * Will enter the literal into the specified element
       */
      case r"""I enter "(.+?)"$value in "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        webElement.sendKeys(value)
        env.pageScopes.set(s"$element/text", value)
        
      case r"""I select "?(.+?)"?$value in "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        new Select(webElement).selectByVisibleText(value)
        env.pageScopes.set(s"$element/select", value)
        
      case r"""I (click|submit|tick|check|untick|uncheck)$action "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        action match {
          case "click" => webElement.click
          case "submit" => webElement.submit
          case "tick" | "check" if (!webElement.isSelected()) => webElement.click()
          case "untick" | "uncheck" if (webElement.isSelected()) => webElement.click()
        }
        env.pageScopes.set(s"$element/action", action)
        
        // sleep if wait time is configured for this element action
        env.pageScopes.getOpt(s"$element/$action/wait") foreach { secs => 
          Thread.sleep(secs.toLong * 1000)
        }
        
      case r"""I wait "?([0-9]+?)"?$duration second(?:s?) when "?(.+?)"?$element is (click|submitt|tick|check|untick|uncheck)${action}ed""" =>
        env.pageScopes.set(s"$element/$action/wait", duration)
        
      case r"""I wait "?([0-9]+?)"?$duration second(?:s?)""" =>
        Thread.sleep(duration.toLong * 1000)
        
      case r"""I highlight ((?:[^"]).+?(?:[^"]))$$$element""" =>
        env.highlight(locate(env, element))
        
        
      case _ => super.evaluate(step, env)
      
    }
  }
  
  private def compare(expected: String, actual: String, operator: String) = operator match {
    case "be" => assert(expected.equals(actual), s"Expected '$expected' but was '$actual'")
    case "contain" => assert(actual.contains(expected), s"'$actual' expected to contain '$expected' but does not")
    case _ => sys.error(s"Unsupported operator: ${operator}")
  }
  
  private def doFeatureScopeAction(attribute: String, env: WebEnvContext)(action: (String) => Unit) {
    action(env.featureScopes.get(attribute))
  }
  
  private def doPageScopeAction(attribute: String, env: WebEnvContext)(action: (String) => Unit) {
    action(env.pageScopes.get(attribute))
  }
  
  private def getElementText(webElement: WebElement) = Option(webElement.getAttribute("value")) match {
    case Some(value) => value
    case None => Option(webElement.getAttribute("text")) match {
      case Some(value) => value
      case None => webElement.getText()
    }
  }
  
}
