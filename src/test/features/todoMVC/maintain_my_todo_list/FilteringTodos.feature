Feature: Filtering todos

  Background: Open a new browser
    Given I have no open browser
     When I start a browser for James
     Then I should have 1 open browser

  Scenario: I should be able to view only completed todos
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I complete the "Walk the dog" item
      And I apply the "Completed" filter
     Then the displayed items should be "Walk the dog"
      And the "Completed" filter should be selected

  Scenario: I should be able to view only incomplete todos
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I complete the "Walk the dog" item
      And I apply the "Active" filter
     Then the displayed items should be "Put out the garbage"
      And the "Active" filter should be selected

  Scenario: I should be able to view both complete and incomplete todos
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I complete the "Walk the dog" item
      And I apply the "Active" filter
      And I apply the "All" filter
     Then the displayed items should be "Walk the dog,Put out the garbage"
      And the "All" filter should be selected
