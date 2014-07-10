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

import gwen.Predefs.Kestrel
import gwen.dsl.Step
import gwen.eval.EvalEngine
import gwen.eval.GwenInterpreter
import gwen.eval.GwenOptions
import gwen.gwenSetting
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import gwen.eval.GwenApp
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.NoSuchElementException
import java.util.concurrent.TimeUnit


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
    
      case r"""I navigate to the "?(.+?)"?$page page""" =>
        env.webDriver.get(env.pageScopes.getIn(page, "navigation/url"))
        env.pageScopes.addScope(page)
        
      case r"""I navigate to "?(.+?)"?$$$url""" =>
        env.pageScopes.addScope(url)
        env.webDriver.get(url)
        
      case r"""I am on the "?(.+?)"?$page page""" => 
        env.pageScopes.current match {
          case None => 
            env.pageScopes.addScope(page)
          case Some(scope) =>
            if (scope.name != page) {
              env.pageScopes.addScope(page)
            }
        }
        
  
      case r""""?(.+?)"?$element can be located by "?(id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)"?$locatorType "?(.+?)"?$$$locatorValue""" =>
        env.pageScopes.set(s"$element/locator", locatorType);
        env.pageScopes.set(s"$element/locator/$locatorType", locatorValue)
        
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
        assert(webElement.isDisplayed(), s"Cannot access text of invisible '$element' element")
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
        assert(webElement.isDisplayed(), s"Cannot access text of invisible '$element' element")
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
        
      //search for and use the element's textvalue to determine wait
      case r"""I wait for ((?:[^"]).+?(?:[^"]))$element textvalue for (.+?)$seconds second(?:s?)""" =>
        val dynamicElement = new WebDriverWait(env.webDriver, seconds.toInt).until(
            new ExpectedCondition[Boolean] {
               override def apply(d: WebDriver) = getElementText(locate(env, element)).length() > 0
            }
        )  
        
      //search for and use the element's visibility/availability(on the page) to determine wait
      case r"""I wait for ((?:[^"]).+?(?:[^"]))$element for (.+?)$seconds second(?:s?)""" =>
        val dynamicElement = new WebDriverWait(env.webDriver, seconds.toInt).until(
            new ExpectedCondition[WebElement] {
               override def apply(d: WebDriver) = locate(env, element)
            }
        )
      
      case r"""I enter my ((?:[^"]).+?(?:[^"]))$attribute attribute in "?(.+?)"?$$$element""" =>
        
        val webElement = locate(env, element)
        assert(webElement.isDisplayed(), s"Cannot enter text in invisible '$element' element")
        assert(webElement.isEnabled(), s"Cannot enter text in disabled '$element' element")
        
        val sValue = env.featureScopes.get(attribute);
        
        webElement.sendKeys(sValue)
        env.pageScopes.set(s"$element/text", sValue)
        
      /**
       * Will perform a lookup on the page scope retrieve the text value and place it in the specified element
       */
      case r"""I enter ((?:[^"]).+?(?:[^"]))$attribute in "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        assert(webElement.isDisplayed(), s"Cannot enter text in invisible '$element' element")
        assert(webElement.isEnabled(), s"Cannot enter text in disabled '$element' element")
        val sValue = getElementText(locate(env, attribute))
        webElement.sendKeys(sValue)
        env.pageScopes.set(s"$element/text", sValue)
        
      /**
       * Will enter the literal into the specified element
       */
      case r"""I enter "(.+?)"$value in "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        assert(webElement.isDisplayed(), s"Cannot enter text in invisible '$element' element")
        assert(webElement.isEnabled(), s"Cannot enter text in disabled '$element' element")
        webElement.sendKeys(value)
        env.pageScopes.set(s"$element/text", value)
        
      case r"""I select "?(.+?)"?$value in "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        assert(webElement.isDisplayed(), s"Cannot enter text in invisible '$element' element")
        assert(webElement.isEnabled(), s"Cannot enter text in disabled '$element' element")
        new Select(webElement).selectByVisibleText(value)
        env.pageScopes.set(s"$element/select", value)
        
      case r"""I (click|submit|tick|check|untick|uncheck)$action "?(.+?)"?$$$element""" =>
        val webElement = locate(env, element)
        assert(webElement.isDisplayed(), s"Cannot $action invisible '$element' element")
        assert(webElement.isEnabled(), s"Cannot $action disabled '$element' element")
        action match {
          case "click" => webElement.click
          case "submit" => webElement.submit
          case "tick" | "check" if (!webElement.isSelected()) => webElement.click()
          case "untick" | "uncheck" if (webElement.isSelected()) => webElement.click()
        }
        env.pageScopes.set(s"$element/action", action)
        
      case r"""I wait "?([0-9]+?)"?$duration second(?:s?)""" =>
        Thread.sleep(duration.toLong * 1000)
        
      case r"""I highlight ((?:[^"]).+?(?:[^"]))$$$element""" =>
        highlight(env, locate(env, element))
        
        
      case _ => super.evaluate(step, env)
      
    }
  }
  
  /**
   * Highlights (blinks) a Webdriver element.
    In pure javascript, as suggested by https://github.com/alp82.
   */
  private def highlight(env: WebEnvContext, element: WebElement) {
    env.webDriver.asInstanceOf[JavascriptExecutor].executeScript("""
        element = arguments[0];
        original_style = element.getAttribute('style');
        element.setAttribute('style', original_style + "; background: yellow; border: 2px solid red;");
        setTimeout(function(){
            element.setAttribute('style', original_style);
        }, 300);
    """, element)
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

/**
 * The gwen-web interpreter.
 * 
 * @author Branko Juric, Brady Wood
 */
class GwenWebInterpreter extends GwenInterpreter[WebEnvContext] with GwenWebEngine

/**
 * The gwen-web standalone application.
 * 
 * @author Branko Juric, Brady Wood
 */
object GwenWebInterpreter extends GwenApp(new GwenWebInterpreter)
