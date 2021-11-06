# This is a Gwen executable feature that mimics the Serenity feature test here:
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/completing_todos/ToggleAllTodos.java

Feature: Toggle all todos
  
  Background: Open a new browser
    Given I have no open browser
     When I start a browser for James
     Then I should have 1 open browser
      
  Scenario: I should be able to quickly complete all todos
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I toggle all items
     Then the "Walk the dog" item should be completed
      And the "Put out the garbage" item should be completed

  Scenario: I should be able to toggle status of all todos
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I toggle all items
      And I toggle all items
     Then the "Walk the dog" item should be active
      And the "Put out the garbage" item should be active
