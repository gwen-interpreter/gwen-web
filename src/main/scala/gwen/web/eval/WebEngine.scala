/*
 * Copyright 2014-2025 Branko Juric, Brady Wood
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

import gwen.core.ValueLiteral
import gwen.web.eval.binding._
import gwen.web.eval.driver.DriverManager
import gwen.web.eval.action.composite._
import gwen.web.eval.action.unit._

import gwen.core._
import gwen.core.behavior.BehaviorType
import gwen.core.eval.ComparisonOperator
import gwen.core.eval.EvalEngine
import gwen.core.eval.action.CompositeStepAction
import gwen.core.eval.action.UnitStepAction
import gwen.core.node.gherkin.Step
import gwen.core.state.EnvState

import org.openqa.selenium.WindowType

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.chaining._

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
    * @param envState the initial environment state
    */
  override def init(options: GwenOptions, envState: EnvState): WebContext = {
    WebSettings.check()
    Grid.impl.foreach { grid =>
      val disableVideo = sys.env.get("GWEN_VIDEO").map(v => v.trim != "true").getOrElse(false)
      val selenoidParallel = grid == Grid.selenoid && options.parallel
      if (disableVideo || ((selenoidParallel || !options.batch) && grid.videoEnabled)) {
        logger.info(s"Disabling video in ${if (selenoidParallel) "parallel" else "interactive"} mode")
        sys.props.put(grid.enableVideoKey, "false")
      }
    }
    if (WebSettings.`gwen.web.capture.screenshots.highlighting`) {
      val fps = GwenSettings.`gwen.report.slideshow.framespersecond`
      Settings.setLocal("gwen.report.slideshow.framespersecond", (fps.toDouble * 1.8d).toInt.toString)
    }
    new WebContext(options, envState, new DriverManager()) tap { _ =>
      logger.info(s"Web context initialised")
    }
  }

  /**
    * Translates composite web engine steps.
    */
  override def translateCompositeStep(step: Step): Option[CompositeStepAction[WebContext]] = {
    step.expression.match {
      case r"""(.+)$doStep if(?:(?!\bif\b)) (.+?)$element is( not)?$negation (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$state""" =>
        Some(new IfElementCondition(doStep, element, ElementState.valueOf(state), Option(negation).nonEmpty, this))
      case r"""(.+?)$doStep (until|while)$operation (.+?)$element is( not)?$negation (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$state""" if (doStep != "I wait" && !step.expression.matches(""".*".*(until|while).*".*""")) =>
        Some(new RepeatElementState(doStep, operation, element, ElementState.valueOf(state), Option(negation).nonEmpty, step.delayOpt.getOrElse(Duration(1, TimeUnit.SECONDS)), step.timeoutOpt.getOrElse(Duration(1, TimeUnit.MINUTES)), this))
      case _ =>
        super.translateCompositeStep(step) orElse {
          step.expression match {
            case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container""" =>
              Some(new ForEachWebElement(doStep, element, SelectorType.parse(selectorType), expression, Some((RelativeSelectorType.in, container, None)), step.timeoutOpt, step.isShadowRoot, this))
            case r"""(.+?)$doStep for each (.+?)$element located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression""" =>
              Some(new ForEachWebElement(doStep, element, SelectorType.parse(selectorType), step.orDocString(expression), None, step.timeoutOpt, step.isShadowRoot, this))
            case r"""(.+?)$doStep for each (.+?)$element in (.+?)$iteration""" =>
              Some(new ForEachWebElementInIteration(doStep, element, iteration, this))
            case _ => 
              None
          }
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
  override def translateStep(step: Step): UnitStepAction[WebContext] = {
    step.expression match {
      case r"""I wait for (.+?)$element text""" =>
        new WaitForText(element, step.timeoutOpt.map(_.toSeconds))
      case r"""I wait for (.+?)$element""" =>
        new WaitForElement(element, step.timeoutOpt.map(_.toSeconds))
      case r"""I wait until "(.+?)$javascript"""" =>
        new WaitForCondition(step.orDocString(javascript), step.delayOpt.map(_.toMillis), step.timeoutOpt.map(_.toSeconds))
      case r"""I wait until (.+?)$element is( not)?$negation (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$state""" =>
        new WaitForElementState(element, ElementState.valueOf(state), Option(negation).nonEmpty, step.timeoutOpt.map(_.toSeconds))
      case r"""I wait until (.+?)$condition""" if !condition.matches(".+ file (exists|not exists|does not exist|is empty|is not empty)") =>
        new WaitForBoundCondition(condition, step.delayOpt.map(_.toMillis), step.timeoutOpt.map(_.toSeconds))
      case r"""I navigate to "(.+?)"$url""" =>
        new NavigateToUrl(step.orDocString(url))
      case r"""I scroll to the (top|bottom)$position of the page""" =>
        new ScrollPage(ScrollTo.valueOf(position))
      case r"""I scroll to the (top|bottom)$position of (.+?)$element""" =>
        new ScrollToElement(element, ScrollTo.valueOf(position))
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index in (.+?)$container""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some((RelativeSelectorType.in, container, None)), step.timeoutOpt.map(_.toSeconds), Some(index.toInt), step.isShadowRoot)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression in (.+?)$container""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some((RelativeSelectorType.in, container, None)), step.timeoutOpt.map(_.toSeconds), None, step.isShadowRoot)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text)$selectorType "(.+?)"$expression near and within (\d+)$pixels pixel(?:s?) of (.+?)$rElement""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some((RelativeSelectorType.near, rElement, Some(pixels.toInt))), step.timeoutOpt.map(_.toSeconds), None, step.isShadowRoot)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text)$selectorType "(.+?)"$expression (above|below|near|to left of|to right of)$rSelectorType (.+?)$rElement""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, Some((RelativeSelectorType.valueOf(rSelectorType), rElement, None)), step.timeoutOpt.map(_.toSeconds), None, step.isShadowRoot)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression at index (\d+)$index""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), expression, None, step.timeoutOpt.map(_.toSeconds), Some(index.toInt), step.isShadowRoot)
      case r"""(.+?)$element can be located at index (\d+)$index by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), step.orDocString(expression), None, step.timeoutOpt.map(_.toSeconds), Some(index.toInt), step.isShadowRoot)
      case r"""(.+?)$element can be located by (id|name|tag name|tag|css selector|css|xpath|class name|class|link text|partial link text|javascript|js)$selectorType "(.+?)"$expression""" =>
        new BindElementLocator(element, SelectorType.parse(selectorType), step.orDocString(expression), None, step.timeoutOpt.map(_.toSeconds), None, step.isShadowRoot)
      case r"""(.+?)$element can be located at index (\d+)$index in (.+?)$container by""" if step.hasDualColumnTable =>
        new BindMultipleElementLocators(element, Some(container), step.timeoutOpt.map(_.toSeconds), Some(index.toInt))
      case r"""(.+?)$element can be located in (.+?)$container by""" if step.hasDualColumnTable =>
        new BindMultipleElementLocators(element, Some(container), step.timeoutOpt.map(_.toSeconds), None)
      case r"""(.+?)$element can be located at index (\d+)$index by""" if step.hasDualColumnTable =>
        new BindMultipleElementLocators(element, None, step.timeoutOpt.map(_.toSeconds), Some(index.toInt))
      case r"""(.+?)$element can be located by""" if step.hasDualColumnTable =>
        new BindMultipleElementLocators(element, None, step.timeoutOpt.map(_.toSeconds), None)
      case r"""(.+?)$element can be (clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to)$event by (?:javascript|js) "(.+?)"$expression""" =>
        new BindActionHandler(element, ElementEvent.valueOf(event), step.orDocString(expression))
      case r"""the page title should( not)?$negation be (blank|empty|true|false)$literal""" =>
        new CompareTitle("title", ValueLiteral.valueOf(literal).value, false, ComparisonOperator.be, Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$expression""" =>
        new CompareTitle("title", step.orDocString(expression), false, ComparisonOperator.valueOf(operator), Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""the page title should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$attribute""" =>
        new CompareTitle("title", attribute, true, ComparisonOperator.valueOf(operator), Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""the (alert|confirmation)$name popup message should( not)?$negation be (blank|empty|true|false)$literal""" =>
        new ComparePopupMessage(name, ValueLiteral.valueOf(literal).value, false, ComparisonOperator.be, Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$expression""" =>
        new ComparePopupMessage(name, step.orDocString(expression), false, ComparisonOperator.valueOf(operator), Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""the (alert|confirmation)$name popup message should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$attribute""" =>
        new ComparePopupMessage(name, attribute, true, ComparisonOperator.valueOf(operator), Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""(.+?)$element should( not)?$negation be (displayed|hidden|checked|ticked|unchecked|unticked|enabled|disabled)$state""" =>
        new CompareElementState(element, ElementState.valueOf(state), Option(negation).nonEmpty, step.message, step.timeoutOpt)
      case r"""(.+?)$element( text| value)?$selection should( not)?$negation be (blank|empty|true|false)$literal""" if !element.matches(".+at (json path|xpath).+") && !element.matches(".+? file") =>
        new CompareValueOrSelectionToValue(element, Option(selection).map(_.trim).map(DropdownSelection.valueOf), ValueLiteral.valueOf(literal).value, ComparisonOperator.be, Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path|match template|match template file)$operator "(.*?)"$expression""" if !element.matches(".+at (json path|xpath).+") =>
        new CompareValueOrSelectionToValue(element, Option(selection).map(_.trim).map(DropdownSelection.valueOf), step.orDocString(expression), ComparisonOperator.valueOf(operator), Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""(.+?)$element( text| value)?$selection should( not)?$negation (be|contain|start with|end with|match regex|match xpath|match json path)$operator (.+?)$attribute""" if !attribute.matches("(absent|defined|empty|no accumulated errors)") && !attribute.contains("% similar to ") && !attribute.contains('"') && !element.matches(".+at (json path|xpath).+") =>
        new CompareValueOrSelectionToBoundValue(element, Option(selection).map(_.trim).map(DropdownSelection.valueOf), attribute, ComparisonOperator.valueOf(operator), Option(negation).nonEmpty, step.message, step.timeoutOpt, step.isTrim, step.isIgnoreCase)
      case r"""I capture (.+?)$attribute (?:of|on|in) (.+?)$element by (?:javascript|js) "(.+?)"$expression""" =>
        new CaptureElementAttribute(element, attribute, step.orDocString(expression))
      case r"""I capture the current URL""" =>
        new CaptureUrl(None)
      case r"""I capture the current URL as (.+?)$name""" =>
        new CaptureUrl(Some(name))
      case r"""I capture the current screenshot""" =>
        new CaptureScreenshot(None)
      case r"""I capture the current screenshot as (.+?)$attribute""" =>
        new CaptureScreenshot(Some(attribute))
      case r"""I capture element screenshot of (.+?)$element as (.+?)$attribute""" =>
        new CaptureElementScreenshot(element, Some(attribute))
      case r"""I capture element screenshot of (.+?)$element""" =>
        new CaptureElementScreenshot(element, None)
      case r"""I capture the PDF text from the current URL as (.+?)$name""" =>
        new CaptureCurrentUrlPDF(name)
      case r"""I capture the PDF text from the current URL""" =>
        new CaptureCurrentUrlPDF("the PDF text")
      case r"""I capture (.+?)$element( value| text)$selection as (.+?)$attribute""" if !element.startsWith("the PDF text from") =>
        new CaptureDropdownOrElement(Some(attribute), element, DropdownSelection.valueOf(selection.trim))
      case r"""I capture (.+?)$element( value| text)$selection""" if !element.startsWith("the PDF text from") =>
        new CaptureDropdownOrElement(None, element, DropdownSelection.valueOf(selection.trim))
      case r"I capture the (alert|confirmation)$name popup message" =>
        new CapturePopupMessage(s"the $name popup message")
      case r"I capture the (?:alert|confirmation) popup message as (.+?)$attribute" =>
        new CapturePopupMessage(attribute)
      case r"""I drag and drop (.+?)$source to (.+?)$target""" =>
        new DragAndDrop(source, target)
      case r"""I clear (.+?)$element""" =>
        new ClearElement(element)
      case r"""I press (enter|tab)$key in (.+?)$element""" =>
        new SendKeyToElement(element, key)
      case r"""I send "(.+?)"$keys""" =>
        new SendKeysToBrowser(keys.split("[,\\+]"))
      case r"""I send "(.+?)"$keys to (.+?)$element""" =>
        new SendKeysToElement(element, keys.split("[,\\+]"))
      case r"""I (enter|type)$action "(.*?)"$value in (.+?)$element""" =>
        new SendValueToElement(element, value, ElementAction.valueOf(action) == ElementAction.enter)
      case r"""I (enter|type)$action (.+?)$attribute in (.+?)$element""" =>
        new SendBoundValueToElement(element, attribute, ElementAction.valueOf(action) == ElementAction.enter)
      case r"""I (select|deselect)$action the (\d+?)$position(?:st|nd|rd|th) option in (.+?)$element""" =>
        new ChangeDropdownSelection(element, DropdownSelection.index, position, false, ElementAction.valueOf(action))
      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$element by value""" =>
        new ChangeDropdownSelection(element, DropdownSelection.value, value, false, ElementAction.valueOf(action))
      case r"""I (select|deselect)$action "(.*?)"$value in (.+?)$element""" =>
        new ChangeDropdownSelection(element, DropdownSelection.text, value, false, ElementAction.valueOf(action))
      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$element by value""" =>
        new ChangeDropdownSelection(element, DropdownSelection.value, attribute, true, ElementAction.valueOf(action))
      case r"""I (select|deselect)$action (.+?)$attribute in (.+?)$element""" =>
        new ChangeDropdownSelection(element, DropdownSelection.text, attribute, true, ElementAction.valueOf(action))
      case r"""I (click|right click|double click|check|tick|uncheck|untick|move to)$action (.+?)$element of (.+?)$context""" =>
        new PerformElementAction(element, ElementAction.valueOf(action), Some(context))
      case r"""I (click|right click|double click|submit|check|tick|uncheck|untick|move to)$action (.+?)$element""" =>
        new PerformElementAction(element, ElementAction.valueOf(action), None)
      case r"""I (.+?)$modifiers (click|right click|double click)$clickAction (.+?)$element""" =>
        new HoldAndClick(element, modifiers.split("\\+"), ElementAction.valueOf(clickAction))
      case r"""I (?:highlight|locate) (.+?)$element""" =>
        new HighlightElement(element)
      case "I refresh the current page" =>
        new RefreshPage()
      case r"I start a new browser" =>
        new StartBrowserSession("primary")
      case r"I start a new browser (tab|window)$winType" =>
        new SwitchToNewWindow(WindowType.valueOf(winType.toUpperCase))
      case r"""I start a browser for (.+?)$session""" =>
        new StartBrowserSession(session)
      case r"""I should have (\d+?)$count open browser(?:s?)""" =>
        new AssertBrowserCount(count.toInt, step.message)
      case r"""I should have (\d+?)$count open (?:window|tab)(?:s?)""" =>
        new AssertBrowserWindowCount(count.toInt, step.message)
      case r"I have (no|an)$open open browser" =>
        new OpenOrCloseBrowser(open == "an", None, BehaviorType.Context)
      case r"I close the(?: current)? browser" =>
        new OpenOrCloseBrowser(false, None, BehaviorType.Action)
      case r"""I close the browser for (.+?)$session""" =>
        new OpenOrCloseBrowser(false, Some(session), BehaviorType.Action)
      case r"""I switch to the child (?:window|tab)""" =>
        new SwitchToWindow(None)
      case r"""I switch to child (?:window|tab) (\d+?)$occurrence""" =>
        new SwitchToWindow(Some(occurrence.toInt))
      case r"""I switch to (?:window|tab) (\d+?)$occurrence""" =>
        new SwitchToWindow(Some(occurrence.toInt))
      case r"""I close the child (?:window|tab)""" =>
        new OpenOrCloseWindow(false, None)
      case r"""I close child (?:window|tab) (\d+?)$occurrence""" =>
        new OpenOrCloseWindow(false, Some(occurrence.toInt))
      case r"""I close (?:window|tab) (\d+?)$occurrence""" =>
        new OpenOrCloseWindow(false, Some(occurrence.toInt))
      case r"""I switch to the (?:root|parent) (?:window|tab)""" =>
        new SwitchToRootWindow()
      case """I switch to the default content""" =>
        new SwitchToDefaultContent()
      case r"""I switch to (.+?)$frame content""" =>
        new SwitchToFrame(frame)
      case r"""I switch to (.+?)$session""" =>
        new SwitchToBrowserSession(session)
      case r"I (accept|dismiss)$action the (?:alert|confirmation) popup" =>
        new HandlePopup(PopupAction.valueOf(action))
      case r"""I resize the window to width (\d+?)$width and height (\d+?)$height""" =>
        new ResizeWindow(width.toInt, height.toInt)
      case r"""I set the window position to x (\d+?)$x and y (\d+?)$y""" =>
        new PositionWindow(x.toInt, y.toInt)
      case r"""I maximi(?:z|s)e the window""" =>
        new MaximiseWindow()
      case r"""I append "(.+?)"$text to (.+?)$element""" if !element.endsWith("file") =>
        new AppendTextToElement(element, text, false)
      case r"""I append (.+?)$attribute to (.+?)$element""" if !element.endsWith("file") =>
        new AppendTextToElement(element, attribute, true)
      case r"I insert a new line in (.+?)$element" =>
        new AppendNewLineToElement(element)
      case r"""I download the current URL to "(.+?)"$filepath""" =>
        new DownloadCurrentUrlToFile(Some(filepath), None, defaultConditionTimeoutSecs)
      case r"""I download the current URL to (.+? file)$filepathRef""" =>
        new DownloadCurrentUrlToFile(None, Some(filepathRef), defaultConditionTimeoutSecs)
      case r"""(.+?)$attribute is defined by (.+?)$function applied to (.+?)$element""" if !element.contains("\"") =>
        new BindElementFunction(attribute, function, element, step.isMasked)
      case _ => 
        super.translateStep(step)
    }
  }
  
  override def defaultConditionTimeoutSecs: Long = WebSettings.`gwen.web.wait.seconds`

  override def defaultRepeatDelay: Duration = {
    val waitSecs = WebSettings.`gwen.web.wait.seconds` 
    if (waitSecs > 9 && waitSecs % 10 == 0) Duration(waitSecs / 10, SECONDS) else Duration(waitSecs * 100, MILLISECONDS)
  }
  
}
