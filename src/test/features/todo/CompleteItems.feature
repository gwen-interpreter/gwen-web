Feature: Create and complete Todo items

  Scenario: Add items in my Todo list
    Given I launch the Todo app
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
     Then the number of active items should contain "2"

  Scenario: Complete one item
    Given item count is "1"
     When I tick the "Get the milk" item
     Then the number of active items should contain item count

  Scenario: Complete another item
    Given item count is "0"
     When I tick the "Walk the dog" item
     Then the number of active items should contain item count

  Scenario: Clear my completed items
    Given item count is blank
     When I click the clear completed button
     Then the number of active items should not be displayed
