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
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select
import gwen.Predefs.Kestrel
import gwen.dsl.Step
import gwen.eval.DataScopes
import gwen.eval.EvalEngine
import gwen.eval.GwenOptions
import gwen.gwenSetting
import org.openqa.selenium.TimeoutException


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
   * @param dataScopes
   * 			initial data scopes
   */
  override def init(options: GwenOptions, dataScopes: DataScopes) = 
    new WebEnvContext(
      gwenSetting.get("gwen.web.browser") tap { webdriver =>
        logger.info(s"$webdriver web driver configured")
	  },
	  dataScopes
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
  override def evaluate(step: Step, env: WebEnvContext): Unit = {
    
    step.expression match {
    
      case r"""I navigate to the (.+?)$pageScope page""" =>
        env.webDriver.get(env.pageScopes.getIn(pageScope, "navigation/url"))
        env.pageScopes.addScope(pageScope)
        
      case r"""I navigate to "(.+?)"$$$url""" =>
        env.pageScopes.addScope(url)
        env.webDriver.get(url)
        
      case r"""I am on the (.+?)$pageScope page""" =>
        env.pageScopes.addScope(pageScope)
  
      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$$$expression""" =>
        env.pageScopes.set(s"$element/locator", locator);
        env.pageScopes.set(s"$element/locator/$locator", expression)

      case r"""the page title should (be|contain)$operator "(.+?)"$$$expected""" =>
        compare("title", expected, getTitle(env), operator)
        
      case r"""the page title should (be|contain)$operator my (.+?)$$$attribute""" =>
        doFeatureScopeAction(attribute, env) { expected => 
          compare("title", expected, getTitle(env), operator) 
        }
        
      case r"""I switch to (.+?)$element frame by source""" =>
        env.webDriver.switchTo().frame(env.webDriver.findElement(By.cssSelector("iframe[src='" + element + "']")))
        
      case r"""I switch to the default frame""" =>
        env.webDriver.switchTo().defaultContent()

      case r"""(.+?)$element text should (be|contain)$operator "(.+?)"$$$expected""" =>
        compare(element, expected, getElementText(element, env), operator)
        
      case r"""(.+?)$element text should (be|contain)$operator my (.+?)$$$attribute""" =>
        doFeatureScopeAction(attribute, env) { expected => 
          compare(element, expected, getElementText(element, env), operator) 
        }
        
      case r"""I capture (.+?)$element text""" =>
        env.featureScopes.set(element, getElementText(element, env))
        
      case r"""(.+?)$element should be (displayed|hidden|checked|unchecked|enabled|disabled)$$$state""" =>
        val webElement = locate(env, element)
        state match {
          case "displayed" => assert(webElement.isDisplayed(), s"$element should be $state")
          case "hidden" => assert(!webElement.isDisplayed(), s"$element should be $state")
          case "checked" => assert(webElement.isSelected(), s"$element should be $state")
          case "unchecked" => assert(!webElement.isSelected(), s"$element should be $state")
          case "enabled" => assert(webElement.isEnabled(), s"$element should be $state")
          case "disabled" => assert(!webElement.isEnabled(), s"$element should be $state")
        }
        bindAndWait(element, state, "true", env)
        
      case r"""my role is "(.+?)"$role""" => 
        env.featureScopes.addScope(role)
        
      case r"""my (.+?)$setting setting (?:is|will be) "(.+?)"$$$value""" => 
        sys.props += ((setting, value))
        
      case r"""my (.+?)$attribute (?:is|will be) "(.+?)"$$$value""" => 
        env.featureScopes.set(attribute, value)
        
      case r"""the url will be "(.+?)"$$$url""" => 
        env.pageScopes.set("navigation/url", url)   
        
      //search for and use the element's text to determine wait
      case r"""I wait for (.+?)$element text for (.+?)$seconds second(?:s?)""" =>
        env.waitUntil(s"Timed out waiting for $element text after $seconds second(s)", seconds.toInt) {
          getElementText(element, env).length() > 0
        } 
        
      case r"""I wait for (.+?)$element text""" =>
        env.waitUntil(s"Timed out waiting for $element text") {
          getElementText(element, env).length() > 0
        }
        
      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" =>
        env.waitUntil(s"Timed out waiting for $element after $seconds second(s)", seconds.toInt) {
          locateOpt(env, element).isDefined
        }
        
      case r"""I wait for (.+?)$$$element""" =>
       env.waitUntil(s"Timed out waiting for $element") {
         locateOpt(env, element).isDefined
       }
       
      case r"""I press enter in "?(.+?)"?$$$element""" =>
        locate(env, element).sendKeys(Keys.RETURN)
		bindAndWait(element, "enter", "true", env)

      case r"""I enter "(.+?)"$value in (.+?)$$$element""" =>
        sendKeys(element, value, env)
        
      case r"""I enter my (.+?)$attribute in (.+?)$$$element""" =>
        sendKeys(element, env.featureScopes.get(attribute), env)
        
      case r"""I enter (.+?)$source in (.+?)$$$target""" =>
        sendKeys(target, getElementText(source, env), env)

      case r"""I select "(.+?)"$value in (.+?)$$$element""" =>
        env.waitUntil(s"Timed out attempting to select '$value' in $element") {
          selectByVisibleText(element, value, env)
		  true
		}
        
      case r"""I select my (.+?)$attribute in (.+?)$$$element""" =>
	    doFeatureScopeAction(attribute, env) { value => 
		  env.waitUntil(s"Timed out attempting to select '$value' in $element") {
		    selectByVisibleText(element, value, env)
			true
		  }
        }
        
      case r"""I (click|submit|check|uncheck)$action (.+?)$$$element""" =>
        env.waitUntil(s"Timed out attempting to $action $element") {
          val webElement = locate(env, element)
          action match {
            case "click" => webElement.click
            case "submit" => webElement.submit
            case "check" if (!webElement.isSelected()) => webElement.click()
            case "uncheck" if (webElement.isSelected()) => webElement.click()
          }
          bindAndWait(element, action, "true", env)
          true
        }
        
      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (click|submit(?:t)|tick|check|untick|uncheck|select|enter)${action}ed""" =>
        env.pageScopes.set(s"$element/$action/wait", duration)
        
      case r"""I wait until "(.+?)"$condition when (.+?)$element is (click|submit(?:t)|tick|check|untick|uncheck|select|enter)${action}ed""" =>
        env.pageScopes.set(s"$element/$action/condition", condition)
        
      case r"""I wait ([0-9]+?)$duration second(?:s?)""" =>
        Thread.sleep(duration.toLong * 1000)
        
      case r"""I highlight (.+?)$$$element""" =>
        env.highlight(locate(env, element))
        
      case _ => super.evaluate(step, env)
      
    }
  }
  
  private def compare(element: String, expected: String, actual: String, operator: String) = operator match {
    case "be" => assert(expected.equals(actual), s"Expected $element '$expected' but was '$actual'")
    case "contain" => assert(actual.contains(expected), s"$element '$actual' expected to contain '$expected' but does not")
    case _ => sys.error(s"Unsupported operator: ${operator}")
  }
  
  private def doFeatureScopeAction(attribute: String, env: WebEnvContext)(action: (String) => Unit) {
    action(env.featureScopes.get(attribute))
  }
  
  private def doPageScopeAction(attribute: String, env: WebEnvContext)(action: (String) => Unit) {
    action(env.pageScopes.get(attribute))
  }
  
  private def getTitle(env: WebEnvContext): String = env.webDriver.getTitle() tap { title =>
    bindAndWait("page", "title", title, env)
  }
  
  private def getElementText(element: String, env: WebEnvContext): String = { 
    val webElement = locate(env, element)
    (Option(webElement.getAttribute("value")) match {
      case Some(value) => value
      case None => Option(webElement.getAttribute("text")) match {
        case Some(value) => value
        case None => webElement.getText()
      }
    }) tap { text => 
      bindAndWait(element, "text", text, env)
    }
  }
  
  private def sendKeys(element: String, value: String, env: WebEnvContext) {
    val webElement = locate(env, element)
    webElement.sendKeys(value)
    bindAndWait(element, "text", value, env)
  }
  
  private def selectByVisibleText(element: String, value: String, env: WebEnvContext) {
    val webElement = locate(env, element)
    new Select(webElement).selectByVisibleText(value)
    bindAndWait(element, "select", value, env)
  }

  private def bindAndWait(element: String, action: String, value: String, env: WebEnvContext) {
    env.pageScopes.set(s"$element/$action", value)
    
    // sleep if wait time is configured for this action
	env.pageScopes.getOpt(s"$element/$action/wait") foreach { secs => 
	  logger.info(s"Waiting for ${secs} second(s)..")
      Thread.sleep(secs.toLong * 1000)
    }
	
	// wait for javascript post condition if one is configured for this action
	env.pageScopes.getOpt(s"$element/$action/condition") foreach { javascript =>
	  logger.info(s"Waiting for ${javascript} to return true..")
	  env.waitUntil(s"Timed out waiting for $element post-$action condition to be satisifed") {
	    env.executeScript(s"return $javascript").asInstanceOf[Boolean] tap { satisfied =>
	      if (satisfied) {
	        Thread.sleep(gwenSetting.getOpt("gwen.web.animations.delay.msecs").getOrElse("200").toLong)
	      }
	    }
	  }
    }
  }
  
}
