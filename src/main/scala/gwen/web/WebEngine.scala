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
import gwen.eval.support.RegexSupport
import gwen.eval.support.SystemProcessSupport
import gwen.eval.support.XPathSupport
import gwen.eval.UnsupportedStepException


/**
  * A web engine that uses the Selenium web driver
  * API to automate various web operations.
  * 
  * @author Branko Juric, Brady Wood
  */
trait WebEngine extends EvalEngine[WebEnvContext] with WebElementLocator with SystemProcessSupport[WebEnvContext] {
  
  /**
    * Initialises and returns a new web environment context.
    * 
    * @param options command line options
    * @param scopes initial data scopes
    */
  override def init(options: GwenOptions, scopes: ScopedDataStack) = new WebEnvContext(options, scopes)
  
  /**
    * Evaluates a given step.  This method matches the incoming step against a 
    * set of supported steps and evaluates only those that are successfully 
    * matched.
    *
    * @param step the step to evaluate
    * @param env the web environment context
    */
  override def evaluate(step: Step, env: WebEnvContext): Unit = {
    
   step.expression match {
    
      case r"""I am on the (.+?)$$$page""" =>
        env.scopes.addScope(page)
        
      case r"""I navigate to the (.+?)$$$page""" => 
        env.scopes.addScope(page)
        val url = env.getAttribute("url")
        env.execute {
          env.withWebDriver(_.get(url))(WebSettings.`gwen.web.capture.screenshots`)
        }
        
      case r"""I navigate to "(.+?)"$$$url""" => 
        env.scopes.addScope(url)
        env.execute {
          env.withWebDriver(_.get(url))(WebSettings.`gwen.web.capture.screenshots`)
        }
        
      case r"""I scroll to the (top|bottom)$position of (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.scrollIntoView(elementBinding, ScrollTo.withName(position))
        }
      }
      
      case r"""the url will be defined by (?:property|setting) "(.+?)"$$$name""" => 
        env.scopes.set("url", Settings.get(name))
        
      case r"""the url will be "(.+?)"$$$url""" => 
        env.scopes.set("url", url)   
  
      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$$$expression""" =>
        env.scopes.set(s"$element/locator", locator);
        env.scopes.set(s"$element/locator/$locator", expression)

      case r"""the page title should( not)?$negation (be|contain|match regex|match xpath)$operator "(.*?)"$$$expression""" =>  env.execute {
        compare("title", expression, env.getTitle, operator, Option(negation).isDefined, env)
      }
        
      case r"""the page title should( not)?$negation (be|contain|match regex|match xpath)$operator (.+?)$$$attribute""" => {
        val expected = env.getAttribute(attribute)
        env.execute {
          compare("title", expected, env.getTitle, operator, Option(negation).isDefined, env)
        }
      }
      
      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|unchecked|enabled|disabled)$$$state""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.withWebElement(elementBinding) { webElement =>
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
      }
        
      case r"""(.+?)$element should( not)?$negation (be|contain|match regex|match xpath)$operator "(.*?)"$$$expression""" => { 
        if (element == "I") throw new UnsupportedStepException(step)
        val actual = env.getBoundValue(element)
        env.execute {
          compare(element, expression, actual, operator, Option(negation).isDefined, env)
        }
      }
        
      case r"""(.+?)$element should( not)?$negation (be|contain|match regex|match xpath)$operator (.+?)$$$attribute""" => {
        if (element == "I") throw new UnsupportedStepException(step)
        val expected = env.getAttribute(attribute)
        val actual = env.getBoundValue(element)
        env.execute {
          compare(element, expected, actual, operator, Option(negation).isDefined, env)
        }
      }
      
      case r"""I capture the (text|node|nodeset)$targetType in (.+?)$source by xpath "(.+?)"$expression as (.+?)$$$name""" => {
        val src = env.getBoundValue(source)
        env.featureScope.set(name, env.execute(env.evaluateXPath(expression, src, env.XMLNodeType.withName(targetType))).getOrElse(s"$$[xpath:$expression]"))
      }
      
      case r"""I capture the text in (.+?)$source by regex "(.+?)"$expression as (.+?)$$$name""" => {
        val src = env.getBoundValue(source)
        env.featureScope.set(name, env.execute(env.extractByRegex(expression, src)).getOrElse(s"$$[regex:$expression"))
      }
      
      case r"""I capture the current URL""" => 
        env.featureScope.set("the current URL", env.execute(env.withWebDriver(_.getCurrentUrl())).getOrElse("$[currentUrl]"))
      
      case r"""I capture the current URL as (.+?)$name""" => 
        env.featureScope.set(name, env.execute(env.withWebDriver(_.getCurrentUrl())).getOrElse("$[currentUrl]"))
      
      case r"""I capture (.+?)$element as (.+?)$attribute""" =>
        env.featureScope.set(attribute, env.getBoundValue(element))
        
      case r"""I capture (.+?)$element""" =>
        env.featureScope.set(element, env.getBoundValue(element))
        
      case r"""I wait for (.+?)$element text for (.+?)$seconds second(?:s?)""" =>  {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.waitUntil(s"Waiting for $element text after $seconds second(s)", seconds.toInt) {
            env.waitForText(elementBinding)
          }
        }
      }
        
