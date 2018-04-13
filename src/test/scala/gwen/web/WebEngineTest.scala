/*
 * Copyright 2016 Brady Wood, Branko Juric
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

import java.io.File

import gwen.{Settings, UserOverrides}
import org.mockito.Mockito.{verify, _}
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import org.scalatest.mockito.MockitoSugar
import gwen.dsl.{FlatTable, Step, StepKeyword}
import gwen.eval.{FeatureScope, GwenOptions, ScopedDataStack}
import gwen.Predefs.Kestrel
import gwen.Predefs.FileIO
import org.openqa.selenium.WebElement
import org.mockito.Matchers.any
import gwen.errors.UnboundAttributeException

class WebEngineTest extends FlatSpec with WebEngine with Matchers with MockitoSugar with BeforeAndAfterEach {

  val templateFile: File = {
    val rootDir: File = new File("target" + File.separator + "WebEngineTest") tap { _.mkdirs() }
    val file = new File(rootDir + File.separator + "value.template")
    file.getParentFile.mkdirs()
    file.createNewFile()
    file.writeText("value")
    file.deleteOnExit()
    file
  }

  private val locators = List("id", "name", "tag name", "css selector", "xpath", "class name", "link text", "partial link text", "javascript")
  private val matchers =
    List(
      ("be", "value", "value"),
      ("contain", "value", "a"),
      ("start with", "value", "v"),
      ("end with", "value", "e"),
      ("match regex", "value", "value"),
      ("match xpath", "<value>x</value>", "/value"),
      ("match json path", """{value:"x"}""", "$.value"),
      ("match template", "value", "value"),
      ("match template file", "value", templateFile.getPath)
    )
  private val matchers2 = matchers.filter(!_._1.contains("template"))
  private val matchers3 = matchers.filter(!_._1.contains("path"))
    List(
      ("be", "value", "value"),
      ("contain", "value", "a"),
      ("start with", "value", "v"),
      ("end with", "value", "e"),
      ("match regex", "value", "value"),
      ("match xpath", "<value>x</value>", "/value"),
      ("match json path", """{value:"x"}""", "$.value"),
      ("match template", "value", "value"),
      ("match template file", "value", templateFile.getPath)
    )
  private val elemStates = List("displayed", "hidden", "checked", "ticked", "unchecked", "unticked", "enabled", "disabled")
  private val events = List("clicked", "right clicked", "double clicked", "submitted", "checked", "ticked", "unchecked", "unticked", "selected", "deselected", "typed", "entered", "tabbed", "cleared", "moved to")
  private val timeUnits1 = List("second", "millisecond")
  private val timeUnits2= List("minute", "second", "millisecond")
  private var webContext: WebContext = _
  private var env: WebEnvContext = _
  private var mockScopes: ScopedDataStack = _
  private var mockFeatureScope: FeatureScope = _
  private var mockDriverManager: DriverManager = _

  override def beforeEach(): Unit = {
    env = spy(new WebEnvContext(GwenOptions(), new ScopedDataStack()))
    mockDriverManager = mock[DriverManager]
    webContext = spy(new WebContext(env, mockDriverManager))
    mockScopes = mock[ScopedDataStack]
    mockFeatureScope = mock[FeatureScope]
    doReturn(webContext).when(env).webContext
    doReturn(mockScopes).when(env).scopes
    doReturn(mockFeatureScope).when(env).featureScope
  }

  private def evaluate(expression: String): Unit = {
    evaluate(Step(StepKeyword.Given, expression), env)
  }

  private def evaluatePriority(expression: String): Option[Step] = {
    evaluatePriority(Step(StepKeyword.Given, expression), env)
  }

  private def withSetting[T](name: String, value: String)(f: => T):T = {
    Settings.synchronized {
      try {
        sys.props += ((name, value))
        f
      } finally {
        sys.props -= name
        Settings.loadAll(UserOverrides.UserProperties.toList)
      }
    }
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

  "I navigate to the <page>" should "evaluate" in {
    doReturn("http://home.com").when(env).getAttribute("url")
    evaluate("I navigate to the <page>")
    verify(mockScopes).addScope("<page>")
    verify(webContext).navigateTo("http://home.com")
  }

  """I navigate to "<url>""""" should "evaluate" in {
    evaluate("""I navigate to "<url>"""")
    verify(mockScopes).addScope("<url>")
    verify(webContext).navigateTo("<url>")
  }

  "I scroll to the top of <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).scrollIntoView(mockBinding, ScrollTo.top)
    evaluate("I scroll to the top of <element>")
    verify(webContext).scrollIntoView(mockBinding, ScrollTo.top)
  }

  "I scroll to the bottom of <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).scrollIntoView(mockBinding, ScrollTo.bottom)
    evaluate("I scroll to the bottom of <element>")
    verify(webContext).scrollIntoView(mockBinding, ScrollTo.bottom)
  }

  """<element> can be located by <locator> "<value>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<container>")
    locators.foreach { locator =>
      evaluate(s"""<element> can be located by $locator "<value>" in <container>""")
      verify(mockScopes).set("<element>/locator", locator)
      verify(mockScopes).set(s"<element>/locator/$locator", "<value>")
      verify(mockScopes).set(s"<element>/locator/$locator/container", "<container>")
    }
  }

  """<element> can be located by <locator> "<value>"""" should "evaluate" in {
    locators.foreach { locator =>
      when(mockScopes.getOpt(s"<element>/locator/$locator/container")).thenReturn(Some("container"))
      evaluate(s"""<element> can be located by $locator "<value>"""")
      verify(mockScopes).set("<element>/locator", locator)
      verify(mockScopes).set(s"<element>/locator/$locator", "<value>")
      verify(mockScopes).set(s"<element>/locator/$locator/container", null)
    }
  }

  """the page title should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(source).when(webContext).getTitle
      evaluate(s"""the page title should $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  """the page title should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(webContext).getTitle
      evaluate(s"""the page title should not $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  "the page title should <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(source).when(webContext).getTitle
      doReturn(expression).when(env).getBoundReferenceValue("<reference>")
      evaluate(s"""the page title should $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  "the page title should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(source).when(webContext).getTitle
      doReturn(expression).when(env).getBoundReferenceValue("<reference>")
      evaluate(s"""the page title should not $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  "<element> should be <state>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    elemStates.foreach { state =>
      doNothing().when(webContext).checkElementState(mockBinding, state, negate = false)
      evaluate(s"<element> should be $state")
      verify(webContext).checkElementState(mockBinding, state, negate = false)
    }
  }

   "<element> should not be <state>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    elemStates.foreach { state =>
      doNothing().when(webContext).checkElementState(mockBinding, state, negate = true)
      evaluate(s"<element> should not be $state")
      verify(webContext).checkElementState(mockBinding, state, negate = true)
    }
  }

  """<reference> should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(env).boundAttributeOrSelection("<reference>", None)
      doReturn(None).when(mockScopes).getOpt("<reference>")
      evaluate(s"""<reference> should $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  """<reference> should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(env).boundAttributeOrSelection("<reference>", None)
      doReturn(None).when(mockScopes).getOpt("<reference>")
      evaluate(s"""<reference> should not $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  "<reference A> should <operator> <reference B>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(env).boundAttributeOrSelection("<reference A>", None)
      doReturn(expression).when(env).getBoundReferenceValue("<reference B>")
      doReturn(None).when(mockScopes).getOpt("<reference>")
      evaluate(s"<reference A> should $operator <reference B>")
    }
  }

  "<reference A> should not <operator> <reference B>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(env).boundAttributeOrSelection("<reference A>", None)
      doReturn(expression).when(env).getBoundReferenceValue("<reference B>")
      doReturn(None).when(mockScopes).getOpt("<reference>")
      evaluate(s"<reference A> should not $operator <reference B>")
    }
  }

  """<dropdown> text should <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(env).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(env).boundAttributeOrSelection("<dropdown>", Some(" text"))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      evaluate(s"""<dropdown> text should $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  """<dropdown> text should not <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(env).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(env).boundAttributeOrSelection("<dropdown>", Some(" text"))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      evaluate(s"""<dropdown> text should not $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  "<dropdown> text should <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(env).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(env).boundAttributeOrSelection("<dropdown>", Some(" text"))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      doReturn(expression).when(env).getBoundReferenceValue("<reference>")
      evaluate(s"<dropdown> text should $operator <reference>")
    }
  }

  "<dropdown> text should not <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(env).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(env).boundAttributeOrSelection("<dropdown>", Some(" text"))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      doReturn(expression).when(env).getBoundReferenceValue("<reference>")
      evaluate(s"<dropdown> text should not $operator <reference>")
    }
  }

  """<dropdown> value should <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(env).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(env).boundAttributeOrSelection("<dropdown>", Some(" value"))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      evaluate(s"""<dropdown> value should $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  """<dropdown> value should not <operator> "<value>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(env).getLocatorBinding("<dropdown>", optional = false)
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(env).boundAttributeOrSelection("<dropdown>", Some(" value"))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      evaluate(s"""<dropdown> value should not $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  "<dropdown> value should <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(env).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(env).boundAttributeOrSelection("<dropdown>", Some(" value"))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      doReturn(expression).when(env).getBoundReferenceValue("<reference>")
      evaluate(s"<dropdown> value should $operator <reference>")
    }
  }

  "<dropdown> value should not <operator> <reference>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(Some(mockBinding)).when(env).getLocatorBinding("<dropdown>", optional = false)
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(env).boundAttributeOrSelection("<dropdown>", Some(" value"))
      doReturn(Some("<dropdown>")).when(mockScopes).getOpt("<dropdown>")
      doReturn(expression).when(env).getBoundReferenceValue("<reference>")
      evaluate(s"<dropdown> value should not $operator <reference>")
    }
  }

  """the current URL should <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(env).boundAttributeOrSelection("the current URL", None)
      doReturn(None).when(mockScopes).getOpt("the current URL")
      evaluate(s"""the current URL should $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  """the current URL should not <operator> "<value>"""" should "evaluate" in {
    matchers.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(env).boundAttributeOrSelection("the current URL", None)
      doReturn(None).when(mockScopes).getOpt("the current URL")
      evaluate(s"""the current URL should not $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  "the current URL should <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, source, expression) =>
      doReturn(() => source).when(env).boundAttributeOrSelection("the current URL", None)
      doReturn(None).when(mockScopes).getOpt("the current URL")
      doReturn(expression).when(env).getBoundReferenceValue("<reference>")
      evaluate(s"""the current URL should $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  "the current URL should not <operator> <reference>" should "evaluate" in {
    matchers2.foreach { case (operator, src, expression) =>
      val source = src.replaceAll("value", "other")
      doReturn(() => source).when(env).boundAttributeOrSelection("the current URL", None)
      doReturn(None).when(mockScopes).getOpt("the current URL")
      doReturn(expression).when(env).getBoundReferenceValue("<reference>")
      evaluate(s"""the current URL should not $operator "$expression"""")
      verify(env).parseExpression(operator, expression)
    }
  }

  """I capture the text in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<value>x</value>").when(env).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the text in <reference A> by xpath "/value" as <reference B>""")
    verify(mockFeatureScope).set("<reference B>", "x")
  }

  """I capture the node in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<value>x</value>").when(env).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the node in <reference A> by xpath "/value" as <reference B>""")
    verify(mockFeatureScope).set("<reference B>", "<value>x</value>")
  }

  """I capture the nodeset in <reference A> by xpath "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("<values><value>x</value><value>y</value></values>").when(env).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the nodeset in <reference A> by xpath "/values/value" as <reference B>""")
    verify(mockFeatureScope).set("<reference B>",
      """<value>x</value>
        |<value>y</value>""".stripMargin)
  }

  """I capture the text in <referenceA> by regex "<expression>" as <reference B>""" should "evaluate" in {
    doReturn("""Now get <this>""").when(env).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the text in <reference A> by regex "Now get (.+)" as <reference B>""")
    verify(mockFeatureScope).set("<reference B>", "<this>")
  }

  """I capture the content in <reference A> by json path "<expression B>" as <reference>""" should "evaluate" in {
    doReturn("""{value:"<this>"}""").when(env).getBoundReferenceValue("<reference A>")
    evaluate("""I capture the content in <reference A> by json path "$.value" as <reference B>""")
    verify(mockFeatureScope).set("<reference B>", "<this>")
  }

  """I capture the text in the current URL by regex "<expression>" as <reference>""" should "evaluate" in {
    doReturn("http://site.com?param1=<this>&param2=<that>").when(env).getBoundReferenceValue("the current URL")
    evaluate("""I capture the text in the current URL by regex "param1=(.+)&" as <reference>""")
    verify(mockFeatureScope).set("<reference>", "<this>")
  }

  "I capture the current URL" should "evaluate" in {
    doNothing().when(webContext).captureCurrentUrl(None)
    evaluate("I capture the current URL")
    verify(webContext).captureCurrentUrl(None)
  }

  "I capture the current URL as <attribute>" should "evaluate" in {
    doNothing().when(webContext).captureCurrentUrl(Some("<attribute>"))
    evaluate("I capture the current URL as <attribute>")
    verify(webContext).captureCurrentUrl(Some("<attribute>"))
  }

  "I capture <reference> as <attribute>" should "evaluate" in {
    doReturn("value").when(env).getBoundReferenceValue("<reference>")
    evaluate("I capture <reference> as <attribute>")
    verify(mockFeatureScope).set("<attribute>", "value")
  }

  "I capture <reference>" should "evaluate" in {
    doReturn("value").when(env).getBoundReferenceValue("<reference>")
    evaluate("I capture <reference>")
    verify(mockFeatureScope).set("<reference>", "value")
  }

  "I capture <dropdown> text as <attribute>" should "evaluate" in {
    doReturn("value").when(webContext).getElementSelection("<dropdown>", " text")
    evaluate("I capture <dropdown> text as <attribute>")
    verify(mockFeatureScope).set("<attribute>", "value")
    verify(env).addAttachment("<attribute>", "txt", "value")
  }

  "I capture <dropdown> value as <attribute>" should "evaluate" in {
    doReturn("value").when(webContext).getElementSelection("<dropdown>", " value")
    evaluate("I capture <dropdown> value as <attribute>")
    verify(mockFeatureScope).set("<attribute>", "value")
    verify(env).addAttachment("<attribute>", "txt", "value")
  }

  "I capture <dropdown> text" should "evaluate" in {
    doReturn("value").when(webContext).getElementSelection("<dropdown>", " text")
    evaluate("I capture <dropdown> text")
    verify(mockFeatureScope).set("<dropdown>", "value")
    verify(env).addAttachment("<dropdown>", "txt", "value")
  }

  "I capture <dropdown> value" should "evaluate" in {
    doReturn("value").when(webContext).getElementSelection("<dropdown>", " value")
    evaluate("I capture <dropdown> value")
    verify(mockFeatureScope).set("<dropdown>", "value")
    verify(env).addAttachment("<dropdown>", "txt", "value")
  }

  """I capture <attribute> by javascript "<expression>"""" should "evaluate" in {
    doReturn("value").when(env).evaluateJS("""return (function(){return "value"})()""")
    evaluate("""I capture <attribute> by javascript "(function(){return "value"})()"""")
    verify(mockFeatureScope).set("<attribute>", "value")
    verify(env).addAttachment("<attribute>", "txt", "value")
  }

  """I capture <attribute> <of|on|in> <element> by javascript "<expression>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    val mockElement = mock[WebElement]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doReturn(mockElement).when(webContext).locate(mockBinding)
    doReturn("value").when(env).getBoundReferenceValue("<attribute>")
    List("of", "on", "in").foreach { x =>
      evaluate(s"""I capture <attribute> $x <element> by javascript "<expression>"""")
    }
    verify(mockFeatureScope, times(3)).set("<attribute>", "value")
    verify(env, times(3)).addAttachment("<attribute>", "txt", "value")
  }

  """my <name> property <is|will be> "<value>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""my property-$i property $x "value-$i"""")
      Settings.get(s"property-$i") should be (s"value-$i")
    }
  }

  """my <name> setting <is|will be> "<value>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""my setting-$i setting $x "value-$i"""")
      Settings.get(s"setting-$i") should be (s"value-$i")
    }
  }

  """<attribute> <is|will> be defined by javascript "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""attribute-$i $x defined by javascript "expression-$i"""")
      verify(mockActiveScope).set(s"attribute-$i/javascript", s"expression-$i")
    }
  }

  """<attribute> <is|will> be defined by property "<name>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      withSetting(s"name-$i", s"$i") {
        evaluate(s"""attribute-$i $x defined by property "name-$i"""")
        verify(mockFeatureScope).set(s"attribute-$i", s"$i")
      }
    }
  }

  """<attribute> <is|will> be defined by setting "<name>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      withSetting(s"name-$i", s"$i") {
        evaluate(s"""attribute-$i $x defined by setting "name-$i"""")
        verify(mockFeatureScope).set(s"attribute-$i", s"$i")
      }
    }
  }

   """<attribute> <is|will> be defined by system process "<process>"""" should "evaluate" in {
      val mockActiveScope = mock[ScopedDataStack]
      doReturn(mockActiveScope).when(env).activeScope
      List("is", "will be").zipWithIndex.foreach { case (x, i) =>
        evaluate(s"""attribute-$i $x defined by system process "process-$i"""")
        verify(mockActiveScope).set(s"attribute-$i/sysproc", s"process-$i")
      }
    }

  """<attribute> <is|will> be defined by file "<filepath>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""attribute-$i $x defined by file "filepath-$i"""")
      verify(mockActiveScope).set(s"attribute-$i/file", s"filepath-$i")
    }
  }

  """<attribute> is defined by the <text|node|nodeset> in <reference> by xpath "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    List("text", "node", "nodeset").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> is defined by the $x in <reference-$i> by xpath "<expression-$i>"""")
      verify(mockActiveScope).set("<attribute>/xpath/source", s"<reference-$i>")
      verify(mockActiveScope).set("<attribute>/xpath/targetType", x)
      verify(mockActiveScope).set("<attribute>/xpath/expression", s"<expression-$i>")
    }
  }

  """<attribute> will be defined by the <text|node|nodeset> in <reference> by xpath "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    List("text", "node", "nodeset").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> will be defined by the $x in <reference-$i> by xpath "<expression-$i>"""")
      verify(mockActiveScope).set("<attribute>/xpath/source", s"<reference-$i>")
      verify(mockActiveScope).set("<attribute>/xpath/targetType", x)
      verify(mockActiveScope).set("<attribute>/xpath/expression", s"<expression-$i>")
    }
  }

  """<attribute> <is|will be> defined in <reference> by regex "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> $x defined in <reference-$i> by regex "<expression-$i>"""")
      verify(mockActiveScope).set(s"<attribute>/regex/source", s"<reference-$i>")
      verify(mockActiveScope).set(s"<attribute>/regex/expression", s"<expression-$i>")
    }
  }

  """<attribute> <is|will be> defined in <reference> by json path "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute> $x defined in <reference-$i> by json path "<expression-$i>"""")
      verify(mockActiveScope).set(s"<attribute>/json path/source", s"<reference-$i>")
      verify(mockActiveScope).set(s"<attribute>/json path/expression", s"<expression-$i>")
    }
  }

  """<attribute> <is|will be> "<value>"""" should "evaluate" in {
    List("is", "will be").zipWithIndex.foreach { case (x, i) =>
      evaluate(s"""<attribute-$i> $x "<value-$i>"""")
      verify(mockFeatureScope).set(s"<attribute-$i>", s"<value-$i>")
    }
  }

  "I wait for <element> text for 1 second" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    evaluate("I wait for <element> text for 1 second")
  }

  "I wait for <element> text for <duration> seconds" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    evaluate("I wait for <element> text for 2 seconds")
  }

  "I wait for <element> text" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    evaluate("I wait for <element> text")
  }

  "I wait for <element> for 1 second" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    evaluate("I wait for <element> for 1 second")
  }

  "I wait for <element> for <duration> seconds" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    evaluate("I wait for <element> for 2 seconds")
  }

  "I wait for <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    evaluate("I wait for <element>")
  }

  "I clear <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).performAction("clear", mockBinding)
    evaluate("I clear <element>")
    verify(webContext).performAction("clear", mockBinding)
  }

  "I press <enter|tab> in <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    List("enter", "tab").foreach { key =>
      doNothing().when(webContext).sendKeys(mockBinding, Array(key))
      doNothing().when(env).bindAndWait("<element>", key, "true")
      evaluate(s"I press $key in <element>")
      verify(webContext).sendKeys(mockBinding, Array(key))
      verify(env).bindAndWait("<element>", key, "true")
    }
  }

  """I <enter|type> "<text>" in <element>""" should "evaluate" in {
    List("enter", "type").foreach { action =>
      val mockBinding = mock[LocatorBinding]
      doReturn(mockBinding).when(env).getLocatorBinding("<element>")
      doNothing().when(webContext).sendValue(mockBinding, "<text>", clearFirst = true, sendEnterKey = action == "enter")
      evaluate(s"""I $action "<text>" in <element>""")
      verify(webContext).sendValue(mockBinding, "<text>", clearFirst = true, sendEnterKey = action == "enter")
    }
  }

  "I <enter|type> <reference> in <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doReturn("<text>").when(env).getBoundReferenceValue("<reference>")
    List("enter", "type").foreach { action =>
      doNothing().when(webContext).sendValue(mockBinding, "<text>", clearFirst = true, sendEnterKey = action == "enter")
      evaluate(s"I $action <reference> in <element>")
      verify(webContext).sendValue(mockBinding, "<text>", clearFirst = true, sendEnterKey = action == "enter")
    }
  }

  "I select the <position><st|nd|rd|th> option in <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    List("st", "nd", "rd", "th").zipWithIndex.foreach { case (x, i) =>
      doNothing.when(webContext).selectByIndex(mockBinding, i)
      evaluate(s"I select the ${i+1}$x option in <element>")
      verify(webContext).selectByIndex(mockBinding, i)
    }
  }

  """I select "<text>" in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).selectByVisibleText(mockBinding, "<text>")
    evaluate("""I select "<text>" in <element>""")
    verify(webContext).selectByVisibleText(mockBinding, "<text>")
  }

  """I select "<text>" in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).selectByValue(mockBinding, "<text>")
    evaluate("""I select "<text>" in <element> by value""")
    verify(webContext).selectByValue(mockBinding, "<text>")
  }

  """I select <reference> in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doReturn("<text>").when(env).getBoundReferenceValue("<reference>")
    doNothing().when(webContext).selectByVisibleText(mockBinding, "<text>")
    evaluate("""I select <reference> in <element>""")
    verify(webContext).selectByVisibleText(mockBinding, "<text>")
  }

  """I select <reference> in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doReturn("<text>").when(env).getBoundReferenceValue("<reference>")
    doNothing().when(webContext).selectByValue(mockBinding, "<text>")
    evaluate("""I select <reference> in <element> by value""")
    verify(webContext).selectByValue(mockBinding, "<text>")
  }

  "I deselect the <position><st|nd|rd|th> option in <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    List("st", "nd", "rd", "th").zipWithIndex.foreach { case (x, i) =>
      doNothing.when(webContext).deselectByIndex(mockBinding, i)
      evaluate(s"I deselect the ${i+1}$x option in <element>")
      verify(webContext).deselectByIndex(mockBinding, i)
    }
  }

  """I deselect "<text>" in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).deselectByVisibleText(mockBinding, "<text>")
    evaluate("""I deselect "<text>" in <element>""")
    verify(webContext).deselectByVisibleText(mockBinding, "<text>")
  }

  """I deselect "<text>" in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).deselectByValue(mockBinding, "<text>")
    evaluate("""I deselect "<text>" in <element> by value""")
    verify(webContext).deselectByValue(mockBinding, "<text>")
  }

  """I deselect <reference> in <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doReturn("<text>").when(env).getBoundReferenceValue("<reference>")
    doNothing().when(webContext).deselectByVisibleText(mockBinding, "<text>")
    evaluate("""I deselect <reference> in <element>""")
    verify(webContext).deselectByVisibleText(mockBinding, "<text>")
  }

  """I deselect <reference> in <element> by value""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doReturn("<text>").when(env).getBoundReferenceValue("<reference>")
    doNothing().when(webContext).deselectByValue(mockBinding, "<text>")
    evaluate("""I deselect <reference> in <element> by value""")
    verify(webContext).deselectByValue(mockBinding, "<text>")
  }

  "I <click|right click|double click|submit|check|tick|uncheck|untick|move to> <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    List("click", "right click", "double click", "submit", "check", "tick", "uncheck", "untick", "move to").foreach { action =>
      doNothing().when(webContext).performAction(action, mockBinding)
      evaluate(s"I $action <element>")
      verify(webContext).performAction(action, mockBinding)
    }
  }

  "I <click|right click|double click> <element>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    List("click", "right click", "double click").foreach { action =>
      doNothing().when(webContext).holdAndClick(Array("COMMAND", "SHIFT"), action, mockBinding)
      evaluate(s"I COMMAND+SHIFT $action <element>")
      verify(webContext).holdAndClick(Array("COMMAND", "SHIFT"), action, mockBinding)
    }
  }

  "I <click|right click|double click|check|tick|uncheck|untick|move to> <element> of <context>" should "evaluate" in {
    List("click", "right click", "double click", "check", "tick", "uncheck", "untick", "move to").foreach { action =>
      doNothing().when(webContext).performActionInContext(action, "<element>", "<context>")
      evaluate(s"I $action <element> of <context>")
      verify(webContext).performActionInContext(action, "<element>", "<context>")
    }
  }

  """I send "<keys>" to <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).sendKeys(mockBinding, Array("CONTROL", "C"))
    evaluate("""I send "CONTROL,C" to <element>""")
    verify(webContext).sendKeys(mockBinding, Array("CONTROL", "C"))
  }

  "I wait 1 second when <element> is <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"I wait 1 second when <element> is $event")
      verify(mockScopes).set(s"<element>/${WebEvents.EventToAction(event)}/wait", "1")
    }
  }

  "I wait <duration> seconds when <element> is <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"I wait 2 seconds when <element> is $event")
      verify(mockScopes).set(s"<element>/${WebEvents.EventToAction(event)}/wait", "2")
    }
  }

  "I wait until <condition> when <element> is <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"I wait until <condition> when <element> is $event")
      verify(mockScopes).set(s"<element>/${WebEvents.EventToAction(event)}/condition", "<condition>")
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
    val mockWebElement = mock[WebElement]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doReturn(mockWebElement).when(webContext).locate(mockBinding)
    List("highlight", "locate").foreach { x =>
      evaluate(s"I $x <element>")
    }
    verify(webContext, times(2)).locate(mockBinding)
  }

  """I execute javascript "<javascript>"""" should "evaluate" in {
    doReturn(true).when(env).evaluateJS("<javascript>")
    evaluate("""I execute javascript "<javascript>"""")
    verify(env).evaluateJS("<javascript>")
  }

  """I execute system process "<command>"""" should "evaluate" in {
    evaluate("""I execute system process "hostname"""")
  }

  "I refresh the current page" should "evaluate" in {
    doNothing().when(webContext).refreshPage()
    evaluate("I refresh the current page")
    verify(webContext).refreshPage()
  }

  "I base64 decode <reference> as <attribute>" should "evaluate" in {
    doReturn("value").when(env).getBoundReferenceValue("<reference>")
    doReturn("decoded").when(env).decodeBase64("value")
    evaluate("I base64 decode <reference> as <attribute>")
    verify(env).addAttachment("<attribute>", "txt", "decoded")
    verify(mockFeatureScope).set("<attribute>", "decoded")
  }

  "I base64 decode <reference>" should "evaluate" in {
    doReturn("value").when(env).getBoundReferenceValue("<reference>")
    doReturn("decoded").when(env).decodeBase64("value")
    evaluate("I base64 decode <reference>")
    verify(env).addAttachment("<reference>", "txt", "decoded")
    verify(mockFeatureScope).set("<reference>", "decoded")
  }

  "<step> <until|while> <condition> using <delayPeriod> <second|millisecond> delay and <timeoutPeriod> <minute|second|millisecond> timeout" should "evaluate" in {
    timeUnits1.foreach { unit1 =>
      timeUnits2.foreach { unit2 =>
        List("until", "while").foreach { repeat =>
          val step = evaluatePriority(s"""x is "1" $repeat <condition> using 1 $unit1 delay and 2000 $unit2 timeout""")
          step should not be (None)
          step.get.toString should be (s"""Given x is "1" $repeat <condition> using 1 $unit1 delay and 2000 $unit2 timeout""")
        }
      }
    }
  }

  "<step> <until|while> <condition> using <delayPeriod> <second|millisecond> delay" should "evaluate" in {
    timeUnits1.foreach { unit =>
      List("until", "while").foreach { repeat =>
        val step = evaluatePriority(s"""x is "1" $repeat <condition> using 1 $unit delay""")
        step should not be (None)
        step.get.toString should be (s"""Given x is "1" $repeat <condition> using 1 $unit delay""")
      }
    }
  }

  "<step> <until|while> <condition> using no delay and <timeoutPeriod> <minute|second|millisecond> timeout" should "evaluate" in {
    timeUnits2.foreach { unit =>
      List("until", "while").foreach { repeat =>
        val step = evaluatePriority(s"""x is "1" $repeat <condition> using no delay and 2000 $unit timeout""")
        step should not be (None)
        step.get.toString should be (s"""Given x is "1" $repeat <condition> using no delay and 2000 $unit timeout""")
      }
    }
  }

  "<step> <until|while> <condition> using no delay" should "evaluate" in {
    List("until", "while").foreach { repeat =>
      val step = evaluatePriority(s"""x is "1" $repeat <condition> using no delay""")
      step should not be (None)
      step.get.toString should be (s"""Given x is "1" $repeat <condition> using no delay""")
    }
  }

  "<step> <until|while> <condition>" should "evaluate" in {
    List("until", "while").foreach { repeat =>
      val step = evaluatePriority(s"""x is "1" $repeat <condition>""")
      step should not be (None)
      step.get.toString should be (s"""Given x is "1" $repeat <condition>""")
    }
  }

  "I close the current browser" should "evaluate" in {
    doNothing().when(webContext).close()
    evaluate("I close the current browser")
    verify(webContext).close()
  }

  "I close the browser" should "evaluate" in {
    doNothing().when(webContext).close()
    evaluate("I close the browser")
    verify(webContext).close()
  }

  "I start a new browser" should "evaluate" in {
    doNothing().when(webContext).close("primary")
    doNothing().when(webContext).switchToSession("primary")
    evaluate("I start a new browser")
    verify(webContext).close("primary")
    verify(webContext).switchToSession("primary")
  }

  "I start a browser for <session>" should "evaluate" in {
    doNothing().when(webContext).close("<session>")
    doNothing().when(webContext).switchToSession("<session>")
    evaluate("I start a browser for <session>")
    verify(webContext).close("<session>")
    verify(webContext).switchToSession("<session>")
  }

  "I close the browser for <session>" should "evaluate" in {
    doNothing().when(webContext).close("<session>")
    evaluate("I close the browser for <session>")
    verify(webContext).close("<session>")
  }

  "I switch to <session>" should "evaluate" in {
    doNothing().when(webContext).switchToSession("<session>")
    evaluate("I switch to <session>")
    verify(webContext).switchToSession("<session>")
  }

  "I <accept|dismiss> the <alert|confirmation> popup" should "evaluate" in {
    List("accept", "dismiss").foreach { action =>
      val accept = action == "accept"
      doNothing().when(webContext).handleAlert(accept)
      List("alert", "confirmation").foreach { mode =>
        evaluate(s"I $action the $mode popup")
      }
    }
    verify(webContext, times(2)).handleAlert(true)
    verify(webContext, times(2)).handleAlert(false)
  }

  "I switch to the child <window|tab>" should "evaluate" in {
    doNothing().when(webContext).switchToChild()
    List("window", "tab").foreach { x =>
      evaluate(s"I switch to the child $x")
    }
    verify(webContext, times(2)).switchToChild()
  }

  "I close the child <window|tab>" should "evaluate" in {
    doNothing().when(webContext).closeChild()
    List("window", "tab").foreach { x =>
      evaluate(s"I close the child $x")
    }
    verify(webContext, times(2)).closeChild()
  }

  "I switch to the parent <window|tab>" should "evaluate" in {
    List("window", "tab").foreach { x =>
      doNothing().when(webContext).switchToParent(false)
      evaluate(s"I switch to the parent $x")
    }
    verify(webContext, times(2)).switchToParent(false)
  }

  "I switch to the default content" should "evaluate" in {
    doNothing().when(webContext).switchToDefaultContent()
    evaluate("I switch to the default content")
    verify(webContext).switchToDefaultContent()
  }

  "I capture the current screenshot" should "evaluate" in {
    doNothing().when(webContext).captureScreenshot(true)
    evaluate("I capture the current screenshot")
    verify(webContext).captureScreenshot(true)
  }

  """<element> can be <clicked|right clicked|double clicked|submitted|checked|ticked|unchecked|unticked|selected|deselected|typed|entered|tabbed|cleared|moved to> by javascript "<javascript>"""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    events.foreach { event =>
      evaluate(s"""<element> can be $event by javascript "<javascript>"""")
      verify(mockScopes).set(s"<element>/action/${WebEvents.EventToAction(event)}/javascript", "<javascript>")
    }
  }

  """<reference> <is|will be> defined by sql "<selectStmt>" in the <dbName> database""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        List("is", "will be").foreach { x =>
          evaluate(s"""<reference> $x defined by sql "<selectStmt>" in the <dbName> database""")
        }
        verify(mockActiveScope, times(2)).set(s"<reference>/sql/selectStmt", "<selectStmt>")
        verify(mockActiveScope, times(2)).set(s"<reference>/sql/dbName", "<dbName>")
      }
    }
  }

  """<reference> <is|will be> defined in the <dbName> database by sql "<selectStmt>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        List("is", "will be").foreach { x =>
          evaluate(s"""<reference> $x defined in the <dbName> database by sql "<selectStmt>"""")
        }
        verify(mockActiveScope, times(2)).set(s"<reference>/sql/selectStmt", "<selectStmt>")
        verify(mockActiveScope, times(2)).set(s"<reference>/sql/dbName", "<dbName>")
      }
    }
  }

  """I update the <dbName> database by sql "<updateStmt>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    doReturn(1).when(env).executeSQLUpdate("<updateStmt>", "<dbName>")
    withSetting("gwen.db.<dbName>.driver", "driver") {
      withSetting("gwen.db.<dbName>.url", "url") {
        evaluate("""I update the <dbName> database by sql "<updateStmt>"""")
        verify(mockActiveScope).set(s"<dbName> rows affected", "1")
      }
    }
  }

  "I resize the window to width <w> and height <h>" should "evaluate" in {
    doNothing().when(webContext).resizeWindow(400, 200)
    evaluate("I resize the window to width 400 and height 200")
    verify(webContext).resizeWindow(400, 200)
  }

  "I <maximize|maximise> the window" should "evaluate" in {
    doNothing().when(webContext).maximizeWindow()
    List("maximize", "maximise").foreach { x =>
      evaluate(s"I $x the window")
    }
    verify(webContext, times(2)).maximizeWindow()
  }

  """<step> for each <element> located by id "<expression>"""" should "evaluate" in {
    doReturn(Nil).when(webContext).locateAll(any[LocatorBinding])
    val step = evaluatePriority(s"""x is "1" for each <element> located by id "<expression>"""")
    step should not be (None)
    step.get.toString should be (s"""Given x is "1" for each <element> located by id "<expression>"""")
  }

  """<step> for each <element> located by id "<expression>" in <container>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<container>")
    doReturn(Nil).when(webContext).locateAll(any[LocatorBinding])
    val step = evaluatePriority(s"""x is "1" for each <element> located by id "<expression>" in <container>""")
    step should not be (None)
    step.get.toString should be (s"""Given x is "1" for each <element> located by id "<expression>" in <container>""")
  }

  "<step> for each <element> in <elements>" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<elements>")
    doReturn(Nil).when(webContext).locateAll(any[LocatorBinding])
    val step = evaluatePriority("""x is "1" for each <element> in <elements>""")
    step should not be (None)
    step.get.toString should be (s"""Given x is "1" for each <element> in <elements>""")
  }

  "<step> for each data record" should "evaluate" in {
    val mockTable = mock[FlatTable]
    doReturn(Some(mockTable)).when(mockFeatureScope).getObject("table")
    doReturn(Nil).when(mockTable).records
    val step = evaluatePriority("""x is "1" for each data record""")
    step should not be (None)
    step.get.toString should be (s"""Given x is "1" for each data record""")
  }

  "<attribute> should be absent" should "evaluate" in {
    doThrow(new UnboundAttributeException("<attribute>", None)).when(env).getBoundReferenceValue("<attribute>")
    evaluate("<attribute> should be absent")
  }

  """<source> at json path "<path>" should be "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""{x:"value"}""").when(mockActiveScope).get("<source>")
      doReturn(None).when(mockScopes).getOpt("<source>")
      evaluate(s"""<source> at json path "$$.x" should $operator "$expression"""")
    }
  }

  """<source> at json path "<path>" should not be "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""{x:"other"}""").when(mockActiveScope).get("<source>")
      doReturn(None).when(mockScopes).getOpt("<source>")
      evaluate(s"""<source> at json path "$$.x" should not $operator "$expression"""")
    }
  }

  """<source> at xpath "<path>" should be "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""<x>value</x>""").when(mockActiveScope).get("<source>")
      doReturn(None).when(mockScopes).getOpt("<source>")
      evaluate(s"""<source> at xpath "x" should $operator "$expression"""")
    }
  }

  """<source> at xpath "<path>" should not be "<expression>"""" should "evaluate" in {
    val mockActiveScope = mock[ScopedDataStack]
    doReturn(mockActiveScope).when(env).activeScope
    matchers3.foreach { case (operator, _, expression) =>
      doReturn("""<x>other</x>""").when(mockActiveScope).get("<source>")
      doReturn(None).when(mockScopes).getOpt("<source>")
      evaluate(s"""<source> at xpath "x" should not $operator "$expression"""")
    }
  }

  "<step> if <condition>" should "evaluate" in {
    val step = evaluatePriority("""x is "1" if <condition>""")
    step should not be (None)
    step.get.toString should be (s"""Given x is "1" if <condition>""")
  }

  """<step> for each <entry> in <source> delimited by "<delimiter>"""" should "evaluate" in {
    doReturn("").when(env).getBoundReferenceValue("<source>")
    val step = evaluatePriority("""x is "1" for each <entry> in <source> delimited by ","""")
    step should not be (None)
    step.get.toString should be (s"""Given x is "1" for each <entry> in <source> delimited by ","""")
  }

  "I drag and drop <element A> to <element B>" should "evaluate" in {
    val mockLocatorA: LocatorBinding = mock[LocatorBinding]
    val mockLocatorB: LocatorBinding = mock[LocatorBinding]
    doReturn(mockLocatorA).when(env).getLocatorBinding("<element A>")
    doReturn(mockLocatorB).when(env).getLocatorBinding("<element B>")
    evaluate("I drag and drop <element A> to <element B>")
    verify(webContext).dragAndDrop(mockLocatorA, mockLocatorB)
  }

  """I append "<text>" to <element>""" should "evaluate" in {
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).sendValue(mockBinding, "<text>", clearFirst = false, sendEnterKey = false)
    evaluate("""I append "<text>" to <element>""")
    verify(webContext).sendValue(mockBinding, "<text>", clearFirst = false, sendEnterKey = false)
  }

  """I append <reference> to <element>""" should "evaluate" in {
    doReturn("<text>").when(env).getBoundReferenceValue("<reference>")
    val mockBinding = mock[LocatorBinding]
    doReturn(mockBinding).when(env).getLocatorBinding("<element>")
    doNothing().when(webContext).sendValue(mockBinding, "<text>", clearFirst = false, sendEnterKey = false)
    evaluate("""I append <reference> to <element>""")
    verify(webContext).sendValue(mockBinding, "<text>", clearFirst = false, sendEnterKey = false)
  }
  
}