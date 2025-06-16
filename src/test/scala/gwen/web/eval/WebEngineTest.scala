/*
 * Copyright 2016-2024 Brady Wood, Branko Juric
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
import gwen.web.eval.driver.DriverManager

import gwen.core._
import gwen.core.Errors
import gwen.core.eval.binding.SimpleBinding
import gwen.core.eval.ComparisonOperator
import gwen.core.eval.binding.JSBinding
import gwen.core.node.Root
import gwen.core.node.gherkin.SpecType
import gwen.core.node.gherkin.Step
import gwen.core.node.gherkin.StepKeyword
import gwen.core.node.gherkin.Tag
import gwen.core.node.gherkin.table.DataTable
import gwen.core.state._
import gwen.core.status.Pending

import scala.compiletime.uninitialized
import scala.util.chaining._

import org.apache.commons.text.StringEscapeUtils
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.openqa.selenium.WindowType
import org.openqa.selenium.WebElement
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers

import java.io.File

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

  private val selectorTypesNoJS = selectorTypes.filter(_._2 != SelectorType.javascript)
  private val rSelectorTypes = RelativeSelectorType.values.filter(_ != RelativeSelectorType.in)

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
  private val timeUnits1 = List("s", "ms")
  private val timeUnits2= List("m", "s", "ms")

  private var envState: EnvState = uninitialized
  private var ctx: WebContext = uninitialized
  private var mockTopScope: TopScope = uninitialized
  private var mockParamScope: ScopedDataStack = uninitialized
  private var mockRuleScope: ScopedDataStack = uninitialized
  private var mockScenarioScope: ScopedDataStack = uninitialized
  private var mockStepDefScope: ScopedDataStack = uninitialized
  private var mockLocator: WebElementLocator = uninitialized

  override def beforeEach(): Unit = {
    envState = spy(EnvState())
    ctx = spy(new WebContext(GwenOptions(), envState, mock[DriverManager]))
    mockTopScope = mock[TopScope]
    mockParamScope = mock[ScopedDataStack]
    mockRuleScope = mock[ScopedDataStack]
    mockScenarioScope = mock[ScopedDataStack]
    mockStepDefScope = mock[ScopedDataStack]
    mockLocator = mock[WebElementLocator]
    doReturn(mockTopScope).when(ctx).topScope
    doReturn(mockParamScope).when(mockTopScope).paramScope
    doReturn(mockRuleScope).when(mockTopScope).ruleScope
    doReturn(None).when(mockRuleScope).getOpt(any())
    doReturn(mockScenarioScope).when(mockTopScope).scenarioScope
    doReturn(None).when(mockScenarioScope).getOpt(any())
    doReturn(mockStepDefScope).when(mockTopScope).stepDefScope
    doReturn(None).when(mockStepDefScope).getOpt(any())
    doReturn(mockLocator).when(ctx).webElementlocator
    doReturn(false).when(ctx).isEvaluatingTopLevelStep
    doReturn(SpecType.Meta).when(ctx).specType
    doNothing().when(mockTopScope).setStatus(any(), any())
    when(mockTopScope.getOpt("gwen.feature.file.path")).thenReturn(Some("file.feature"))
  }

  private def evaluate(name: String): Step = {
    evaluate(Nil, name)
  }

  private def evaluate(tags: List[Tag], name: String): Step = {
    val step = new Step(None, StepKeyword.Given.toString, name, Nil, None, Nil, None, Pending, Nil, Nil, tags, None, Nil) {
      override def interpolate(interpolator: String => String): Step = this
    }
    engine.evaluateStep(Root, step, ctx)
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
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(s"""<element> can be located by $selectorType "<value>" in <container>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", "<container>")
    }
  }

  """<element> can be located by <locator> "<value>" <relativeLocator> <otherElement>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<otherElement>")
    selectorTypesNoJS.foreach { case (selectorType, pSelectorType) =>
      rSelectorTypes.foreach { rSelectorType =>
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType")).thenReturn(Some("<otherElement>"))
        if (rSelectorType == RelativeSelectorType.near) {
          when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType/withinPixels")).thenReturn(None)
        }
        rSelectorTypes.filter(_ != rSelectorType) foreach { rSelectorType2 =>
          when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2")).thenReturn(None)
          if (rSelectorType2 == RelativeSelectorType.near) {
            when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2/withinPixels")).thenReturn(None)
          }
        }
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(None)
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
        evaluate(s"""<element> can be located by $selectorType "<value>" $rSelectorType <otherElement>""")
        verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/$rSelectorType", "<otherElement>")
      }
    }
  }

  """<element> can be located by <locator> "<value>" near and within <pixels> pixels of <otherElement>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<otherElement>")
    selectorTypesNoJS.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(Some("<otherElement>"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      rSelectorTypes.filter(_ != RelativeSelectorType.near) foreach { rSelectorType2 =>
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2")).thenReturn(None)
      }
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(s"""<element> can be located by $selectorType "<value>" near and within 100 pixels of <otherElement>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/near", "<otherElement>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/near/withinPixels", "100")
    }
  }

  """<element> can be located by <locator> "<value>" at index <index> in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(s"""<element> can be located by $selectorType "<value>" at index 2 in <container>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", "<container>")
    }
  }

  """@Timeout('0s') <element> can be located by <locator> "<value>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('0s')")), s"""<element> can be located by $selectorType "<value>" in <container>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", "<container>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
      reset(mockTopScope)
    }
  }

  """@Timeout('0s') <element> can be located by <locator> "<value>" <relativeLocator> <otherElement>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<otherElement>")
    selectorTypesNoJS.foreach { case (selectorType, pSelectorType) =>
      rSelectorTypes.foreach { rSelectorType =>
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType")).thenReturn(Some("<otherElement>"))
        if (rSelectorType == RelativeSelectorType.near) {
          when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType/withinPixels")).thenReturn(None)
        }
        rSelectorTypes.filter(_ != rSelectorType) foreach { rSelectorType2 =>
          when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2")).thenReturn(None)
          if (rSelectorType2 == RelativeSelectorType.near) {
            when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2/withinPixels")).thenReturn(None)
          }
        }
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
        evaluate(List(Tag("@Timeout('0s')")), s"""<element> can be located by $selectorType "<value>" $rSelectorType <otherElement>""")
        verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/$rSelectorType", "<otherElement>")
        verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
        reset(mockTopScope)
      }
    }
  }

  """@Timeout('0s') <element> can be located by <locator> "<value>" near and within <pixels> pixels of <otherElement>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<otherElement>")
    selectorTypesNoJS.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(Some("<otherElement>"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      rSelectorTypes.filter(_ != RelativeSelectorType.near) foreach { rSelectorType2 =>
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2")).thenReturn(None)
      }
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('0s')")), s"""<element> can be located by $selectorType "<value>" near and within 100 pixels of <otherElement>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/near", "<otherElement>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/near/withinPixels", "100")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
      reset(mockTopScope)
    }
  }

  """@Timeout('0s') <element> can be located by <locator> "<value>" at index 2 in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('0s')")), s"""<element> can be located by $selectorType "<value>" at index 2 in <container>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", "<container>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
      reset(mockTopScope)
    }
  }

  """@Timeout('2s') <element> can be located by <locator> "<value>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('2s')")), s"""<element> can be located by $selectorType "<value>" in <container>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", "<container>")
      reset(mockTopScope)
    }
  }

  """@Timeout('2s') <element> can be located by <locator> "<value>" <relativeLocator> <otherElement>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<otherElement>")
    selectorTypesNoJS.foreach { case (selectorType, pSelectorType) =>
      rSelectorTypes.foreach { rSelectorType =>
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType")).thenReturn(Some("<otherElement>"))
        if (rSelectorType == RelativeSelectorType.near) {
          when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType/withinPixels")).thenReturn(None)
        }
        rSelectorTypes.filter(_ != rSelectorType) foreach { rSelectorType2 =>
          when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2")).thenReturn(None)
          if (rSelectorType2 == RelativeSelectorType.near) {
            when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2/withinPixels")).thenReturn(None)
          }
        }
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
        evaluate(List(Tag("@Timeout('2s')")), s"""<element> can be located by $selectorType "<value>" $rSelectorType <otherElement>""")
        verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
        verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
        verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/$rSelectorType", "<otherElement>")
        reset(mockTopScope)
      }
    }
  }

  """@Timeout('2s') <element> can be located by <locator> "<value>" near and within <pixels> pixels of <otherElement>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<otherElement>")
    selectorTypesNoJS.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(Some("<otherElement>"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      rSelectorTypes.filter(_ != RelativeSelectorType.near) foreach { rSelectorType2 =>
        when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/$rSelectorType2")).thenReturn(None)
      }
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('2s')")), s"""<element> can be located by $selectorType "<value>" near and within 100 pixels of <otherElement>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/near", "<otherElement>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/near/withinPixels", "100")
      reset(mockTopScope)
    }
  }

   """@Timeout('2s') <element> can be located by <locator> "<value>" at index 2 in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('2s')")), s"""<element> can be located by $selectorType "<value>" at index 2 in <container>""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", "<container>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "2")
      reset(mockTopScope)
    }
  }

  """<element> can be located by <locator> "<value>"""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/in")).thenReturn(Some("container"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/above")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/below")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to left of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to right of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(s"""<element> can be located by $selectorType "<value>"""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", null)
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", null)
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", null)
    }
  }

  """<element> can be located by <locator> "<value>" at index 2""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/in")).thenReturn(Some("container"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/above")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/below")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to left of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to right of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(s"""<element> can be located by $selectorType "<value>" at index 2""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", null)
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", null)
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
    }
  }

   """@Timeout('0s') <element> can be located by <locator> "<value>"""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/in")).thenReturn(Some("container"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/above")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/below")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to left of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to right of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('0s')")), s"""<element> can be located by $selectorType "<value>"""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", null)
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", null)
      reset(mockTopScope)
    }
  }

   """@Timeout('0s') <element> can be located by <locator> "<value>" at index 2""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/in")).thenReturn(Some("container"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/above")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/below")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to left of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to right of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('0s')")), s"""<element> can be located by $selectorType "<value>" at index 2""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", null)
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "0")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
      reset(mockTopScope)
    }
  }

  """@Timeout('2s') <element> can be located by <locator> "<value>"""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/in")).thenReturn(Some("container"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/above")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/below")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to left of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to right of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('2s')")), s"""<element> can be located by $selectorType "<value>"""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", null)
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "2")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", null)
      reset(mockTopScope)
    }
  }

  """@Timeout('2s') <element> can be located by <locator> "<value>" at index 2""" should "evaluate" in {
    selectorTypes.foreach { case (selectorType, pSelectorType) =>
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/in")).thenReturn(Some("container"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/above")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/below")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/near/withinPixels")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to left of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/to right of")).thenReturn(None)
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/timeoutSecs")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/index")).thenReturn(Some("2"))
      when(mockTopScope.getOpt(s"<element>/locator/$pSelectorType/shadowRoot")).thenReturn(None)
      evaluate(List(Tag("@Timeout('2s')")), s"""<element> can be located by $selectorType "<value>" at index 2""")
      verify(mockTopScope, atLeastOnce()).set("<element>/locator", pSelectorType.toString())
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType", "<value>")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/in", null)
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/timeoutSecs", "2")
      verify(mockTopScope, atLeastOnce()).set(s"<element>/locator/$pSelectorType/index", "2")
      reset(mockTopScope)
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
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"""the page title should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the page title should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getTitle
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
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
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"""the alert popup message should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the alert popup message should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getPopupMessage
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
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
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"""the confirmation popup message should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the confirmation popup message should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(ctx).getPopupMessage
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"""the confirmation popup message should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "I wait until <element> is <state>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeoutSeconds(None)
    elemStates.foreach { state =>
      doReturn(None).when(mockTopScope).getOpt(s"<element> is $state/javascript")
      doNothing().when(ctx).waitForElementState(mockBinding, state, false)
      evaluate(s"I wait until <element> is $state")
      verify(ctx).waitForElementState(mockBinding, state, false)
    }
  }

  "I wait until <element> is not <state>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeoutSeconds(None)
    elemStates.foreach { state =>
      doReturn(None).when(mockTopScope).getOpt(s"<element> is not $state/javascript")
      doNothing().when(ctx).waitForElementState(mockBinding, state, true)
      evaluate(s"I wait until <element> is not $state")
      verify(ctx).waitForElementState(mockBinding, state, true)
    }
  }

  """<reference> should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("<reference>", None, None)
      doReturn(None).when(mockTopScope).getOpt("<reference>")
      evaluate(s"""<reference> should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<reference> should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("<reference>", None, None)
      doReturn(None).when(mockTopScope).getOpt("<reference>")
      evaluate(s"""<reference> should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<captured> should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(Some(("<captured>", source))).when(mockTopScope).findEntry(any())
      doReturn(Some(source)).when(mockTopScope).getOpt("<captured>")
      evaluate(s"""<captured> should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<captured> should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(Some(("<captured>", src))).when(mockTopScope).findEntry(any())
      doReturn(Some(source)).when(mockTopScope).getOpt("<captured>")
      evaluate(s"""<captured> should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "<reference A> should <operator> <reference B>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(true).when(ctx).isWebBinding("<reference A>")
      doReturn(source).when(ctx).boundAttributeOrSelection("<reference A>", None, None)
      doReturn(expression).when(ctx).getBoundValue("<reference B>", None)
      doReturn(None).when(mockTopScope).getOpt("<reference>")
      evaluate(s"<reference A> should $operator <reference B>")
    }
  }

  "<reference A> should not <operator> <reference B>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(true).when(ctx).isWebBinding("<reference A>")
      doReturn(source).when(ctx).boundAttributeOrSelection("<reference A>", None, None)
      doReturn(expression).when(ctx).getBoundValue("<reference B>", None)
      doReturn(None).when(mockTopScope).getOpt("<reference>")
      evaluate(s"<reference A> should not $operator <reference B>")
    }
  }

  """<dropdown> text should <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, source, expression) =>
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text), None)
      doReturn(Some("<dropdown>")).when(mockTopScope).getOpt("<dropdown>")
      evaluate(s"""<dropdown> text should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<dropdown> text should not <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text), None)
      doReturn(Some("<dropdown>")).when(mockTopScope).getOpt("<dropdown>")
      evaluate(s"""<dropdown> text should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "<dropdown> text should <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(true).when(ctx).isWebBinding("<dropdown>")
      doReturn(source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text), None)
      doReturn(Some("<dropdown>")).when(mockTopScope).getOpt("<dropdown>")
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"<dropdown> text should $operator <reference>")
    }
  }

  "<dropdown> text should not <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(true).when(ctx).isWebBinding("<dropdown>")
      doReturn(source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text), None)
      doReturn(Some("<dropdown>")).when(mockTopScope).getOpt("<dropdown>")
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"<dropdown> text should not $operator <reference>")
    }
  }

  """<dropdown> value should <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, source, expression) =>
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value), None)
      doReturn(Some("<dropdown>")).when(mockTopScope).getOpt("<dropdown>")
      evaluate(s"""<dropdown> value should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """<dropdown> value should not <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value), None)
      doReturn(Some("<dropdown>")).when(mockTopScope).getOpt("<dropdown>")
      evaluate(s"""<dropdown> value should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "<dropdown> value should <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(true).when(ctx).isWebBinding("<dropdown>")
      doReturn(source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value), None)
      doReturn(Some("<dropdown>")).when(mockTopScope).getOpt("<dropdown>")
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"<dropdown> value should $operator <reference>")
    }
  }

  "<dropdown> value should not <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(ctx).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(true).when(ctx).isWebBinding("<dropdown>")
      doReturn(source).when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value), None)
      doReturn(Some("<dropdown>")).when(mockTopScope).getOpt("<dropdown>")
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"<dropdown> value should not $operator <reference>")
    }
  }

  """the current URL should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn("http://site.com").when(ctx).captureCurrentUrl
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("the current URL", None, None)
      doReturn(None).when(mockTopScope).getOpt("the current URL")
      evaluate(s"""the current URL should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """the current URL should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn("http://site.com").when(ctx).captureCurrentUrl
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("the current URL", None, None)
      doReturn(None).when(mockTopScope).getOpt("the current URL")
      evaluate(s"""the current URL should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the current URL should <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn("http://site.com").when(ctx).captureCurrentUrl
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("the current URL", None, None)
      doReturn(None).when(mockTopScope).getOpt("the current URL")
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"""the current URL should $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  "the current URL should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn("http://site.com").when(ctx).captureCurrentUrl
      doReturn(None).when(mockTopScope).findEntry(any())
      doReturn(source).when(ctx).boundAttributeOrSelection("the current URL", None, None)
      doReturn(None).when(mockTopScope).getOpt("the current URL")
      doReturn(expression).when(ctx).getBoundValue("<reference>", None)
      evaluate(s"""the current URL should not $operator "$expression"""")
      verify(ctx).parseExpression(operator, expression)
    }
  }

  """I capture the text in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<value>x</value>").when(ctx).getBoundValue("<reference A>", None)
    evaluate("""I capture the text in <reference A> by xpath "/value" as <reference B>""")
    verify(mockTopScope).set("<reference B>", "x")
  }

  """I capture the node in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<value>x</value>").when(ctx).getBoundValue("<reference A>", None)
    evaluate("""I capture the node in <reference A> by xpath "/value" as <reference B>""")
    verify(mockTopScope).set("<reference B>", "<value>x</value>")
  }

  """I capture the nodeset in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<values><value>x</value><value>y</value></values>").when(ctx).getBoundValue("<reference A>", None)
    evaluate("""I capture the nodeset in <reference A> by xpath "/values/value" as <reference B>""")
    verify(mockTopScope).set("<reference B>",
      """<value>x</value>
        |<value>y</value>""".stripMargin)
  }

  """I capture the text in <referenceA> by regex "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("""Now get <this>""").when(ctx).getBoundValue("<reference A>", None)
    evaluate("""I capture the text in <reference A> by regex "Now get (.+)" as <reference B>""")
    verify(mockTopScope).set("<reference B>", "<this>")
  }

  """I capture the content in <reference A> by json path "<expression B>" as <reference>""" should "evaluate" in {
    doReturn("""{value:"<this>"}""").when(ctx).getBoundValue("<reference A>", None)
    evaluate("""I capture the content in <reference A> by json path "$.value" as <reference B>""")
    verify(mockTopScope).set("<reference B>", "<this>")
  }

  """I capture the text in the current URL by regex "<expression>" as <reference>""" should "evaluate" in {
    doReturn("http://site.com?param1=<this>&param2=<that>").when(ctx).getBoundValue("the current URL", None)
    evaluate("""I capture the text in the current URL by regex "param1=(.+)&" as <reference>""")
    verify(mockTopScope).set("<reference>", "<this>")
  }

  "I capture the current URL" should "evaluate" in {
    doReturn("http://site.com").when(ctx).captureCurrentUrl
    evaluate("I capture the current URL")
    verify(ctx).captureCurrentUrl
  }

  "I capture <reference> as <attribute>" should "evaluate" in {
    doReturn("value").when(ctx).getBoundValue("<reference>", None)
    evaluate("I capture <reference> as <attribute>")
    verify(mockTopScope).set("<attribute>", "value")
  }

  "I capture <reference>" should "evaluate" in {
    doReturn("value").when(ctx).getBoundValue("<reference>", None)
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
    doReturn("value").when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text), None)
    evaluate("I capture <dropdown> text as <attribute>")
    verify(mockTopScope).set("<attribute>", "value")
  }

  "I capture <dropdown> value as <attribute>" should "evaluate" in {
    doReturn("value").when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value), None)
    evaluate("I capture <dropdown> value as <attribute>")
    verify(mockTopScope).set("<attribute>", "value")
  }

  "I capture <dropdown> text" should "evaluate" in {
    doReturn("value").when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.text), None)
    evaluate("I capture <dropdown> text")
    verify(mockTopScope).set("<dropdown>", "value")
  }

  "I capture <dropdown> value" should "evaluate" in {
    doReturn("value").when(ctx).boundAttributeOrSelection("<dropdown>", Option(DropdownSelection.value), None)
    evaluate("I capture <dropdown> value")
    verify(mockTopScope).set("<dropdown>", "value")
  }

  """I capture <attribute> by javascript "<expression>"""" should "evaluate" in {
    doReturn("value").when(ctx).evaluateJS("""(function(){return "value"})()""")
    evaluate("""I capture <attribute> by javascript "(function(){return "value"})()"""")
    verify(mockTopScope).set("<attribute>", "value")
  }

  """I capture <attribute> <of|on|in> <element> by javascript "<expression>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    val mockElement = mock[WebElement]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeout(None)
    doReturn(mockElement).when(mockBinding).resolve()
    doReturn(0L).when(mockBinding).timeoutSeconds
    doReturn("value").when(ctx).getBoundValue("<attribute>", None)
    List("of", "on", "in").foreach { x =>
      evaluate(s"""I capture <attribute> $x <element> by javascript "<expression>"""")
    }
    verify(mockTopScope, times(3)).set("<attribute>", "value", false)
  }

  """my <name> property is "<value>"""" should "evaluate" in {
    evaluate(s"""my gwen.property-0 property is "value-0"""")
    Settings.get(s"gwen.property-0") should be (s"value-0")
  }

  """my <name> setting is "<value>"""" should "evaluate" in {
    evaluate(s"""my gwen.setting-0 setting is "value-0"""")
    Settings.get(s"gwen.setting-0") should be (s"value-0")
  }

  "I reset my <name> property" should "evaluate" in {
    evaluate(s"I reset my gwen.web property")
    Settings.getOpt(s"gwen.web") should be (None)
  }

  "I reset my <name> setting" should "evaluate" in {
    evaluate(s"I reset my gwen.web setting")
    Settings.getOpt(s"gwen.web") should be (None)
  }

  """<attribute> is defined by javascript "<expression>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    evaluate(s"""attribute-0 is defined by javascript "expression-0"""")
    verify(mockTopScope).set(s"attribute-0/javascript", s"expression-0", false)
  }

  """<attribute> is defined by property "<name>"""" should "evaluate" in {
    withSetting(s"name-0", "0") {
      evaluate(s"""attribute-0 is defined by property "name-0"""")
      verify(mockTopScope).set(s"attribute-0", "0", false)
    }
  }

  """<attribute> is defined by setting "<name>"""" should "evaluate" in {
    withSetting(s"name-0", "0") {
      evaluate(s"""attribute-0 is defined by setting "name-0"""")
      verify(mockTopScope).set(s"attribute-0", "0", false)
    }
  }

   """<attribute> is defined by system process "<process>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    evaluate(s"""attribute-0 is defined by system process "process-0"""")
    verify(mockTopScope).set(s"attribute-0/sysproc", s"process-0", false)
  }

   """<attribute> is defined by system process "<process>" delimited by "<delimiter>"""" should "evaluate" in {
      doReturn(mockTopScope).when(envState).topScope
      evaluate(s"""attribute-0 is defined by system process "process-0" delimited by ","""")
      verify(mockTopScope).set(s"attribute-0/sysproc", s"process-0", false)
      verify(mockTopScope).set(s"attribute-0/delimiter", s",", false)
    }

  """<attribute> is defined by file "<filepath>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    evaluate(s"""attribute-0 is defined by file "filepath-0"""")
    verify(mockTopScope).set(s"attribute-0/file", s"filepath-0", false)
  }

  """<attribute> is defined by the <text|node|nodeset> in <reference> by xpath "<expression>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    List("text", "node", "nodeset").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> is defined by the $x in <reference-$i> by xpath "<expression-$i>"""")
      verify(mockTopScope).set("<attribute>/xpath/source", s"<reference-$i>", false)
      verify(mockTopScope).set("<attribute>/xpath/targetType", x, false)
      verify(mockTopScope).set("<attribute>/xpath/expression", s"<expression-$i>", false)
      reset(mockTopScope)
    }
  }

  """<attribute> is defined in <reference> by regex "<expression>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    evaluate(s"""<attribute> is defined in <reference-0> by regex "<expression-0>"""")
    verify(mockTopScope).set(s"<attribute>/regex/source", s"<reference-0>", false)
    verify(mockTopScope).set(s"<attribute>/regex/expression", s"<expression-0>", false)
  }

  """<attribute> is defined in <reference> by json path "<expression>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    evaluate(s"""<attribute> is defined in <reference-0> by json path "<expression-0>"""")
    verify(mockTopScope).set(s"<attribute>/json path/source", s"<reference-0>", false)
    verify(mockTopScope).set(s"<attribute>/json path/expression", s"<expression-0>", false)
  }

  """<attribute> is "<value>"""" should "evaluate" in {
    evaluate(s"""<attribute-0> is "<value-0>"""")
    verify(mockTopScope).set(s"<attribute-0>", s"<value-0>", false)
  }

  "@Timeout('1s') I wait for <element> text" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeoutSeconds(Some(1))
    doReturn(1L).when(mockBinding).timeoutSeconds
    evaluate(List(Tag("@Timeout('1s')")), "I wait for <element>")
  }

  "@Timeout('2s') I wait for <element> text" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeoutSeconds(Some(2))
    doReturn(2L).when(mockBinding).timeoutSeconds
    evaluate(List(Tag("@Timeout('2s')")), "I wait for <element> text")
  }

  "I wait for <element> text" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeoutSeconds(None)
    evaluate("I wait for <element> text")
  }

  "@Timeout('1s') I wait for <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeoutSeconds(Some(1))
    doReturn(1L).when(mockBinding).timeoutSeconds
    evaluate(List(Tag("@Timeout('1s')")), "I wait for <element>")
  }

  "@Timeout('2s') I wait for <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeoutSeconds(Some(2))
    doReturn(2L).when(mockBinding).timeoutSeconds
    evaluate(List(Tag("@Timeout('2s')")), "I wait for <element>")
  }

  "I wait for <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(mockBinding).when(mockBinding).withTimeoutSeconds(None)
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
      evaluate(s"I press $key in <element>")
      verify(ctx).sendKeys(mockBinding, Array(key))
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
    doReturn("<text>").when(ctx).getBoundValue("<reference>", None)
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
    doReturn("<text>").when(ctx).getBoundValue("<reference>", None)
    doNothing().when(ctx).selectByVisibleText(mockBinding, "<text>")
    evaluate("""I select <reference> in <element>""")
    verify(ctx).selectByVisibleText(mockBinding, "<text>")
  }

  """I select <reference> in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn("<text>").when(ctx).getBoundValue("<reference>", None)
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
    doReturn("<text>").when(ctx).getBoundValue("<reference>", None)
    doNothing().when(ctx).deselectByVisibleText(mockBinding, "<text>")
    evaluate("""I deselect <reference> in <element>""")
    verify(ctx).deselectByVisibleText(mockBinding, "<text>")
  }

  """I deselect <reference> in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn("<text>").when(ctx).getBoundValue("<reference>", None)
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

  """I wait until "<javascript>"""" should "evaluate" in {
    evaluate("""I wait until "<javascript>"""")
  }

  "I wait until <condition> with JS" should "evaluate" in {
    val binding = JSBinding("<condition>", Nil, ctx)
    doReturn(binding).when(ctx).getBinding("<condition>")
    doReturn("(function(){return true;})()").when(mockTopScope).get("<condition>/javascript")
    evaluate("I wait until <condition>")
  }

  "I wait until <condition> with literal" should "evaluate" in {
    val binding = SimpleBinding("<condition>", ctx)
    doReturn(binding).when(ctx).getBinding("<condition>")
    doReturn(Some("true")).when(mockTopScope).getOpt("<condition>")
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
    doReturn("value").when(ctx).getBoundValue("<reference>", None)
    doReturn("decoded").when(ctx).decodeBase64("value")
    evaluate("I base64 decode <reference> as <attribute>")
    verify(mockTopScope).set("<attribute>", "decoded")
  }

  "I base64 decode <reference>" should "evaluate" in {
    doReturn("value").when(ctx).getBoundValue("<reference>", None)
    doReturn("decoded").when(ctx).decodeBase64("value")
    evaluate("I base64 decode <reference>")
    verify(mockTopScope).set("<reference>", "decoded")
  }

  "@Delay('1x') @Timeout('2000x') <step> <until|while> <condition>" should "evaluate" in {
    timeUnits1.foreach { unit1 =>
      timeUnits2.foreach { unit2 =>
        List("until", "while").foreach { repeat =>
          val step = evaluate(List(Tag(s"@Delay('1${unit1}')"), Tag(s"@Timeout('2000${unit2}')")), s"""x is "1" $repeat <condition>""")
          step.toString should be (s"""Given x is "1" $repeat <condition>""")
          step.tags(0).toString should be (s"@Delay('1${unit1}')")
          step.tags(1).toString should be (s"@Timeout('2000${unit2}')")
        }
      }
    }
  }

  "@Delay('1x') <step> <until|while> <condition>" should "evaluate" in {
    timeUnits1.foreach { unit =>
      List("until", "while").foreach { repeat =>
        val step = evaluate(List(Tag(s"@Delay('1${unit}')")), s"""x is "1" $repeat <condition>""")
        step.toString should be (s"""Given x is "1" $repeat <condition>""")
        step.tags(0).toString should be (s"@Delay('1${unit}')")
      }
    }
  }

  "@Delay('0s') @Timeout('2000x') <step> <until|while> <condition>" should "evaluate" in {
    timeUnits2.foreach { unit =>
      List("until", "while").foreach { repeat =>
        val step = evaluate(List(Tag(s"@Delay('0s')"), Tag(s"@Timeout('2000${unit}')")), s"""x is "1" $repeat <condition>""")
        step.toString should be (s"""Given x is "1" $repeat <condition>""")
        step.tags(0).toString should be (s"@Delay('0s')")
        step.tags(1).toString should be (s"@Timeout('2000${unit}')")
      }
    }
  }

  "@Delay('0s') <step> <until|while> <condition>" should "evaluate" in {
    List("until", "while").foreach { repeat =>
      val step = evaluate(List(Tag(s"@Delay('0s')")), s"""x is "1" $repeat <condition>""")
      step.toString should be (s"""Given x is "1" $repeat <condition>""")
      step.tags(0).toString should be (s"@Delay('0s')")
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

  "I start a new browser tab" should "evaluate" in {
    doNothing().when(ctx).switchToNewWindow(WindowType.TAB)
    evaluate("I start a new browser tab")
  }

  "I start a new browser window" should "evaluate" in {
    doNothing().when(ctx).switchToNewWindow(WindowType.WINDOW)
    evaluate("I start a new browser window")
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
      doNothing().when(ctx).handlePopup(accept, None)
      List("alert", "confirmation").foreach { mode =>
        evaluate(s"I $action the $mode popup")
      }
    }
    verify(ctx, times(2)).handlePopup(true, None)
    verify(ctx, times(2)).handlePopup(false, None)
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

  "I switch to child <window|tab> <occurrence>" should "evaluate" in {
    val occurNo = 1
    doNothing().when(ctx).switchToWindow(occurNo)
    List("window", "tab").foreach { x =>
      evaluate(s"I switch to child $x $occurNo")
    }
    verify(ctx, times(2)).switchToWindow(occurNo)
  }

  "I switch to <window|tab> <occurrence>" should "evaluate" in {
    val occurNo = 1
    doNothing().when(ctx).switchToWindow(occurNo)
    List("window", "tab").foreach { x =>
      evaluate(s"I switch to $x $occurNo")
    }
    verify(ctx, times(2)).switchToWindow(occurNo)
  }

  "I close child <window|tab> <occurrence>" should "evaluate" in {
    val occurNo = 1
    doNothing().when(ctx).closeWindow(occurNo)
    List("window", "tab").foreach { x =>
      evaluate(s"I close child $x $occurNo")
    }
    verify(ctx, times(2)).closeWindow(occurNo)
  }

  "I close <window|tab> <occurrence>" should "evaluate" in {
    val occurNo = 1
    doNothing().when(ctx).closeWindow(occurNo)
    List("window", "tab").foreach { x =>
      evaluate(s"I close $x $occurNo")
    }
    verify(ctx, times(2)).closeWindow(occurNo)
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
    verify(mockTopScope).set("name", file.getAbsolutePath)
  }

  "I capture element screenshot of <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    doReturn(Option(new File("elementshot"))).when(ctx).captureElementScreenshot(mockBinding, "Element Screenshot")
    evaluate("I capture element screenshot of <element>")
    verify(ctx).captureElementScreenshot(mockBinding, "Element Screenshot")
  }

  "I capture element screenshot of <element> as name" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    val file = new File("elementshot")
    doReturn(Option(file)).when(ctx).captureElementScreenshot(mockBinding, "name")
    evaluate("I capture element screenshot of <element> as name")
    verify(ctx).captureElementScreenshot(mockBinding, "name")
    verify(mockTopScope).set("name", file.getAbsolutePath)
  }

  """<element> can be <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to> by javascript "<javascript>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"""<element> can be $event by javascript "<javascript>"""")
      verify(mockTopScope).set(s"<element>/action/${ElementEvent.actionOf(event)}/javascript", "<javascript>", false)
    }
  }

  """<reference> is defined by sql "<selectStmt>" in the <dbName> database""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        evaluate(s"""<reference> is defined by sql "<selectStmt>" in the <dbName> database""")
        verify(mockTopScope).set(s"<reference>/sql/selectStmt", "<selectStmt>", false)
        verify(mockTopScope).set(s"<reference>/sql/dbName", "<dbName>", false)
      }
    }
  }

  """<reference> is defined in the <dbName> database by sql "<selectStmt>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        evaluate(s"""<reference> is defined in the <dbName> database by sql "<selectStmt>"""")
        verify(mockTopScope).set(s"<reference>/sql/selectStmt", "<selectStmt>", false)
        verify(mockTopScope).set(s"<reference>/sql/dbName", "<dbName>", false)
      }
    }
  }

  """I update the <dbName> database by sql "<updateStmt>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    doReturn(1).when(ctx).executeSQLUpdate("<updateStmt>", "<dbName>")
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        evaluate("""I update the <dbName> database by sql "<updateStmt>"""")
        verify(mockTopScope).set(s"<dbName> rows affected", "1")
      }
    }
  }

  "I resize the window to width <w> and height <h>" should "evaluate" in {
    doNothing().when(ctx).resizeWindow(400, 200)
    evaluate("I resize the window to width 400 and height 200")
    verify(ctx).resizeWindow(400, 200)
  }

  "I set the window position to x <x> and y <y>" should "evaluate" in {
    doNothing().when(ctx).positionWindow(40, 20)
    evaluate("I set the window position to x 40 and y 20")
    verify(ctx).positionWindow(40, 20)
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

  """@Timeout('0s') <step> for each <element> located by id "<expression>"""" should "evaluate" in {
    doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
    val step = evaluate(List(Tag("@Timeout('0s')")), s"""x is "1" for each <element> located by id "<expression>"""")
    step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>"""")
    step.tags(0).toString should be ("@Timeout('0s')")
  }

   """@Timeout('2s') <step> for each <element> located by id "<expression>"""" should "evaluate" in {
      doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
      val step = evaluate(List(Tag("@Timeout('2s')")), s"""x is "1" for each <element> located by id "<expression>"""")
      step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>"""")
      step.tags(0).toString should be ("@Timeout('2s')")
   }

  """<step> for each <element> located by id "<expression>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
    val step = evaluate(s"""x is "1" for each <element> located by id "<expression>" in <container>""")
    step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" in <container>""")
  }

  """@Timeout('0s') <step> for each <element> located by id "<expression>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
    val step = evaluate(List(Tag("@Timeout('0s')")), s"""x is "1" for each <element> located by id "<expression>" in <container>""")
    step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" in <container>""")
    step.tags(0).toString should be ("@Timeout('0s')")
  }

  """@Timeout('2s') <step> for each <element> located by id "<expression>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(ctx).getLocatorBinding("<container>")
    doReturn(Nil).when(mockLocator).locateAll(any[LocatorBinding])
    val step = evaluate(List(Tag("@Timeout('2s')")), s"""x is "1" for each <element> located by id "<expression>" in <container>""")
    step.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" in <container>""")
    step.tags(0).toString should be ("@Timeout('2s')")
  }

  "<step> for each <element> in <elements>" should "evaluate" in {
    val binding = new LocatorBinding("", Nil, ctx)
    doReturn(binding).when(ctx).getLocatorBinding("<elements>")
    doReturn(Nil).when(mockLocator).locateAll(binding)
    val step = evaluate("""x is "1" for each <element> in <elements>""")
    step.toString should be (s"""Given x is "1" for each <element> in <elements>""")
  }

  "<step> for each data record" should "evaluate" in {
    val mockTable = mock[DataTable]
    doReturn(Some(mockTable)).when(mockTopScope).getObject("table")
    doReturn(Nil).when(mockTable).records
    val step = evaluate("""x is "1" for each data record""")
    step.toString should be (s"""Given x is "1" for each data record""")
  }

  "<attribute> should be absent" should "evaluate" in {
    doThrow(new Errors.UnboundReferenceException("<attribute>", None, None)).when(ctx).getBoundValue("<reference>", None)
    evaluate("<attribute> should be absent")
  }

  """<source> at json path "<path>" should be "<expression>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""{x:"value"}""").when(mockTopScope).get("<source>")
      doReturn(None).when(mockTopScope).getOpt("<source>")
      evaluate(s"""<source> at json path "$$.x" should $operator "$expression"""")
    }
  }

  """<source> at json path "<path>" should not be "<expression>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""{x:"other"}""").when(mockTopScope).get("<source>")
      doReturn(None).when(mockTopScope).getOpt("<source>")
      evaluate(s"""<source> at json path "$$.x" should not $operator "$expression"""")
    }
  }

  """<source> at xpath "<path>" should be "<expression>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""<x>value</x>""").when(mockTopScope).get("<source>")
      doReturn(None).when(mockTopScope).getOpt("<source>")
      evaluate(s"""<source> at xpath "x" should $operator "$expression"""")
    }
  }

  """<source> at xpath "<path>" should not be "<expression>"""" should "evaluate" in {
    doReturn(mockTopScope).when(envState).topScope
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""<x>other</x>""").when(mockTopScope).get("<source>")
      doReturn(None).when(mockTopScope).getOpt("<source>")
      evaluate(s"""<source> at xpath "x" should not $operator "$expression"""")
    }
  }

  """<step> for each <entry> in <source> delimited by "<delimiter>"""" should "evaluate" in {
    doReturn("").when(ctx).getBoundValue("<source>", None)
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
    doReturn("<text>").when(ctx).getBoundValue("<reference>", None)
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
