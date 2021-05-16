/*
 * Copyright 2014-2020 Branko Juric, Brady Wood
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

import gwen._
import gwen.dsl._
import gwen.Errors.StepFailure
import gwen.Errors.undefinedStepError
import gwen.Errors.disabledStepError
import gwen.Formatting.DurationFormatter
import gwen.eval.GwenOptions
import gwen.eval.support.DefaultEngineSupport
import gwen.web.Errors.LocatorBindingException

import com.applitools.eyes.{MatchLevel, RectangleSize}
import org.apache.commons.text.StringEscapeUtils

import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

import java.util.concurrent.TimeUnit

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
    */
  override def init(options: GwenOptions) = {
    if (WebSettings.`gwen.web.capture.screenshots.highlighting`) {
      val fps = GwenSettings.`gwen.report.slideshow.framespersecond`
      Settings.setLocal("gwen.report.slideshow.framespersecond", (fps.toDouble * 1.8d).toInt.toString)
    }
    new WebEnvContext(options)
  }

  /**
    * Evaluates priority steps supported by this engine. For example, a step that calls another step needs to execute
    * with priority to ensure that there is no match conflict between the two (which can occur if the step being
    * called by a step is a StepDef or another step that matches the entire calling step)
    */
  override def evaluatePriority(parent: Identifiable, step: Step, env: WebEnvContext): Option[Step] = {

    super.evaluatePriority(parent, step, env) orElse {

      Option {
      
        val webContext = env.webContext

        step.expression match {

          case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression in (.+?)$container with no (?:timeout|wait)""" => 
            val containerBinding = env.getLocatorBinding(container)
            val binding = LocatorBinding(s"${element}/list", Locator.parse(locator), expression, Some(containerBinding), Some(Duration.Zero), None)
            env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, parent, step, doStep, env)) {
              foreach(() => webContext.locateAll(binding), element, parent, step, doStep, env)
            }

          case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" =>
            val containerBinding = env.getLocatorBinding(container)
            val binding = LocatorBinding(s"${element}/list", Locator.parse(locator), expression, Some(containerBinding), Some(Duration.create(timeout.toLong, TimeUnit.SECONDS)), None)
            env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, parent, step, doStep, env)) {
              foreach(() => webContext.locateAll(binding), element, parent, step, doStep, env)
            }

          case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression in (.+?)$container""" =>
            val containerBinding = env.getLocatorBinding(container)
            val binding = LocatorBinding(s"${element}/list", Locator.parse(locator), expression, Some(containerBinding), None, None)
            env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, parent, step, doStep, env)) {
              foreach(() => webContext.locateAll(binding), element, parent, step, doStep, env)
            }

          case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression with no (?:timeout|wait)""" =>
            val binding = LocatorBinding(s"${element}/list", Locator.parse(locator), expression, None, Some(Duration.Zero), None)
            env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, parent, step, doStep, env)) {
              foreach(() => webContext.locateAll(binding), element, parent, step, doStep, env)
            }

          case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression with (\d+)$timeout second (?:timeout|wait)""" =>
            val binding = LocatorBinding(s"${element}/list", Locator.parse(locator), step.orDocString(expression), None, Some(Duration.create(timeout.toLong, TimeUnit.SECONDS)), None)
            env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, parent, step, doStep, env)) {
              foreach(() => webContext.locateAll(binding), element, parent, step, doStep, env)
            }

          case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression""" =>
            val binding = LocatorBinding(s"${element}/list", Locator.parse(locator), step.orDocString(expression), None, None, None)
            env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, parent, step, doStep, env)) {
              foreach(() => webContext.locateAll(binding), element, parent, step, doStep, env)
            }

          case r"""(.+?)$doStep for each (.+?)$element in (.+?)$$$iteration""" if !iteration.contains("delimited by") =>
            val binding = env.getLocatorBinding(iteration)
            env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, parent, step, doStep, env)) {
              foreach(() => webContext.locateAll(binding), element, parent, step, doStep, env)
            }

          case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using no delay and (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit (?:timeout|wait)""" =>
            repeat(operation, parent, step, doStep, condition, Duration.Zero, Duration(timeoutPeriod.toLong, timeoutUnit), env)

          case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using no delay""" =>
            repeat(operation, parent, step, doStep, condition, Duration.Zero, defaultRepeatTimeout(DefaultRepeatDelay), env)

          case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$delayPeriod (second|millisecond)$delayUnit delay and (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit (?:timeout|wait)""" =>
            repeat(operation, parent, step, doStep, condition, Duration(delayPeriod.toLong, delayUnit), Duration(timeoutPeriod.toLong, timeoutUnit), env)

          case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$delayPeriod (second|millisecond)$delayUnit delay""" =>
            val delayDuration = Duration(delayPeriod.toLong, delayUnit)
            repeat(operation, parent, step, doStep, condition, delayDuration, defaultRepeatTimeout(delayDuration), env)

          case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit (?:timeout|wait)""" =>
            repeat(operation, parent, step, doStep, condition, DefaultRepeatDelay, Duration(timeoutPeriod.toLong, timeoutUnit), env)

          case r"""(.+?)$doStep (until|while)$operation (.+?)$$$condition""" if (doStep != "I wait" && !step.expression.matches(""".*".*(until|while).*".*""")) =>
            repeat(operation, parent, step, doStep, condition, DefaultRepeatDelay, defaultRepeatTimeout(DefaultRepeatDelay), env)

          case _ => null
        }
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
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(seconds.toInt, s"waiting for text of $elementBinding") {
          webContext.waitForText(elementBinding)
        }

      case r"""I wait for (.+?)$element text""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(s"waiting for text of $elementBinding") {
          webContext.waitForText(elementBinding)
        }

      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(seconds.toInt, s"waiting for $elementBinding to be displayed") {
          Try(webContext.locateAndHighlight(elementBinding)).isSuccess
        }

      case r"""I wait for (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(s"waiting for $elementBinding to be displayed") {
          Try(webContext.locateAndHighlight(elementBinding)).isSuccess
        }

      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$$$event""" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/wait", duration)

      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|right clicked|double clicked||submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$$$event""" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.scopes.get(s"$condition/javascript")
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/condition", condition)

      case r"""I wait until "(.+?)$javascript"""" => step.orDocString(javascript) tap { javascript =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.waitUntil(s"waiting for true return from javascript: $javascript") {
          env.evaluateJSPredicate(javascript)
        }
      }

      case r"""I wait until (.+?)$element is( not)?$negation (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" if env.scopes.getOpt(s"$element is${if(Option(negation).isDefined) " not" else ""} $state/javascript").isEmpty =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element).jsEquivalent
        val negate = Option(negation).isDefined
        webContext.waitForElementState(elementBinding, state, negate)

      case r"""I wait until (.+?)$$$condition""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val javascript = env.scopes.get(s"$condition/javascript")
        webContext.waitUntil(s"waiting for true return from javascript: $javascript") {
          env.evaluateJSPredicate(javascript)
        }

      case r"""I am on the (.+?)$$$page""" =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.addScope(page)

      case r"""I navigate to the (.+?)$$$page""" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.scopes.addScope(page)
        val url = env.getAttribute("url")
        webContext.navigateTo(url)

      case r"""I navigate to "(.+?)"$$$url""" => step.orDocString(url) tap { url =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.navigateTo(url)
      }

      case r"""I scroll to the (top|bottom)$position of (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.scrollIntoView(elementBinding, ScrollTo.withName(position))

      case r"""the url will be defined by (?:property|setting) "(.+?)"$$$name""" => step.orDocString(name) tap { name =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set("url", Settings.get(name))
      }

      case r"""the url will be "(.+?)"$$$url""" => step.orDocString(url) tap { url =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set("url", url)
      }

      case r"""the (.+?)$page url is "(.+?)"$$$url""" => step.orDocString(url) tap { url =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.addScope(page)
        env.scopes.set("url", url)
      }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression at index (\d+)$index in (.+?)$container with no (?:timeout|wait)""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.set(s"$element/locator/$loc/container", container)
        env.scopes.set(s"$element/locator/$loc/timeoutSecs", "0")
        env.scopes.set(s"$element/locator/$loc/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression in (.+?)$container with no (?:timeout|wait)""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.set(s"$element/locator/$loc/container", container)
        env.scopes.set(s"$element/locator/$loc/timeoutSecs", "0")
        env.scopes.getOpt(s"$element/locator/$loc/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression at index (\d+)$index in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.set(s"$element/locator/$loc/container", container)
        env.scopes.set(s"$element/locator/$loc/timeoutSecs", timeout)
        env.scopes.set(s"$element/locator/$loc/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.set(s"$element/locator/$loc/container", container)
        env.scopes.set(s"$element/locator/$loc/timeoutSecs", timeout)
        env.scopes.getOpt(s"$element/locator/$loc/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression at index (\d+)$index in (.+?)$container""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.set(s"$element/locator/$loc/container", container)
        env.scopes.getOpt(s"$element/locator/$loc/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/timeoutSecs", null)
        }
        env.scopes.set(s"$element/locator/$loc/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression in (.+?)$container""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.set(s"$element/locator/$loc/container", container)
        env.scopes.getOpt(s"$element/locator/$loc/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/timeoutSecs", null)
        }
        env.scopes.getOpt(s"$element/locator/$loc/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression at index (\d+)$index with no (?:timeout|wait)""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.getOpt(s"$element/locator/$loc/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/container", null)
        }
        env.scopes.set(s"$element/locator/$loc/timeoutSecs", "0")
        env.scopes.set(s"$element/locator/$loc/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression with no (?:timeout|wait)""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.getOpt(s"$element/locator/$loc/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/container", null)
        }
        env.scopes.set(s"$element/locator/$loc/timeoutSecs", "0")
        env.scopes.getOpt(s"$element/locator/$loc/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression at index (\d+)$index with (\d+)$timeout second (?:timeout|wait)""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.getOpt(s"$element/locator/$loc/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/container", null)
        }
        env.scopes.set(s"$element/locator/$loc/timeoutSecs", timeout)
        env.scopes.set(s"$element/locator/$loc/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression with (\d+)$timeout second (?:timeout|wait)""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.getOpt(s"$element/locator/$loc/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/container", null)
        }
        env.scopes.set(s"$element/locator/$loc/timeoutSecs", timeout)
        env.scopes.getOpt(s"$element/locator/$loc/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression at index (\d+)$index""" =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.getOpt(s"$element/locator/$loc/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/container", null)
        }
        env.scopes.getOpt(s"$element/locator/$loc/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/timeoutSecs", null)
        }
        env.scopes.set(s"$element/locator/$loc/index", index)

      case r"""(.+?)$element can be located at index (\d+)$index by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression""" => step.orDocString(expression) tap { expression =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.getOpt(s"$element/locator/$loc/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/container", null)
        }
        env.scopes.getOpt(s"$element/locator/$loc/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/timeoutSecs", null)
        }
        env.scopes.set(s"$element/locator/$loc/index", index)
      }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$locator "(.+?)"$expression""" => step.orDocString(expression) tap { expression =>
        val loc = Locator.parse(locator)
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", loc)
        env.scopes.set(s"$element/locator/$loc", expression)
        env.scopes.getOpt(s"$element/locator/$loc/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/container", null)
        }
        env.scopes.getOpt(s"$element/locator/$loc/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/timeoutSecs", null)
        }
        env.scopes.getOpt(s"$element/locator/$loc/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$loc/index", null)
        }
      }

      case r"""(.+?)$element can be located at index (\d+)$index in (.+?)$container by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = Locator.parse(row.head)
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.set(s"$element/locator/$locator/container", container)
          env.scopes.set(s"$element/locator/$locator/index", index)
        }

      case r"""(.+?)$element can be located in (.+?)$container by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = Locator.parse(row.head)
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.set(s"$element/locator/$locator/container", container)
          env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/index", null)
          }
        }

      case r"""(.+?)$element can be located at index (\d+)$index with no (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => {
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = Locator.parse(row.head)
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/container", null)
          }
          env.scopes.set(s"$element/locator/$locator/timeoutSecs", "0")
          env.scopes.set(s"$element/locator/$locator/index", index)
        }
      }

      case r"""(.+?)$element can be located with no (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => {
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = Locator.parse(row.head)
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/container", null)
          }
          env.scopes.set(s"$element/locator/$locator/timeoutSecs", "0")
          env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/index", null)
          }
        }
      }

      case r"""(.+?)$element can be located at index (\d+)$index with (\d+)$timeout second (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => {
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = Locator.parse(row.head)
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/container", null)
          }
          env.scopes.set(s"$element/locator/$locator/timeoutSecs", timeout)
          env.scopes.set(s"$element/locator/$locator/index", index)
        }
      }

      case r"""(.+?)$element can be located with (\d+)$timeout second (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => {
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = Locator.parse(row.head)
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/container", null)
          }
          env.scopes.set(s"$element/locator/$locator/timeoutSecs", timeout)
          env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/index", null)
          }
        }
      }

      case r"""(.+?)$element can be located at index (\d+)$index by""" if step.table.nonEmpty && step.table.head._2.size == 2 => {
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = Locator.parse(row.head)
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/container", null)
          }
          env.scopes.getOpt(s"$element/locator/$locator/timeoutSecs") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/timeoutSecs", null)
          }
          env.scopes.set(s"$element/locator/$locator/index", index)
        }
      }

      case r"""(.+?)$element can be located by""" if step.table.nonEmpty && step.table.head._2.size == 2 => {
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = Locator.parse(row.head)
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/container", null)
          }
          env.scopes.getOpt(s"$element/locator/$locator/timeoutSecs") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/timeoutSecs", null)
          }
          env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/index", null)
          }
        }
      }

      case r"""(.+?)$element can be (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$event by (?:javascript|js) "(.+?)"$$$expression""" => step.orDocString(expression) tap { expression =>
        checkStepRules(step, BehaviorType.Context, env)
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/action/${WebEvents.EventToAction(event)}/javascript", expression)
      }

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" => step.orDocString(expression) tap { expression =>
        checkStepRules(step, BehaviorType.Assertion, env)
        val expected = env.parseExpression(operator, expression)
        env.perform {
          env.compare("title", expected, () => webContext.getTitle, operator, Option(negation).isDefined)
        }
      }

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" =>
      checkStepRules(step, BehaviorType.Assertion, env)
        val expected = env.getBoundReferenceValue(attribute)
        env.perform {
          env.compare("title", expected, () => webContext.getTitle, operator, Option(negation).isDefined)
        }

      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" => step.orDocString(expression) tap { expression =>
        checkStepRules(step, BehaviorType.Assertion, env)
        val expected = env.parseExpression(operator, expression)
        env.perform {
          env.compare(name, expected, () => webContext.getPopupMessage, operator, Option(negation).isDefined)
        }
      }

      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" =>
        checkStepRules(step, BehaviorType.Assertion, env)
        val expected = env.getBoundReferenceValue(attribute)
        env.perform {
          env.compare(name, expected, () => webContext.getPopupMessage, operator, Option(negation).isDefined)
        }

      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" =>
        checkStepRules(step, BehaviorType.Assertion, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.checkElementState(elementBinding, state, Option(negation).nonEmpty)

      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" if !element.matches(".+at (json path|xpath).+") => step.orDocString(expression) tap { expression =>
        checkStepRules(step, BehaviorType.Assertion, env)
        if (element == "I") undefinedStepError(step)
        if (element == "the current URL") webContext.captureCurrentUrl(None)
        val negate = Option(negation).isDefined
        val expected = env.parseExpression(operator, expression)
        val actual = env.boundAttributeOrSelection(element, Option(selection))
        env.perform {
          if (env.scopes.findEntry { case (n, _) => n.startsWith(element) } forall { case (n, _) => n != element }) {
            val nameSuffix = Option(selection)
            env.compare(element + nameSuffix.getOrElse(""), expected, actual, operator, negate, nameSuffix)
          } else {
            val actualValue = env.scopes.getOpt(element).getOrElse(actual())
            val result = env.compare(element, expected, actualValue, operator, negate)
            result match {
              case Success(assertion) =>
                val binding = env.getLocatorBinding(element, optional = true)
                assert(assertion, s"Expected ${binding.map(_.toString).getOrElse(element)} to ${if(negate) "not " else ""}$operator '$expected' but got '$actualValue'")
              case Failure(error) =>
                assert(assertion = false, error.getMessage)
            }
          }
        } getOrElse {
          actual()
        }
      }

      case r"""(.+?)$element( value| text)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" if attribute != "absent" && !element.matches(".+at (json path|xpath).+") =>
        checkStepRules(step, BehaviorType.Assertion, env)
        if (element == "I") undefinedStepError(step)
        if (element == "the current URL") webContext.captureCurrentUrl(None)
        val expected = env.getBoundReferenceValue(attribute)
        val actual = env.boundAttributeOrSelection(element, Option(selection))
        env.perform {
          val nameSuffix = Option(selection)
          env.compare(element + nameSuffix.getOrElse(""), expected, actual, operator, Option(negation).isDefined, nameSuffix)
        } getOrElse  {
          actual()
        }

      case r"""I capture (.+?)$attribute (?:of|on|in) (.+?)$element by (?:javascript|js) "(.+?)"$$$expression""" => step.orDocString(expression) tap { expression =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        env.scopes.set(s"$attribute/javascript", expression)
        try {
          env.perform {
            env.topScope.pushObject(s"$attribute/javascript/param/webElement", webContext.locate(elementBinding))
          }
          val value = env.getBoundReferenceValue(attribute)
          env.topScope.set(attribute, value tap { content =>
            env.addAttachment(attribute, "txt", content)
          })
        } finally {
          env.perform {
            env.topScope.popObject(s"$attribute/javascript/param/webElement")
          }
        }
      }

      case r"""I capture the current URL""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.captureCurrentUrl(None)

      case r"""I capture the current URL as (.+?)$name""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.captureCurrentUrl(Some(name))

      case r"""I capture the current screenshot""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.captureScreenshot(true)

      case r"""I capture the current screenshot as (.+?)$attribute""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.captureScreenshot(true, attribute) foreach { file =>
          env.scopes.set(attribute, file.getAbsolutePath)
        }

      case r"""I capture (.+?)$element( value| text)$selection as (.+?)$attribute""" =>
        checkStepRules(step, BehaviorType.Action, env)
        try {
          val value = env.boundAttributeOrSelection(element, Option(selection))
          env.topScope.set(attribute, value() tap { content =>
            env.addAttachment(attribute, "txt", content)
          })
        } catch {
          case _: LocatorBindingException =>
            super.evaluate(step, env)
        }

      case r"""I capture (.+?)$element( value| text)$$$selection""" =>
        checkStepRules(step, BehaviorType.Action, env)
        try {
          val value = env.boundAttributeOrSelection(element, Option(selection))
          env.topScope.set(element, value() tap { content =>
            env.addAttachment(element, "txt", content)
          })
        } catch {
          case _: LocatorBindingException =>
            super.evaluate(step, env)
        }

      case r"I capture the (alert|confirmation)$name popup message" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.topScope.set(s"the $name popup message", webContext.getPopupMessage tap { content =>
          env.addAttachment(s"the $name popup message", "txt", content)
        })

      case r"I capture the (?:alert|confirmation) popup message as (.+?)$attribute" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.topScope.set(attribute, webContext.getPopupMessage tap { content =>
          env.addAttachment(attribute, "txt", content)
        })

      case r"""I start visual test as "(.+?)"$testName in (\d+?)$width x (\d+?)$height viewport""" =>
        checkStepRules(step, BehaviorType.Action, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.startVisualTest(testName, Some(new RectangleSize(width.toInt, height.toInt)))
        } else {
          disabledStepError(step)
        }

      case r"""I start visual test as "(.+?)"$testName""" =>
        checkStepRules(step, BehaviorType.Action, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.startVisualTest(testName, None)
        } else {
          disabledStepError(step)
        }

      case r"""I check (viewport|full page)?$mode visual as "(.+?)"$name using (.+?)$matchLevel match""" =>
        checkStepRules(step, BehaviorType.Action, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.checkVisual(name, mode == "full page", Some(MatchLevel.valueOf(matchLevel)))
        } else {
          disabledStepError(step)
        }

      case r"""I check (viewport|full page)?$mode visual as "(.+?)"$name""" =>
        checkStepRules(step, BehaviorType.Action, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.checkVisual(name, mode == "full page", None)
        } else {
          disabledStepError(step)
        }

      case "the visual test should pass" =>
        checkStepRules(step, BehaviorType.Assertion, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.asertVisuals()
        } else {
          disabledStepError(step)
        }

      case r"""I drag and drop (.+?)$source to (.+?)$$$target""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val sourceBinding = env.getLocatorBinding(source)
        val targetBinding = env.getLocatorBinding(target)
        webContext.dragAndDrop(sourceBinding, targetBinding)

      case r"""I clear (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.performAction("clear", elementBinding)

      case r"""I press (enter|tab)$key in (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendKeys(elementBinding, Array[String](key))
        env.bindAndWait(element, key, "true")

      case r"""I send "(.+?)"$keys""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.sendKeys(keys.split(","))

      case r"""I send "(.+?)"$keys to (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendKeys(elementBinding, keys.split(","))

      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendValue(elementBinding, value, clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = WebSettings.`gwen.web.sendKeys.clearFirst`, sendEnterKey = action == "enter")

      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        val value = env.getBoundReferenceValue(attribute)
        webContext.sendValue(elementBinding, value, clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = WebSettings.`gwen.web.sendKeys.clearFirst`, sendEnterKey = action == "enter")

      case r"""I (select|deselect)$action the (\d+?)$position(?:st|nd|rd|th) option in (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByIndex(elementBinding, position.toInt - 1)
        } else {
          webContext.deselectByIndex(elementBinding, position.toInt - 1)
        }

      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$element by value""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByValue(elementBinding, value)
        } else {
          webContext.deselectByValue(elementBinding, value)
        }

      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByVisibleText(elementBinding, value)
        } else {
          webContext.deselectByVisibleText(elementBinding, value)
        }

      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$element by value""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val value = env.getBoundReferenceValue(attribute)
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByValue(elementBinding, value)
        } else {
          webContext.deselectByValue(elementBinding, value)
        }

      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val value = env.getBoundReferenceValue(attribute)
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByVisibleText(elementBinding, value)
        } else {
          webContext.deselectByVisibleText(elementBinding, value)
        }

      case r"""I (click|right click|double click|check|tick|uncheck|untick|move to)$action (.+?)$element of (.+?)$$$context""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.performActionInContext(action, element, context)

      case r"""I (click|right click|double click|submit|check|tick|uncheck|untick|move to)$action (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.performAction(action, elementBinding)

      case r"""I (.+?)$modifiers (click|right click|double click)$clickAction (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.holdAndClick(modifiers.split("\\+"), clickAction, elementBinding)

      case r"""I (?:highlight|locate) (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        env.perform {
          webContext.locateAndHighlight(elementBinding)
        }

      case "I refresh the current page" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.refreshPage()

      case r"I start a new browser" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.close("primary")
        webContext.switchToSession("primary")

      case r"""I start a browser for (.+?)$$$session""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.close(session)
        webContext.switchToSession(session)

      case r"""I should have (\d+?)$count open browser(?:s?)""" =>
        checkStepRules(step, BehaviorType.Assertion, env)
        env.perform {
          env.compare("open browser sessions", count, () => webContext.noOfSessions().toString, "be", false)
        }

      case r"""I should have (\d+?)$count open (?:window|tab)(?:s?)""" =>
        checkStepRules(step, BehaviorType.Assertion, env)
        env.perform {
          env.compare("open windows/tabs", count, () => webContext.noOfWindows().toString, "be", false)
        }

      case r"I have (no|an)$open open browser" =>
        checkStepRules(step, BehaviorType.Context, env)
        if (open == "no") {
          webContext.close()
        } else {
          webContext.newOrCurrentSession()
        }

      case r"I close the(?: current)? browser" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.close()

      case r"""I close the browser for (.+?)$session""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.close(session)

      case r"""I switch to the child (?:window|tab)""" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.perform {
          webContext.switchToChild()
        }

      case r"""I switch to child (?:window|tab) (\d+?)$occurrence""" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.perform {
          webContext.switchToChild(occurrence.toInt)
        }

      case r"""I close the child (?:window|tab)""" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.perform {
          webContext.closeChild()
        }

      case r"""I close child (?:window|tab) (\d+?)$occurrence""" =>
        checkStepRules(step, BehaviorType.Action, env)
        env.perform {
          webContext.closeChild(occurrence.toInt)
        }

      case r"""I switch to the (?:root|parent) (?:window|tab)""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.switchToParent()

      case """I switch to the default content""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.switchToDefaultContent()

      case r"""I switch to (.+?)$session""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.switchToSession(session)

      case r"I (accept|dismiss)$action the (?:alert|confirmation) popup" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.handleAlert(action == "accept")

      case r"""I resize the window to width (\d+?)$width and height (\d+?)$$$height""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.resizeWindow(width.toInt, height.toInt)

      case r"""I maximi(?:z|s)e the window""" =>
        checkStepRules(step, BehaviorType.Action, env)
        webContext.maximizeWindow()

      case r"""I append "(.+?)"$text to (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendValue(elementBinding, text, clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = false, sendEnterKey = false)

      case r"""I append (.+?)$attribute to (.+?)$$$element""" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        val text = env.getBoundReferenceValue(attribute)
        webContext.sendValue(elementBinding, text, clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = false, sendEnterKey = false)

      case r"I insert a new line in (.+?)$$$element" =>
        checkStepRules(step, BehaviorType.Action, env)
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendValue(elementBinding, StringEscapeUtils.unescapeJava("""\n"""), clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = false, sendEnterKey = false)

      case _ => super.evaluate(step, env)

    }
  }
  
  /**
    * Performs a repeat until or while operation 
    */
  private def repeat(operation: String, parent: Identifiable, step: Step, doStep: String, condition: String, delay: Duration, timeout: Duration, env: WebEnvContext): Step = {
    assert(delay.gteq(Duration.Zero), "delay cannot be less than zero")
    assert(timeout.gt(Duration.Zero), "timeout must be greater than zero")
    assert(timeout.gteq(delay), "timeout cannot be less than or equal to delay")
    val operationTag = Tag(if (operation == "until") ReservedTags.RepeatUntil else ReservedTags.RepeatWhile)
    val tags = List(Tag(ReservedTags.Synthetic), operationTag, Tag(ReservedTags.StepDef))
    val preCondStepDef = Scenario(step.sourceRef, tags, operationTag.name, condition, Nil, None, Nil, Nil, Nil)
    var condSteps: List[Step] = Nil
    var evaluatedStep = step
    val start = System.nanoTime()
    env.perform {
      var iteration = 0
      try {
        env.webContext.waitUntil(timeout.toSeconds, s"trying to repeat: ${step.name}") {
          val index = iteration
          iteration = iteration + 1
          env.topScope.set("iteration number", iteration.toString)
          val preStep = step.copy(withKeyword = if(iteration == 1) step.keyword else StepKeyword.And.toString, withName = doStep)
          operation match {
            case "until" =>
              logger.info(s"repeat-until $condition: iteration $iteration")
              if (condSteps.isEmpty) {
                lifecycle.beforeStepDef(step, preCondStepDef, env.scopes)
              }
              val iterationStep = evaluateStep(preCondStepDef, preStep, index, env)
              condSteps = iterationStep :: condSteps
              iterationStep.evalStatus match {
                case Failed(_, e) => throw e
                case _ =>
                  val javascript = env.interpolate(env.scopes.get(s"$condition/javascript"))(env.getBoundReferenceValue)
                  env.evaluateJSPredicate(javascript) tap { result =>
                    if (!result) {
                      logger.info(s"repeat-until $condition: not complete, will repeat ${if (delay.gt(Duration.Zero)) s"in ${DurationFormatter.format(delay)}" else "now"}")
                      if (delay.gt(Duration.Zero)) Thread.sleep(delay.toMillis)
                    } else {
                      logger.info(s"repeat-until $condition: completed")
                    }
                  }
              }
            case "while" =>
              val javascript = env.interpolate(env.scopes.get(s"$condition/javascript"))(env.getBoundReferenceValue)
              val result = env.evaluateJSPredicate(javascript)
              if (result) {
                logger.info(s"repeat-while $condition: iteration $iteration")
                if (condSteps.isEmpty) {
                  lifecycle.beforeStepDef(step, preCondStepDef, env.scopes)
                }
                val iterationStep = evaluateStep(preCondStepDef, preStep, index, env)
                condSteps = iterationStep :: condSteps
                iterationStep.evalStatus match {
                  case Failed(_, e) => throw e
                  case _ =>
                    logger.info(s"repeat-while $condition: not complete, will repeat ${if (delay.gt(Duration.Zero)) s"in ${DurationFormatter.format(delay)}" else "now"}")
                    if (delay.gt(Duration.Zero)) Thread.sleep(delay.toMillis)
                }
              } else {
                logger.info(s"repeat-while $condition: completed")
              }
              !result
          }
        }
      } catch {
        case e: Throwable =>
          logger.error(e.getMessage)
          val nanos = System.nanoTime() - start
          val durationNanos = {
            if (nanos > timeout.toNanos) timeout.toNanos
            else nanos
          }
          evaluatedStep = step.copy(withEvalStatus = Failed(durationNanos, new StepFailure(step, e)))
      } finally {
        env.topScope.set("iteration number", null)
      }
    } getOrElse {
      try {
        operation match {
          case "until" =>
            evaluatedStep = this.evaluateStep(step, step.copy(withName = doStep), 0, env)
            env.scopes.get(s"$condition/javascript")
          case _ =>
            env.scopes.get(s"$condition/javascript")
            evaluatedStep = this.evaluateStep(step, step.copy(withName = doStep), 0, env)
        }
      } catch {
        case _: Throwable => 
          // ignore in dry run mode
      }
    }
    if (condSteps.nonEmpty) {
      val steps = evaluatedStep.evalStatus match {
        case Failed(nanos, error) if (EvalStatus(condSteps.map(_.evalStatus)).status == StatusKeyword.Passed) => 
          val preStep = condSteps.head.copy(withKeyword = StepKeyword.And.toString, withName = doStep)
          lifecycle.beforeStep(preCondStepDef, preStep, env.scopes)
          val fStep = preStep.copy(
            withEvalStatus = Failed(nanos - condSteps.map(_.evalStatus.nanos).sum, error),
            withStepDef = None
          )
          lifecycle.afterStep(fStep, env.scopes)
          fStep :: condSteps
        case _ => 
          condSteps
          
      }
      val condStepDef = preCondStepDef.copy(withSteps = steps.reverse)
      lifecycle.afterStepDef(condStepDef, env.scopes)
      evaluatedStep.copy(
        withEvalStatus = condStepDef.evalStatus,
        withStepDef = Some(condStepDef)
      )
    } else {
      evaluatedStep
    }
  }
  
  lazy val DefaultRepeatDelay: Duration = {
    val waitSecs = WebSettings.`gwen.web.wait.seconds` 
    if (waitSecs > 9 && waitSecs % 10 == 0) Duration(waitSecs / 10, "second") else Duration(waitSecs * 100, "millisecond")
  }
  
  private def defaultRepeatTimeout(delay: Duration): Duration = delay * 30
  
}
