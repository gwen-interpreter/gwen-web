
Feature: Add todo items (for each)
  
  Add items to a todo list

  Scenario: Create todo list (for each)
    Given I have the following active items
          | Item         |
          | Walk the dog |
          | Get the milk |
          | Feed the cat |
      And the active count can be located by css ".todo-count strong"
     When I capture the active count as count
     Then count should be "3"
