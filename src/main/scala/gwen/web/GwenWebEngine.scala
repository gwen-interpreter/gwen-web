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

import java.io.StringReader
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.openqa.selenium.Keys
import org.openqa.selenium.support.ui.Select
import org.xml.sax.InputSource
import gwen.Predefs.Kestrel
import gwen.dsl.Step
import gwen.eval.EvalEngine
import gwen.eval.GwenOptions
import gwen.eval.ScopedDataStack
import gwen.gwenSetting
import javax.xml.xpath.XPathFactory
import javax.xml.namespace.NamespaceContext
import java.util.Iterator
import javax.xml.xpath.XPath


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
      gwenSetting.getOpt("gwen.web.browser").getOrElse("firefox") tap { webdriver =>
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
    
      case r"""I am on the (.+?)$$$name""" =>
        env.scopes.addScope(name)
        
      case r"""I navigate to the (.+?)$$$name""" => env.withScreenShot {
    	env.scopes.addScope(name)
        env.webDriver.get(getAttribute("url", env))
      }
        
      case r"""I navigate to "(.+?)"$$$url""" => env.withScreenShot {
        env.scopes.addScope(url)
        env.webDriver.get(url)
      }
      
      case r"""the url will be defined by (?:property|setting) "(.+?)"$$$name""" => 
        env.scopes.set("url", gwenSetting.get(name))
        
      case r"""the url will be "(.+?)"$$$url""" => 
        env.scopes.set("url", url)   
  
      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$$$expression""" =>
        env.scopes.set(s"$element/locator", locator);
        env.scopes.set(s"$element/locator/$locator", expression)

      case r"""the page title should( not)?$negation (be|contain|match regex|match xpath)$operator "(.*?)"$$$expression""" => env.withScreenShot {
        compare("title", expression, getTitle(env), operator, Option(negation).isDefined, env)
      }
        
      case r"""the page title should( not)?$negation (be|contain|match regex|match xpath)$operator (.+?)$$$attribute""" => env.withScreenShot {
        compare("title", getAttribute(attribute, env), getTitle(env), operator, Option(negation).isDefined, env) 
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
          bindAndWait(element, state, "true", env)
        }
      }
        
      case r"""(.+?)$element should( not)?$negation (be|contain|match regex|match xpath)$operator "(.*?)"$$$expression""" => env.withScreenShot {
        compare(element, expression, getAttributeOrElementText(element, env), operator, Option(negation).isDefined, env)
      }
        
      case r"""(.+?)$element should( not)?$negation (be|contain|match regex|match xpath)$operator (.+?)$$$attribute""" => env.withScreenShot {
        compare(element, getAttribute(attribute, env), getAttributeOrElementText(element, env), operator, Option(negation).isDefined, env) 
      }
      
      case r"""I capture (.+?)$name from (.+?)$source by (xpath|regex)$operator "(.+?)"$$$expression""" => env.withScreenShot {
        val input = getAttributeOrElementText(source, env)
        (operator match {
          case "xpath" => evaluateXPath(expression, input, env)
          case "regex" => evaluateRegex(expression, input, env)
        }) tap { value =>
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
        env.featureScope.set(attribute, getElementText(element, env))
      }
        
      case r"""I capture (.+?)$element""" => env.withScreenShot {
        getElementText(element, env)
      }
        
      case r"""my (.+?)$name (?:property|setting) (?:is|will be) "(.*?)"$$$value""" =>
        gwenSetting.add(name, value)
        
      case r"""(.+?)$attribute (?:is|will be) defined by (javascript|property|setting)$attrType "(.+?)"$$$expression""" =>
        attrType match {
          case "javascript" => env.scopes.set(s"$attribute/javascript", expression)
          case _ => env.featureScope.set(attribute, gwenSetting.get(expression))
        }
        
      case r"""(.+?)$attribute (?:is|will be) "(.*?)"$$$value""" => 
        env.featureScope.set(attribute, value)
        
      case r"""I wait for (.+?)$element text for (.+?)$seconds second(?:s?)""" => env.withScreenShot {
        env.waitUntil(s"Waiting for $element text after $seconds second(s)", seconds.toInt) {
          getElementText(element, env).length() > 0
        } 
      }
        
      case r"""I wait for (.+?)$element text""" => env.withScreenShot {
        env.waitUntil(s"Waiting for $element text") {
          getElementText(element, env).length() > 0
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
       
      case r"""I press enter in "?(.+?)"?$$$element""" => env.withScreenShot {
        locate(env, element).sendKeys(Keys.RETURN)
		bindAndWait(element, "enter", "true", env)
      }

      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$$$element""" => env.withScreenShot {
        sendKeys(element, action, value, env)
      }
        
      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" => env.withScreenShot {
        sendKeys(element, action, getAttribute(attribute, env), env)
      }
        
      case r"""I select the (\d+?)$position(st|nd|rd|th)$suffix option in (.+?)$$$element""" => env.withScreenShot {
        env.waitUntil(s"Selecting '${position}${suffix}' option in $element") {
          selectByIndex(element, position.toInt - 1, env)
		  true
		}
      }
        
      case r"""I select "(.*?)"$value in (.+?)$$$element""" => env.withScreenShot {
        env.waitUntil(s"Selecting '$value' in $element") {
          selectByVisibleText(element, value, env)
		  true
		}
      }
        
      case r"""I select (.+?)$attribute in (.+?)$$$element""" => env.withScreenShot {
	    getAttribute(attribute, env) tap { value => 
		  env.waitUntil(s"Selecting '$value' in $element") {
		    selectByVisibleText(element, value, env)
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
            bindAndWait(element, action, "true", env)
          }
          true
        }
      }
        
      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered)$$$event""" =>
        env.scopes.set(s"$element/${eventToAction(event)}/wait", duration)
        
      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered)$$$event""" =>
        env.scopes.set(s"$element/${eventToAction(event)}/condition", condition)
        
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
  
  private def getAttribute(name: String, env: WebEnvContext): String = 
    env.scopes.getOpt(name) match {
      case Some(value) => value
      case _ => env.scopes.getOpt(s"$name/text") match {
        case Some(value) => value
        case _ => env.scopes.getOpt(s"$name/javascript") match {
          case Some(javascript) => env.executeScript(s"return $javascript").asInstanceOf[String]
          case _ => env.scopes.get(name)
        }
      }
    } 
  
  private def compare(element: String, expression: String, actual: String, operator: String, negate: Boolean, env: WebEnvContext) = 
    (operator match {
      case "be"      => expression.equals(actual)
      case "contain" => actual.contains(expression)
      case "match regex" => actual.matches(expression)
      case "match xpath" => !evaluateXPath(expression, actual, env).isEmpty()
    }) tap { result =>
      if (!negate) assert(result, s"$element '$actual' should $operator '$expression'")
      else assert(!result, s"$element should not $operator '$expression'")
    } 
  
  private def getTitle(env: WebEnvContext): String = env.webDriver.getTitle() tap { title =>
    bindAndWait("page", "title", title, env)
  }
  
  private def getAttributeOrElementText(element: String, env: WebEnvContext): String = 
    Try(getAttribute(element, env)) match {
      case Success(text) => text
      case Failure(e1) => Try(getElementText(element, env)) match {
        case Success(text) => text
        case Failure(e2) => sys.error(s"${e1.getMessage()} and ${e2.getMessage()}")
      }
    }
  
  private def getElementText(element: String, env: WebEnvContext): String = 
    env.withWebElement(element) { webElement =>
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
    env.withWebElement(element) { webElement =>
      webElement.sendKeys(value)
      bindAndWait(element, "type", value, env)
      if (action == "enter") {
        webElement.sendKeys(Keys.RETURN)
	    bindAndWait(element, "enter", "true", env)
      }
    }
  }
  
  private def selectByVisibleText(element: String, value: String, env: WebEnvContext) {
    env.withWebElement(element) { webElement =>
      new Select(webElement).selectByVisibleText(value)
      bindAndWait(element, "select", value, env)
    }
  }
  
  private def selectByIndex(element: String, index: Int, env: WebEnvContext) {
    env.withWebElement(element) { webElement =>
      val select = new Select(webElement)
      select.selectByIndex(index)
      bindAndWait(element, "select", select.getFirstSelectedOption().getText(), env)
    }
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
	    env.executeScript(s"return $javascript").asInstanceOf[Boolean]
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
  
  private def withXPath[T](expression: String)(f: (XPath, String) => T): T = expression match {
    case r"""(.+?)$expr where (.+?)$$$namespaces""" =>
      val xPath = XPathFactory.newInstance().newXPath() tap { xPath =>
        xPath.setNamespaceContext(new NamespaceContext() { 
          def getNamespaceURI(prefix: String): String = {
            val mappings = namespaces.split(",").map(_.split("=")).map(pair => (pair(0).trim, pair(1).trim)).toMap
            return mappings.getOrElse(prefix, sys.error(s"Unknown namespace prefix: $prefix"));
          }
          def getPrefix(uri: String): String = null
          def getPrefixes(uri: String): Iterator[String] = null
        })
      }
      f(xPath, expr)
    case _ =>
      f(XPathFactory.newInstance().newXPath(), expression)  
  }
  
  private def evaluateXPath(xpath: String, source: String, env: WebEnvContext): String = 
    withXPath(xpath) { (xPath, expr) =>
      xPath.evaluate(expr, new InputSource(new StringReader(source))) 
    }
  
  private def evaluateRegex(regex: String, source: String, env: WebEnvContext): String = 
    regex.r.findFirstMatchIn(source).getOrElse(sys.error(s"'Regex match '$regex' not found in '$source'")).group(1)
  
}
