
# This is a Gwen executable feature that mimics the Serenity feature test here:
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/completing_todos/CompleteATodo.java

   Feature: Complete a Todo
  
Background: Open a new browser
      Given I start a browser for James
      
  Scenario: I should be able to complete a todo
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I complete the "Walk the dog" item
       Then the "Walk the dog" item should be completed

  Scenario: I should see the number of todos decrease when an item is completed
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I complete the "Put out the garbage" item
       Then the number of items left should contain "1"
     
  Scenario: I should see that there are zero items todo when all are toggled complete
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I toggle all items
       Then the number of items left should contain "0"
     
  Scenario: I should see how many items todo when all are toggled to incomplete
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I toggle all items
        And I toggle all items
       Then the number of items left should contain "2"