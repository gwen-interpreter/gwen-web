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

import org.openqa.selenium.Keys
import gwen.Predefs.Kestrel
import gwen.Predefs.RegexContext
import gwen.Settings
import gwen.dsl.Step
import gwen.eval.EvalEngine
import gwen.eval.GwenOptions
import gwen.eval.ScopedDataStack
import gwen.eval.support.XPathSupport
import gwen.eval.support.SystemProcessSupport


/**
 * A web engine that uses the Selenium web driver
 * API to automate various web operations.
 * 
 * @author Branko Juric, Brady Wood
 */
trait GwenWebEngine extends EvalEngine[WebEnvContext] with WebElementLocator with XPathSupport with SystemProcessSupport[WebEnvContext] {
  
  /**
   * Initialises and returns a new web environment context.
   * 
   * @param options
   * 			command line options
   * @param scopes
   * 			initial data scopes
   */
  override def init(options: GwenOptions, scopes: ScopedDataStack) = new WebEnvContext(scopes)
  
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
    
      case r"""I am on the (.+?)$$$page""" =>
        env.scopes.addScope(page)
        
      case r"""I navigate to the (.+?)$$$page""" => env.withScreenShot {
    	env.scopes.addScope(page)
        env.webDriver.get(env.getAttribute("url"))
      }
        
      case r"""I navigate to "(.+?)"$$$url""" => env.withScreenShot {
        env.scopes.addScope(url)
        env.webDriver.get(url)
      }
      
      case r"""the url will be defined by (?:property|setting) "(.+?)"$$$name""" => 
        env.scopes.set("url", Settings.get(name))
        
      case r"""the url will be "(.+?)"$$$url""" => 
        env.scopes.set("url", url)   
  
      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$$$expression""" =>
        env.scopes.set(s"$element/locator", locator);
        env.scopes.set(s"$element/locator/$locator", expression)

      case r"""the page title should( not)?$negation (be|contain|match regex|match xpath)$operator "(.*?)"$$$expression""" => env.withScreenShot {
        compare("title", expression, env.getTitle, operator, Option(negation).isDefined, env)
      }
        
      case r"""the page title should( not)?$negation (be|contain|match regex|match xpath)$operator (.+?)$$$attribute""" => env.withScreenShot {
        compare("title", env.getAttribute(attribute), env.getTitle, operator, Option(negation).isDefined, env) 
      }
      
      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|unchecked|enabled|disabled)$$$state""" => env.withScreenShot {
        env.withWebElement(element) { webElement =>
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
          env.bindAndWait(element, state, "true")
        }
      }
        
      case r"""(.+?)$element should( not)?$negation (be|contain|match regex|match xpath)$operator "(.*?)"$$$expression""" => env.withScreenShot {
        compare(element, expression, env.getBoundValue(element), operator, Option(negation).isDefined, env)
      }
        
      case r"""(.+?)$element should( not)?$negation (be|contain|match regex|match xpath)$operator (.+?)$$$attribute""" => env.withScreenShot {
        compare(element, env.getAttribute(attribute), env.getBoundValue(element), operator, Option(negation).isDefined, env) 
      }
      
      case r"""I capture the (text|node|nodeset)$targetType in (.+?)$source by xpath "(.+?)"$expression as (.+?)$$$name""" => env.withScreenShot {
        evaluateXPath(expression, env.getBoundValue(source), XMLNodeType.withName(targetType)) tap { value =>
          env.featureScope.set(name, value)
        }
      }
      
      case r"""I capture the text in (.+?)$source by regex "(.+?)"$expression as (.+?)$$$name""" => env.withScreenShot {
        evaluateRegex(expression, env.getBoundValue(source)) tap { value =>
          env.featureScope.set(name, value)
        }
      }
      
      case r"""I capture the current URL""" => env.withScreenShot {
        env.featureScope.set("the current URL", env.webDriver.getCurrentUrl())
      }
      
      case r"""I capture the current URL as (.+?)$name""" => env.withScreenShot {
        env.featureScope.set(name, env.webDriver.getCurrentUrl())
      }
      
      case r"""I capture (.+?)$element as (.+?)$attribute""" => env.withScreenShot {
        env.featureScope.set(attribute, env.getBoundValue(element))
      }
        
      case r"""I capture (.+?)$element""" => env.withScreenShot {
        env.featureScope.set(element, env.getBoundValue(element))
      }
        
      case r"""my (.+?)$name (?:property|setting) (?:is|will be) "(.*?)"$$$value""" =>
        Settings.add(name, value)
        
      case r"""(.+?)$attribute (?:is|will be) defined by (javascript|property|setting)$attrType "(.+?)"$$$expression""" =>
        attrType match {
          case "javascript" => env.scopes.set(s"$attribute/javascript", expression)
          case _ => env.featureScope.set(attribute, Settings.get(expression))
        }
        
      case r"""(.+?)$attribute (?:is|will be) "(.*?)"$$$value""" => 
        env.featureScope.set(attribute, value)
        
