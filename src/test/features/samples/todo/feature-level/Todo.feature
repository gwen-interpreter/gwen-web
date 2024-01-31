Feature: Create and complete Todo items

  Scenario: Add items in my Todo list
    Given I launch the Todo app
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
      And I add a "Feed the cat" item
     Then the number of open items should contain "3"

  Scenario: Complete second item
    Given I am on the Todo page
     When I complete the "Get the milk" item
     Then the number of open items should contain "2"

  Scenario: Complete first item
    Given I am on the Todo page
     When I complete the "Walk the dog" item
     Then the number of open items should contain "1"

  Scenario: Complete third item
    Given I am on the Todo page
     When I complete the "Feed the cat" item
     Then the number of open items should contain "0"

  Scenario: Clear my completed items
    Given I am on the Todo page
     When I clear all completed items
     Then the number of open items should not be displayed
