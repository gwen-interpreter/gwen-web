[![Gwen-web](https://github.com/gwen-interpreter/gwen/wiki/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/wiki/The-Gwen-Logo)

Gwen-web
========

A [Gwen](https://github.com/gwen-interpreter/gwen) interpreter that enables teams to quickly automate front end web
tests and robotic online processes with [Gherkin](https://docs.cucumber.io/gherkin/reference/) feature specifications.
A [web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL) interacts with
[Selenium](http://www.seleniumhq.org/projects/webdriver) under the covers for you so you don't have to setup up native
drivers or do any selenium programming or technical development work. It supports all the standard web element locators
provided by Selenium and additionally allows you to inject JavaScript in places where dynamically locating web elements
or running functions on pages may be necessary.

>  [User network and support](https://www.gwenify.com/)

### Current Status

[![Build Status](https://travis-ci.org/gwen-interpreter/gwen-web.svg?branch=master)](https://travis-ci.org/gwen-interpreter/gwen-web)

- [Latest release](https://github.com/gwen-interpreter/gwen-web/releases/latest)
- [Change log](CHANGELOG)

### What's New?
- Integrated [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) now manages all native webdrivers for you
- [Mobile Emulation](https://github.com/gwen-interpreter/gwen-web/wiki/Mobile-Emulation)
- [Visual Testing with AppliTools](https://github.com/gwen-interpreter/gwen-web/wiki/Visual-Testing)

Quick Links
-------------------------------
- [Installation](https://github.com/gwen-interpreter/gwen-web/wiki/Installation)
- [Getting Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)
- [Gwen Web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html)
- [Blog](https://gweninterpreter.wordpress.com)
- [Wiki](https://github.com/gwen-interpreter/gwen-web/wiki)
- [FAQ](https://github.com/gwen-interpreter/gwen-web/wiki/FAQ)

Why Gwen-web?
-------------
So you can drive automation with Gherkin features like this..
```gherkin
   Feature: Google search

  Scenario: Perform a google search
      Given I have Google in my browser
       When I do a search for "Gwen automation"
       Then the first result should open a Gwen page
```

..by writing [Gwen Meta features](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features) like this..
```gherkin
   Feature: Google search meta

  @StepDef
  Scenario: I have Google in my browser
      Given I start a new browser
       When I navigate to "http://www.google.com"
       Then the page title should be "Google"

  @StepDef
  Scenario: I do a search for "<query>"
      Given the search field can be located by name "q"
       When I enter "$<query>" in the search field
       Then the page title should contain "$<query>"

  @StepDef
  Scenario: the first result should open a Gwen page
      Given the first match can be located by css selector ".r > a"
       When I click the first match
       Then the current URL should contain "gwen-interpreter"
```
..with [no Page Objects](https://gweninterpreter.wordpress.com/2016/03/08/nopageobjects-theres-no-long-way-to-go-were-already-there/) or Selenium coding required!

Runtime Requirements
--------------------

- Java SE 8 Runtime Environment
- A web browser
- Native web driver
  - [Safari](https://webkit.org/blog/6900/webdriver-support-in-safari-10/)
  - [Chrome](https://sites.google.com/a/chromium.org/chromedriver/)
  - [IE](https://github.com/SeleniumHQ/selenium/wiki/InternetExplorerDriver)
  - [Firefox](https://github.com/mozilla/geckodriver/releases)

Key Features
------------

- Automation driven by plain text [Gherkin](https://docs.cucumber.io/gherkin/reference/) specifications
  - See [Gwen-Web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html)
- Features can execute in [batch mode](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#batch-execution) or [interactively](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#interactive-repl-execution)
- Features can execute [sequentially](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#serial-execution) or in [parallel](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#parallel-execution)
- [Runtime settings](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings)
- Execution can be [data driven](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#csv-data-feeds) (using csv data feeds)
- [REPL console](https://github.com/gwen-interpreter/gwen/wiki/REPL-Console) allows verifying before running
- Cross browser support
- [Remote web driver](https://gweninterpreter.wordpress.com/2015/04/23/remote-webdriver-feature-now-available-in-gwen-web/) support
- [Screenshot capture and slideshow](https://github.com/gwen-interpreter/gwen-web/wiki/Screenshot-Capture-and-Slideshows) playback
- [Interchangeable Selenium](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#changing-the-selenium-version) implementation
- [Locator Chaining](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Chaining)
- [Headless Browser Execution](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#gwenwebbrowserheadless)
- [Gwen Workspaces](https://gweninterpreter.wordpress.com/2017/12/18/gwen-workspaces/) for easy and consistent Gwen installation, configuration and execution on any workstation or build server.
- [Template matching](https://github.com/gwen-interpreter/gwen/wiki/Template-Matching)
- [Drag and Drop](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL#i-drag-and-drop-sourceelement-to-targetelement)
- [Locator level timeouts](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Level-Timeouts)
- [Implicit JavaScript Locators](https://github.com/gwen-interpreter/gwen-web/wiki/Implicit-JavaScript-Locators)
- [Running Gwen on BrowserStack](https://gweninterpreter.wordpress.com/2018/06/16/running-gwen-on-browserstack/)
- [Synchronized StepDef execution](https://github.com/gwen-interpreter/gwen/wiki/Synchronized-StepDefs)
- [Indexed locators](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL#element-can-be-located-by-idnametag-namecss-selectorxpathclass-namelink-textpartial-link-textjavascript-expression-at-index-index)
- Hard, soft, and sustained [assertion modes](https://github.com/gwen-interpreter/gwen/wiki/Assertion-Modes)
- [Visual Testing with AppliTools](https://github.com/gwen-interpreter/gwen-web/wiki/Visual-Testing)
- [Mobile Emulation](https://github.com/gwen-interpreter/gwen-web/wiki/Mobile-Emulation)
- Integrated [WebDriverManager](https://github.com/bonigarcia/webdrivermanager)

License
-------

Copyright 2014-2019 Brady Wood, Branko Juric and [Gwen contributors](#code-contributors).

This software is open sourced under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

See also: [LICENSE](LICENSE).

This project has dependencies on [gwen](https://github.com/gwen-interpreter/gwen) and other open source projects. All
distributed third party dependencies and their licenses are listed in the [LICENSE-THIRDPARTY](LICENSE-THIRDPARTY) file.

Open sourced 28 June 2014 03:27 pm AEST

Contributions
-------------

New capabilities, improvements, and fixes are all valid candidates for contribution. Submissions can be made using
pull requests. Each submission is reviewed and verified by the project [maintainers](#maintainers) before being
integrated and released to the community. We ask that all code submissions include unit tests or sample test features
providing relevant coverage.

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

The following [contributors](https://github.com/gwen-interpreter/gwen/graphs/contributors) submitted pull requests
that have been merged:

- [Jacob Juric](https://github.com/TheReturningVoid)
- [Alexandru Cuciureanu](https://github.com/acuciureanu)

The following users raised issues or requests that have been addressed:

- [Rebecca Abriam](https://github.com/mairbar)
- [George Tsihitas](https://github.com/gtsihitas)
- [Zoltan Penzeli](https://github.com/siaynoq)
- [inkbleed](https://github.com/inkbleed)
- [Pradeep Thawani](https://github.com/pradeep-thawani)
- [anshu781126](https://github.com/anshu781126)
- [ketu4u2010](https://github.com/ketu4u2010)
- [Rahul9844](https://github.com/Rahul9844)
- [rkevin99](https://github.com/rkevin99)

Credits
-------
- [Selenium](https://www.seleniumhq.org/)
- [Cucumber/Gherkin](https://docs.cucumber.io/gherkin/reference/)
- [WebDriverManager](https://github.com/bonigarcia/webdrivermanager)

Known Users
-----------
<a href="https://www.matrak.com.au" target="_blank"><img src="https://gwen-interpreter.github.io/assets/img/users/matrak-logo.png" height="40"/></a> &nbsp; &nbsp; &nbsp; <a href="https://www.smartstream-stp.com/" target="_blank"><img src="https://gwen-interpreter.github.io/assets/img/users/smartstream-logo.png" height="40"/></a> &nbsp; &nbsp; &nbsp; <a href="https://crystaldelta.com/" target="_blank"><img src="https://gwen-interpreter.github.io/assets/img/users/crystaldelta-logo.png" height="70"/></a>

Integrates With
---------------
<a href="https://applitools.com/" target="_blank"><img src="https://gwen-interpreter.github.io/assets/img/integration/applitools-logo.png" height="40"/></a>

---

