Feature: Shadow DOM

  Scenario: Access shodow DOM
    Given I have an open browser
      And @ShadowRoot shadow host can be located by id "shadow_host"
      And some text can be located by css "#shadow_content > span" in shadow host
      And @ShadowRoot nested shadow host can be located by css "#nested_shadow_host" in shadow host
      And nested text can be located by css "#nested_shadow_content > div" in nested shadow host
     When I navigate to "http://watir.com/examples/shadow_dom.html"
     Then some text should be "some text"
      And nested text should be "nested text"
