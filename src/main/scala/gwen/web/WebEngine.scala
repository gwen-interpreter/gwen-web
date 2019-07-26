/*
 * Copyright 2014-2019 Branko Juric, Brady Wood
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

import java.util.concurrent.TimeUnit

import com.applitools.eyes.{MatchLevel, RectangleSize}

import scala.concurrent.duration.Duration
import gwen.Predefs.Formatting.DurationFormatter
import gwen.Predefs.Kestrel
import gwen.Predefs.RegexContext
import gwen.Settings
import gwen.dsl._
import gwen.errors.StepFailure
import gwen.errors.undefinedStepError
import gwen.errors.disabledStepError
import gwen.eval.{GwenOptions, ScopedDataStack}
import gwen.eval.support.DefaultEngineSupport
import gwen.web.errors.LocatorBindingException
import org.apache.commons.text.StringEscapeUtils

import scala.util.{Failure, Success, Try}

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

        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$container with no (?:timeout|wait)""" =>
          env.getLocatorBinding(container)
          val binding = LocatorBinding(s"${element}/list", locator, expression, Some(container), Some(Duration.Zero), None)
          env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" =>
          env.getLocatorBinding(container)
          val binding = LocatorBinding(s"${element}/list", locator, expression, Some(container), Some(Duration.create(timeout.toLong, TimeUnit.SECONDS)), None)
          env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$container""" =>
          env.getLocatorBinding(container)
          val binding = LocatorBinding(s"${element}/list", locator, expression, Some(container), None, None)
          env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression with no (?:timeout|wait)""" =>
          val binding = LocatorBinding(s"${element}/list", locator, expression, None, Some(Duration.Zero), None)
          env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression with (\d+)$timeout second (?:timeout|wait)""" =>
          val binding = LocatorBinding(s"${element}/list", locator, step.orDocString(expression), None, Some(Duration.create(timeout.toLong, TimeUnit.SECONDS)), None)
          env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression""" =>
          val binding = LocatorBinding(s"${element}/list", locator, step.orDocString(expression), None, None, None)
          env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep for each (.+?)$element in (.+?)$$$iteration""" if !iteration.contains("delimited by") =>
          val binding = env.getLocatorBinding(iteration)
          env.evaluate(foreach(() => List("$[dryRun:webElements]"), element, step, doStep, env)) {
            foreach(() => webContext.locateAll(binding), element, step, doStep, env)
          }

        case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using no delay and (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit (?:timeout|wait)""" =>
          repeat(operation, step, doStep, condition, Duration.Zero, Duration(timeoutPeriod.toLong, timeoutUnit), env)

        case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using no delay""" =>
          repeat(operation, step, doStep, condition, Duration.Zero, defaultRepeatTimeout(DefaultRepeatDelay), env)

        case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$delayPeriod (second|millisecond)$delayUnit delay and (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit (?:timeout|wait)""" =>
          repeat(operation, step, doStep, condition, Duration(delayPeriod.toLong, delayUnit), Duration(timeoutPeriod.toLong, timeoutUnit), env)

        case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$delayPeriod (second|millisecond)$delayUnit delay""" =>
          val delayDuration = Duration(delayPeriod.toLong, delayUnit)
          repeat(operation, step, doStep, condition, delayDuration, defaultRepeatTimeout(delayDuration), env)

        case r"""(.+?)$doStep (until|while)$operation (.+?)$condition using (.+?)$timeoutPeriod (minute|second|millisecond)$timeoutUnit (?:timeout|wait)""" =>
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
        webContext.waitUntil(seconds.toInt) {
          webContext.waitForText(elementBinding)
        }

      case r"""I wait for (.+?)$element text""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil {
          webContext.waitForText(elementBinding)
        }

      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil(seconds.toInt) {
          Try(webContext.locateAndHighlight(elementBinding)).isSuccess
        }

      case r"""I wait for (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.waitUntil {
          Try(webContext.locateAndHighlight(elementBinding)).isSuccess
        }

      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$$$event""" =>
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/wait", duration)

      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|right clicked|double clicked||submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$$$event""" =>
        env.scopes.get(s"$condition/javascript")
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/condition", condition)

      case r"""I wait until "(.+?)$javascript"""" => step.orDocString(javascript) tap { javascript =>
        webContext.waitUntil {
          env.evaluateJSPredicate(javascript)
        }
      }

      case r"""I wait until (.+?)$element is( not)?$negation (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" if env.scopes.getOpt(s"$element is${if(Option(negation).isDefined) " not" else ""} $state/javascript").isEmpty =>
        val elementBinding = env.getLocatorBinding(element).jsEquivalent
        val negate = Option(negation).isDefined
        webContext.waitForElementState(elementBinding, state, negate)

      case r"""I wait until (.+?)$$$condition""" =>
        val javascript = env.scopes.get(s"$condition/javascript")
        webContext.waitUntil {
          env.evaluateJSPredicate(javascript)
        }

      case r"""I am on the (.+?)$$$page""" =>
        env.scopes.addScope(page)

      case r"""I navigate to the (.+?)$$$page""" =>
        env.scopes.addScope(page)
        val url = env.getAttribute("url")
        webContext.navigateTo(url)

      case r"""I navigate to "(.+?)"$$$url""" => step.orDocString(url) tap { url =>
        webContext.navigateTo(url)
      }

      case r"""I scroll to the (top|bottom)$position of (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.scrollIntoView(elementBinding, ScrollTo.withName(position))

      case r"""the url will be defined by (?:property|setting) "(.+?)"$$$name""" => step.orDocString(name) tap { name =>
        env.scopes.set("url", Settings.get(name))
      }

      case r"""the url will be "(.+?)"$$$url""" => step.orDocString(url) tap { url =>
        env.scopes.set("url", url)
      }

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression at index (\d+)$index in (.+?)$container with no (?:timeout|wait)""" =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.set(s"$element/locator/$locator/container", container)
        env.scopes.set(s"$element/locator/$locator/timeoutSecs", "0")
        env.scopes.set(s"$element/locator/$locator/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$container with no (?:timeout|wait)""" =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.set(s"$element/locator/$locator/container", container)
        env.scopes.set(s"$element/locator/$locator/timeoutSecs", "0")
        env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression at index (\d+)$index in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.set(s"$element/locator/$locator/container", container)
        env.scopes.set(s"$element/locator/$locator/timeoutSecs", timeout)
        env.scopes.set(s"$element/locator/$locator/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.set(s"$element/locator/$locator/container", container)
        env.scopes.set(s"$element/locator/$locator/timeoutSecs", timeout)
        env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression at index (\d+)$index in (.+?)$container""" =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.set(s"$element/locator/$locator/container", container)
        env.scopes.getOpt(s"$element/locator/$locator/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/timeoutSecs", null)
        }
        env.scopes.set(s"$element/locator/$locator/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression in (.+?)$container""" =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.set(s"$element/locator/$locator/container", container)
        env.scopes.getOpt(s"$element/locator/$locator/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/timeoutSecs", null)
        }
        env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression at index (\d+)$index with no (?:timeout|wait)""" =>
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/container", null)
        }
        env.scopes.set(s"$element/locator/$locator/timeoutSecs", "0")
        env.scopes.set(s"$element/locator/$locator/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression with no (?:timeout|wait)""" =>
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/container", null)
        }
        env.scopes.set(s"$element/locator/$locator/timeoutSecs", "0")
        env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression at index (\d+)$index with (\d+)$timeout second (?:timeout|wait)""" =>
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/container", null)
        }
        env.scopes.set(s"$element/locator/$locator/timeoutSecs", timeout)
        env.scopes.set(s"$element/locator/$locator/index", index)

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression with (\d+)$timeout second (?:timeout|wait)""" =>
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/container", null)
        }
        env.scopes.set(s"$element/locator/$locator/timeoutSecs", timeout)
        env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/index", null)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression at index (\d+)$index""" =>
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/container", null)
        }
        env.scopes.getOpt(s"$element/locator/$locator/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/timeoutSecs", null)
        }
        env.scopes.set(s"$element/locator/$locator/index", index)

      case r"""(.+?)$element can be located at index (\d+)$index by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression""" => step.orDocString(expression) tap { expression =>
        env.scopes.set(s"$element/locator", locator)
        env.scopes.set(s"$element/locator/$locator", expression)
        env.scopes.getOpt(s"$element/locator/$locator/container") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/container", null)
        }
        env.scopes.getOpt(s"$element/locator/$locator/timeoutSecs") foreach { _ =>
          env.scopes.set(s"$element/locator/$locator/timeoutSecs", null)
        }
        env.scopes.set(s"$element/locator/$locator/index", index)
      }

      case r"""(.+?)$element can be located by (id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript)$locator "(.+?)"$expression""" => step.orDocString(expression) tap { expression =>
        env.scopes.set(s"$element/locator", locator)
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

      case r"""(.+?)$element can be located at index (\d+)$index in (.+?)$container by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = row.head
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.set(s"$element/locator/$locator/container", container)
          env.scopes.set(s"$element/locator/$locator/index", index)
        }

      case r"""(.+?)$element can be located in (.+?)$container by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        env.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = row.head
          val expression = row(1)
          env.scopes.set(s"$element/locator/$locator", expression)
          env.scopes.set(s"$element/locator/$locator/container", container)
          env.scopes.getOpt(s"$element/locator/$locator/index") foreach { _ =>
            env.scopes.set(s"$element/locator/$locator/index", null)
          }
        }

      case r"""(.+?)$element can be located at index (\d+)$index with no (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => {
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = row.head
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
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = row.head
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
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = row.head
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
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = row.head
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
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = row.head
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
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val locator = row.head
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

      case r"""(.+?)$element can be (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$event by javascript "(.+?)"$$$expression""" => step.orDocString(expression) tap { expression =>
        env.getLocatorBinding(element)
        env.scopes.set(s"$element/action/${WebEvents.EventToAction(event)}/javascript", expression)
      }

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" => step.orDocString(expression) tap { expression =>
        val expected = env.parseExpression(operator, expression)
        env.perform {
          env.compare("title", expected, () => webContext.getTitle, operator, Option(negation).isDefined)
        }
      }

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" =>
        val expected = env.getBoundReferenceValue(attribute)
        env.perform {
          env.compare("title", expected, () => webContext.getTitle, operator, Option(negation).isDefined)
        }

      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" => step.orDocString(expression) tap { expression =>
        val expected = env.parseExpression(operator, expression)
        env.perform {
          env.compare(name, expected, () => webContext.getPopupMessage, operator, Option(negation).isDefined)
        }
      }

      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" =>
        val expected = env.getBoundReferenceValue(attribute)
        env.perform {
          env.compare(name, expected, () => webContext.getPopupMessage, operator, Option(negation).isDefined)
        }

      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" =>
        val elementBinding = env.getLocatorBinding(element).jsEquivalent
        webContext.checkElementState(elementBinding, state, Option(negation).nonEmpty)

      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" if !element.matches(".+at (json path|xpath).+") => step.orDocString(expression) tap { expression =>
        if (element == "I") undefinedStepError(step)
        if (element == "the current URL") webContext.captureCurrentUrl(None)
        val negate = Option(negation).isDefined
        val expected = env.parseExpression(operator, expression)
        val actual = env.boundAttributeOrSelection(element, Option(selection))
        env.perform {
          if (env.scopes.findEntry { case (n, _) => n.startsWith(element) } forall { case (n, _) => n != element }) {
            env.compare(element + Option(selection).getOrElse(""), expected, actual, operator, negate)
          } else {
            val actualValue = env.scopes.getOpt(element).getOrElse(actual())
            val result = env.compare(element, expected, actualValue, operator, negate)
            result match {
              case Success(assertion) =>
                assert(assertion, s"Expected $element to ${if(negate) "not " else ""}$operator '$expected' but got '$actualValue'")
              case Failure(error) =>
                assert(assertion = false, error.getMessage)
            }
          }
        } getOrElse  {
          actual()
        }
      }

      case r"""(.+?)$element( value| text)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" if attribute != "absent" && !element.matches(".+at (json path|xpath).+") =>
        if (element == "I") undefinedStepError(step)
        if (element == "the current URL") webContext.captureCurrentUrl(None)
        val expected = env.getBoundReferenceValue(attribute)
        val actual = env.boundAttributeOrSelection(element, Option(selection))
        env.perform {
          env.compare(element + Option(selection).getOrElse(""), expected, actual, operator, Option(negation).isDefined)
        } getOrElse  {
          actual()
        }

      case r"""I capture (.+?)$attribute (?:of|on|in) (.+?)$element by javascript "(.+?)"$$$expression""" => step.orDocString(expression) tap { expression =>
        val elementBinding = env.getLocatorBinding(element)
        env.activeScope.set(s"$attribute/javascript", expression)
        try {
          env.perform {
            env.featureScope.pushObject(s"$attribute/javascript/param/webElement", webContext.locate(elementBinding))
          }
          val value = env.getBoundReferenceValue(attribute)
          env.featureScope.set(attribute, value tap { content =>
            env.addAttachment(attribute, "txt", content)
          })
        } finally {
          env.perform {
            env.featureScope.popObject(s"$attribute/javascript/param/webElement")
          }
        }
      }

      case r"""I capture the current URL""" =>
        webContext.captureCurrentUrl(None)

      case r"""I capture the current URL as (.+?)$name""" =>
        webContext.captureCurrentUrl(Some(name))

      case r"""I capture the current screenshot""" =>
        webContext.captureScreenshot(true)

      case r"""I capture (.+?)$element( value| text)$selection as (.+?)$attribute""" =>
        try {
          val value = env.boundAttributeOrSelection(element, Option(selection))
          env.featureScope.set(attribute, value() tap { content =>
            env.addAttachment(attribute, "txt", content)
          })
        } catch {
          case _: LocatorBindingException =>
            super.evaluate(step, env)
        }

      case r"""I capture (.+?)$element( value| text)$$$selection""" =>
        try {
          val value = env.boundAttributeOrSelection(element, Option(selection))
          env.featureScope.set(element, value() tap { content =>
            env.addAttachment(element, "txt", content)
          })
        } catch {
          case _: LocatorBindingException =>
            super.evaluate(step, env)
        }

      case r"I capture the (alert|confirmation)$name popup message" =>
        env.featureScope.set(s"the $name popup message", webContext.getPopupMessage tap { content =>
          env.addAttachment(s"the $name popup message", "txt", content)
        })

      case r"I capture the (?:alert|confirmation) popup message as (.+?)$attribute" =>
        env.featureScope.set(attribute, webContext.getPopupMessage tap { content =>
          env.addAttachment(attribute, "txt", content)
        })

      case r"""I start visual test as "(.+?)"$testName in (\d+?)$width x (\d+?)$height viewport""" =>
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.startVisualTest(testName, Some(new RectangleSize(width.toInt, height.toInt)))
        } else {
          disabledStepError(step)
        }

      case r"""I start visual test as "(.+?)"$testName""" =>
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.startVisualTest(testName, None)
        } else {
          disabledStepError(step)
        }

      case r"""I check (viewport|full page)?$mode visual as "(.+?)"$name using (.+?)$matchLevel match""" =>
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.checkVisual(name, mode == "full page", Some(MatchLevel.valueOf(matchLevel)))
        } else {
          disabledStepError(step)
        }

      case r"""I check (viewport|full page)?$mode visual as "(.+?)"$name""" =>
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.checkVisual(name, mode == "full page", None)
        } else {
          disabledStepError(step)
        }

      case "the visual test should pass" =>
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          webContext.asertVisuals()
        } else {
          disabledStepError(step)
        }

      case r"""I drag and drop (.+?)$source to (.+?)$$$target""" =>
        val sourceBinding = env.getLocatorBinding(source)
        val targetBinding = env.getLocatorBinding(target)
        webContext.dragAndDrop(sourceBinding, targetBinding)

      case r"""I clear (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.performAction("clear", elementBinding)

      case r"""I press (enter|tab)$key in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendKeys(elementBinding, Array[String](key))
        env.bindAndWait(element, key, "true")

      case r"""I send "(.+?)"$keys to (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendKeys(elementBinding, keys.split(","))

      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendValue(elementBinding, value, clearFirst = WebSettings.`gwen.web.sendKeys.clearFirst`, sendEnterKey = action == "enter")

      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        val value = env.getBoundReferenceValue(attribute)
        webContext.sendValue(elementBinding, value, clearFirst = WebSettings.`gwen.web.sendKeys.clearFirst`, sendEnterKey = action == "enter")

      case r"""I (select|deselect)$action the (\d+?)$position(?:st|nd|rd|th) option in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByIndex(elementBinding, position.toInt - 1)
        } else {
          webContext.deselectByIndex(elementBinding, position.toInt - 1)
        }

      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$element by value""" =>
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByValue(elementBinding, value)
        } else {
          webContext.deselectByValue(elementBinding, value)
        }

      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByVisibleText(elementBinding, value)
        } else {
          webContext.deselectByVisibleText(elementBinding, value)
        }

      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$element by value""" =>
        val value = env.getBoundReferenceValue(attribute)
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByValue(elementBinding, value)
        } else {
          webContext.deselectByValue(elementBinding, value)
        }

      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$$$element""" =>
        val value = env.getBoundReferenceValue(attribute)
        val elementBinding = env.getLocatorBinding(element)
        if (action == "select") {
          webContext.selectByVisibleText(elementBinding, value)
        } else {
          webContext.deselectByVisibleText(elementBinding, value)
        }

      case r"""I (click|right click|double click|check|tick|uncheck|untick|move to)$action (.+?)$element of (.+?)$$$context""" =>
        webContext.performActionInContext(action, element, context)

      case r"""I (click|right click|double click|submit|check|tick|uncheck|untick|move to)$action (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.performAction(action, elementBinding)

      case r"""I (.+?)$modifiers (click|right click|double click)$clickAction (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.holdAndClick(modifiers.split("\\+"), clickAction, elementBinding)

      case r"""I (?:highlight|locate) (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        env.perform {
          webContext.locateAndHighlight(elementBinding)
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

      case """I switch to the default content""" =>
        webContext.switchToDefaultContent()

      case r"""I switch to (.+?)$session""" =>
        webContext.switchToSession(session)

      case r"I (accept|dismiss)$action the (?:alert|confirmation) popup" =>
        webContext.handleAlert(action == "accept")

      case r"""I resize the window to width (\d+?)$width and height (\d+?)$$$height""" =>
        webContext.resizeWindow(width.toInt, height.toInt)

      case r"""I maximi(?:z|s)e the window""" =>
        webContext.maximizeWindow()

      case r"""I append "(.+?)"$text to (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendValue(elementBinding, text, clearFirst = false, sendEnterKey = false)

      case r"""I append (.+?)$attribute to (.+?)$$$element""" =>
        val elementBinding = env.getLocatorBinding(element)
        val text = env.getBoundReferenceValue(attribute)
        webContext.sendValue(elementBinding, text, clearFirst = false, sendEnterKey = false)

      case r"I insert a new line in (.+?)$$$element" =>
        val elementBinding = env.getLocatorBinding(element)
        webContext.sendValue(elementBinding, StringEscapeUtils.unescapeJava("""\n"""), clearFirst = false, sendEnterKey = false)

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
    var evaluatedStep = step
    env.perform {
      var iteration = 0L
      val start = System.nanoTime
      try {
        env.webContext.waitUntil(timeout.toSeconds) {
          iteration = iteration + 1
          env.featureScope.set("iteration number", iteration.toString)
          operation match {
            case "until" =>
              logger.info(s"repeat-until $condition: iteration $iteration")
              evaluateStep(Step(step.keyword, doStep), env).evalStatus match {
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
                evaluateStep(Step(step.keyword, doStep), env).evalStatus match {
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
          evaluatedStep = Step(step, Failed(System.nanoTime - start, new StepFailure(step, e)))
      } finally {
        env.featureScope.set("iteration number", null)
      }
    } getOrElse {
      operation match {
        case "until" =>
          this.evaluateStep(Step(step.pos, step.keyword, doStep), env)
          env.scopes.get(s"$condition/javascript")
        case _ =>
          env.scopes.get(s"$condition/javascript")
          this.evaluateStep(Step(step.pos, step.keyword, doStep), env)
      }
    }
    evaluatedStep
  }
  
  lazy val DefaultRepeatDelay: Duration = {
    val waitSecs = WebSettings.`gwen.web.wait.seconds` 
    if (waitSecs > 9 && waitSecs % 10 == 0) Duration(waitSecs / 10, "second") else Duration(waitSecs * 100, "millisecond")
  }
  
  private def defaultRepeatTimeout(delay: Duration): Duration = delay * 30
  
}

