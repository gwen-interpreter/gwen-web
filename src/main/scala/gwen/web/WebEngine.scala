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
import gwen.eval.support.DefaultEngineSupport
import gwen.eval.support.RegexSupport
import gwen.eval.support.XPathSupport
import gwen.errors._
import gwen.dsl.Failed
import gwen.eval.support.DecodingSupport
import gwen.eval.support.DefaultEngineSupport
import gwen.dsl.Failed
import org.openqa.selenium.net.UrlChecker.TimeoutException
import org.openqa.selenium.By
import scala.util.Try
import scala.util.Failure
import org.openqa.selenium.interactions.Actions
import scala.concurrent.duration.Duration
import gwen.Predefs.Formatting._
import scala.util.Success
import java.io.FileNotFoundException

/**
  * A web engine that uses the Selenium web driver
  * API to automate various web operations.
  * 
  * @author Branko Juric, Brady Wood
  */
trait WebEngine extends EvalEngine[WebEnvContext] 
  with DefaultEngineSupport[WebEnvContext] 
  with DecodingSupport {
  
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
    
      case r"""I wait for (.+?)$element text for (.+?)$seconds second(?:s?)""" =>  {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.waitUntil(s"Waiting for $element text after $seconds second(s)", seconds.toInt) {
            env.waitForText(elementBinding)
          }
        } getOrElse {
          env.scopes.set(s"$element/text", "text")
        }
      }
        
      case r"""I wait for (.+?)$element text""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.waitUntil(s"Waiting for $element text") {
            env.waitForText(elementBinding)
          }
        } getOrElse {
          env.scopes.set(s"$element/text", "text")
        }
      }
        
      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" =>  {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.waitUntil(s"Waiting for $element after $seconds second(s)", seconds.toInt) {
            env.withWebElement(elementBinding) { _=> true }
          }
        }
      }
        
      case r"""I wait for (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.waitUntil(s"Waiting for $element") {
            env.withWebElement(elementBinding) { _=> true }
          }
        }
      }
      
      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered|tabbed|cleared)$$$event""" =>
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/wait", duration)
        
      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|submitted|checked|unchecked|selected|typed|entered|tabbed|cleared)$$$event""" =>
        env.scopes.get(s"$condition/javascript")
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/condition", condition)
        
      case r"""I wait until "(.+?)$javascript"""" => env.execute {
        env.waitUntil(s"Waiting until $javascript") {
          env.executeScriptPredicate(javascript)
        }
      }
        
      case r"""I wait until (.+?)$$$condition""" =>  {
        val javascript = env.scopes.get(s"$condition/javascript")
        env.execute {
          env.waitUntil(s"Waiting until $condition") {
            env.executeScriptPredicate(javascript)
          }
        }
      }
      
      case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using no delay and (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit timeout""" =>
        repeat(operation, step, doStep, condition, Duration.Zero, Duration(timeoutPeriod.toLong, timeoutUnit), env)
        
      case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using no delay""" =>
        repeat(operation, step, doStep, condition, Duration.Zero, defaultRepeatTimeout(DefaultRepeatDelay), env)
        
      case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$delayPeriod (second|millisecond)$delayUnit delay and (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit timeout""" =>
        repeat(operation, step, doStep, condition, Duration(delayPeriod.toLong, delayUnit), Duration(timeoutPeriod.toLong, timeoutUnit), env)
        
      case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$delayPeriod (second|millisecond)$delayUnit delay""" =>
        val delayDuration = Duration(delayPeriod.toLong, delayUnit)
        repeat(operation, step, doStep, condition, delayDuration, defaultRepeatTimeout(delayDuration), env)

      case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit timeout""" =>
        repeat(operation, step, doStep, condition, DefaultRepeatDelay, Duration(timeoutPeriod.toLong, timeoutUnit), env)
        
      case r"""(.+?)$doStep (until|while)$operation (.+?)$$$condition""" =>
        repeat(operation, step, doStep, condition, DefaultRepeatDelay, defaultRepeatTimeout(DefaultRepeatDelay), env)

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

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$$$container""" =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", locator);
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.set(s"$element/locator/$locator/container", container)
        
      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$$$expression""" =>
        env.scopes.set(s"$element/locator", locator);
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/container", null)
        }
        
      case r"""(.+?)$element can be (clicked|submitted|checked|unchecked)$event by javascript "(.+?)"$$$expression""" =>
        val elementBinding = env.getLocatorBinding(element)
        env.scopes.set(s"$element/action/${WebEvents.EventToAction(event)}/javascript", expression)

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator "(.*?)"$$$expression""" =>  env.execute {
        compare("title", expression, () => env.getTitle, operator, Option(negation).isDefined, env)
      }
        
      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" => {
        val expected = env.getAttribute(attribute)
        env.execute {
          compare("title", expected, () => env.getTitle, operator, Option(negation).isDefined, env)
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
        
      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator "(.*?)"$$$expression""" => {
        if (element == "I") undefinedStepError(step)
        val actual = boundAttributeOrSelection(element, selection, env)
        env.execute {
          compare(element + Option(selection).getOrElse(""), expression, actual, operator, Option(negation).isDefined, env)
        }
      }
        
      case r"""(.+?)$element( value| text)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" => {
        if (element == "I") undefinedStepError(step)
        val expected = env.getAttribute(attribute)
        val actual = boundAttributeOrSelection(element, selection, env)
        env.execute {
          compare(element + Option(selection).getOrElse(""), expected, actual, operator, Option(negation).isDefined, env)
        }
      }
      
      case r"""I capture the (text|node|nodeset)$targetType in (.+?)$source by xpath "(.+?)"$expression as (.+?)$$$name""" => {
        val src = env.getBoundReferenceValue(source)
        env.featureScope.set(name, env.execute(env.evaluateXPath(expression, src, env.XMLNodeType.withName(targetType)) tap { content => 
          env.addAttachment(name, "txt", content) 
        }).getOrElse(s"$$[xpath:$expression]"))
      }
      
      case r"""I capture the text in (.+?)$source by regex "(.+?)"$expression as (.+?)$$$name""" => {
        val src = env.getBoundReferenceValue(source)
        env.featureScope.set(name, env.execute(env.extractByRegex(expression, src) tap { content => 
          env.addAttachment(name, "txt", content) 
        }).getOrElse(s"$$[regex:$expression"))
      }
      
      case r"""I capture the content in (.+?)$source by json path "(.+?)"$expression as (.+?)$$$name""" => {
        val src = env.getBoundReferenceValue(source)
        env.featureScope.set(name, env.execute(env.evaluateJsonPath(expression, src) tap { content => 
          env.addAttachment(name, "txt", content) 
        }).getOrElse(s"$$[json path:$expression"))
      }
      
      case r"""I capture the current URL""" => 
        env.captureCurrentUrl()
      
      case r"""I capture the current URL as (.+?)$name""" => 
        env.featureScope.set(name, env.execute(env.withWebDriver(_.getCurrentUrl()) tap { content => 
          env.addAttachment(name, "txt", content) 
        }).getOrElse("$[currentUrl]"))
        
      case r"""I capture the current screenshot""" => 
        env.execute {
          env.captureScreenshot(true)
        }
        
      case r"""I capture (.+?)$element( value| text)?$selection as (.+?)$attribute""" =>
        val value = Option(selection) match {
          case None => env.getBoundReferenceValue(element)
          case Some(sel) => env.getElementSelection(element, sel)
        }
        env.featureScope.set(attribute, value tap { content => 
          env.addAttachment(attribute, "txt", content) 
        })
        
      case r"""I capture (.+?)$element( value| text)?$$$selection""" =>
        val value = Option(selection) match {
          case None => env.getBoundReferenceValue(element)
          case Some(sel) => env.getElementSelection(element, sel)
        }
        env.featureScope.set(element, value tap { content => 
          env.addAttachment(element, "txt", content) 
        })
        
      case r"""I base64 decode (.+?)$attribute as (.+?)$$$name""" => 
        val source = env.getBoundReferenceValue(attribute)
        env.featureScope.set(name, env.execute(decodeBase64(source) tap { content => 
          env.addAttachment(name, "txt", content) 
        }).getOrElse(s"$$[base64 decoded $attribute]"))
        
      case r"""I base64 decode (.+?)$attribute""" => 
        val source = env.getBoundReferenceValue(attribute)
        env.featureScope.set(attribute, env.execute(decodeBase64(source) tap { content => 
          env.addAttachment(attribute, "txt", content) 
        }).getOrElse(s"$$[base64 decoded $attribute]"))
        
      case r"""(.+?)$attribute (?:is|will be) defined by (javascript|system process|property|setting|file)$attrType "(.+?)"$$$expression""" =>
        attrType match {
          case "javascript" => env.scopes.set(s"$attribute/javascript", expression)
          case "system process" => env.scopes.set(s"$attribute/sysproc", expression)
          case "file" => env.scopes.set(s"$attribute/file", expression)
          case _ => env.featureScope.set(attribute, Settings.get(expression))
        }

      case r"""(.+?)$attribute (?:is|will be) defined by the (text|node|nodeset)$targetType in (.+?)$source by xpath "(.+?)"$$$expression""" =>
        env.scopes.set(s"$attribute/xpath/source", source)
        env.scopes.set(s"$attribute/xpath/targetType", targetType)
        env.scopes.set(s"$attribute/xpath/expression", expression)
      
      case r"""(.+?)$attribute (?:is|will be) defined in (.+?)$source by regex "(.+?)"$$$expression""" => 
        env.scopes.set(s"$attribute/regex/source", source)
        env.scopes.set(s"$attribute/regex/expression", expression)
        
      case r"""(.+?)$attribute (?:is|will be) defined in (.+?)$source by json path "(.+?)"$$$expression""" => 
        env.scopes.set(s"$attribute/json path/source", source)
        env.scopes.set(s"$attribute/json path/expression", expression)
      
      case r"""I clear (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element)
        env.execute {
          env.clearText(elementBinding)
        }
      }
      
      case r"""I press (enter|tab)$key in (.+?)$$$element""" => {
        val elementBinding = env.getLocatorBinding(element) 
        env.execute {
          env.withWebElement(elementBinding) { elem =>
            key match {
              case "enter" =>
                elem.sendKeys(Keys.RETURN)
              case _ =>
                elem.sendKeys(Keys.TAB)
            }
            env.bindAndWait(element, key, "true")
          }
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
        
      case r"""I (click|check|uncheck)$action (.+?)$element of (.+?)$$$context""" => {
        try {
          val contextBinding = env.getLocatorBinding(context)
          val elementBinding = env.getLocatorBinding(element)
          env.execute { 
            env.performActionIn(action, elementBinding, contextBinding)
          }
        } catch {
          case e1: LocatorBindingException =>
            try {
              val elementBinding = env.getLocatorBinding(s"$element of $context")
              env.execute { 
                env.performAction(action, elementBinding)
              }
            } catch {
              case e2: LocatorBindingException =>
                throw new LocatorBindingException(s"'$element', '$context', or '$element of $context'", s"${e1.getMessage}, ${e2.getMessage}")
            }
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
          env.withWebElement(elementBinding) { _ => }
        }
      }
      
      case "I refresh the current page" => env.execute { 
        env.withWebDriver { _.navigate().refresh() }
      }
      
      case r"I start a new browser" => env.execute {
        env.quit("primary")
        env.switchToSession("primary")
      }
      
      case r"""I start a browser for (.+?)$$$session""" => env.execute {
        env.quit(session)
        env.switchToSession(session)
      }
      
      case r"I close the(?: current)? browser" => env.execute {
        env.quit()
      }
      
      case r"""I close the browser for (.+?)$session""" => env.execute {
        env.quit(session)
      }
      
      case r"""I switch to the child window""" => env.execute {
        env.withWebDriver(env.switchToChild)
      }
      
      case r"""I close the child window""" => env.execute {
        env.closeChild()
      }
      
      case r"""I switch to the parent window""" => env.execute {
        env.switchToParent(false)
      }
      
      case r"""I switch to (.+?)$session""" => env.execute {
        env.switchToSession(session)
      }
      
      case r"I (accept|dismiss)$action the (?:alert|confirmation) popup" => env.execute {
        env.withWebDriver { driver =>
          if (action == "accept") {
            driver.switchTo().alert().accept()
          } else {
            driver.switchTo().alert().dismiss()
          }
        }
      }
        
      case _ => super.evaluate(step, env)
      
    }
  }
  
  private def boundAttributeOrSelection(element: String, selection: String, env: WebEnvContext): () => String = () => Option(selection) match {
    case None => env.getBoundReferenceValue(element)
    case Some(sel) => 
      try { 
        env.getBoundReferenceValue(element + sel)
      } catch {
        case _: UnboundAttributeException => env.getElementSelection(element, sel)
        case e: Throwable => throw e
      }
  }
  
  /**
    * Compares the actual value of an attribute with an expected value or condition.
    * 
    * @param name the name of the attribute being compared
    * @param expected the expected value, regex, xpath, or json path
    * @param actual the actual value of the element
    * @param operator the comparison operator
    * @param negate true to negate the result
    * @param env the web environment context
    * @return true if the actual value matches the expected value
    */
  private def compare(name: String, expected: String, actual: () => String, operator: String, negate: Boolean, env: WebEnvContext) = {
    var actualValue = ""
    val result = Try {
      env.waitUntil {
        actualValue = actual()
        if (actualValue != null) {
          val res = operator match {
            case "be"      => expected.equals(actualValue)
            case "contain" => actualValue.contains(expected)
            case "start with" => actualValue.startsWith(expected)
            case "end with" => actualValue.endsWith(expected)
            case "match regex" => actualValue.matches(expected)
            case "match xpath" => !env.evaluateXPath(expected, actualValue, env.XMLNodeType.text).isEmpty()
            case "match json path" => !env.evaluateJsonPath(expected, actualValue).isEmpty()
          }
          if (!negate) res else !res
        } else false
      }
    } match {
      case Success(_) => true
      case Failure(failure) => failure.getCause match {
        case _: FileNotFoundException => throw failure
        case _ => false
      }
    }
    assert(result, s"Expected $name to ${if(negate) "not " else ""}$operator '$expected' but got '$actualValue'")
  }
  
  /**
    * Performs a repeat until or while operation 
    */
  private def repeat(operation: String, step: Step, doStep: String, condition: String, delay: Duration, timeout: Duration, env: WebEnvContext) {
    assert(delay.gteq(Duration.Zero), "delay cannot be less than zero")
    assert(timeout.gt(Duration.Zero), "timeout must be greater than zero")
    assert(timeout.gteq(delay), "timeout cannot be less than or equal to delay")
    env.execute {
      var attempt = 0L
      env.waitUntil(s"Repeating $operation $condition", timeout.toSeconds) {
        attempt = attempt + 1
        operation match {
          case "until" =>
            logger.info(s"Repeat-until[$attempt]")
            evaluateStep(Step(step.keyword, doStep), env).evalStatus match {
              case Failed(_, e) => throw e
              case _ =>
                val javascript = env.scopes.get(s"$condition/javascript")
                env.executeScriptPredicate(javascript) tap { result =>
                  if (!result) {
                    logger.info(s"Repeat-until[$attempt] not completed, ..${if (delay.gt(Duration.Zero)) s"will try again in ${DurationFormatter.format(delay)}" else "trying again"}")
                    Thread.sleep(delay.toMillis)
                  } else {
                    logger.info(s"Repeat-until[$attempt] completed")
                  }
                }
            }
          case "while" =>
            val javascript = env.scopes.get(s"$condition/javascript")
            val result = env.executeScriptPredicate(javascript)
            if (result) {
              logger.info(s"Repeat-while[$attempt]")
              evaluateStep(Step(step.keyword, doStep), env).evalStatus match {
                case Failed(_, e) => throw e
                case _ => 
                  logger.info(s"Repeat-while[$attempt] not completed, ..${if (delay.gt(Duration.Zero)) s"will try again in ${DurationFormatter.format(delay)}" else "trying again"}")
                  Thread.sleep(delay.toMillis)
              }
            } else {
              logger.info(s"Repeat-while[$attempt] completed")
            }
            !result
        }
      }
    } getOrElse { 
      env.scopes.get(s"$condition/javascript")
      this.evaluateStep(Step(step.keyword, doStep), env).evalStatus match {
        case Failed(_, e) => throw e
        case _ => step
      }
    }
  }
  
  lazy val DefaultRepeatDelay: Duration = {
    val waitSecs = WebSettings.`gwen.web.wait.seconds` 
    if (waitSecs > 9 && waitSecs % 10 == 0) Duration(waitSecs / 10, "second") else Duration(waitSecs * 100, "millisecond")
  }
  
  private def defaultRepeatTimeout(delay: Duration): Duration = delay * 30
  
}

