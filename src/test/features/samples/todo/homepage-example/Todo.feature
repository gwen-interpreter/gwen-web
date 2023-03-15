Feature: Todo

  Scenario: Create todo list
    Given a new todo list
     When the following items are added
          | Get the milk |
          | Walk the dog |
     Then the list will contain 2 items
