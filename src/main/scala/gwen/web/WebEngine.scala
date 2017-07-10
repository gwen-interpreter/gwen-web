/*
 * Copyright 2014-2017 Branko Juric, Brady Wood
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

import scala.concurrent.duration.Duration
import gwen.Predefs.Formatting.DurationFormatter
import gwen.Predefs.Kestrel
import gwen.Predefs.RegexContext
import gwen.Settings
import gwen.dsl._
import gwen.errors.undefinedStepError
import gwen.eval.GwenOptions
import gwen.eval.ScopedDataStack
import gwen.eval.support.DefaultEngineSupport

import scala.util.Try

/**
  * A web engine that uses the Selenium web driver
  * API to automate various web operations.
  * 
  * @author Branko Juric, Brady Wood
  */
trait WebEngine extends DefaultEngineSupport[WebEnvContext] {
  
  /**
    * Initialises and returns a new web environment context.
    * 
    * @param options command line options
    * @param scopes initial data scopes
    */
  override def init(options: GwenOptions, scopes: ScopedDataStack) = new WebEnvContext(options, scopes)

  /**
    * Evaluates priority steps supported by this engine. For example, a step that calls another step needs to execute
    * with priority to ensure that there is no match conflict between the two (which can occur if the step being
    * called by a step is a StepDef or another step that matches the entire calling step)
    *
    * @param step the step to evaluate
    * @param env the environment context
    */
  override def evaluatePriority(step: Step, env: WebEnvContext): Option[Step] = {

    val webContext = env.webContext

    Option {

      step.expression match {

        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$$$container""" =>
          env.getLocatorBinding(container)
          val binding = LocatorBinding(s"${element}/list", locator, expression, Some(container))
          env.evaluate(foreach(() => List("dryRun[webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$$$expression""" =>
          val binding = LocatorBinding(s"${element}/list", locator, expression, None)
          env.evaluate(foreach(() => List("dryRun[webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep for each (.+?)$element in (.+?)$$$iteration""" =>
          val binding = env.getLocatorBinding(iteration)
          env.evaluate(foreach(() => List("dryRun[webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
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

        case r"""(.+?)$doStep (until|while)$operation (.+?)$$$condition""" if doStep != "I wait" =>
          repeat(operation, step, doStep, condition, DefaultRepeatDelay, defaultRepeatTimeout(DefaultRepeatDelay), env)

        case _ =>
          super.evaluatePriority(step, env).orNull

      }
    }
  }

  /**
    * Evaluates a given step.  This method matches the incoming step against a
    * set of supported steps and evaluates only those that are successfully
    * matched.
    *
    * @param step the step to evaluate
    * @param env the web environment context
    */
  override def evaluate(step: Step, env: WebEnvContext): Unit = {

    val webContext = env.webContext

    step.expression match {

      case r"""I wait for (.+?)$element text for (.+?)$seconds second(?:s?)""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(s"Waiting for $element text after $seconds second(s)", seconds.toInt) {
          webContext.waitForText(elementBinding)
        }

      case r"""I wait for (.+?)$element text""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(s"Waiting for $element text") {
          webContext.waitForText(elementBinding)
        }

      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(s"Waiting for $element after $seconds second(s)", seconds.toInt) {
          Try(webContext.locate(elementBinding)).isSuccess
        }

      case r"""I wait for (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(s"Waiting for $element") {
          Try(webContext.locate(elementBinding)).isSuccess
        }

      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|submitted|checked|ticked|unchecked|unticked|selected|typed|entered|tabbed|cleared)$$$event""" =>
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/wait", duration)

        case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|submitted|checked|ticked|unchecked|unticked|selected|typed|entered|tabbed|cleared)$$$event""" =>
        env.scopes.get(s"$condition/javascript")
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/condition", condition)

      case r"""I wait until "(.+?)$javascript"""" =>
        webContext.waitUntil(s"Waiting until $javascript") {
          env.evaluateJSPredicate(javascript)
        }

      case r"""I wait until (.+?)$$$condition""" =>
        val javascript = env.scopes.get(s"$condition/javascript")
        webContext.waitUntil(s"Waiting until $condition") {
          env.evaluateJSPredicate(javascript)
        }

      case r"""I am on the (.+?)$$$page""" =>
        env.scopes.addScope(page)

      case r"""I navigate to the (.+?)$$$page""" =>
        env.scopes.addScope(page)
        val url = env.getAttribute("url")
        webContext.navigateTo(url)

      case r"""I navigate to "(.+?)"$$$url""" =>
        env.scopes.addScope(url)
        webContext.navigateTo(url)

      case r"""I scroll to the (top|bottom)$position of (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.scrollIntoView(elementBinding, ScrollTo.withName(position))

      case r"""the url will be defined by (?:property|setting) "(.+?)"$$$name""" =>
        env.scopes.set("url", Settings.get(name))

      case r"""the url will be "(.+?)"$$$url""" =>
        env.scopes.set("url", url)

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$$$container""" =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.set(s"$element/locator/$locator/container", container)

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$$$expression""" =>
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/container", null)
        }

      case r"""(.+?)$element can be (clicked|right clicked|submitted|checked|ticked|unchecked|unticked)$event by javascript "(.+?)"$$$expression""" =>
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/action/${WebEvents.EventToAction(event)}/javascript", expression)

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator "(.*?)"$$$expression""" =>
        env.perform {
          env.compare("title", expression, () => webContext.getTitle, operator, Option(negation).isDefined)
        }

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" =>
        val expected = env.getAttribute(attribute)
        env.perform {
          env.compare("title", expected, () => webContext.getTitle, operator, Option(negation).isDefined)
        }

      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.checkElementState(elementBinding, state, Option(negation).nonEmpty)

      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator "(.*?)"$$$expression""" =>
        if (element == "I") undefinedStepError(step)
        val actual = env.boundAttributeOrSelection(element, Option(selection))
        env.perform {
          env.compare(element + Option(selection).getOrElse(""), expression, actual, operator, Option(negation).isDefined)
        }

      case r"""(.+?)$element( value| text)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" if(attribute != "absent") =>
        if (element == "I") undefinedStepError(step)
        val expected = env.getAttribute(attribute)
        val actual = env.boundAttributeOrSelection(element, Option(selection))
        env.perform {
          env.compare(element + Option(selection).getOrElse(""), expected, actual, operator, Option(negation).isDefined)
        }

      case r"""I capture the current URL""" =>
        webContext.captureCurrentUrl(None)

      case r"""I capture the current URL as (.+?)$name""" =>
        webContext.captureCurrentUrl(Some(name))

      case r"""I capture the current screenshot""" =>
        webContext.captureScreenshot(true)

      case r"""I capture (.+?)$element( value| text)$selection as (.+?)$attribute""" =>
        val value = webContext.getElementSelection(element, selection)
        env.featureScope.set(attribute, value tap { content =>
          env.addAttachment(attribute, "txt", content)
        })

      case r"""I capture (.+?)$element( value| text)$$$selection""" =>
        val value = webContext.getElementSelection(element, selection)
        env.featureScope.set(element, value tap { content =>
          env.addAttachment(element, "txt", content)
        })

      case r"""I clear (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.clearText(elementBinding)

      case r"""I press (enter|tab)$key in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendKeys(elementBinding, Array[String](key))
        env.bindAndWait(element, key, "true")

      case r"""I send "(.+?)"$keys to (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendKeys(elementBinding, keys.split(","))

      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendValue(elementBinding, value, clearFirst = true, sendEnterKey = action == "enter")

      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        val value = env.getAttribute(attribute)
        webContext.sendValue(elementBinding, value, clearFirst = true, sendEnterKey = action == "enter")

      case r"""I select the (\d+?)$position(?:st|nd|rd|th) option in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.selectByIndex(elementBinding, position.toInt - 1)

      case r"""I select "(.*?)"$value in (.+?)$element by value""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.selectByValue(elementBinding, value)

      case r"""I select "(.*?)"$value in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.selectByVisibleText(elementBinding, value)

      case r"""I select (.+?)$attribute in (.+?)$element by value""" =>
        val value = env.getAttribute(attribute)
        val elementBinding = env.getLocatorBinding(element)
        webContext.selectByValue(elementBinding, value)

      case r"""I select (.+?)$attribute in (.+?)$$$element""" =>
        val value = env.getAttribute(attribute)
        val elementBinding = env.getLocatorBinding(element)
        webContext.selectByVisibleText(elementBinding, value)

      case r"""I (click|right click|check|tick|uncheck|untick)$action (.+?)$element of (.+?)$$$context""" =>
        webContext.performActionInContext(action, element, context)

      case r"""I (click|right click|submit|check|tick|uncheck|untick)$action (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.performAction(action, elementBinding)

      case r"""I (.+?)$modifiers (click|right click)$clickAction (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.holdAndClick(modifiers.split("\\+"), clickAction, elementBinding)

      case r"""I (?:highlight|locate) (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        env.perform {
          webContext.locate(elementBinding)
        }

      case "I refresh the current page" =>
        webContext.refreshPage()

      case r"I start a new browser" =>
        webContext.close("primary")
        webContext.switchToSession("primary")

      case r"""I start a browser for (.+?)$$$session""" =>
        webContext.close(session)
        webContext.switchToSession(session)

      case r"I close the(?: current)? browser" =>
        webContext.close()

      case r"""I close the browser for (.+?)$session""" =>
        webContext.close(session)

      case r"""I switch to the child (?:window|tab)""" =>
        webContext.switchToChild()

      case r"""I close the child (?:window|tab)""" =>
        webContext.closeChild()

      case r"""I switch to the parent (?:window|tab)""" =>
        webContext.switchToParent(false)

      case r"""I switch to (.+?)$session""" =>
        webContext.switchToSession(session)

      case r"I (accept|dismiss)$action the (?:alert|confirmation) popup" =>
        webContext.handleAlert(action == "accept")

      case r"""I resize the window to width (\d+?)$width and height (\d+?)$$$height""" =>
        webContext.resizeWindow(width.toInt, height.toInt)

      case r"""I maximi(?:z|s)e the window""" =>
        webContext.maximizeWindow()

      case _ => super.evaluate(step, env)

    }
  }
  
  /**
    * Performs a repeat until or while operation 
    */
  private def repeat(operation: String, step: Step, doStep: String, condition: String, delay: Duration, timeout: Duration, env: WebEnvContext): Step = {
    assert(delay.gteq(Duration.Zero), "delay cannot be less than zero")
    assert(timeout.gt(Duration.Zero), "timeout must be greater than zero")
    assert(timeout.gteq(delay), "timeout cannot be less than or equal to delay")
    env.perform {
      var attempt = 0L
      env.webContext.waitUntil(s"Repeating $operation $condition", timeout.toSeconds) {
        attempt = attempt + 1
        operation match {
          case "until" =>
            logger.info(s"Repeat-until[$attempt]")
            evaluateStep(Step(step.keyword, doStep), env).evalStatus match {
              case Failed(_, e) => throw e
              case _ =>
                val javascript = env.scopes.get(s"$condition/javascript")
                env.evaluateJSPredicate(javascript) tap { result =>
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
            val result = env.evaluateJSPredicate(javascript)
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
      this.evaluateStep(Step(step.keyword, doStep), env)
    }
    step
  }
  
  lazy val DefaultRepeatDelay: Duration = {
    val waitSecs = WebSettings.`gwen.web.wait.seconds` 
    if (waitSecs > 9 && waitSecs % 10 == 0) Duration(waitSecs / 10, "second") else Duration(waitSecs * 100, "millisecond")
  }
  
  private def defaultRepeatTimeout(delay: Duration): Duration = delay * 30
  
}

