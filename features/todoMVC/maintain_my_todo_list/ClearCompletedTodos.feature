
# This is a Gwen executable feature that mimics the Serenity feature test here:
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/maintain_my_todo_list/ClearCompletedTodos.java

   Feature: Clear completed todos
  
Background: Open a new browser
      Given I start a browser for James
      
  Scenario: I should be able to clear completed todos
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I complete the "Walk the dog" item
        And I clear all completed items
       Then the displayed items should be "Put out the garbage"

  Scenario: I should not be able to clear completed todos if none are complete
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
       Then the clear completed items option should be unavailable