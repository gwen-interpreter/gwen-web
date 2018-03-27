package gwen.web

class WebInterpreterSwitchToDefaultContentTest extends WebInterpreterTest {
  "Locator Chaining feature" should "should evaluate" in {
    evaluate(List("features/locators-chaining"), parallel = false, dryRun = false, "target/reports/locators-chainings", None)
  }
}
