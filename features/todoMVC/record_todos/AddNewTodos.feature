   Feature: Add new todos
  
Background: Open a new browser
      Given I start a browser for James
      
  Scenario: I should be able to add the first todo item
      Given I browse to the application home page
       When I add a "Buy some milk" item
       Then the displayed items should contain "Buy some milk"
       
  Scenario: I should be able to add additional todo items
      Given I browse to the application home page
       When I add a "Walk the dog" item
        And I add a "Put out the garbage" item
        And I complete the "Walk the dog" item
        And I add a "Buy some milk" item
       Then the displayed items should be "Walk the dog,Put out the garbage,Buy some milk"