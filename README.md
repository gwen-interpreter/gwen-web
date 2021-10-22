Gwen-Web
========

A [Gwen](https://github.com/gwen-interpreter/gwen/) interpreter that enables teams to automate front end web tests and repetitive online processes with [Gherkin](https://docs.cucumber.io/gherkin/reference/) feature specifications. A web engine interacts with [Selenium](http://www.seleniumhq.org/projects/webdriver) under the covers to perform operations and checks in browsers. All the standard Selenium locators are supported and you can additionally inject JavaScript in places where dynamically finding elements or running functions on web pages may be necessary.

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
      When I enter data[item] in the todo field
      Then count should be record.number

 @StepDef
 Scenario: the list will contain <expected> items
      Then count should be "$<expected>"
 ```

3. Launch Gwen to bind your meta and execute your feature specs to automate.

System Requirements
-------------------

- Linux, Mac or Windows OS
- Chrome, Firefox, Safari, Edge or IE web browser
- Java 8 (version 1.8) or higher

License
-------

Copyright 2014-2021 Brady Wood, Branko Juric and [Gwen contributors](#code-contributors).

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

Contributors
------------

We thank the following contributors and active users for helping to make Gwen better.

The following [contributors](https://github.com/gwen-interpreter/gwen/graphs/contributors) submitted pull requests that have been merged:

- [Jacob Juric](https://github.com/Sorixelle)
| [Alexandru Cuciureanu](https://github.com/acuciureanu)

The following users raised issues or requests that have been addressed:

- [Rebecca Abriam](https://github.com/mairbar)
| [George Tsihitas](https://github.com/gtsihitas)
| [Zoltan Penzeli](https://github.com/siaynoq)
| [inkbleed](https://github.com/inkbleed)
| [Pradeep Thawani](https://github.com/pradeep-thawani)
| [anshu781126](https://github.com/anshu781126)
| [ketu4u2010](https://github.com/ketu4u2010)
| [Rahul9844](https://github.com/Rahul9844)
| [rkevin99](https://github.com/rkevin99)
| [Sergio Freire](https://github.com/bitcoder)
| [dfriquet](https://github.com/dfriquet)


Credits
-------
- [Selenium](https://www.selenium.dev/)
- [Cucumber/Gherkin](https://docs.cucumber.io/gherkin/reference/)
- [WebDriverManager](https://github.com/bonigarcia/webdrivermanager)
