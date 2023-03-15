Feature: Learn about the application

  Background: Open a new browser
    Given I have no open browser
     When I start a browser for James
     Then I should have 1 open browser

  Scenario: I should be able to identify the application
    Given I have an open browser
     When I browse to the application home page
     Then the page title should contain "Todo"
      And the heading should be "todos"
      And the info panel should contain "todo"

  Scenario: I should see how to begin
    Given I have an open browser
     When I browse to the application home page
     Then the placeholder string should be "What needs to be done?"
