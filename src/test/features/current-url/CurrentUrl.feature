Feature: Current URL checks

  Scenario: Current URL in gwen page
    Given the gwen page url is "https://github.com/gwen-interpreter/gwen-web"
     When I navigate to the gwen page
     Then the current URL should contain "gwen"
