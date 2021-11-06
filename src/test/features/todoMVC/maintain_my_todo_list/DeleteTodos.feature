# This is a Gwen executable feature that mimics the Serenity feature test here:
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/maintain_my_todo_list/DeleteTodos.java

Feature: Delete todos
  
  Background: Open a new browser
    Given I have no open browser
     When I start a browser for James
     Then I should have 1 open browser
      
  Scenario: I should be able to delete todos
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I delete the "Walk the dog" item
     Then the displayed items should be "Put out the garbage"
       
  Scenario: I should see deleting a todo decreases the remaining items count
    Given I have an open browser
     When I browse to the application home page
      And I add a "Walk the dog" item
      And I add a "Put out the garbage" item
      And I delete the "Walk the dog" item
     Then the number of items left should contain "1"
