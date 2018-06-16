[![Gwen-web](https://github.com/gwen-interpreter/gwen/wiki/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/wiki/The-Gwen-Logo)

Gwen-web
========

A [Gwen](https://github.com/gwen-interpreter/gwen) interpreter that enables teams to quickly automate front end web tests and robotic online processes with
[Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin) feature specifications.
A [web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL) interacts with
[Selenium](http://www.seleniumhq.org/projects/webdriver) under the covers for you so you don't have to do
any of that programming or technical development work. It supports all the standard web element locators provided by
Selenium and additionally allows you to inject JavaScript in places where dynamically locating web elements or running
functions on pages may be necessary.

- [Join our user community](#user-community)

### Current Status

[![Build Status](https://travis-ci.org/gwen-interpreter/gwen-web.svg?branch=master)](https://travis-ci.org/gwen-interpreter/gwen-web)

- [Latest release](https://github.com/gwen-interpreter/gwen-web/releases/latest)
- [Change log](CHANGELOG)

### What's New?
- [Running Gwen on BrowserStack](https://gweninterpreter.wordpress.com/2018/06/16/running-gwen-on-browserstack/)
    ![browserstack-logo](https://user-images.githubusercontent.com/1369994/41496098-74652d14-717a-11e8-894e-2150298e62b8.png)
- [Thread local settings](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL#my-name-propertysetting-iswill-be-value)
- [Implicit JavaScript locators](https://github.com/gwen-interpreter/gwen-web/wiki/Implicit-JavaScript-Locators)
- [Locator level timeouts](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Level-Timeouts)

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
       Then the current URL should start with "https://github.com/gwen-interpreter/gwen"
```
..with [no Page Objects](https://gweninterpreter.wordpress.com/2016/03/08/nopageobjects-theres-no-long-way-to-go-were-already-there/) or Selenium coding required!

Links
-----

- [Blog](https://gweninterpreter.wordpress.com)
- [Wiki](https://github.com/gwen-interpreter/gwen-web/wiki)
- [Gwen-Web FAQ](https://github.com/gwen-interpreter/gwen-web/wiki/FAQ)
- [Gwen FAQ](https://github.com/gwen-interpreter/gwen/wiki/FAQ)

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

- Automation driven by plain text [Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin) specifications
  - See [Gwen-Web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html)
- Features can execute in [batch mode](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#batch-execution) or [interactively](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#interactive-repl-execution)
- Features can execute [sequentially](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#serial-execution) or in [parallel](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#parallel-execution)
- Execution can be [data driven](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#csv-data-feeds) (using csv data feeds)
- [REPL console](https://github.com/gwen-interpreter/gwen/wiki/REPL-Console) allows verifying before running
- Cross browser support
- Remote web driver support
- Screenshot capture and slideshow playback
- [Interchangeable Selenium](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#changing-the-selenium-version) implementation
- [Locator Chaining](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Chaining)
- [Headless Browser Execution](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#gwenwebbrowserheadless)
- [Gwen Workspaces](https://gweninterpreter.wordpress.com/2017/12/18/gwen-workspaces/) for easy and consistent Gwen installation, configuration and execution on any workstation or build server.
- [Locator level timeouts](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Level-Timeouts)
- [Implicit JavaScript Locators](https://github.com/gwen-interpreter/gwen-web/wiki/Implicit-JavaScript-Locators)

Quick Links to Wiki Information
-------------------------------
- [Installation](https://github.com/gwen-interpreter/gwen-web/wiki/Installation)
- [Getting Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)
- [Gwen-Web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html)

User Community
--------------

All announcements and discussions are posted and broadcast to all members in the Gwen mail group. Active users who
[join the group](https://groups.google.com/d/forum/gwen-interpreter) will also receive an invitation to our Gwen Slack
community where they can interact with other users and help each other out.

License
-------

Copyright 2014-2018 Brady Wood, Branko Juric and [Gwen contributors](#code-contributors).

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

### Code Contributors

The following [contributors](https://github.com/gwen-interpreter/gwen/graphs/contributors) submitted pull requests
that have been merged:

- [Jacob Juric](https://github.com/TheReturningVoid)
- [Alexandru Cuciureanu](https://github.com/acuciureanu)

### Active Users

The following users raised issues or requests that have been addressed:

- [Rebecca Abriam](https://github.com/mairbar)
- [George Tsihitas](https://github.com/gtsihitas)
- [Zoltan Penzeli](https://github.com/siaynoq)
- [inkbleed](https://github.com/inkbleed)
- [Pradeep Thawani](https://github.com/pradeep-thawani)
- [anshu781126](https://github.com/anshu781126)
- [ketu4u2010](https://github.com/ketu4u2010)
- [Rahul9844](https://github.com/Rahul9844)

Credits
-------
- [Selenium](https://www.seleniumhq.org/)
- [Cucumber/Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin)

---

