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

import org.openqa.selenium.Keys
import org.openqa.selenium.support.ui.Select

import gwen.Predefs.Kestrel
import gwen.dsl.Step
import gwen.eval.EvalEngine
import gwen.eval.GwenOptions
import gwen.eval.ScopedDataStack
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
   * @param scopes
   * 			initial data scopes
   */
  override def init(options: GwenOptions, scopes: ScopedDataStack) = 
    new WebEnvContext(
      gwenSetting.get("gwen.web.browser") tap { webdriver =>
        logger.info(s"$webdriver web driver configured")
	  },
	  scopes
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
   */
  override def evaluate(step: Step, env: WebEnvContext): Unit = {
    
    step.expression match {
    
      case r"""I navigate to the (.+?)$pageScope page""" =>
        env.webDriver.get(env.scopes.getIn(pageScope, "url"))
        env.scopes.addScope(pageScope)
        
      case r"""I navigate to "(.+?)"$$$url""" =>
        env.scopes.addScope(url)
        env.webDriver.get(url)
        
      case r"""I am on the (.+?)$pageScope page""" =>
        env.scopes.addScope(pageScope)
  
      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$$$expression""" =>
        env.scopes.set(s"$element/locator", locator);
        env.scopes.set(s"$element/locator/$locator", expression)

      case r"""the page title should( not)?$negation (be|contain)$operator "(.+?)"$$$expected""" =>
        compare("title", expected, getTitle(env), operator, Option(negation).isDefined)
        
      case r"""the page title should( not)?$negation (be|contain)$operator (.+?)$$$attribute""" =>
        compare("title", getAttribute(attribute, env), getTitle(env), operator, Option(negation).isDefined) 
        
      case r"""(.+?)$element should( not)?$negation (be|contain)$operator "(.+?)"$$$expected""" =>
        compare(element, expected, getElementText(element, env), operator, Option(negation).isDefined)
        
      case r"""(.+?)$element should( not)?$negation (be|contain)$operator (.+?)$$$attribute""" =>
        compare(element, getAttribute(attribute, env), getElementText(element, env), operator, Option(negation).isDefined) 
        
      case r"""I capture (.+?)$element as (.+?)$attribute""" =>
        env.featureScope.set(attribute, getElementText(element, env))
        
      case r"""I capture (.+?)$element""" =>
        env.featureScope.set(element, getElementText(element, env))
        
      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|unchecked|enabled|disabled)$$$state""" =>
        val webElement = locate(env, element)
        val result = state match {
          case "displayed" => webElement.isDisplayed()
          case "hidden"    => !webElement.isDisplayed()
          case "checked"   => webElement.isSelected()
          case "unchecked" => !webElement.isSelected()
          case "enabled"   => webElement.isEnabled()
          case "disabled"  => !webElement.isEnabled()
        }
        if (!Option(negation).isDefined) assert(result,  s"$element should be $state")
        else assert(!result,  s"$element should not be $state")
        bindAndWait(element, state, "true", env)
        
      case r"""the url will be "(.+?)"$$$url""" => 
        env.scopes.set("url", url)   
        
      case r"""my (.+?)$setting setting (?:is|will be) "(.+?)"$$$value""" => 
        sys.props += ((setting, value))
        
      case r"""(.+?)$attribute (?:is|will be) defined by javascript "(.+?)"$$$expression""" =>
	    env.scopes.set(s"$attribute/javascript", expression)
        
      case r"""(.+?)$attribute (?:is|will be) "(.+?)"$$$value""" => 
        env.featureScope.set(attribute, value)
        
      case r"""I wait for (.+?)$element text for (.+?)$seconds second(?:s?)""" =>
        env.waitUntil(s"Waiting for $element text after $seconds second(s)", seconds.toInt) {
          getElementText(element, env).length() > 0
        } 
        
      case r"""I wait for (.+?)$element text""" =>
        env.waitUntil(s"Waiting for $element text") {
          getElementText(element, env).length() > 0
        }
        
      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" =>
        env.waitUntil(s"Waiting for $element after $seconds second(s)", seconds.toInt) {
          locateOpt(env, element).isDefined
        }
        
      case r"""I wait for (.+?)$$$element""" =>
       env.waitUntil(s"Waiting for $element") {
         locateOpt(env, element).isDefined
       }
       
      case r"""I press enter in "?(.+?)"?$$$element""" =>
        locate(env, element).sendKeys(Keys.RETURN)
		bindAndWait(element, "enter", "true", env)

      case r"""I (enter|type)$action "(.+?)"$value in (.+?)$$$element""" =>
        sendKeys(element, action, value, env)
        
      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" =>
        sendKeys(element, action, getAttribute(attribute, env), env)
        
      case r"""I select the (\d+?)$position(st|nd|rd|th)$suffix option in (.+?)$$$element""" =>
        env.waitUntil(s"Selecting '${position}${suffix}' option in $element") {
          selectByIndex(element, position.toInt - 1, env)
		  true
		}
        
      case r"""I select "(.+?)"$value in (.+?)$$$element""" =>
        env.waitUntil(s"Selecting '$value' in $element") {
          selectByVisibleText(element, value, env)
		  true
		}
        
      case r"""I select (.+?)$attribute in (.+?)$$$element""" =>
	    getAttribute(attribute, env) tap { value => 
		  env.waitUntil(s"Selecting '$value' in $element") {
		    selectByVisibleText(element, value, env)
			true
		  }
        }
        
      case r"""I (click|submit|check|uncheck)$action (.+?)$$$element""" =>
        env.waitUntil(s"${action match {
            case "click" => "Clicking"
            case "submit" => "Submitting"
            case "check" => "Checking"
            case "uncheck" => "Unchecking"
          }} $element") {
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
        
      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered)$$$event""" =>
        env.scopes.set(s"$element/${eventToAction(event)}/wait", duration)
        
      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered)$$$event""" =>
        env.scopes.set(s"$element/${eventToAction(event)}/condition", condition)
        
      case r"""I wait until (.+?)$$$condition""" =>
        env.waitUntil(s"Waiting until $condition") {
	      env.executeScript(s"return ${env.scopes.get(s"$condition/javascript")}").asInstanceOf[Boolean]
	    }
        
      case r"""I wait ([0-9]+?)$duration second(?:s?)""" =>
        Thread.sleep(duration.toLong * 1000)
        
      case r"""I highlight (.+?)$$$element""" =>
        env.highlight(locate(env, element))
        
      case _ => super.evaluate(step, env)
      
    }
  }
  
  private def getAttribute(name: String, env: WebEnvContext): String = 
    env.scopes.getOpt(name) match {
      case Some(value) => value
      case _ => env.scopes.getOpt(s"$name/javascript") match {
        case Some(expression) => env.executeScript(s"return $expression").asInstanceOf[String]
        case _ => env.scopes.get(name)
      }
    } 
  
  private def compare(element: String, expected: String, actual: String, operator: String, negate: Boolean) = 
    (operator match {
      case "be"      => expected.equals(actual)
      case "contain" => actual.contains(expected)
    }) tap { result =>
      if (!negate) assert(result, s"$element '$actual' should $operator '$expected'")
      else assert(!result, s"$element should not $operator '$expected'")
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
  
  private def sendKeys(element: String, action: String, value: String, env: WebEnvContext) {
    val webElement = locate(env, element)
    webElement.sendKeys(value)
    bindAndWait(element, "type", value, env)
    if (action == "enter") {
      webElement.sendKeys(Keys.RETURN)
	  bindAndWait(element, "enter", "true", env)
    }
  }
  
  private def selectByVisibleText(element: String, value: String, env: WebEnvContext) {
    val webElement = locate(env, element)
    new Select(webElement).selectByVisibleText(value)
    bindAndWait(element, "select", value, env)
  }
  
  private def selectByIndex(element: String, index: Int, env: WebEnvContext) {
    val webElement = locate(env, element)
    val select = new Select(webElement)
    select.selectByIndex(index)
    bindAndWait(element, "select", select.getFirstSelectedOption().getText(), env)
  }

  private def bindAndWait(element: String, action: String, value: String, env: WebEnvContext) {
    env.scopes.set(s"$element/$action", value)
    
    // sleep if wait time is configured for this action
	env.scopes.getOpt(s"$element/$action/wait") foreach { secs => 
	  logger.info(s"Waiting for ${secs} second(s) (post-$action wait)")
      Thread.sleep(secs.toLong * 1000)
    }
	
	// wait for javascript post condition if one is configured for this action
	env.scopes.getOpt(s"$element/$action/condition") foreach { condition =>
	  val javascript = env.scopes.get(s"$condition/javascript")
	  logger.debug(s"Waiting for script to return true: ${javascript}")
	  env.waitUntil(s"Waiting until $condition (post-$action condition)") {
	    env.executeScript(s"return $javascript").asInstanceOf[Boolean] tap { satisfied =>
	      if (satisfied) {
	        Thread.sleep(gwenSetting.getOpt("gwen.web.throttle.msecs").getOrElse("200").toLong)
	      }
	    }
	  }
    }
  }
  
  private def eventToAction(event: String): String = event match {
    case "clicked"   => "click"
    case "submitted" => "submit" 
    case "checked"   => "check" 
    case "unchecked" => "uncheck" 
    case "selected"  => "select" 
    case "typed"     => "type" 
    case "entered"   => "enter"
  }
  
}
