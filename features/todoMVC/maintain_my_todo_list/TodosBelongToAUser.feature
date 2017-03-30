
# This is a Gwen executable feature that mimics the Serenity feature test here:
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/maintain_my_todo_list/TodosBelongToAUser.java

   Feature: Todos belong to a user
  
Background: Open two browser sessions for two users
      Given I start a browser for James
        And I start a browser for Jane
        
  Scenario: I should not affect todos belonging to another user 
      Given I switch to James
        And I browse to the application home page
        And I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I switch to Jane
        And I browse to the application home page
        And I add a "Walk the dog" item
        And I add a "Walk the cat" item
        And I switch to James
        And I complete the "Walk the dog" item
       When I switch to Jane
       Then the displayed items should be "Walk the dog,Walk the cat"