      case r"""I wait for (.+?)$element text for (.+?)$seconds second(?:s?)""" => env.withScreenShot {
        env.waitUntil(s"Waiting for $element text after $seconds second(s)", seconds.toInt) {
          waitForText(element, env)
        } 
      }
        
      case r"""I wait for (.+?)$element text""" => env.withScreenShot {
        env.waitUntil(s"Waiting for $element text") {
          waitForText(element, env)
        }
      }
        
      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" => env.withScreenShot {
        env.waitUntil(s"Waiting for $element after $seconds second(s)", seconds.toInt) {
          locateOpt(env, element).isDefined
        }
      }
        
      case r"""I wait for (.+?)$$$element""" => env.withScreenShot {
        env.waitUntil(s"Waiting for $element") {
          locateOpt(env, element).isDefined
        }
      }
       
      case r"""I press enter in (.+?)$$$element""" => env.withScreenShot {
        locate(env, element).sendKeys(Keys.RETURN)
		    env.bindAndWait(element, "enter", "true")
      }

      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$$$element""" => env.withScreenShot {
        env.sendKeys(element, action, value)
      }
        
      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" => env.withScreenShot {
        env.sendKeys(element, action, env.getAttribute(attribute))
      }
        
      case r"""I select the (\d+?)$position(st|nd|rd|th)$suffix option in (.+?)$$$element""" => env.withScreenShot {
        env.waitUntil(s"Selecting '${position}${suffix}' option in $element") {
          env.selectByIndex(element, position.toInt - 1)
		      true
		    }
      }
        
      case r"""I select "(.*?)"$value in (.+?)$$$element""" => env.withScreenShot {
        env.waitUntil(s"Selecting '$value' in $element") {
          env.selectByVisibleText(element, value)
		      true
		    }
      }
        
      case r"""I select (.+?)$attribute in (.+?)$$$element""" => env.withScreenShot {
	      env.getAttribute(attribute) tap { value => 
		      env.waitUntil(s"Selecting '$value' in $element") {
		        env.selectByVisibleText(element, value)
			      true
		      }
        }
      }
        
      case r"""I (click|submit|check|uncheck)$action (.+?)$$$element""" => env.withScreenShot {
        env.waitUntil {
          env.withWebElement(action, element) { webElement =>
            action match {
              case "click" => webElement.click
              case "submit" => webElement.submit
              case "check" if (!webElement.isSelected()) => webElement.sendKeys(Keys.SPACE)
              case "uncheck" if (webElement.isSelected()) => webElement.sendKeys(Keys.SPACE)
            }
            env.bindAndWait(element, action, "true")
          }
          true
        }
      }
        
      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered)$$$event""" =>
        env.scopes.set(s"$element/${WebElementActions.EventToAction(event)}/wait", duration)
        
      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered)$$$event""" =>
        env.scopes.set(s"$element/${WebElementActions.EventToAction(event)}/condition", condition)
        
      case r"""I wait until "(.+?)$javascript"""" => env.withScreenShot {
        env.waitUntil(s"Waiting until $javascript") {
	        env.executeScript(s"return $javascript").asInstanceOf[Boolean]
	      }
      }
        
      case r"""I wait until (.+?)$$$condition""" => env.withScreenShot {
        env.waitUntil(s"Waiting until $condition") {
	        env.executeScript(s"return ${env.scopes.get(s"$condition/javascript")}").asInstanceOf[Boolean]
	      }
      }
        
      case r"""I wait ([0-9]+?)$duration second(?:s?)""" => env.withScreenShot {
        Thread.sleep(duration.toLong * 1000)
      }
        
      case r"""I (?:highlight|locate) (.+?)$$$element""" =>
        env.highlight(locate(env, element))
        
      case _ => super.evaluate(step, env)
      
    }
  }
  
  private def waitForText(element: String, env: WebEnvContext): Boolean = {
    val text = env.getElementText(element)
    text != null && text.length > 0
  }
  
  private def compare(element: String, expression: String, actual: String, operator: String, negate: Boolean, env: WebEnvContext) = 
    (operator match {
      case "be"      => expression.equals(actual)
      case "contain" => actual.contains(expression)
      case "match regex" => actual.matches(expression)
      case "match xpath" => !evaluateXPath(expression, actual, XMLNodeType.text).isEmpty()
    }) tap { result =>
      if (!negate) assert(result, s"$element '$actual' should $operator '$expression'")
      else assert(!result, s"$element should not $operator '$expression'")
    } 
  
  private def evaluateRegex(regex: String, source: String): String =  
    regex.r.findFirstMatchIn(source).getOrElse(sys.error(s"'Regex match '$regex' not found in '$source'")).group(1)
  
}

object WebElementActions {
  val EventToAction = Map(
    "clicked"   -> "click",
    "submitted" -> "submit", 
    "checked"   -> "check", 
    "unchecked" -> "uncheck", 
    "selected"  -> "select", 
    "typed"     -> "type", 
    "entered"   -> "enter"
  )
}
