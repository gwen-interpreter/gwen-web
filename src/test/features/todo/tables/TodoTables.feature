Feature: Managing todo items with data tables

  Scenario: Complete a todo item
    Given I have the following active items
          | Item         |
          | Walk the dog |
          | Get the milk |
          | Feed the cat |
     When I complete the "Get the milk" item
     Then the status of my items should be
          | Item         | Status    |
          | Walk the dog | active    |
          | Get the milk | completed |
          | Feed the cat | active    |