      case r"""I wait for (.+?)$element text""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.waitUntil(s"Waiting for $element text") {
            env.waitForText(elementBinding)
          }
        }
      }
        
      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" =>  {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.waitUntil(s"Waiting for $element after $seconds second(s)", seconds.toInt) {
            locateOpt(env, elementBinding).isDefined
          }
        }
      }
        
      case r"""I wait for (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.waitUntil(s"Waiting for $element") {
            locateOpt(env, elementBinding).isDefined
          }
        }
      }
      
      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered|cleared)$$$event""" =>
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/wait", duration)
        
      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered|cleared)$$$event""" =>
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/condition", condition)
        
      case r"""I wait until "(.+?)$javascript"""" => env.execute {
        env.waitUntil(s"Waiting until $javascript") {
          env.executeScript(s"return $javascript").asInstanceOf[Boolean]
        }
      }
        
      case r"""I wait until (.+?)$$$condition""" =>  {
        val javascript = env.scopes.get(s"$condition/javascript")
        env.execute {
          env.waitUntil(s"Waiting until $condition") {
            env.executeScript(s"return ${javascript}").asInstanceOf[Boolean]
          }
        }
      }
        
      case r"""I wait ([0-9]+?)$duration second(?:s?)""" => env.execute {
        Thread.sleep(duration.toLong * 1000)
      }
      
      case r"""my (.+?)$name (?:property|setting) (?:is|will be) "(.*?)"$$$value""" =>
        Settings.add(name, value)
        
      case r"""(.+?)$attribute (?:is|will be) defined by (javascript|property|setting)$attrType "(.+?)"$$$expression""" =>
        attrType match {
          case "javascript" => env.scopes.set(s"$attribute/javascript", expression)
          case _ => env.featureScope.set(attribute, Settings.get(expression))
        }

      case r"""(.+?)$attribute (?:is|will be) defined by the (text|node|nodeset)$targetType in (.+?)$source by xpath "(.+?)"$$$expression""" =>
        env.scopes.set(s"$attribute/xpath/source", source)
        env.scopes.set(s"$attribute/xpath/targetType", targetType)
        env.scopes.set(s"$attribute/xpath/expression", expression)
      
      case r"""(.+?)$attribute (?:is|will be) defined in (.+?)$source by regex "(.+?)"$$$expression""" => 
        env.scopes.set(s"$attribute/regex/source", source)
        env.scopes.set(s"$attribute/regex/expression", expression)
      
      case r"""(.+?)$attribute (?:is|will be) "(.*?)"$$$value""" => 
        env.featureScope.set(attribute, value)
        
      case r"""I clear (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.clearText(elementBinding)
        }
      }
      
      case r"""I press enter in (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element) 
        env.execute {
          locate(env, elementBinding).sendKeys(Keys.RETURN)
          env.bindAndWait(element, "enter", "true")
        }
      }

      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.sendKeys(elementBinding, value, true, action == "enter")
        }
      }
        
      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        val value = env.getAttribute(attribute)
        env.execute {
          env.sendKeys(elementBinding, value, true, action == "enter")
        }
      }
        
      case r"""I select the (\d+?)$position(st|nd|rd|th)$suffix option in (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.selectByIndex(elementBinding, position.toInt - 1)
        }
      }
        
      case r"""I select "(.*?)"$value in (.+?)$element by value""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute { 
          env.selectByValue(elementBinding, value)
        }
      }
        
      case r"""I select "(.*?)"$value in (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute { 
          env.selectByVisibleText(elementBinding, value)
        }
      }
        
      case r"""I select (.+?)$attribute in (.+?)$element by value""" => {
        val value = env.getAttribute(attribute)
        val elementBinding = env.getLocatorBinding(element)
        env.execute { 
          env.selectByValue(elementBinding, value)
        }
      }
        
      case r"""I select (.+?)$attribute in (.+?)$$$element""" => {
        val value = env.getAttribute(attribute)
        val elementBinding = env.getLocatorBinding(element)
        env.execute { 
          env.selectByVisibleText(elementBinding, value)
        }
      }
        
      case r"""I (click|submit|check|uncheck)$action (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute { 
          env.performAction(action, elementBinding)
        }
      }
        
      case r"""I (?:highlight|locate) (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.highlight(locate(env, elementBinding))
        }
      }
        
      case _ => super.evaluate(step, env)
      
    }
  }
  
  /**
    * Compares the value of an element with an expected value.
    * 
    * @param element the name of the element to compare from
    * @param expected the expected value, regex, or xpath
    * @param actual the actual value of the element
    * @param operator the comparison operator
    * @param negate true to negate the result
    * @param env the web environment context
    * @return true if the actual value matches the expected value
    */
  private def compare(element: String, expected: String, actual: String, operator: String, negate: Boolean, env: WebEnvContext) = 
    (operator match {
      case "be"      => expected.equals(actual)
      case "contain" => actual.contains(expected)
      case "match regex" => actual.matches(expected)
      case "match xpath" => !env.evaluateXPath(expected, actual, env.XMLNodeType.text).isEmpty()
    }) tap { result =>
      if (!negate) assert(result, s"$element '$actual' should $operator '$expected'")
      else assert(!result, s"$element should not $operator '$expected'")
    } 
  
}

