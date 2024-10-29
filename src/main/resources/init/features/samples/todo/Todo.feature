@Sample
Feature: Add todo items

  Add items to a todo list and check that the
  displayed count matches the number added

  Scenario: Create todo list

    Open the todomvc online app to add items
    and check the displayed count when done

    Given a new todo list
     When the following items are added
          | Item         |
          | Get the milk |
          | Walk the dog |
     Then the list will contain 2 items
