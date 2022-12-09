Feature: Google search
   
  Scenario: Perform a google search
    Given I have Google in my browser
     When I do a search for "gwen-web automation"
     Then the first result should open a Gwen page

  # test fix for issue: https://github.com/gwen-interpreter/gwen-web/issues/65
  Scenario: Perform a google search with append
    Given I have Google in my browser
      And the search field can be located by name "q"
     When I type "search string" in the search field
      And I append " " to the search field
      And I capture element screenshot of the search field
     Then the search field should be "search string "

  Scenario: Perform a google search with DocString parameter
    Given I have Google in my browser
     When I do a search for
          """
          Gwen automation
          """
      Then the first result should open a Gwen page

  Scenario: Perform a google search with element guard
    Given I have Google in my browser
      And the search field can be located by name "q"
     When I type "Gwen " in the search field
      And I enter "automation" in the search field if the search field is displayed
     Then the first result should open a Gwen page

  Scenario: Perform a google search with result guard
    Given I have Google in my browser
      And the search field can be located by name "q"
      And topic is "Automation"
      And topic link can be located by partial link text "${topic}"
     When I enter "gwen automation" in the search field
      And I click topic link if topic link is displayed
     Then the page title should contain "${topic}"
