/*
 * Copyright 2014-2017 Brady Wood, Branko Juric
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

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import gwen.Predefs.Kestrel
import gwen.dsl.Failed
import gwen.eval.{EnvContext, GwenOptions, ScopedDataStack}
import gwen.web.errors.{locatorBindingError, WaitTimeoutException}
import gwen.errors.{unboundAttributeError, UnboundAttributeException}

import scala.io.Source

/**
  * Manages the web context and environment bindings.
  *
  *  @author Branko Juric
  */
class WebEnvContext(val options: GwenOptions, val scopes: ScopedDataStack) extends EnvContext(options, scopes) {

   Try(logger.info(s"GWEN_CLASSPATH = ${sys.env("GWEN_CLASSPATH")}"))
   Try(logger.info(s"SELENIUM_HOME = ${sys.env("SELENIUM_HOME")}"))

  /** The web context. */
  val webContext = new WebContext(this)

   /** Resets the current context and closes the web browser. */
  override def reset() {
    super.reset()
    webContext.reset()
    close()
  }

  /** Closes the current web context. */
  override def close() {
    webContext.close()
    super.close()
  }

  /**
    * Add a list of error attachments which includes the current
    * screenshot and all current error attachments.
    *
    * @param failure the failed status
    */
  override def addErrorAttachments(failure: Failed): Unit = {
    super.addErrorAttachments(failure)
    webContext.captureScreenshot(true)
  }

  /**
    * Gets a bound value from memory. A search for the value is made in 
    * the following order and the first value found is returned:
    *  - Web element text on the current page
    *  - Currently active page scope
    *  - The global feature scope
    *  - Settings
    *  
    * @param name the name of the bound value to find
    */
  override def getBoundReferenceValue(name: String): String = {
    if (name == "the current URL") webContext.captureCurrentUrl(Some(name))
    (getLocatorBinding(name, optional = true) match {
      case Some(binding) =>
        Try(webContext.getElementText(binding)) match {
          case Success(text) => text.getOrElse(getAttribute(name))
          case Failure(e) => throw e
        }
      case _ => getAttribute(name)
    }) tap { value =>
      logger.debug(s"getBoundReferenceValue($name)='$value'")
    }
  }

  /**
    * Resolves a bound attribute value from the visible scope.
    *  
    * @param name the name of the bound attribute to find
    */
  def getAttribute(name: String): String = {
    Try(super.getBoundReferenceValue(name)) match {
      case Success(value) => value
      case Failure(e) => e match {
        case _: UnboundAttributeException =>
          Try(getLocatorBinding(name).locators.map(_.expression).mkString(",")).getOrElse(unboundAttributeError(name))
        case _ => throw e
      }
    }
  }
  
  def boundAttributeOrSelection(element: String, selection: Option[String]): () => String = () => selection match {
    case None => getBoundReferenceValue(element)
    case Some(sel) => 
      try { 
        getBoundReferenceValue(element + sel)
      } catch {
        case _: UnboundAttributeException => webContext.getElementSelection(element, sel)
        case e: Throwable => throw e
      }
  }

  /**
    * Gets a web element binding.
    *
    * @param element the name of the web element
    */
  def getLocatorBinding(element: String): LocatorBinding = getLocatorBinding(element, optional = false).get
  
  /**
   * Gets a web element binding.
   * 
   * @param element the name of the web element
   * @param optional true to return None if not found; false to throw error
   */
  def getLocatorBinding(element: String, optional: Boolean): Option[LocatorBinding] = {
    featureScope.getObject(element) match {
      case None =>
        val locatorBinding = s"$element/locator"
        scopes.getOpt(locatorBinding) match {
          case Some(boundValue) =>
            val locators = boundValue.split(",") flatMap { locatorType =>
              if (!locatorType.matches("(id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)"))
                locatorBindingError(element, s"unsupported locator type: $locatorType")
              val lookupBinding = interpolate(s"$element/locator/$locatorType")(getBoundReferenceValue)
              scopes.getOpt(lookupBinding) match {
                case Some(expression) =>
                  val expr = interpolate(expression)(getBoundReferenceValue)
                  val container: Option[String] = scopes.getOpt(interpolate(s"$element/locator/$locatorType/container")(getBoundReferenceValue))
                  if (isDryRun) {
                    container.foreach(c => getLocatorBinding(c, optional))
                  }
                  Some(Locator(locatorType, expr, container))
                case None =>
                  if (optional) None else locatorBindingError(element, s"locator lookup binding not found: $lookupBinding")
              }
            }
            if (locators.nonEmpty) Some(LocatorBinding(element, locators.toList))
            else None
          case None => if (optional) None else locatorBindingError(element, s"locator binding not found: $locatorBinding")
        }
      case _ => Some(LocatorBinding(element, "cache", element, None))
    }
  } tap { binding =>
      binding foreach { b => logger.debug(s"getLocatorBinding($element,$optional)='$b'") }
  }
  
  /**
    * Binds the given element and value to a given action (element/action=value)
    * and then waits for any bound post conditions to be satisfied.
    * 
    * @param element the element to bind the value to
    * @param action the action to bind the value to
    * @param value the value to bind
    */
  def bindAndWait(element: String, action: String, value: String) {
    scopes.set(s"$element/$action", value)
    
    // sleep if wait time is configured for this action
    scopes.getOpt(s"$element/$action/wait") foreach { secs => 
      logger.info(s"Waiting for $secs second(s) (post-$action wait)")
      Thread.sleep(secs.toLong * 1000)
    }
    
    // wait for javascript post condition if one is configured for this action
    scopes.getOpt(s"$element/$action/condition") foreach { condition =>
      val javascript = scopes.get(s"$condition/javascript")
      logger.debug(s"Waiting for script to return true: $javascript")
      webContext.waitUntil(s"wait-until $condition (post-$action condition)") {
        evaluateJSPredicate(javascript)
      }
    }
  }

  /**
    * Gets the actual value of an attribute and compares it with an expected value or condition.
    * 
    * @param name the name of the attribute being compared
    * @param expected the expected value, regex, xpath, or json path
    * @param actual the actual value of the element
    * @param operator the comparison operator
    * @param negate true to negate the result
    * @return true if the actual value matches the expected value
    */
  def compare(name: String, expected: String, actual: () => String, operator: String, negate: Boolean): Unit = {
    var result = false
    var actualValue = actual()
    try {
      webContext.waitUntil {
        result = if (actualValue != null) {
          super.compare(expected, actualValue, operator, negate)
        } else false
        result tap { r =>
          if (!r) actualValue = actual()
        }
      }
    } catch {
      case _: WaitTimeoutException => result = false
    }
    assert(result, s"Expected $name to ${if(negate) "not " else ""}$operator '$expected' but got '$actualValue'")
  }
  
  /**
   * Adds web engine dsl steps to super implementation. The entries 
   * returned by this method are used for tab completion in the REPL.
   */
  override def dsl: List[String] = 
    Source.fromInputStream(getClass.getResourceAsStream("/gwen-web.dsl")).getLines().toList ++ super.dsl

  /**
    * Appends a return keyword in front of the given javascript expression in preparation for execute-with-return
    * (since web driver requires return prefix).
    *
    * @param javascript the javascript function
    */
  override def formatJSReturn(javascript: String) = s"return $javascript"

  /**
    * Executes a javascript expression on the current page through the web driver.
    *
    * @param javascript the script expression to execute
    * @param params optional parameters to the script
    */
  override def evaluateJS(javascript: String, params: Any*): Any =
    webContext.executeJS(javascript, params.map(_.asInstanceOf[AnyRef]) : _*)
  
}
