/*
 * Copyright 2016-2021 Brady Wood, Branko Juric
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

import gwen.web._
import gwen.web.eval.binding._

import gwen.core._
import gwen.core.Errors
import gwen.core.eval.ComparisonOperator
import gwen.core.node.Root
import gwen.core.node.gherkin.SpecType
import gwen.core.node.gherkin.Step
import gwen.core.node.gherkin.StepKeyword
import gwen.core.node.gherkin.table.FlatTable
import gwen.core.state._
import gwen.core.status.Pending

import scala.util.chaining._

import org.apache.commons.text.StringEscapeUtils
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar

import org.openqa.selenium.WebElement

import java.io.File
import org.scalatest.matchers.should.Matchers

class WebEngineTest extends BaseTest with Matchers with MockitoSugar with BeforeAndAfterEach {

  val engine = new WebEngine()

  val templateFile: File = {
    val rootDir: File = new File("target" + File.separator + "WebEngineTest") tap { _.mkdirs() }
    val file = new File(rootDir, "value.template")
    file.getParentFile.mkdirs()
    file.createNewFile()
    file.writeText("value")
    file.deleteOnExit()
    file
  }

  private val selectorTypes = List(
    "id", "name", "tag name", "tag", "css selector", "css", "xpath", "class name", "class", "link text", "partial link text", "javascript", "js"
  ).map(s => (s, SelectorType.parse(s)))

  private val matchers =
    List(
      (ComparisonOperator.be, "value", "value"),
      (ComparisonOperator.contain, "value", "a"),
      (ComparisonOperator.`start with`, "value", "v"),
      (ComparisonOperator.`end with`, "value", "e"),
      (ComparisonOperator.`match regex`, "value", "value"),
      (ComparisonOperator.`match xpath`, "<value>x</value>", "/value"),
      (ComparisonOperator.`match json path`, """{value:"x"}""", "$.value"),
      (ComparisonOperator.`match template`, "value", "value"),
      (ComparisonOperator.`match template file`, "value", templateFile.getPath)
    )
  private val matchers2 = matchers.filter(!_._1.toString.contains("template"))
  private val matchers3 = matchers.filter(!_._1.toString.contains("path"))
    List(
      (ComparisonOperator.be, "value", "value"),
      (ComparisonOperator.contain, "value", "a"),
      (ComparisonOperator.`start with`, "value", "v"),
      (ComparisonOperator.`end with`, "value", "e"),
      (ComparisonOperator.`match regex`, "value", "value"),
      (ComparisonOperator.`match xpath`, "<value>x</value>", "/value"),
      (ComparisonOperator.`match json path`, """{value:"x"}""", "$.value"),
      (ComparisonOperator.`match template`, "value", "value"),
      (ComparisonOperator.`match template file`, "value", templateFile.getPath)
    )
  private val elemStates = ElementState.values.toList
  private val events = ElementEvent.values.toList
  private val timeUnits1 = List("second", "millisecond")
  private val timeUnits2= List("minute", "second", "millisecond")
  private val waits = List("wait", "timeout")

  private var envState: EnvState = _
  private var ctx: WebContext = _
  private var mockScopes: ScopedDataStack = _
  private var mockTopScope: TopScope = _
  private var mockParamScope: ParameterStack = _
  private var mockLocator: WebElementLocator = _

  override def beforeEach(): Unit = {
    envState = spy(EnvState())
    ctx = spy(new WebContext(GwenOptions(), envState, mock[DriverManager]))
    mockScopes = mock[ScopedDataStack]
    mockTopScope = mock[TopScope]
    mockParamScope = mock[ParameterStack]
    mockLocator = mock[WebElementLocator]
    doReturn(mockScopes).when(envState).scopes
    doReturn(mockTopScope).when(mockScopes).topScope
    doReturn(mockParamScope).when(mockScopes).paramScope
    doReturn(mockLocator).when(ctx).locator
    doReturn(false).when(ctx).isEvaluatingTopLevelStep
    doReturn(SpecType.Meta).when(ctx).specType
    when(mockTopScope.getOpt("gwen.feature.file.path")).thenReturn(Some("file.feature"))
  }

  private def evaluate(name: String): Step = {
    val step = Step(None, StepKeyword.Given.toString, name, Nil, None, Nil, None, Pending, Nil, Nil)
    engine.evaluateStep(Root, step, ctx)
  }

  "I am on the <page>" should "evaluate" in {
    evaluate("I am on the <page>")
    verify(mockScopes).addScope("<page>")
  }

  """the url will be "<url>""""" should "evaluate" in {
    evaluate("""the url will be "<url>"""")
    verify(mockScopes).set("url", "<url>")
  }

  """the url will be defined by property "<name>""""" should "evaluate" in {
    withSetting("<name>", "http://site.com") {
      evaluate("""the url will be defined by property "<name>"""")
      verify(mockScopes).set("url", "http://site.com")
    }
  }

  """the url will be defined by setting "<name>"""" should "evaluate" in {
    withSetting("<name>", "http://site.com") {
      evaluate("""the url will be defined by setting "<name>"""")
      verify(mockScopes).set("url", "http://site.com")
    }
  }

  """the <page> url is "<url>""""" should "evaluate" in {
    evaluate("""the page url is "https://site.com"""")
    verify(mockScopes).addScope("page")
    verify(mockScopes).set("url", "https://site.com")
  }

  "I navigate to the <page>" should "evaluate" in {
    doReturn("http://home.com").when(ctx).getAttribute("url")
    evaluate("I navigate to the <page>")
    verify(mockScopes).addScope("<page>")
    verify(ctx).navigateTo("http://home.com")
  }

  """I navigate to "<url>""""" should "evaluate" in {
    evaluate("""I navigate to "<url>"""")
    verify(ctx).navigateTo("<url>")
  }

  "I scroll to the top of <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).scrollIntoView(mockBinding, ScrollTo.top)
    evaluate("I scroll to the top of <element>")
    verify(ctx).scrollIntoView(mockBinding, ScrollTo.top)
  }

  "I scroll to the bottom of <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).scrollIntoView(mockBinding, ScrollTo.bottom)
    evaluate("I scroll to the bottom of <element>")
    verify(ctx).scrollIntoView(mockBinding, ScrollTo.bottom)
  }

  """<element> can be located by <locator> "<value>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(None)
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      evaluate(s"""<element> can be located by $selectorType "<value>" in <container>""")
      verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", "<container>")
    }
  }

   """<element> can be located by <locator> "<value>" at index <index> in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(None)
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      evaluate(s"""<element> can be located by $selectorType "<value>" at index 2 in <container>""")
      verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", "<container>")
    }
  }

  """<element> can be located by <locator> "<value>" in <container> with no <wait|timeout>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      waits.foreach { wait =>
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
        evaluate(s"""<element> can be located by $selectorType "<value>" in <container> with no $wait""")
        verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", "<container>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
        reset(mockScopes)
      }
    }
  }

  """<element> can be located by <locator> "<value>" at index 2 in <container> with no <wait|timeout>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      waits.foreach { wait =>
        evaluate(s"""<element> can be located by $selectorType "<value>" at index 2 in <container> with no $wait""")
        verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", "<container>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
        reset(mockScopes)
      }
    }
  }

  """<element> can be located by <locator> "<value>" in <container> with <timeoutPeriod> second <wait|timeout>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      waits.foreach { wait =>
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
        evaluate(s"""<element> can be located by $selectorType "<value>" in <container> with 2 second $wait""")
        verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", "<container>")
        reset(mockScopes)
      }
    }
  }

   """<element> can be located by <locator> "<value>" at index 2 in <container> with <timeoutPeriod> second <wait|timeout>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      waits.foreach { wait =>
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
        evaluate(s"""<element> can be located by $selectorType "<value>" at index 2 in <container> with 2 second $wait""")
        verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", "<container>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "2")
        reset(mockScopes)
      }
    }
  }

  """<element> can be located by <locator> "<value>"""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/container")).thenReturn(Some("container"))
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      evaluate(s"""<element> can be located by $selectorType "<value>"""")
      verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", null)
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", null)
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", null)
    }
  }

  """<element> can be located by <locator> "<value>" at index 2""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/container")).thenReturn(Some("container"))
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
      when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      evaluate(s"""<element> can be located by $selectorType "<value>" at index 2""")
      verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", null)
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", null)
      verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
    }
  }

   """<element> can be located by <locator> "<value>" with no <wait|timeout>""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      waits.foreach { wait =>
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/container")).thenReturn(Some("container"))
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
        evaluate(s"""<element> can be located by $selectorType "<value>" with no $wait""")
        verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", null)
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", null)
        reset(mockScopes)
      }
    }
  }

   """<element> can be located by <locator> "<value>" at index 2 with no <wait|timeout>""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      waits.foreach { wait =>
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/container")).thenReturn(Some("container"))
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
        evaluate(s"""<element> can be located by $selectorType "<value>" at index 2 with no $wait""")
        verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", null)
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
        reset(mockScopes)
      }
    }
  }

  """<element> can be located by <locator> "<value>" with <timeoutPeriod> second <wait|timeout>""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      waits.foreach { wait =>
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/container")).thenReturn(Some("container"))
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
        evaluate(s"""<element> can be located by $selectorType "<value>" with 2 second $wait""")
        verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", null)
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "2")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", null)
        reset(mockScopes)
      }
    }
  }

  """<element> can be located by <locator> "<value>" at index 2 with <timeoutPeriod> second <wait|timeout>""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      waits.foreach { wait =>
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/container")).thenReturn(Some("container"))
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
        when(mockScopes.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
        evaluate(s"""<element> can be located by $selectorType "<value>" at index 2 with 2 second $wait""")
        verify(mockScopes, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/container", null)
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "2")
        verify(mockScopes, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
        reset(mockScopes)
      }
    }
  }

  """the page title should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(source).when(ctx).getTitle
      evaluate(s"""the page title should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """the page title should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getTitle
      evaluate(s"""the page title should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the page title should <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(source).when(ctx).getTitle
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"""the page title should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the page title should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getTitle
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"""the page title should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

   """the alert popup message should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(source).when(ctx).getPopupMessage
      evaluate(s"""the alert popup message should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """the alert popup message should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getPopupMessage
      evaluate(s"""the alert popup message should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the alert popup message should <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(source).when(ctx).getPopupMessage
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"""the alert popup message should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the alert popup message should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getPopupMessage
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"""the alert popup message should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """the confirmation popup message should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(source).when(ctx).getPopupMessage
      evaluate(s"""the confirmation popup message should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """the confirmation popup message should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getPopupMessage
      evaluate(s"""the confirmation popup message should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the confirmation popup message should <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(source).when(ctx).getPopupMessage
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"""the confirmation popup message should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the confirmation popup message should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getPopupMessage
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"""the confirmation popup message should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "<element> should be <state>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).jsEquivalent
    elemStates.foreach { state =>
      doNothing().when(ctx).checkElementState(mockBinding, state, negate = false)
      evaluate(s"<element> should be $state")
      verify(ctx).checkElementState(mockBinding, state, negate = false)
    }
  }

   "<element> should not be <state>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).jsEquivalent
    elemStates.foreach { state =>
      doNothing().when(ctx).checkElementState(mockBinding, state, negate = true)
      evaluate(s"<element> should not be $state")
      verify(ctx).checkElementState(mockBinding, state, negate = true)
    }
  }

  "I wait until <element> is <state>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).jsEquivalent
    elemStates.foreach { state =>
      doReturn(None).when(mockScopes).getOpt(s"<element> is $state/javascript")
      doNothing().when(ctx).waitForElementState(mockBinding, state, false)
      evaluate(s"I wait until <element> is $state")
      verify(ctx).waitForElementState(mockBinding, state, false)
    }
  }

  "I wait until <element> is not <state>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).jsEquivalent
    elemStates.foreach { state =>
      doReturn(None).when(mockScopes).getOpt(s"<element> is not $state/javascript")
      doNothing().when(ctx).waitForElementState(mockBinding, state, true)
      evaluate(s"I wait until <element> is not $state")
      verify(ctx).waitForElementState(mockBinding, state, true)
    }
  }

  """<reference> should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<reference>", None)
      doReturn(None).when(mockScopes).getOpt("<reference>")
      evaluate(s"""<reference> should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<reference> should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<reference>", None)
      doReturn(None).when(mockScopes).getOpt("<reference>")
      evaluate(s"""<reference> should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<captured> should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(Some(("<captured>", source))).when(mockScopes).findEntry(any())
      doReturn(Some(source)).when(mockScopes).getOpt("<captured>")
      evaluate(s"""<captured> should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<captured> should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(Some(("<captured>", src))).when(mockScopes).findEntry(any())
      doReturn(Some(source)).when(mockScopes).getOpt("<captured>")
      evaluate(s"""<captured> should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "<reference A> should <operator> <reference B>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<reference A>", None)
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference B>")
      doReturn(None).when(mockScopes).getOpt("<reference>")
      evaluate(s"<reference A> should $operator <reference B>")
    }
  }

  "<reference A> should not <operator> <reference B>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<reference A>", None)
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference B>")
      doReturn(None).when(mockScopes).getOpt("<reference>")
      evaluate(s"<reference A> should not $operator <reference B>")
    }
  }

  """<dropdown> text should <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, source, expression) =>
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      evaluate(s"""<dropdown> text should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<dropdown> text should not <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      evaluate(s"""<dropdown> text should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "<dropdown> text should <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"<dropdown> text should $operator <reference>")
    }
  }

  "<dropdown> text should not <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"<dropdown> text should not $operator <reference>")
    }
  }

  """<dropdown> value should <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, source, expression) =>
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      evaluate(s"""<dropdown> value should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<dropdown> value should not <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      evaluate(s"""<dropdown> value should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "<dropdown> value should <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"<dropdown> value should $operator <reference>")
    }
  }

  "<dropdown> value should not <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"<dropdown> value should not $operator <reference>")
    }
  }

  """the current URL should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn("http://site.com").when(ctx).captureCurrentUrl
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("the current URL", None)
      doReturn(None).when(mockScopes).getOpt("the current URL")
      evaluate(s"""the current URL should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """the current URL should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn("http://site.com").when(ctx).captureCurrentUrl
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("the current URL", None)
      doReturn(None).when(mockScopes).getOpt("the current URL")
      evaluate(s"""the current URL should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the current URL should <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn("http://site.com").when(ctx).captureCurrentUrl
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("the current URL", None)
      doReturn(None).when(mockScopes).getOpt("the current URL")
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"""the current URL should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the current URL should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn("http://site.com").when(ctx).captureCurrentUrl
      doReturn(None).when(mockScopes).findEntry(any())
      doReturn(() => source).when(ctx).boundAttributeOrSelection("the current URL", None)
      doReturn(None).when(mockScopes).getOpt("the current URL")
      doReturn(expression).when(ctx).getBoundReferenceValue("<reference>")
      evaluate(s"""the current URL should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """I capture the text in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<value>x</value>").when(ctx).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the text in <reference A> by xpath "/value" as <reference B>""")
    verify(mockTopScope).set("<reference B>", "x")
  }

  """I capture the node in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<value>x</value>").when(ctx).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the node in <reference A> by xpath "/value" as <reference B>""")
    verify(mockTopScope).set("<reference B>", "<value>x</value>")
  }

  """I capture the nodeset in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<values><value>x</value><value>y</value></values>").when(ctx).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the nodeset in <reference A> by xpath "/values/value" as <reference B>""")
    verify(mockTopScope).set("<reference B>",
      """<value>x</value>
        |<value>y</value>""".stripMargin)
  }

  """I capture the text in <referenceA> by regex "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("""Now get <this>""").when(ctx).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the text in <reference A> by regex "Now get (.+)" as <reference B>""")
    verify(mockTopScope).set("<reference B>", "<this>")
  }

  """I capture the content in <reference A> by json path "<expression B>" as <reference>""" should "evaluate" in {
    doReturn("""{value:"<this>"}""").when(ctx).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the content in <reference A> by json path "$.value" as <reference B>""")
    verify(mockTopScope).set("<reference B>", "<this>")
  }

  """I capture the text in the current URL by regex "<expression>" as <reference>""" should "evaluate" in {
    doReturn("http://site.com?param1=<this>&param2=<that>").when(ctx).getBoundReferenceValue("the current URL")
    evaluate("""I capture the text in the current URL by regex "param1=(.+)&" as <reference>""")
    verify(mockTopScope).set("<reference>", "<this>")
  }

  "I capture the current URL" should "evaluate" in {
    doReturn("http://site.com").when(ctx).captureCurrentUrl
    evaluate("I capture the current URL")
    verify(ctx).captureCurrentUrl
  }

  "I capture <reference> as <attribute>" should "evaluate" in {
    doReturn("value").when(ctx).getBoundReferenceValue("<reference>")
    evaluate("I capture <reference> as <attribute>")
    verify(mockTopScope).set("<attribute>", "value")
  }

  "I capture <reference>" should "evaluate" in {
    doReturn("value").when(ctx).getBoundReferenceValue("<reference>")
    evaluate("I capture <reference>")
    verify(mockTopScope).set("<reference>", "value")
  }

  "I capture the alert popup message" should "evaluate" in {
    doReturn("message").when(ctx).getPopupMessage
    evaluate("I capture the alert popup message")
    verify(mockTopScope).set("the alert popup message", "message")
  }

  "I capture the confirmation popup message" should "evaluate" in {
    doReturn("message").when(ctx).getPopupMessage
    evaluate("I capture the confirmation popup message")
    verify(mockTopScope).set("the confirmation popup message", "message")
  }

  "I capture the alert popup message as <attribute>" should "evaluate" in {
    doReturn("message").when(ctx).getPopupMessage
    evaluate("I capture the alert popup message as <attribute>")
    verify(mockTopScope).set("<attribute>", "message")
  }

  "I capture the confirmation popup message as <attribute>" should "evaluate" in {
    doReturn("message").when(ctx).getPopupMessage
    evaluate("I capture the confirmation popup message as <attribute>")
    verify(mockTopScope).set("<attribute>", "message")
  }

  "I capture <dropdown> text as <attribute>" should "evaluate" in {
    doReturn(() => "value").when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text))
    evaluate("I capture <dropdown> text as <attribute>")
    verify(mockTopScope).set("<attribute>", "value")
  }

  "I capture <dropdown> value as <attribute>" should "evaluate" in {
    doReturn(() => "value").when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value))
    evaluate("I capture <dropdown> value as <attribute>")
    verify(mockTopScope).set("<attribute>", "value")
  }

  "I capture <dropdown> text" should "evaluate" in {
    doReturn(() => "value").when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text))
    evaluate("I capture <dropdown> text")
    verify(mockTopScope).set("<dropdown>", "value")
  }

  "I capture <dropdown> value" should "evaluate" in {
    doReturn(() => "value").when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value))
    evaluate("I capture <dropdown> value")
    verify(mockTopScope).set("<dropdown>", "value")
  }

  """I capture <attribute> by javascript "<expression>"""" should "evaluate" in {
    doReturn("value").when(ctx).evaluateJS("""return (function(){return "value"})()""")
    evaluate("""I capture <attribute> by javascript "(function(){return "value"})()"""")
    verify(mockTopScope).set("<attribute>", "value")
  }

  """I capture <attribute> <of|on|in> <element> by javascript "<expression>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    val mockElement = mock[WebElement]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockElement).when(mockBinding).resolve()
    doReturn("value").when(ctx).getBoundReferenceValue("<attribute>")
    List("of", "on", "in").foreach { x =>
      evaluate(s"""I capture <attribute> $x <element> by javascript "<expression>"""")
    }
    verify(mockTopScope, times(3)).set("<attribute>", "value")
  }

  """my <name> property <is|will be> "<value>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""my gwen.property-$i property $x "value-$i"""")
      Settings.get(s"gwen.property-$i") should be (s"value-$i")
    }
  }

  """my <name> setting <is|will be> "<value>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""my gwen.setting-$i setting $x "value-$i"""")
      Settings.get(s"gwen.setting-$i") should be (s"value-$i")
    }
  }

  "I reset my <name> property" should "evaluate" in {
    evaluate(s"I reset my gwen.web property")
    Settings.getOpt(s"gwen.web") should be (None)
  }

  "I reset my <name> setting" should "evaluate" in {
    evaluate(s"I reset my gwen.web setting")
    Settings.getOpt(s"gwen.web") should be (None)
  }

  """<attribute> <is|will> be defined by javascript "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""attribute-$i $x defined by javascript "expression-$i"""")
      verify(mockScopes).set(s"attribute-$i/javascript", s"expression-$i")
    }
  }

  """<attribute> <is|will> be defined by property "<name>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      Settings.set(s"name-$i", s"$i")
      evaluate(s"""attribute-$i $x defined by property "name-$i"""")
      verify(mockTopScope).set(s"attribute-$i", s"$i")
    }
  }

  """<attribute> <is|will> be defined by setting "<name>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      Settings.set(s"name-$i", s"$i")
      evaluate(s"""attribute-$i $x defined by setting "name-$i"""")
      verify(mockTopScope).set(s"attribute-$i", s"$i")
    }
  }

   """<attribute> <is|will> be defined by system process "<process>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""attribute-$i $x defined by system process "process-$i"""")
      verify(mockScopes).set(s"attribute-$i/sysproc", s"process-$i")
    }
  }

   """<attribute> <is|will> be defined by system process "<process>" delimited by "<delimiter>"""" should "evaluate" in {
      val mockScopes = mock[ScopedDataStack]
      doReturn(mockScopes).when(envState).scopes
      List("is", "will be").zipWithIndex.foreach { case (x, i) =>
        evaluate(s"""attribute-$i $x defined by system process "process-$i" delimited by ","""")
        verify(mockScopes).set(s"attribute-$i/sysproc", s"process-$i")
        verify(mockScopes).set(s"attribute-$i/delimiter", s",")
      }
    }

  """<attribute> <is|will> be defined by file "<filepath>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""attribute-$i $x defined by file "filepath-$i"""")
      verify(mockScopes).set(s"attribute-$i/file", s"filepath-$i")
    }
  }

  """<attribute> is defined by the <text|node|nodeset> in <reference> by xpath "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    List("text", "node", "nodeset").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> is defined by the $x in <reference-$i> by xpath "<expression-$i>"""")
      verify(mockScopes).set("<attribute>/xpath/source", s"<reference-$i>")
      verify(mockScopes).set("<attribute>/xpath/targetType", x)
      verify(mockScopes).set("<attribute>/xpath/expression", s"<expression-$i>")
    }
  }

  """<attribute> will be defined by the <text|node|nodeset> in <reference> by xpath "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    List("text", "node", "nodeset").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> will be defined by the $x in <reference-$i> by xpath "<expression-$i>"""")
      verify(mockScopes).set("<attribute>/xpath/source", s"<reference-$i>")
      verify(mockScopes).set("<attribute>/xpath/targetType", x)
      verify(mockScopes).set("<attribute>/xpath/expression", s"<expression-$i>")
    }
  }

  """<attribute> <is|will be> defined in <reference> by regex "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> $x defined in <reference-$i> by regex "<expression-$i>"""")
      verify(mockScopes).set(s"<attribute>/regex/source", s"<reference-$i>")
      verify(mockScopes).set(s"<attribute>/regex/expression", s"<expression-$i>")
    }
  }

  """<attribute> <is|will be> defined in <reference> by json path "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> $x defined in <reference-$i> by json path "<expression-$i>"""")
      verify(mockScopes).set(s"<attribute>/json path/source", s"<reference-$i>")
      verify(mockScopes).set(s"<attribute>/json path/expression", s"<expression-$i>")
    }
  }

  """<attribute> <is|will be> "<value>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute-$i> $x "<value-$i>"""")
      verify(mockTopScope).set(s"<attribute-$i>", s"<value-$i>")
    }
  }

  "I wait for <element> text for 1 second" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    evaluate("I wait for <element> text for 1 second")
  }

  "I wait for <element> text for <duration> seconds" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    evaluate("I wait for <element> text for 2 seconds")
  }

  "I wait for <element> text" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    evaluate("I wait for <element> text")
  }

  "I wait for <element> for 1 second" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    evaluate("I wait for <element> for 1 second")
  }

  "I wait for <element> for <duration> seconds" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    evaluate("I wait for <element> for 2 seconds")
  }

  "I wait for <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    evaluate("I wait for <element>")
  }

  "I clear <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).performAction(ElementAction.clear, mockBinding)
    evaluate("I clear <element>")
    verify(ctx).performAction(ElementAction.clear, mockBinding)
  }

  "I press <enter|tab> in <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    List("enter", "tab").foreach { key =>
      doNothing().when(ctx).sendKeys(mockBinding, Array(key))
      doNothing().when(ctx).bindAndWait("<element>", key, "true")
      evaluate(s"I press $key in <element>")
      verify(ctx).sendKeys(mockBinding, Array(key))
      verify(ctx).bindAndWait("<element>", key, "true")
    }
  }

  """I <enter|type> "<text>" in <element>""" should "evaluate" in {
    List("enter", "type").foreach { action =>
      val mockBinding = mock[LocatorBinding]
      doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
      doNothing().when(ctx).sendValue(mockBinding, "<text>", clickFirst = false, clearFirst = false, sendEnterKey = action == "enter")
      evaluate(s"""I $action "<text>" in <element>""")
      verify(ctx).sendValue(mockBinding, "<text>", clickFirst = false, clearFirst = false, sendEnterKey = action == "enter")
    }
  }

  "I <enter|type> <reference> in <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn("<text>").when(ctx).getBoundReferenceValue("<reference>")
    List("enter", "type").foreach { action =>
      doNothing().when(ctx).sendValue(mockBinding, "<text>", clickFirst = false, clearFirst = false, sendEnterKey = action == "enter")
      evaluate(s"I $action <reference> in <element>")
      verify(ctx).sendValue(mockBinding, "<text>", clickFirst = false, clearFirst = false, sendEnterKey = action == "enter")
    }
  }

  "I select the <position><st|nd|rd|th> option in <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    List("st", "nd", "rd", "th").zipWithIndex.foreach { case (x, i) =>
      doNothing.when(ctx).selectByIndex(mockBinding, i)
      evaluate(s"I select the ${i+1}$x option in <element>")
      verify(ctx).selectByIndex(mockBinding, i)
    }
  }

  """I select "<text>" in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).selectByVisibleText(mockBinding, "<text>")
    evaluate("""I select "<text>" in <element>""")
    verify(ctx).selectByVisibleText(mockBinding, "<text>")
  }

  """I select "<text>" in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).selectByValue(mockBinding, "<text>")
    evaluate("""I select "<text>" in <element> by value""")
    verify(ctx).selectByValue(mockBinding, "<text>")
  }

  """I select <reference> in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn("<text>").when(ctx).getBoundReferenceValue("<reference>")
    doNothing().when(ctx).selectByVisibleText(mockBinding, "<text>")
    evaluate("""I select <reference> in <element>""")
    verify(ctx).selectByVisibleText(mockBinding, "<text>")
  }

  """I select <reference> in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn("<text>").when(ctx).getBoundReferenceValue("<reference>")
    doNothing().when(ctx).selectByValue(mockBinding, "<text>")
    evaluate("""I select <reference> in <element> by value""")
    verify(ctx).selectByValue(mockBinding, "<text>")
  }

  "I deselect the <position><st|nd|rd|th> option in <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    List("st", "nd", "rd", "th").zipWithIndex.foreach { case (x, i) =>
      doNothing.when(ctx).deselectByIndex(mockBinding, i)
      evaluate(s"I deselect the ${i+1}$x option in <element>")
      verify(ctx).deselectByIndex(mockBinding, i)
    }
  }

  """I deselect "<text>" in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).deselectByVisibleText(mockBinding, "<text>")
    evaluate("""I deselect "<text>" in <element>""")
    verify(ctx).deselectByVisibleText(mockBinding, "<text>")
  }

  """I deselect "<text>" in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).deselectByValue(mockBinding, "<text>")
    evaluate("""I deselect "<text>" in <element> by value""")
    verify(ctx).deselectByValue(mockBinding, "<text>")
  }

  """I deselect <reference> in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn("<text>").when(ctx).getBoundReferenceValue("<reference>")
    doNothing().when(ctx).deselectByVisibleText(mockBinding, "<text>")
    evaluate("""I deselect <reference> in <element>""")
    verify(ctx).deselectByVisibleText(mockBinding, "<text>")
  }

  """I deselect <reference> in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn("<text>").when(ctx).getBoundReferenceValue("<reference>")
    doNothing().when(ctx).deselectByValue(mockBinding, "<text>")
    evaluate("""I deselect <reference> in <element> by value""")
    verify(ctx).deselectByValue(mockBinding, "<text>")
  }

  "I <click|right click|double click|submit|check|tick|uncheck|untick|move to> <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    List("click", "right click", "double click", "submit", "check", "tick", "uncheck", "untick", "move to").map(ElementAction.valueOf).foreach { action =>
      doNothing().when(ctx).performAction(action, mockBinding)
      evaluate(s"I $action <element>")
      verify(ctx).performAction(action, mockBinding)
    }
  }

  "I <click|right click|double click> <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    List("click", "right click", "double click").map(ElementAction.valueOf).foreach { action =>
      doNothing().when(ctx).holdAndClick(Array("COMMAND", "SHIFT"), action, mockBinding)
      evaluate(s"I COMMAND+SHIFT $action <element>")
      verify(ctx).holdAndClick(Array("COMMAND", "SHIFT"), action, mockBinding)
    }
  }

  "I <click|right click|double click|check|tick|uncheck|untick|move to> <element> of <context>" should "evaluate" in {
    List("click", "right click", "double click", "check", "tick", "uncheck", "untick", "move to").map(ElementAction.valueOf).foreach { action =>
      doNothing().when(ctx).performActionInContext(action, "<element>", "<context>")
      evaluate(s"I $action <element> of <context>")
      verify(ctx).performActionInContext(action, "<element>", "<context>")
    }
  }

  """I send "<keys>" to <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).sendKeys(mockBinding, Array("CONTROL", "C"))
    evaluate("""I send "CONTROL,C" to <element>""")
    evaluate("""I send "CONTROL+C" to <element>""")
    verify(ctx, times(2)).sendKeys(mockBinding, Array("CONTROL", "C"))
  }

  """I send "<keys>"""" should "evaluate" in {
    doNothing().when(ctx).sendKeys(Array("CONTROL", "C"))
    evaluate("""I send "CONTROL,C"""")
    evaluate("""I send "CONTROL+C"""")
    verify(ctx, times(2)).sendKeys(Array("CONTROL", "C"))
  }

  "I wait 1 second when <element> is <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"I wait 1 second when <element> is $event")
      verify(mockScopes).set(s"<element>/${ElementEvent.actionOf(event)}/wait", "1")
    }
  }

  "I wait <duration> seconds when <element> is <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"I wait 2 seconds when <element> is $event")
      verify(mockScopes).set(s"<element>/${ElementEvent.actionOf(event)}/wait", "2")
    }
  }

  "I wait until <condition> when <element> is <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"I wait until <condition> when <element> is $event")
      verify(mockScopes).set(s"<element>/${ElementEvent.actionOf(event)}/condition", "<condition>")
    }
    verify(mockScopes, times(events.size)).get("<condition>/javascript")
  }

  """I wait until "<javascript>"""" should "evaluate" in {
    evaluate("""I wait until "<javascript>"""")
  }

  "I wait until <condition>" should "evaluate" in {
    doReturn("(function(){return true;})()").when(mockScopes).get("<condition>/javascript")
    evaluate("I wait until <condition>")
  }

  "I wait 1 second" should "evaluate" in {
    evaluate("I wait 1 second")
  }

  "I wait <duration> seconds" should "evaluate" in {
    evaluate("I wait 2 seconds")
  }

  "I <highlight|locate> <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    List("highlight", "locate").foreach { x =>
      evaluate(s"I $x <element>")
    }
    verify(ctx, times(2)).locateAndHighlight(mockBinding)
  }

  """I execute javascript "<javascript>"""" should "evaluate" in {
    doReturn(true).when(ctx).evaluateJS("<javascript>")
    evaluate("""I execute javascript "<javascript>"""")
    verify(ctx).evaluateJS("<javascript>")
  }

  """I execute system process "<command>"""" should "evaluate" in {
    evaluate("""I execute system process "hostname"""")
  }

  """I execute system process "<command>" delimited by "<delimiter>"""" should "evaluate" in {
    evaluate("""I execute system process "hostname,-s" delimited by ","""")
  }

  "I refresh the current page" should "evaluate" in {
    doNothing().when(ctx).refreshPage()
    evaluate("I refresh the current page")
    verify(ctx).refreshPage()
  }

  "I base64 decode <reference> as <attribute>" should "evaluate" in {
    doReturn("value").when(ctx).getBoundReferenceValue("<reference>")
    doReturn("decoded").when(ctx).decodeBase64("value")
    evaluate("I base64 decode <reference> as <attribute>")
    verify(mockTopScope).set("<attribute>", "decoded")
  }

  "I base64 decode <reference>" should "evaluate" in {
    doReturn("value").when(ctx).getBoundReferenceValue("<reference>")
    doReturn("decoded").when(ctx).decodeBase64("value")
    evaluate("I base64 decode <reference>")
    verify(mockTopScope).set("<reference>", "decoded")
  }

  "<step> <until|while> <condition> using <delayPeriod> <second|millisecond> delay and <timeoutPeriod> <minute|second|millisecond> <wait|timeout>" should "evaluate" in {
    timeUnits1.foreach { unit1 =>
      timeUnits2.foreach { unit2 =>
        waits.foreach { wait =>
          List("until", "while").foreach { repeat =>
            val step = evaluate(s"""x is "1" $repeat <condition> using 1 $unit1 delay and 2000 $unit2 $wait""")
            step.toString should be (s"""Given x is "1" $repeat <condition> using 1 $unit1 delay and 2000 $unit2 $wait""")
          }
        }
      }
    }
  }

  "<step> <until|while> <condition> using <delayPeriod> <second|millisecond> delay" should "evaluate" in {
    timeUnits1.foreach { unit =>
      List("until", "while").foreach { repeat =>
        val step = evaluate(s"""x is "1" $repeat <condition> using 1 $unit delay""")
        step.toString should be (s"""Given x is "1" $repeat <condition> using 1 $unit delay""")
      }
    }
  }

  "<step> <until|while> <condition> using no delay and <timeoutPeriod> <minute|second|millisecond> <wait|timeout>" should "evaluate" in {
    timeUnits2.foreach { unit =>
      List("until", "while").foreach { repeat =>
        waits.foreach { wait =>
          val step = evaluate(s"""x is "1" $repeat <condition> using no delay and 2000 $unit $wait""")
          step.toString should be (s"""Given x is "1" $repeat <condition> using no delay and 2000 $unit $wait""")
        }
      }
    }
  }

  "<step> <until|while> <condition> using no delay" should "evaluate" in {
    List("until", "while").foreach { repeat =>
      val step = evaluate(s"""x is "1" $repeat <condition> using no delay""")
      step.toString should be (s"""Given x is "1" $repeat <condition> using no delay""")
    }
  }

  "<step> <until|while> <condition>" should "evaluate" in {
    List("until", "while").foreach { repeat =>
      val step = evaluate(s"""x is "1" $repeat <condition>""")
      step.toString should be (s"""Given x is "1" $repeat <condition>""")
    }
  }

  "I close the current browser" should "evaluate" in {
    doNothing().when(ctx).close()
    evaluate("I close the current browser")
    verify(ctx).close()
  }

  "I close the browser" should "evaluate" in {
    doNothing().when(ctx).close()
    evaluate("I close the browser")
    verify(ctx).close()
  }

  "I start a new browser" should "evaluate" in {
    doNothing().when(ctx).close("primary")
    doNothing().when(ctx).switchToSession("primary")
    evaluate("I start a new browser")
    verify(ctx).close("primary")
    verify(ctx).switchToSession("primary")
  }

  "I start a browser for <session>" should "evaluate" in {
    doNothing().when(ctx).close("<session>")
    doNothing().when(ctx).switchToSession("<session>")
    evaluate("I start a browser for <session>")
    verify(ctx).close("<session>")
    verify(ctx).switchToSession("<session>")
  }

  "I should have 1 open browser" should "evaluate" in {
    doReturn(1).when(ctx).noOfSessions()
    evaluate("I should have 1 open browser")
    verify(ctx).noOfSessions()
  }

  "I should have 2 open browsers" should "evaluate" in {
    doReturn(2).when(ctx).noOfSessions()
    evaluate("I should have 2 open browsers")
    verify(ctx).noOfSessions()
  }

  "I have no open browser" should "evaluate" in {
    evaluate("I have no open browser")
    verify(ctx).close()
  }

  "I have an open browser" should "evaluate" in {
    evaluate("I have an open browser")
    verify(ctx).newOrCurrentSession()
  }

  "I close the browser for <session>" should "evaluate" in {
    doNothing().when(ctx).close("<session>")
    evaluate("I close the browser for <session>")
    verify(ctx).close("<session>")
  }

  "I switch to <session>" should "evaluate" in {
    doNothing().when(ctx).switchToSession("<session>")
    evaluate("I switch to <session>")
    verify(ctx).switchToSession("<session>")
  }

  "I <accept|dismiss> the <alert|confirmation> popup" should "evaluate" in {
    List("accept", "dismiss").foreach { action =>
      val accept = action == "accept"
      doNothing().when(ctx).handleAlert(accept)
      List("alert", "confirmation").foreach { mode =>
        evaluate(s"I $action the $mode popup")
      }
    }
    verify(ctx, times(2)).handleAlert(true)
    verify(ctx, times(2)).handleAlert(false)
  }

  "I switch to the child <window|tab>" should "evaluate" in {
    doNothing().when(ctx).switchToChild()
    List("window", "tab").foreach { x =>
      evaluate(s"I switch to the child $x")
    }
    verify(ctx, times(2)).switchToChild()
  }

  "I close the child <window|tab>" should "evaluate" in {
    doNothing().when(ctx).closeChild()
    List("window", "tab").foreach { x =>
      evaluate(s"I close the child $x")
    }
    verify(ctx, times(2)).closeChild()
  }

  "I switch to the parent <window|tab>" should "evaluate" in {
    List("window", "tab").foreach { x =>
      doNothing().when(ctx).switchToParent()
      evaluate(s"I switch to the parent $x")
    }
    verify(ctx, times(2)).switchToParent()
  }

  "I switch to the default content" should "evaluate" in {
    doNothing().when(ctx).switchToDefaultContent()
    evaluate("I switch to the default content")
    verify(ctx).switchToDefaultContent()
  }

  "I capture the current screenshot" should "evaluate" in {
    doReturn(Option(new File("screenshot"))).when(ctx).captureScreenshot(any[Boolean], any[String])
    evaluate("I capture the current screenshot")
    verify(ctx).captureScreenshot(true)
  }

  "I capture the current screenshot as name" should "evaluate" in {
    val file = new File("screenshot")
    doReturn(Option(file)).when(ctx).captureScreenshot(any[Boolean], any[String])
    evaluate("I capture the current screenshot as name")
    verify(ctx).captureScreenshot(true, "name")
    verify(mockScopes).set("name", file.getAbsolutePath)
  }

  """<element> can be <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to> by javascript "<javascript>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"""<element> can be $event by javascript "<javascript>"""")
      verify(mockScopes).set(s"<element>/action/${ElementEvent.actionOf(event)}/javascript", "<javascript>")
    }
  }

  """<reference> <is|will be> defined by sql "<selectStmt>" in the <dbName> database""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        List("is", "will be").foreach { x =>
          evaluate(s"""<reference> $x defined by sql "<selectStmt>" in the <dbName> database""")
        }
        verify(mockScopes, times(2)).set(s"<reference>/sql/selectStmt", "<selectStmt>")
        verify(mockScopes, times(2)).set(s"<reference>/sql/dbName", "<dbName>")
      }
    }
  }

  """<reference> <is|will be> defined in the <dbName> database by sql "<selectStmt>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        List("is", "will be").foreach { x =>
          evaluate(s"""<reference> $x defined in the <dbName> database by sql "<selectStmt>"""")
        }
        verify(mockScopes, times(2)).set(s"<reference>/sql/selectStmt", "<selectStmt>")
        verify(mockScopes, times(2)).set(s"<reference>/sql/dbName", "<dbName>")
      }
    }
  }

  """I update the <dbName> database by sql "<updateStmt>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    doReturn(1).when(ctx).executeSQLUpdate("<updateStmt>", "<dbName>")
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        evaluate("""I update the <dbName> database by sql "<updateStmt>"""")
        verify(mockScopes).set(s"<dbName> rows affected", "1")
      }
    }
  }

  "I resize the window to width <w> and height <h>" should "evaluate" in {
    doNothing().when(ctx).resizeWindow(400, 200)
    evaluate("I resize the window to width 400 and height 200")
    verify(ctx).resizeWindow(400, 200)
  }

  "I <maximize|maximise> the window" should "evaluate" in {
    doNothing().when(ctx).maximizeWindow()
    List("maximize", "maximise").foreach { x =>
      evaluate(s"I $x the window")
    }
    verify(ctx, times(2)).maximizeWindow()
  }

  """<step> for each <element> located by id "<expression>"""" should "evaluate" in {
    doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
    val step = evaluate(s"""x is "1" for each <element> located by id "<expression>"""")
    step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>"""")
  }

  """<step> for each <element> located by id "<expression>" with no <wait|timeout>""" should "evaluate" in {
    waits.foreach { wait =>
      doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
      val step = evaluate(s"""x is "1" for each <element> located by id "<expression>" with no $wait""")
      step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" with no $wait""")
    }
  }

   """<step> for each <element> located by id "<expression>" with <timeoutPeriod> second <wait|timeout>""" should "evaluate" in {
     waits.foreach { wait =>
       doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
       val step = evaluate(s"""x is "1" for each <element> located by id "<expression>" with 2 second $wait""")
       step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" with 2 second $wait""")
     }
   }

  """<step> for each <element> located by id "<expression>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
    val step = evaluate(s"""x is "1" for each <element> located by id "<expression>" in <container>""")
    step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" in <container>""")
  }

  """<step> for each <element> located by id "<expression>" in <container> with no <wait|timeout>""" should "evaluate" in {
    waits.foreach { wait =>
      val mockBinding = mock[LocatorBinding]
      doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
      doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
      val step = evaluate(s"""x is "1" for each <element> located by id "<expression>" in <container> with no $wait""")
      step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" in <container> with no $wait""")
    }
  }

  """<step> for each <element> located by id "<expression>" in <container> with <timeoutPeriod> second <wait|timeout>""" should "evaluate" in {
    waits.foreach { wait =>
      val mockBinding = mock[LocatorBinding]
      doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
      doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
      val step = evaluate(s"""x is "1" for each <element> located by id "<expression>" in <container> with 2 second $wait""")
      step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" in <container> with 2 second $wait""")
    }
  }

  "<step> for each <element> in <elements>" should "evaluate" in {
    val binding = new LocatorBinding("", Nil, ctx)
    doReturn(binding).when(ctx).getLocatorBinding("<elements>")
    doReturn(Nil).when(mockLocator).locateAll(binding)
    val step = evaluate("""x is "1" for each <element> in <elements>""")
    step.toString should be (s"""Given x is "1" for each <element> in <elements>""")
  }

  "<step> for each data record" should "evaluate" in {
    val mockTable = mock[FlatTable]
    doReturn(Some(mockTable)).when(mockTopScope).getObject("table")
    doReturn(Nil).when(mockTable).records
    val step = evaluate("""x is "1" for each data record""")
    step.toString should be (s"""Given x is "1" for each data record""")
  }

  "<attribute> should be absent" should "evaluate" in {
    doThrow(new Errors.UnboundAttributeException("<attribute>", None)).when(ctx).getBoundReferenceValue("<attribute>")
    evaluate("<attribute> should be absent")
  }

  """<source> at json path "<path>" should be "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""{x:"value"}""").when(mockScopes).get("<source>")
      doReturn(None).when(mockScopes).getOpt("<source>")
      evaluate(s"""<source> at json path "$$.x" should $operator "$expression"""")
    }
  }

  """<source> at json path "<path>" should not be "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""{x:"other"}""").when(mockScopes).get("<source>")
      doReturn(None).when(mockScopes).getOpt("<source>")
      evaluate(s"""<source> at json path "$$.x" should not $operator "$expression"""")
    }
  }

  """<source> at xpath "<path>" should be "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""<x>value</x>""").when(mockScopes).get("<source>")
      doReturn(None).when(mockScopes).getOpt("<source>")
      evaluate(s"""<source> at xpath "x" should $operator "$expression"""")
    }
  }

  """<source> at xpath "<path>" should not be "<expression>"""" should "evaluate" in {
    val mockScopes = mock[ScopedDataStack]
    doReturn(mockScopes).when(envState).scopes
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""<x>other</x>""").when(mockScopes).get("<source>")
      doReturn(None).when(mockScopes).getOpt("<source>")
      evaluate(s"""<source> at xpath "x" should not $operator "$expression"""")
    }
  }

  "<step> if <condition>" should "evaluate" in {
    doReturn(Some("false")).when(mockScopes).getOpt("<condition>/javascript")
    doReturn("false").when(ctx).interpolate("false")
    doReturn(false).when(ctx).evaluateJS("return false")
    val step = evaluate("""x is "1" if <condition>""")
    step.toString should be (s"""Given x is "1" if <condition>""")
  }

  """<step> for each <entry> in <source> delimited by "<delimiter>"""" should "evaluate" in {
    doReturn("").when(ctx).getBoundReferenceValue("<source>")
    val step = evaluate("""x is "1" for each <entry> in <source> delimited by ","""")
    step.toString should be (s"""Given x is "1" for each <entry> in <source> delimited by ","""")
  }

  "I drag and drop <element A> to <element B>" should "evaluate" in {
    val mockLocatorA: LocatorBinding = mock[LocatorBinding]
    val mockLocatorB: LocatorBinding = mock[LocatorBinding]
    doReturn(mockLocatorA).when(ctx).getLocatorBinding("<element A>")
    doReturn(mockLocatorB).when(ctx).getLocatorBinding("<element B>")
    evaluate("I drag and drop <element A> to <element B>")
    verify(ctx).dragAndDrop(mockLocatorA, mockLocatorB)
  }

  """I append "<text>" to <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).sendValue(mockBinding, "<text>", clickFirst = false, clearFirst = false, sendEnterKey = false)
    evaluate("""I append "<text>" to <element>""")
    verify(ctx).sendValue(mockBinding, "<text>", clickFirst = false, clearFirst = false, sendEnterKey = false)
  }

  """I append <reference> to <element>""" should "evaluate" in {
    doReturn("<text>").when(ctx).getBoundReferenceValue("<reference>")
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doNothing().when(ctx).sendValue(mockBinding, "<text>", clickFirst = false, clearFirst = false, sendEnterKey = false)
    evaluate("""I append <reference> to <element>""")
    verify(ctx).sendValue(mockBinding, "<text>", clickFirst = false, clearFirst = false, sendEnterKey = false)
  }

  """I insert a new line in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    val newLine = StringEscapeUtils.unescapeJava("""\n""")
    doNothing().when(ctx).sendValue(mockBinding, newLine, clickFirst = false, clearFirst = false, sendEnterKey = false)
    evaluate("""I insert a new line in <element>""")
    verify(ctx).sendValue(mockBinding, newLine, clickFirst = false, clearFirst = false, sendEnterKey = false)
  }

}
