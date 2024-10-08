Feature: Add new todos

  Background: Open a new browser
    Given I have no open browser
     When I start a browser for James
     Then I should have 1 open browser

  Scenario: I should be able to add the first todo item
    Given I have an open browser
     When I browse to the application home page
      And I add a "Buy some milk" item
     Then the displayed items should contain "Buy some milk"

  Scenario: I should be able to add additional todo items
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I complete the "Walk the dog" item
      And I add a "Buy some milk" item
     Then the displayed items should be "Walk the dog,Put out the garbage,Buy some milk"
