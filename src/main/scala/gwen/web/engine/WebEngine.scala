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

package gwen.web.engine

import gwen.web.engine.binding._
import gwen.web.engine.lambda.composite._
import gwen.web.engine.lambda.unit._

import gwen.core._
import gwen.core.engine.ComparisonOperator
import gwen.core.engine.EvalEngine
import gwen.core.engine.EvalEnvironment
import gwen.core.engine.lambda.CompositeStep
import gwen.core.engine.lambda.UnitStep
import gwen.core.model._
import gwen.core.model.gherkin.Step

import scala.concurrent.duration._

import com.applitools.eyes.MatchLevel

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
        case _ => 
          None
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
      case r"""I wait for (.+?)$element text for (\d+)$seconds second(?:s?)""" =>
        new WaitForText(element, Some(seconds.toInt), this, ctx)
      case r"""I wait for (.+?)$element text""" =>
        new WaitForText(element, None, this, ctx)
      case r"""I wait for (.+?)$element for (\d+)$seconds second(?:s?)""" =>
        new WaitForElement(element, Some(seconds.toInt), this, ctx)
      case r"""I wait for (.+?)$$$element""" =>
        new WaitForElement(element, None, this, ctx)
      case r"""I wait ([0-9]+?)$duration second(?:s?) when (.+?)$element is (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$$$event""" =>
        new WaitForElementOnEvent(element, ElementEvent.withName(event), duration.toLong, this, ctx)
      case r"""I wait until (.+?)$condition when (.+?)$element is (clicked|right clicked|double clicked||submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$$$event""" =>
        new WaitForConditionOnEvent(element, ElementEvent.withName(event), condition, this, ctx)
      case r"""I wait until "(.+?)$javascript"""" =>
        new WaitForCondition(step.orDocString(javascript), this, ctx)
      case r"""I wait until (.+?)$element is( not)?$negation (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" if env.scopes.getOpt(s"$element is${if(Option(negation).isDefined) " not" else ""} $state/javascript").isEmpty =>
        new WaitForElementState(element, ElementState.withName(state), Option(negation).isDefined, this, ctx)
      case r"""I wait until (.+?)$$$condition""" =>
        new WaitForBoundCondition(condition, this, ctx)
      case r"""I am on the (.+?)$$$page""" =>
        new CreatePageScope(page, this, ctx)
      case r"""I navigate to the (.+?)$$$page""" =>
        new NavigateToPageInScope(page, this, ctx)
      case r"""I navigate to "(.+?)"$$$url""" =>
        new NavigateToUrl(step.orDocString(url), this, ctx)
      case r"""I scroll to the (top|bottom)$position of (.+?)$$$element""" =>
        new ScrollToElement(element, ScrollTo.withName(position), this, ctx)
      case r"""the url will be defined by (?:property|setting) "(.+?)"$$$name""" =>
        new BindUrl(Settings.get(step.orDocString(name)), None, this, ctx)
      case r"""the url will be "(.+?)"$$$url""" =>
        new BindUrl(step.orDocString(url), None, this, ctx)
      case r"""the (.+?)$page url is "(.+?)"$$$url""" =>
        new BindUrl(step.orDocString(url), Some(page), this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index in (.+?)$container with no (?:timeout|wait)""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some(container), Some(0), Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container with no (?:timeout|wait)""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some(container), Some(0), None, this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some(container), Some(timeout.toInt), Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container with (\d+)$timeout second (?:timeout|wait)""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some(container), Some(timeout.toInt), None, this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index in (.+?)$container""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some(container), None, Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some(container), None, None, this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index with no (?:timeout|wait)""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, None, Some(0), Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression with no (?:timeout|wait)""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, None, Some(0), None, this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index with (\d+)$timeout second (?:timeout|wait)""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, None, Some(timeout.toInt), Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression with (\d+)$timeout second (?:timeout|wait)""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, None, Some(timeout.toInt), None, this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, None, None, Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located at index (\d+)$index by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), step.orDocString(expression), None, None, Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), step.orDocString(expression), None, None, None, this, ctx)
      case r"""(.+?)$element can be located at index (\d+)$index in (.+?)$container by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        new BindMultipleElementLocators(element, Some(container), None, Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located in (.+?)$container by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        new BindMultipleElementLocators(element, Some(container), None, None, this, ctx)
      case r"""(.+?)$element can be located at index (\d+)$index with no (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        new BindMultipleElementLocators(element, None, Some(0), Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located with no (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        new BindMultipleElementLocators(element, None, Some(0), None, this, ctx)
      case r"""(.+?)$element can be located at index (\d+)$index with (\d+)$timeout second (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        new BindMultipleElementLocators(element, None, Some(timeout.toInt), Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located with (\d+)$timeout second (?:timeout|wait) by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        new BindMultipleElementLocators(element, None, Some(timeout.toInt), None, this, ctx)
      case r"""(.+?)$element can be located at index (\d+)$index by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        new BindMultipleElementLocators(element, None, None, Some(index.toInt), this, ctx)
      case r"""(.+?)$element can be located by""" if step.table.nonEmpty && step.table.head._2.size == 2 =>
        new BindMultipleElementLocators(element, None, None, None, this, ctx)
      case r"""(.+?)$element can be (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$event by (?:javascript|js) "(.+?)"$$$expression""" =>
        new BindActionHandler(element, ElementEvent.withName(event), step.orDocString(expression), this, ctx)
      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" =>
        new CompareToValue("title", step.orDocString(expression), () => ctx.getTitle, ComparisonOperator.withName(operator), Option(negation).isDefined, this, ctx)
      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" =>
        new CompareToBoundValue("title", attribute, () => ctx.getTitle, ComparisonOperator.withName(operator), Option(negation).isDefined, this, ctx)
      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" =>
        new CompareToValue(name, step.orDocString(expression), () => ctx.getPopupMessage, ComparisonOperator.withName(operator), Option(negation).isDefined, this, ctx)
      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" =>
        new CompareToBoundValue(name, attribute, () => ctx.getPopupMessage, ComparisonOperator.withName(operator), Option(negation).isDefined, this, ctx)
      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$$$state""" =>
        new CompareElementState(element, ElementState.withName(state), Option(negation).nonEmpty, this, ctx)
      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$$$expression""" if !element.matches(".+at (json path|xpath).+") =>
        new CompareValueOrSelectionToValue(element, Option(selection).map(_.trim).map(DropdownSelection.withName), step.orDocString(expression), ComparisonOperator.withName(operator), Option(negation).isDefined, this, ctx)
      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$$$attribute""" if attribute != "absent" && !element.matches(".+at (json path|xpath).+") =>
        new CompareValueOrSelectionToBoundValue(element, Option(selection).map(_.trim).map(DropdownSelection.withName), attribute, ComparisonOperator.withName(operator), Option(negation).isDefined, this, ctx)
      case r"""I capture (.+?)$attribute (?:of|on|in) (.+?)$element by (?:javascript|js) "(.+?)"$$$expression""" =>
        new CaptureElementAttribute(element, attribute, step.orDocString(expression), this, ctx)
      case r"""I capture the current URL""" =>
        new CaptureUrl(None, this, ctx)
      case r"""I capture the current URL as (.+?)$name""" =>
        new CaptureUrl(Some(name), this, ctx)
      case r"""I capture the current screenshot""" =>
        new CaptureScreenshot(None, this, ctx)
      case r"""I capture the current screenshot as (.+?)$attribute""" =>
        new CaptureScreenshot(Some(attribute), this, ctx)
      case r"""I capture (.+?)$element( value| text)$selection as (.+?)$attribute""" =>
        new CaptureDropdownSelection(Some(attribute), element, DropdownSelection.withName(selection.trim), this, ctx)
      case r"""I capture (.+?)$element( value| text)$$$selection""" =>
        new CaptureDropdownSelection(None, element, DropdownSelection.withName(selection.trim), this, ctx)
      case r"I capture the (alert|confirmation)$name popup message" =>
        new CapturePopupMessage(s"the $name popup message", this, ctx)
      case r"I capture the (?:alert|confirmation) popup message as (.+?)$attribute" =>
        new CapturePopupMessage(attribute, this, ctx)
      case r"""I start visual test as "(.+?)"$testName in (\d+?)$width x (\d+?)$height viewport""" =>
        new StartVisualTest(testName, Some(width.toInt), Some(height.toInt), this, ctx)
      case r"""I start visual test as "(.+?)"$testName""" =>
        new StartVisualTest(testName, None, None, this, ctx)
      case r"""I check (viewport|full page)?$mode visual as "(.+?)"$name using (.+?)$matchLevel match""" =>
        new CheckViewPort(name, mode == "full page", Some(MatchLevel.valueOf(matchLevel)), this, ctx)
      case r"""I check (viewport|full page)?$mode visual as "(.+?)"$name""" =>
        new CheckViewPort(name, mode == "full page", None, this, ctx)
      case "the visual test should pass" =>
        new AssertVisuals(this, ctx)
      case r"""I drag and drop (.+?)$source to (.+?)$$$target""" =>
        new DragAndDrop(source, target, this, ctx)
      case r"""I clear (.+?)$$$element""" =>
        new ClearElement(element, this, ctx)
      case r"""I press (enter|tab)$key in (.+?)$$$element""" =>
        new SendKeyToElement(element, key, this, ctx)
      case r"""I send "(.+?)"$keys""" =>
        new SendKeysToBrowser(keys.split(","), this, ctx)
      case r"""I send "(.+?)"$keys to (.+?)$$$element""" =>
        new SendKeysToElement(element, keys.split(","), this, ctx)
      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$$$element""" =>
        new SendValueToElement(element, value, ElementAction.withName(action) == ElementAction.enter, this, ctx)
      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$$$element""" =>
        new SendBoundValueToElement(element, attribute, ElementAction.withName(action) == ElementAction.enter, this, ctx)
      case r"""I (select|deselect)$action the (\d+?)$position(?:st|nd|rd|th) option in (.+?)$$$element""" =>
        new ChangeDropdownSelection(element, DropdownSelection.index, position, false, ElementAction.withName(action), this, ctx)
      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$element by value""" =>
        new ChangeDropdownSelection(element, DropdownSelection.value, value, false, ElementAction.withName(action), this, ctx)
      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$$$element""" =>
        new ChangeDropdownSelection(element, DropdownSelection.text, value, false, ElementAction.withName(action), this, ctx)
      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$element by value""" =>
        new ChangeDropdownSelection(element, DropdownSelection.value, attribute, true, ElementAction.withName(action), this, ctx)
      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$$$element""" =>
        new ChangeDropdownSelection(element, DropdownSelection.text, attribute, true, ElementAction.withName(action), this, ctx)
      case r"""I (click|right click|double click|check|tick|uncheck|untick|move to)$action (.+?)$element of (.+?)$$$context""" =>
        new PerformElementAction(element, ElementAction.withName(action), Some(context), this, ctx)
      case r"""I (click|right click|double click|submit|check|tick|uncheck|untick|move to)$action (.+?)$$$element""" =>
        new PerformElementAction(element, ElementAction.withName(action), None, this, ctx)
      case r"""I (.+?)$modifiers (click|right click|double click)$clickAction (.+?)$$$element""" =>
        new HoldAndClick(element, modifiers.split("\\+"), ElementAction.withName(clickAction), this, ctx)
      case r"""I (?:highlight|locate) (.+?)$$$element""" =>
        new HighlightElement(element, this, ctx)
      case "I refresh the current page" =>
        new RefreshPage(this, ctx)
      case r"I start a new browser" =>
        new StartBrowserSession("primary", this, ctx)
      case r"""I start a browser for (.+?)$$$session""" =>
        new StartBrowserSession(session, this, ctx)
      case r"""I should have (\d+?)$count open browser(?:s?)""" =>
        new AssertBrowserCount(count.toInt, this, ctx)
      case r"""I should have (\d+?)$count open (?:window|tab)(?:s?)""" =>
        new AssertBrowserWindowCount(count.toInt, this, ctx)
      case r"I have (no|an)$open open browser" =>
        new OpenOrCloseBrowser(open == "an", None, BehaviorType.Context, this, ctx)
      case r"I close the(?: current)? browser" =>
        new OpenOrCloseBrowser(false, None, BehaviorType.Action, this, ctx)
      case r"""I close the browser for (.+?)$session""" =>
        new OpenOrCloseBrowser(false, Some(session), BehaviorType.Action, this, ctx)
      case r"""I switch to the child (?:window|tab)""" =>
        new SwitchToChildWindow(None, this, ctx)
      case r"""I switch to child (?:window|tab) (\d+?)$occurrence""" =>
        new SwitchToChildWindow(Some(occurrence.toInt), this, ctx)
      case r"""I close the child (?:window|tab)""" =>
        new OpenOrCloseChildWindow(false, None, this, ctx)
      case r"""I close child (?:window|tab) (\d+?)$occurrence""" =>
        new OpenOrCloseChildWindow(false, Some(occurrence.toInt), this, ctx)
      case r"""I switch to the (?:root|parent) (?:window|tab)""" =>
        new SwitchToRootWindow(this, ctx)
      case """I switch to the default content""" =>
        new SwitchToDefaultContent(this, ctx)
      case r"""I switch to (.+?)$session""" =>
        new SwitchToBrowserSession(session, this, ctx)
      case r"I (accept|dismiss)$action the (?:alert|confirmation) popup" =>
        new HandlePopup(PopupAction.withName(action), this, ctx)
      case r"""I resize the window to width (\d+?)$width and height (\d+?)$$$height""" =>
        new ResizeWindow(width.toInt, height.toInt, this, ctx)
      case r"""I maximi(?:z|s)e the window""" =>
        new MaximiseWindow(this, ctx)
      case r"""I append "(.+?)"$text to (.+?)$$$element""" =>
        new AppendTextToElement(element, text, false, this, ctx)
      case r"""I append (.+?)$attribute to (.+?)$$$element""" =>
        new AppendTextToElement(element, attribute, true, this, ctx)
      case r"I insert a new line in (.+?)$$$element" =>
        new AppendNewLineToElement(element, this, ctx)
      case _ => 
        super.translate(parent, step, env, ctx)
    }
  }
  
  override val DefaultRepeatDelay: Duration = {
    val waitSecs = WebSettings.`gwen.web.wait.seconds` 
    if (waitSecs > 9 && waitSecs % 10 == 0) Duration(waitSecs / 10, SECONDS) else Duration(waitSecs * 100, MILLISECONDS)
  }
  
}
