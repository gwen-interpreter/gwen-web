Gwen-Web
========

<img src="https://gweninterpreter.org/img/gwen-logo-cw--rc.png" width="170"/> <br />

A [Gwen](https://github.com/gwen-interpreter/gwen/) interpreter that enables teams to automate end to end web testing and robotic processing with behaviour defined in [Gherkin](https://docs.cucumber.io/gherkin/reference/) feature specifications. Automation is achieved through Gherkin bindings called meta specs, composed with a prescribed DSL and maintained alongside your feature files. An embedded [Selenium](https://www.selenium.dev/) engine executes scenarios according to the meta you provide to perform operations in browsers for you.

Get Started
-----------

Visit the [Gwen home page](https://gweninterpreter.org) for our user documentation and getting started guide.

Current Status
--------------

[![CI](https://github.com/gwen-interpreter/gwen-web/actions/workflows/ci.yml/badge.svg)](https://github.com/gwen-interpreter/gwen-web/actions/workflows/ci.yml)

- [Latest release](https://github.com/gwen-interpreter/gwen-web/releases/latest)
- [Change log](CHANGELOG)

How it Works?
-------------

1. Declare [feature specs](https://docs.cucumber.io/gherkin/reference/) to describe scenarios in the language of your domain.

```gherkin
 Feature: Todo
   
   Scenario: Create todo list
     Given a new todo list
      When the following items are added
           | Get the milk  |
           | Walk the dog  |
      Then the list will contain 2 items
```

2. Compose [meta specs](https://gweninterpreter.org/docs/meta) to describe how steps will execute to automate scenarios.

```gherkin
 Feature: Todo Meta (automation glue)
                        
   @StepDef
   Scenario: a new todo list
      When I navigate to "http://todomvc.com/examples/react"
      Then the todo field can be located by class "new-todo"
       And count can be located by css ".todo-count strong"

   @StepDef
   @ForEach
   @DataTable(horizontal="item")
   Scenario: the following items are added
      When I enter item in the todo field
      Then count should be gwen.table.record.number

   @StepDef
   Scenario: the list will contain <expected> items
      Then count should be "$<expected>"
 ```

3. Launch Gwen to bind your meta and execute your feature specs to automate.

System Requirements
-------------------

- Linux, Mac or Windows OS
- Chrome, Firefox, Safari, or Edge browser
- Java 17 or higher

License
-------

Copyright 2014-2025 Brady Wood, Branko Juric.

This software is open sourced under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

See also: [LICENSE](LICENSE).

This project has dependencies on the core [Gwen](https://github.com/gwen-interpreter/gwen) interpreter and other open source projects. All distributed third party dependencies and their licenses are listed in the [LICENSE-THIRDPARTY](LICENSE-THIRDPARTY) file.

Open sourced 28 June 2014 03:27 pm AEST

Contributions
-------------

New capabilities, improvements, fixes, and documentation are all welcomed candidates for contribution. Each submission is reviewed and verified by the project [maintainers](#maintainers) before being integrated and released to the community. We ask that all code submissions include unit tests or sample test features providing relevant coverage.

By submitting contributions, you agree to release your work under the [license](#license) that covers this software.

### How to Contribute

1. Fork this repository
2. Create a branch on your forked repository
3. Commit your changes to your branch
4. Push your branch to your forked repository
5. Create a pull request from your branch to here

Maintainers
-----------

- [Branko Juric](https://github.com/bjuric)
- [Brady Wood](https://github.com/bradywood)
- [Ruby Juric](https://github.com/Sorixelle)
