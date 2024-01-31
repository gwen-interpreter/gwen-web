Feature: Clear completed todos

  Background: Open a new browser
    Given I have no open browser
     When I start a browser for James
     Then I should have 1 open browser

  Scenario: I should be able to clear completed todos
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I complete the "Walk the dog" item
      And I clear all completed items
     Then the displayed items should be "Put out the garbage"
