Feature: Selenium Test Page

  Scenario: Submit the test form
    Given I am on the Selenium test page
     When I provide some contact details
      And I submit the test form
     Then nothing should happen
      And the page should still function
