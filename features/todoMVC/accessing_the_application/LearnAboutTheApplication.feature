
# This is a Gwen executable feature that mimics the Serenity feature test here:
#   https://github.com/RiverGlide/serenity-web-todomvc-journey/blob/master/src/test/java/net/serenitybdd/demos/todos/features/accessing_the_application/LearnAboutTheApplication.java

   Feature: Learn about the application
  
Background: Open a new browser
      Given I start a browser for James
      
  Scenario: I should be able to identify the application
      Given I browse to the application home page
       Then the page title should contain "Todo"
        And the heading should be "todos"
        And the info panel should contain "todo"
      
  Scenario: I should see how to begin
      Given I browse to the application home page
       Then the placeholder string should be "What needs to be done?"