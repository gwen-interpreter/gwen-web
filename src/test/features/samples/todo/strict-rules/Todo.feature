Feature: Good todo

  Scenario: Add and complete items
    Given a new todo list
     When the following items are added
          | ITEM         |
          | Get the milk |
          | Walk the dog |
      And all items are completed
     Then no active items should remain
