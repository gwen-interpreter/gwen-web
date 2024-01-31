@Lenient
Feature: Bad todo

  Scenario: Add and complete items
    Given I navigate to "https://todomvc.com/examples/react/dist"
     Then I should be on the todo page
     When I add a "Walk the dog" item
      And I add a "Get the milk" item
     Then the active item count should contain "2"
     When I tick the "Get the milk" item
     Then the active item count should contain "1"
     When I tick the "Walk the dog" item
     Then the active item count should contain "0"
