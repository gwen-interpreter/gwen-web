[![Gwen-web](https://github.com/gwen-interpreter/gwen/wiki/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/wiki/The-Gwen-Logo)

Gwen-Web
========

A [Gherkin](https://docs.cucumber.io/gherkin/reference/) interpreter that enables teams to quickly automate front end web tests and repetitive online processes with feature specifications. A [web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL) interacts with [Selenium](http://www.seleniumhq.org/projects/webdriver) under the covers to perform operations and checks in a consistent reliable manner across browsers for you. All the standard Selenium locators are supported and you can additionally inject JavaScript in places where dynamically finding elements or running functions on web pages may be necessary.

## Current Status

[![CI](https://github.com/gwen-interpreter/gwen-web/actions/workflows/ci.yml/badge.svg)](https://github.com/gwen-interpreter/gwen-web/actions/workflows/ci.yml)

- [Latest release](https://github.com/gwen-interpreter/gwen-web/releases/latest)
- [Change log](CHANGELOG)

### What's New?

- [Doc Strings as Parameters](https://github.com/gwen-interpreter/gwen/wiki/Doc-Strings/_edit#doc-strings-as-parameters)
- [Report Portal integration](https://github.com/gwen-interpreter/gwen/wiki/Report-Portal-Integration) for centralised reporting and real-time analytics.
- [Masked Settings](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#masked-settings) to hide private and sensitive property values from view and prevent them from being logged and reported.

How it Works?
-------------
1. Declare [feature specs](https://docs.cucumber.io/gherkin/reference/) to describe how scenarios and examples must behave.
```gherkin
  Feature: Todo

 Scenario: Create todo list
     Given a new todo list
      When the following items are added
           | Get the milk  |
           | Walk the dog  |
      Then the list will contain 2 items
```

2. Compose [meta specs](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features) to describe what your feature specs will execute.
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
3. Launch Gwen to bind your meta and execute your features to drive automation.

Get Started
-----------

See the [getting started](https://gweninterpreter.org/docs/get-started) guide on our Gwen project website.

Key Features
------------

- Behavour driven automation
  - Declarative features describe behavior
  - Imperative [Meta features](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features) describe automation
  - Gwen binds the two at runtime to drive automation
- A prescribed [Web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL) performs all browser interactions for you
- Automation across browsers and platforms is consistent (Chrome, Firefox, Safari, Edge and IE)
- An interactive [REPL console](https://github.com/gwen-interpreter/gwen/wiki/REPL-Console) provides a step by step execution environment

Learn More
----------

- Features can execute in [batch mode](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#batch-execution) or [interactively](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#interactive-repl-execution)
- Features and scenarios can execute [sequentially](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#serial-execution) or in [parallel](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#parallel-execution)
- [Runtime settings](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings)
- Execution can be [data driven](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#csv-data-feeds) (using csv data feeds)
- [Remote web driver](https://gweninterpreter.wordpress.com/2015/04/23/remote-webdriver-feature-now-available-in-gwen-web/) support
- [Screenshot capture and slideshow](https://github.com/gwen-interpreter/gwen-web/wiki/Screenshot-Capture-and-Slideshows) playback
- [Interchangeable Selenium](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#changing-the-selenium-version) implementation
- [Locator Chaining](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Chaining)
- [Headless Browser Execution](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#gwenwebbrowserheadless)
- [Gwen Workspaces](https://gweninterpreter.wordpress.com/2017/12/18/gwen-workspaces/) for easy installation
- [Template matching](https://github.com/gwen-interpreter/gwen/wiki/Template-Matching)
- [Drag and Drop](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL#i-drag-and-drop-sourceelement-to-targetelement)
- [Locator level timeouts](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Level-Timeouts)
- [Implicit JavaScript Locators](https://github.com/gwen-interpreter/gwen-web/wiki/Implicit-JavaScript-Locators)
- [Remote BrowserStack execution](https://gweninterpreter.wordpress.com/2018/06/16/running-gwen-on-browserstack/)
- [Synchronized StepDef execution](https://github.com/gwen-interpreter/gwen/wiki/Synchronized-StepDefs)
- [Indexed element locators](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL#element-can-be-located-by-idnametag-namecss-selectorxpathclass-namelink-textpartial-link-textjavascript-expression-at-index-index)
- Hard, soft, and sustained [assertion modes](https://github.com/gwen-interpreter/gwen/wiki/Assertion-Modes)
- [Visual Testing with AppliTools](https://github.com/gwen-interpreter/gwen-web/wiki/Visual-Testing)
- [Mobile Emulation](https://github.com/gwen-interpreter/gwen-web/wiki/Mobile-Emulation)
- Integrated [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) manages all native webdrivers for you
- Supports full Gherkin syntax including [example mapping](https://cucumber.io/blog/2015/12/08/example-mapping-introduction)
- [State levels](https://github.com/gwen-interpreter/gwen/wiki/State-Levels) and [parallel execution](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#parallel-scenario-execution) for scenarios in additon to features
- [Declarative feature mode](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#gwenfeaturemode) to force all imperative steps to meta and promote cleaner features.
- [Associative meta](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#gwenassociativemeta)
- [Behavior rules](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#gwenbehaviorrules) to help enforce good Gherkin style
- [Dialects](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#gwenfeaturedialect) for [Gherkin's spoken languages](https://cucumber.io/docs/gherkin/reference/#spoken-languages)
- Configurable [maximum number of threads](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#gwenparallelmaxthreads) for parallel execution
- Simplified [data table iteration with @ForEach](https://github.com/gwen-interpreter/gwen/wiki/Data-Tables#simplified-foreach)

License
-------

Copyright 2014-2021 Brady Wood, Branko Juric.

This software is open sourced under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

See also: [LICENSE](LICENSE).

This project has dependencies on [Gwen](https://github.com/gwen-interpreter/gwen) and other open source projects. All
distributed third party dependencies and their licenses are listed in the [LICENSE-THIRDPARTY](LICENSE-THIRDPARTY) file.

Open sourced 28 June 2014 03:27 pm AEST

Contributions
-------------

New capabilities, improvements, fixes, and documentation are all welcomed candidates for 
contribution. Each submission is reviewed and verified by the project [maintainers](#maintainers) 
before being integrated and released to the community. We ask that all code submissions include 
unit tests or sample test features providing relevant coverage.

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

We thank the following contributors and active users for helping to make Gwen better. You are all awesome!

The following contributors submitted pull requests that have been merged:

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
- [Selenium](https://www.seleniumhq.org/)
- [Cucumber/Gherkin](https://docs.cucumber.io/gherkin/reference/)
- [WebDriverManager](https://github.com/bonigarcia/webdrivermanager)
