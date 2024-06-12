Feature: Load Todo items from JSON files

  Scenario: Launch the Todo application
    Given I launch the Todo app
     When I load items from JSON files
      And I load items from deep JSON file
      And I load items from deep JSON array
      And I load items by loopoing over nested JSON array
      And I load items from empty JSON file
     Then the "Walk the dog" item should be unticked
      And the "Get the milk" item should not be displayed
      And the "Take out trash" item should be unticked
      And the "Feed the cat" item should be unticked
      And the "Wash the car" item should be ticked
      And the "Write some code" item should be unticked
      And the "Mow the lawn" item should be unticked
      And the "Do the washing" item should be ticked
      And the "Take a break" item should be unticked
      And the "Bake a cake" item should be unticked
      And the "Grab a coffee" item should be ticked
      And the "Watch a movie" item should be unticked
      And the "Fly a kite" item should be unticked
      And the "Have a meal" item should be ticked
      And the "Go to bed" item should be unticked
      And the number of active items should contain "10"
