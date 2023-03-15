Feature: Create and complete Todo items

  Scenario: Add items in my Todo list
    Given I launch the Todo app
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
     Then the number of active items should be "2"

  Scenario: Complete one item
    Given I am on the Todo page
     When I tick the "Get the milk" item
     Then the number of active items should be "1"

  Scenario: Complete another item
    Given I am on the Todo page
     When I tick the "Walk the dog" item
     Then the number of active items should be "0"

  Scenario: Clear my completed items
    Given I am on the Todo page
     When I click the clear completed button
     Then the number of active items should not be displayed
