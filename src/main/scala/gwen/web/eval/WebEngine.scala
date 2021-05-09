/*
 * Copyright 2014-2021 Branko Juric, Brady Wood
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

package gwen.web.eval

import gwen.core._
import gwen.core.Errors.undefinedStepError
import gwen.core.Errors.disabledStepError
import gwen.core.eval.EvalEngine
import gwen.core.eval.EvalEnvironment
import gwen.core.eval.binding.JavaScriptBinding
import gwen.core.eval.step.UnitStep
import gwen.core.eval.step.composite._
import gwen.core.model._
import gwen.core.model.gherkin.Step
import gwen.web.WebErrors.LocatorBindingException
import gwen.web.WebSettings
import gwen.web.eval.binding._
import gwen.web.eval.step.composite._
import gwen.web.eval.eyes.EyesSettings

import com.applitools.eyes.{MatchLevel, RectangleSize}
import org.apache.commons.text.StringEscapeUtils

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

import java.util.concurrent.TimeUnit

/**
  * A web engine that uses the Selenium web driver
  * API to automate various web operations.
  * 
  * @author Branko Juric, Brady Wood
  */
class WebEngine extends EvalEngine[WebContext] {
  
  /**
    * Initialises and returns a new evaluation context.
    * 
    * @param options command line options
    * @param envOpt optional environment context to use
    */
  override def init(options: GwenOptions, envOpt: Option[EvalEnvironment] = None): WebContext = {
    if (WebSettings.`gwen.web.capture.screenshots.highlighting`) {
      val fps = GwenSettings.`gwen.report.slideshow.framespersecond`
      Settings.setLocal("gwen.report.slideshow.framespersecond", (fps.toDouble * 1.8d).toInt.toString)
    }
    val env = envOpt.getOrElse(new EvalEnvironment())
    new WebContext(options, env, new DriverManager())
  }

