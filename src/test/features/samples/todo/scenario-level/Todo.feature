Feature: Create and complete Todo items

  Background: Add items to my Todo list

    Prime the list with 3 items

    Given I launch the Todo app
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
      And I add a "Feed the cat" item
     Then the number of open items should be "3"

  Scenario: Complete second item

    Complete the 2nd item
    
    Given I am on the Todo page
     When I complete the "Get the milk" item
     Then the number of open items should be "2"

  Scenario: Complete and clear all items

    Complete and clear all items

    Given I am on the Todo page
     When I complete the "Get the milk" item
      And I complete the "Walk the dog" item
      And I complete the "Feed the cat" item
      And I clear all completed items
     Then the number of open items should not be displayed
