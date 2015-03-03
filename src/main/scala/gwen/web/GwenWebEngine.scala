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

import java.io.StringReader
import java.io.StringWriter
import java.util.Iterator
import org.openqa.selenium.Keys
import org.openqa.selenium.support.ui.Select
import org.xml.sax.InputSource
import gwen.Predefs.Kestrel
import gwen.Predefs.RegexContext
import gwen.dsl.Step
import gwen.eval.EvalEngine
import gwen.eval.GwenOptions
import gwen.eval.ScopedDataStack
import gwen.gwenSetting
import javax.xml.namespace.NamespaceContext
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathFactory
import javax.xml.xpath.XPathConstants
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Node
import org.w3c.dom.NodeList


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
        compare("title", env.getAttribute(attribute), getTitle(env), operator, Option(negation).isDefined, env) 
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
        evaluateXPath(expression, env.getBoundValue(source), targetType, env) tap { value =>
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
        env.featureScope.set(attribute, env.getElementText(element))
      }
        
      case r"""I capture (.+?)$element""" => env.withScreenShot {
        env.getElementText(element)
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
          env.getElementText(element).length() > 0
        } 
      }
        
      case r"""I wait for (.+?)$element text""" => env.withScreenShot {
        env.waitUntil(s"Waiting for $element text") {
          env.getElementText(element).length() > 0
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
        sendKeys(element, action, value, env)
      }
        
      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" => env.withScreenShot {
        sendKeys(element, action, env.getAttribute(attribute), env)
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
	    env.getAttribute(attribute) tap { value => 
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
            env.bindAndWait(element, action, "true")
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
  
  private def compare(element: String, expression: String, actual: String, operator: String, negate: Boolean, env: WebEnvContext) = 
    (operator match {
      case "be"      => expression.equals(actual)
      case "contain" => actual.contains(expression)
      case "match regex" => actual.matches(expression)
      case "match xpath" => !evaluateXPath(expression, actual, "text", env).isEmpty()
    }) tap { result =>
      if (!negate) assert(result, s"$element '$actual' should $operator '$expression'")
      else assert(!result, s"$element should not $operator '$expression'")
    } 
  
  private def getTitle(env: WebEnvContext): String = env.webDriver.getTitle() tap { title =>
    env.bindAndWait("page", "title", title)
  }
  
  private def sendKeys(element: String, action: String, value: String, env: WebEnvContext) {
    env.withWebElement(element) { webElement =>
      webElement.sendKeys(value)
      env.bindAndWait(element, "type", value)
      if (action == "enter") {
        webElement.sendKeys(Keys.RETURN)
	    env.bindAndWait(element, "enter", "true")
      }
    }
  }
  
  private def selectByVisibleText(element: String, value: String, env: WebEnvContext) {
    env.withWebElement(element) { webElement =>
      new Select(webElement).selectByVisibleText(value)
      env.bindAndWait(element, "select", value)
    }
  }
  
  private def selectByIndex(element: String, index: Int, env: WebEnvContext) {
    env.withWebElement(element) { webElement =>
      val select = new Select(webElement)
      select.selectByIndex(index)
      env.bindAndWait(element, "select", select.getFirstSelectedOption().getText())
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
  
  private def evaluateXPath(xpath: String, source: String, targetType: String, env: WebEnvContext): String = 
    withXPath(xpath) { (xPath, expr) =>
      val qname = targetType match {
        case "text" => XPathConstants.STRING
        case "node" => XPathConstants.NODE
        case "nodeset" => XPathConstants.NODESET
        case _ => sys.error(s"Unsupported target XPath output type: $targetType (valid values are text|node|nodeset)")
      }
      val result = xPath.compile(expr).evaluate(new InputSource(new StringReader(source)), qname)
      targetType match {
        case "text" => result.toString
        case "node" => nodeToString(result.asInstanceOf[Node])
        case "nodeset" => nodeListToString(result.asInstanceOf[NodeList])
      }
    }
  
  private def nodeToString(node: Node): String = {
    val sw = new StringWriter()
    val t = TransformerFactory.newInstance().newTransformer()
    t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    t.setOutputProperty(OutputKeys.INDENT, "yes")
    t.transform(new DOMSource(node), new StreamResult(sw))
    sw.toString
  }
  
  private def nodeListToString(nodeList: NodeList): String = {
    val result = for(i <- 0 to nodeList.getLength) yield nodeToString(nodeList.item(i))
    result.mkString(sys.props("line.separator"))
  }
  
  private def evaluateRegex(regex: String, source: String): String =  
    regex.r.findFirstMatchIn(source).getOrElse(sys.error(s"'Regex match '$regex' not found in '$source'")).group(1)
  
}
