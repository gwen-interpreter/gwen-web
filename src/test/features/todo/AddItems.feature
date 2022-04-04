@Todo
Feature: Add Todo items

  @Todo
  Scenario: Add items in my Todo list
    Given I launch the Todo app
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
     Then the number of active items should be "2"  @Message("wrong number of steps added")
