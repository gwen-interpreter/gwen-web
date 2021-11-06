Feature: Todo

  Scenario: Create and complete Todo items
    Given I have three open items
     When I complete each open item
      And I clear all completed items
     Then I should have no items left
