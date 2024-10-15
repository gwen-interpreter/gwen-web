Feature: Create and complete Todo items

  Scenario: Add items in my Todo list
    Given I launch the Todo app
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
      And I add a "Feed the cat" item
     Then the number of open items should contain "3"

  Scenario: Complete second item
    Given item count is "2"
     When I complete the "Get the milk" item
     Then the number of open items should contain item count

  Scenario: Complete first item
    Given item count is "1"
     When I complete the "Walk the dog" item
     Then the number of open items should contain item count

  Scenario: Complete third item
    Given item count is "0"
     When I complete the "Feed the cat" item
     Then the number of open items should contain item count

  Scenario: Clear my completed items
    Given item count is blank
     When I clear all completed items
     Then the number of open items should not be displayed
