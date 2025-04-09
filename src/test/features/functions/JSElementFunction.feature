Feature: JS Element Function

  Scenario: Get href of link with lambda function
    Given getHref1 is defined by js "(a) => a.getAttribute('href')"
      And the todo link can be located by xpath "//a[contains(text(),'TodoMVC')]"
      And href1 is defined by getHref1 applied to the todo link
     When I navigate to "https://todomvc.com/examples/react/dist/"
     Then href1 should be "http://todomvc.com"

  Scenario: Eagerly get href of link with block function
    Given getHref2 is defined by js 
      """
      (elem) => {
        return elem.getAttribute('href')
      }
      """
      And the todo link can be located by xpath "//a[contains(text(),'TodoMVC')]"
     When I navigate to "https://todomvc.com/examples/react/dist/"
      And @Eager href2 is defined by getHref2 applied to the todo link
     Then href2 should be "http://todomvc.com"
