Feature: Copy Todo items

  Scenario: Load items into session 1
    Given I launch the Todo app in session 1
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
      And I add a "Take out trash" item
     Then the number of active items should be "3"

  Scenario: Copy items from session 1 to session 2
    Given I launch the Todo app in session 2
     When I copy all items from session 1 to session 2
     Then the number of active items should be "3"
