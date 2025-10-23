Feature: Perform element actions by JS

  Scenario: Click link using js action ref
    Given the todo link can be located by xpath "//a[contains(text(),'TodoMVC')]"
      And the click function is defined by js "(elem) => elem.click()"
     When I navigate to "https://todomvc.com/examples/react/dist/"
      And I execute the click function on the todo link
     Then the current URL should be "https://todomvc.com/"

  Scenario: Click link using inline js action
    Given the todo link can be located by xpath "//a[contains(text(),'TodoMVC')]"
     When I navigate to "https://todomvc.com/examples/react/dist/"
      And I execute js on the todo link "(elem) => elem.click()"
     Then the current URL should be "https://todomvc.com/"
