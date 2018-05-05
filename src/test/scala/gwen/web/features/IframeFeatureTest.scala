package gwen.web.features

class SwitchToDefaultContentTest extends BaseFeatureTest {
  "Locator Chaining feature" should "should evaluate" in {
    evaluate(List("features/locators-chaining"), parallel = false, dryRun = false, "target/reports/locators-chainings", None)
  }
}