  /**
    * Translates composite web engine steps.
    */
  override def translateComposite(parent: Identifiable, step: Step, env: EvalEnvironment, ctx: WebContext): Option[CompositeStep[WebContext]] = {
    super.translateComposite(parent, step, env, ctx) orElse {
      step.expression match {
        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container with no (?:timeout|wait)""" => Some { 
          new ForEachWebElement(doStep, element, SelectorType.parse(selectorType), expression, Some(container), Some(Duration.Zero), this, ctx)
        }
        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" => Some {
          new ForEachWebElement(doStep, element, SelectorType.parse(selectorType), expression, Some(container), Some(Duration.create(timeout.toLong, TimeUnit.SECONDS)), this, ctx)
        }
        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container""" => Some {
          new ForEachWebElement(doStep, element, SelectorType.parse(selectorType), expression, Some(container), None, this, ctx)
        }
        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression with no (?:timeout|wait)""" => Some { 
          new ForEachWebElement(doStep, element, SelectorType.parse(selectorType), expression, None, Some(Duration.Zero), this, ctx)
        }
        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression with (\d+)$timeout second (?:timeout|wait)""" => Some {
          new ForEachWebElement(doStep, element, SelectorType.parse(selectorType), expression, None, Some(Duration.create(timeout.toLong, TimeUnit.SECONDS)), this, ctx)
        }
        case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression""" => Some {
          new ForEachWebElement(doStep, element, SelectorType.parse(selectorType), expression, None, None, this, ctx)
        }
        case r"""(.+?)$doStep for each (.+?)$element in (.+?)$$$iteration""" if !iteration.contains("delimited by") => Some {
          new ForEachWebElementInIteration(doStep, element, iteration, this, ctx)
        }
        case _ => None
      }
    }
  }

  /**
    * Translates a web engine step.  This method matches the incoming step against a
    * set of supported steps and returns an operation that will evaluate those that are 
    * successfully matched.
    *
    * @param step the step to evaluate
    * @param ctx the web evaluation context
    */
  override def translate(parent: Identifiable, step: Step, env: EvalEnvironment, ctx: WebContext): UnitStep[WebContext] = {

    step.expression match {

      case r"""I wait for (.+?)$element text for (.+?)$seconds second(?:s?)""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.waitUntil(seconds.toInt, s"waiting for text of $binding") {
          ctx.waitForText(binding)
        }

      case r"""I wait for (.+?)$element text""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.waitUntil(s"waiting for text of $binding") {
          ctx.waitForText(binding)
        }

      case r"""I wait for (.+?)$element for (.+?)$seconds second(?:s?)""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.waitUntil(seconds.toInt, s"waiting for $binding to be displayed") {
          Try(ctx.locateAndHighlight(binding)).isSuccess
        }

      case r"""I wait for (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.waitUntil(s"waiting for $binding to be displayed") {
          Try(ctx.locateAndHighlight(binding)).isSuccess
        }

      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$$$event""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/wait", duration)

      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|right clicked|double clicked||submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$$$event""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        env.scopes.get(JavaScriptBinding.key(condition))
        ctx.getLocatorBinding(element)
        env.scopes.set(s"$element/${WebEvents.EventToAction(event)}/condition", condition)

      case r"""I wait until "(.+?)$javascript"""" => step =>
        step.orDocString(javascript) tap { javascript =>
          checkStepRules(step, BehaviorType.Action, env)
          ctx.waitUntil(s"waiting for true return from javascript: $javascript") {
            ctx.evaluateJSPredicate(javascript)
          }
        }

      case r"""I wait until (.+?)$element is( not)?$negation (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" if env.scopes.getOpt(s"$element is${if(Option(negation).isDefined) " not" else ""} $state/javascript").isEmpty => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element).jsEquivalent
        val negate = Option(negation).isDefined
        ctx.waitForElementState(binding, state, negate)

      case r"""I wait until (.+?)$$$condition""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val javascript = env.scopes.get(JavaScriptBinding.key(condition))
        ctx.waitUntil(s"waiting for true return from javascript: $javascript") {
          ctx.evaluateJSPredicate(javascript)
        }

      case r"""I am on the (.+?)$$$page""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.addScope(page)

      case r"""I navigate to the (.+?)$$$page""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        env.scopes.addScope(page)
        val url = ctx.getAttribute("url")
        ctx.navigateTo(url)

      case r"""I navigate to "(.+?)"$$$url""" => step =>
        step.orDocString(url) tap { url =>
          checkStepRules(step, BehaviorType.Action, env)
          ctx.navigateTo(url)
        }

      case r"""I scroll to the (top|bottom)$position of (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.scrollIntoView(binding, ScrollTo.withName(position))

      case r"""the url will be defined by (?:property|setting) "(.+?)"$$$name""" => step =>
        step.orDocString(name) tap { name =>
          checkStepRules(step, BehaviorType.Context, env)
          env.scopes.set("url", Settings.get(name))
        }

      case r"""the url will be "(.+?)"$$$url""" => step =>
        step.orDocString(url) tap { url =>
          checkStepRules(step, BehaviorType.Context, env)
          env.scopes.set("url", url)
        }

      case r"""the (.+?)$page url is "(.+?)"$$$url""" => step =>
        step.orDocString(url) tap { url =>
          checkStepRules(step, BehaviorType.Context, env)
          env.scopes.addScope(page)
          env.scopes.set("url", url)
        }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index in (.+?)$container with no (?:timeout|wait)""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        ctx.getLocatorBinding(container)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, Some(container), Some(0), Some(index.toInt))

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container with no (?:timeout|wait)""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        ctx.getLocatorBinding(container)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, Some(container), Some(0), None)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        ctx.getLocatorBinding(container)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, Some(container), Some(timeout.toInt), Some(index.toInt))

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        ctx.getLocatorBinding(container)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, Some(container), Some(timeout.toInt), None)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index in (.+?)$container""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        ctx.getLocatorBinding(container)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, Some(container), None, Some(index.toInt))

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        ctx.getLocatorBinding(container)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, Some(container), None, None)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index with no (?:timeout|wait)""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, Some(0), Some(index.toInt))

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression with no (?:timeout|wait)""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, Some(0), None)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index with (\d+)$timeout second (?:timeout|wait)""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, Some(timeout.toInt), Some(index.toInt))

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression with (\d+)$timeout second (?:timeout|wait)""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, Some(timeout.toInt), None)

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index""" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, None, Some(index.toInt))
        
      case r"""(.+?)$element can be located at index (\d+)$index by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression""" => step =>
        step.orDocString(expression) tap { expression =>
          checkStepRules(step, BehaviorType.Context, env)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, None, Some(index.toInt))
        }

      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression""" => step =>
        step.orDocString(expression) tap { expression =>
          checkStepRules(step, BehaviorType.Context, env)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, None, None)
        }

      case r"""(.+?)$element can be located at index (\d+)$index in (.+?)$container by""" if step.table.nonEmpty && step.table.head._2.size == 2 => step =>
        checkStepRules(step, BehaviorType.Context, env)
        ctx.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val selectorType = row.head
          val expression = row(1)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, Some(container), None, Some(index.toInt))
        }

      case r"""(.+?)$element can be located in (.+?)$container by""" if step.table.nonEmpty && step.table.head._2.size == 2 => step =>
        checkStepRules(step, BehaviorType.Context, env)
        ctx.getLocatorBinding(container)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val selectorType = row.head
          val expression = row(1)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, Some(container), None, None)
        }

      case r"""(.+?)$element can be located at index (\d+)$index with no (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => step =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val selectorType = row.head
          val expression = row(1)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, Some(0), Some(index.toInt))
        }

      case r"""(.+?)$element can be located with no (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => step =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val selectorType = row.head
          val expression = row(1)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, Some(0), None)
        }

      case r"""(.+?)$element can be located at index (\d+)$index with (\d+)$timeout second (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => step =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val selectorType = row.head
          val expression = row(1)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, Some(timeout.toInt), Some(index.toInt))
        }

      case r"""(.+?)$element can be located with (\d+)$timeout second (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 => step =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val selectorType = row.head
          val expression = row(1)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, Some(timeout.toInt), None)
        }

      case r"""(.+?)$element can be located at index (\d+)$index by""" if step.table.nonEmpty && step.table.head._2.size == 2 => step =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val selectorType = row.head
          val expression = row(1)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, None, Some(index.toInt))
        }

      case r"""(.+?)$element can be located by""" if step.table.nonEmpty && step.table.head._2.size == 2 => step =>
        checkStepRules(step, BehaviorType.Context, env)
        env.scopes.set(s"$element/locator", step.table.map(_._2.head).mkString(","))
        step.table foreach { case (_, row ) =>
          val selectorType = row.head
          val expression = row(1)
          LocatorBinding.bind(env, element, SelectorType.parse(selectorType), expression, None, None, None)
        }

      case r"""(.+?)$element can be (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$event by (?:javascript|js) "(.+?)"$$$expression""" => step =>
        step.orDocString(expression) tap { expression =>
          checkStepRules(step, BehaviorType.Context, env)
          ctx.getLocatorBinding(element)
          env.scopes.set(JavaScriptBinding.key(s"$element/action/${WebEvents.EventToAction(event)}"), expression)
        }

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" => step =>
        step.orDocString(expression) tap { expression =>
          checkStepRules(step, BehaviorType.Assertion, env)
          val expected = ctx.parseExpression(operator, expression)
          ctx.perform {
            ctx.compare("title", expected, () => ctx.getTitle, operator, Option(negation).isDefined)
          }
        }

      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" => step =>
        checkStepRules(step, BehaviorType.Assertion, env)
        val expected = ctx.getBoundReferenceValue(attribute)
        ctx.perform {
          ctx.compare("title", expected, () => ctx.getTitle, operator, Option(negation).isDefined)
        }

      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" => step =>
        step.orDocString(expression) tap { expression =>
          checkStepRules(step, BehaviorType.Assertion, env)
          val expected = ctx.parseExpression(operator, expression)
          ctx.perform {
            ctx.compare(name, expected, () => ctx.getPopupMessage, operator, Option(negation).isDefined)
          }
        }

      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" => step =>
        checkStepRules(step, BehaviorType.Assertion, env)
        val expected = ctx.getBoundReferenceValue(attribute)
        ctx.perform {
          ctx.compare(name, expected, () => ctx.getPopupMessage, operator, Option(negation).isDefined)
        }

      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" => step =>
        checkStepRules(step, BehaviorType.Assertion, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.checkElementState(binding, state, Option(negation).nonEmpty)

      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" if !element.matches(".+at (json path|xpath).+") => step =>
        step.orDocString(expression) tap { expression =>
          checkStepRules(step, BehaviorType.Assertion, env)
          if (element == "I") undefinedStepError(step)
          if (element == "the current URL") ctx.captureCurrentUrl(None)
          val negate = Option(negation).isDefined
          val expected = ctx.parseExpression(operator, expression)
          val actual = ctx.boundAttributeOrSelection(element, Option(selection))
          ctx.perform {
            if (env.scopes.findEntry { case (n, _) => n.startsWith(element) } forall { case (n, _) => n != element }) {
              val nameSuffix = Option(selection)
              ctx.compare(element + nameSuffix.getOrElse(""), expected, actual, operator, negate, nameSuffix)
            } else {
              val actualValue = env.scopes.getOpt(element).getOrElse(actual())
              val result = ctx.compare(element, expected, actualValue, operator, negate)
              result match {
                case Success(assertion) =>
                  val binding = ctx.getLocatorBinding(element, optional = true)
                  assert(assertion, s"Expected ${binding.map(_.toString).getOrElse(element)} to ${if(negate) "not " else ""}$operator '$expected' but got '$actualValue'")
                case Failure(error) =>
                  assert(assertion = false, error.getMessage)
              }
            }
          } getOrElse {
            actual()
          }
        }

      case r"""(.+?)$element( value| text)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" if attribute != "absent" && !element.matches(".+at (json path|xpath).+") => step =>
        checkStepRules(step, BehaviorType.Assertion, env)
        if (element == "I") undefinedStepError(step)
        if (element == "the current URL") ctx.captureCurrentUrl(None)
        val expected = ctx.getBoundReferenceValue(attribute)
        val actual = ctx.boundAttributeOrSelection(element, Option(selection))
        ctx.perform {
          val nameSuffix = Option(selection)
          ctx.compare(element + nameSuffix.getOrElse(""), expected, actual, operator, Option(negation).isDefined, nameSuffix)
        } getOrElse  {
          actual()
        }

      case r"""I capture (.+?)$attribute (?:of|on|in) (.+?)$element by (?:javascript|js) "(.+?)"$$$expression""" => step => 
        step.orDocString(expression) tap { expression =>
          checkStepRules(step, BehaviorType.Action, env)
          val binding = ctx.getLocatorBinding(element)
          env.scopes.set(JavaScriptBinding.key(attribute), expression)
          try {
            ctx.perform {
              env.topScope.pushObject(s"${JavaScriptBinding.key(attribute)}/param/webElement", binding.resolve())
            }
            val value = ctx.getBoundReferenceValue(attribute)
            env.topScope.set(attribute, value tap { content =>
              env.addAttachment(attribute, "txt", content)
            })
          } finally {
            ctx.perform {
              env.topScope.popObject(s"${JavaScriptBinding.key(attribute)}/param/webElement")
            }
          }
        }

      case r"""I capture the current URL""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.captureCurrentUrl(None)

      case r"""I capture the current URL as (.+?)$name""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.captureCurrentUrl(Some(name))

      case r"""I capture the current screenshot""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.captureScreenshot(true)

      case r"""I capture the current screenshot as (.+?)$attribute""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.captureScreenshot(true, attribute) foreach { file =>
          env.scopes.set(attribute, file.getAbsolutePath)
        }

      case r"""I capture (.+?)$element( value| text)$selection as (.+?)$attribute""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        try {
          val value = ctx.boundAttributeOrSelection(element, Option(selection))
          env.topScope.set(attribute, value() tap { content =>
            env.addAttachment(attribute, "txt", content)
          })
        } catch {
          case _: LocatorBindingException =>
            super.translate(parent, step, env, ctx)
        }

      case r"""I capture (.+?)$element( value| text)$$$selection""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        try {
          val value = ctx.boundAttributeOrSelection(element, Option(selection))
          env.topScope.set(element, value() tap { content =>
            env.addAttachment(element, "txt", content)
          })
        } catch {
          case _: LocatorBindingException =>
            super.translate(parent, step, env, ctx)
        }

      case r"I capture the (alert|confirmation)$name popup message" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        env.topScope.set(s"the $name popup message", ctx.getPopupMessage tap { content =>
          env.addAttachment(s"the $name popup message", "txt", content)
        })

      case r"I capture the (?:alert|confirmation) popup message as (.+?)$attribute" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        env.topScope.set(attribute, ctx.getPopupMessage tap { content =>
          env.addAttachment(attribute, "txt", content)
        })

      case r"""I start visual test as "(.+?)"$testName in (\d+?)$width x (\d+?)$height viewport""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          ctx.startVisualTest(testName, Some(new RectangleSize(width.toInt, height.toInt)))
        } else {
          disabledStepError(step)
        }

      case r"""I start visual test as "(.+?)"$testName""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          ctx.startVisualTest(testName, None)
        } else {
          disabledStepError(step)
        }

      case r"""I check (viewport|full page)?$mode visual as "(.+?)"$name using (.+?)$matchLevel match""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          ctx.checkVisual(name, mode == "full page", Some(MatchLevel.valueOf(matchLevel)))
        } else {
          disabledStepError(step)
        }

      case r"""I check (viewport|full page)?$mode visual as "(.+?)"$name""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          ctx.checkVisual(name, mode == "full page", None)
        } else {
          disabledStepError(step)
        }

      case "the visual test should pass" => step =>
        checkStepRules(step, BehaviorType.Assertion, env)
        if (EyesSettings.`gwen.applitools.eyes.enabled`) {
          ctx.asertVisuals()
        } else {
          disabledStepError(step)
        }

      case r"""I drag and drop (.+?)$source to (.+?)$$$target""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val sourceBinding = ctx.getLocatorBinding(source)
        val targetBinding = ctx.getLocatorBinding(target)
        ctx.dragAndDrop(sourceBinding, targetBinding)

      case r"""I clear (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.performAction("clear", binding)

      case r"""I press (enter|tab)$key in (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.sendKeys(binding, Array[String](key))
        ctx.bindAndWait(element, key, "true")

      case r"""I send "(.+?)"$keys""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.sendKeys(keys.split(","))

      case r"""I send "(.+?)"$keys to (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.sendKeys(binding, keys.split(","))

      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.sendValue(binding, value, clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = WebSettings.`gwen.web.sendKeys.clearFirst`, sendEnterKey = action == "enter")

      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        val value = ctx.getBoundReferenceValue(attribute)
        ctx.sendValue(binding, value, clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = WebSettings.`gwen.web.sendKeys.clearFirst`, sendEnterKey = action == "enter")

      case r"""I (select|deselect)$action the (\d+?)$position(?:st|nd|rd|th) option in (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        if (action == "select") {
          ctx.selectByIndex(binding, position.toInt - 1)
        } else {
          ctx.deselectByIndex(binding, position.toInt - 1)
        }

      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$element by value""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        if (action == "select") {
          ctx.selectByValue(binding, value)
        } else {
          ctx.deselectByValue(binding, value)
        }

      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        if (action == "select") {
          ctx.selectByVisibleText(binding, value)
        } else {
          ctx.deselectByVisibleText(binding, value)
        }

      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$element by value""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val value = ctx.getBoundReferenceValue(attribute)
        val binding = ctx.getLocatorBinding(element)
        if (action == "select") {
          ctx.selectByValue(binding, value)
        } else {
          ctx.deselectByValue(binding, value)
        }

      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val value = ctx.getBoundReferenceValue(attribute)
        val binding = ctx.getLocatorBinding(element)
        if (action == "select") {
          ctx.selectByVisibleText(binding, value)
        } else {
          ctx.deselectByVisibleText(binding, value)
        }

      case r"""I (click|right click|double click|check|tick|uncheck|untick|move to)$action (.+?)$element of (.+?)$$$context""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.performActionInContext(action, element, context)

      case r"""I (click|right click|double click|submit|check|tick|uncheck|untick|move to)$action (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.performAction(action, binding)

      case r"""I (.+?)$modifiers (click|right click|double click)$clickAction (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.holdAndClick(modifiers.split("\\+"), clickAction, binding)

      case r"""I (?:highlight|locate) (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.perform {
          ctx.locateAndHighlight(binding)
        }

      case "I refresh the current page" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.refreshPage()

      case r"I start a new browser" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.close("primary")
        ctx.switchToSession("primary")

      case r"""I start a browser for (.+?)$$$session""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.close(session)
        ctx.switchToSession(session)

      case r"""I should have (\d+?)$count open browser(?:s?)""" => step =>
        checkStepRules(step, BehaviorType.Assertion, env)
        ctx.perform {
          ctx.compare("open browser sessions", count, () => ctx.noOfSessions().toString, "be", false)
        }

      case r"""I should have (\d+?)$count open (?:window|tab)(?:s?)""" => step =>
        checkStepRules(step, BehaviorType.Assertion, env)
        ctx.perform {
          ctx.compare("open windows/tabs", count, () => ctx.noOfWindows().toString, "be", false)
        }

      case r"I have (no|an)$open open browser" => step =>
        checkStepRules(step, BehaviorType.Context, env)
        if (open == "no") {
          ctx.close()
        } else {
          ctx.newOrCurrentSession()
        }

      case r"I close the(?: current)? browser" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.close()

      case r"""I close the browser for (.+?)$session""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.close(session)

      case r"""I switch to the child (?:window|tab)""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.perform {
          ctx.switchToChild()
        }

      case r"""I switch to child (?:window|tab) (\d+?)$occurrence""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.perform {
          ctx.switchToChild(occurrence.toInt)
        }

      case r"""I close the child (?:window|tab)""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.perform {
          ctx.closeChild()
        }

      case r"""I close child (?:window|tab) (\d+?)$occurrence""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.perform {
          ctx.closeChild(occurrence.toInt)
        }

      case r"""I switch to the (?:root|parent) (?:window|tab)""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.switchToParent()

      case """I switch to the default content""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.switchToDefaultContent()

      case r"""I switch to (.+?)$session""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.switchToSession(session)

      case r"I (accept|dismiss)$action the (?:alert|confirmation) popup" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.handleAlert(action == "accept")

      case r"""I resize the window to width (\d+?)$width and height (\d+?)$$$height""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.resizeWindow(width.toInt, height.toInt)

      case r"""I maximi(?:z|s)e the window""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        ctx.maximizeWindow()

      case r"""I append "(.+?)"$text to (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.sendValue(binding, text, clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = false, sendEnterKey = false)

      case r"""I append (.+?)$attribute to (.+?)$$$element""" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        val text = ctx.getBoundReferenceValue(attribute)
        ctx.sendValue(binding, text, clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = false, sendEnterKey = false)

      case r"I insert a new line in (.+?)$$$element" => step =>
        checkStepRules(step, BehaviorType.Action, env)
        val binding = ctx.getLocatorBinding(element)
        ctx.sendValue(binding, StringEscapeUtils.unescapeJava("""\n"""), clickFirst = WebSettings.`gwen.web.sendKeys.clickFirst`, clearFirst = false, sendEnterKey = false)

      case _ => super.translate(parent, step, env, ctx)

    }
  }
  
  override val DefaultRepeatDelay: Duration = {
    val waitSecs = WebSettings.`gwen.web.wait.seconds` 
    if (waitSecs > 9 && waitSecs % 10 == 0) Duration(waitSecs / 10, SECONDS) else Duration(waitSecs * 100, MILLISECONDS)
  }
  
}
