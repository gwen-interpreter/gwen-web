[![Gwen-web](https://github.com/gwen-interpreter/gwen/wiki/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/wiki/The-Gwen-Logo)

Gwen-web
========

Gwen-Web is a web automation engine that runs inside the [Gwen](https://github.com/gwen-interpreter/gwen) interpreter.
It enables teams to automate front end web tests and robotic web activities with 
[Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin) feature specs.
A [web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html) interacts with [Selenium WebDriver](http://www.seleniumhq.org/projects/webdriver) under the covers for you so you don't have to do all the programming work. It does however, allow you to also interpolate strings and inject JavaScript for flexibility and tailoring where necessary.

### Current Status

[![Build Status](https://travis-ci.org/gwen-interpreter/gwen-web.svg?branch=master)](https://travis-ci.org/gwen-interpreter/gwen-web)

- [Latest release](https://github.com/gwen-interpreter/gwen-web/releases/latest)
- [Change log](CHANGELOG)

Why Gwen-web?
-------------
So you can do web automation with Gherkin features like this..
```gherkin
   Feature: Google search

  Scenario: Perform a google search
      Given I have Google in my browser
       When I do a search for "Gwen automation"
       Then the first result should open a Gwen page
```

..by writing [Meta features](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features) like this..
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
..and let [Page Objects Begone](https://gweninterpreter.wordpress.com/2014/08/27/page-objects-begone/)!

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
  - [Edge](https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/)
  - [Firefox](https://github.com/mozilla/geckodriver/releases)

Key Features
------------

* Automation driven by plain text [Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin) specifications
  * See [Gwen-Web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html)
* Features can execute in [batch mode](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#batch-execution) or [interactively](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#interactive-repl-execution)
* Features can execute [sequentially](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#serial-execution) or in [parallel](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#parallel-execution)
* Execution can be [data driven](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#csv-data-feeds) (using csv data feeds)
* [REPL console](https://github.com/gwen-interpreter/gwen/wiki/REPL-Console) allows verifying before running
* Cross browser support
* Remote web driver support
* Screenshot capture and slideshow playback
* [Interchangeable Selenium](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#changing-the-selenium-version) implementation
* [Locator Chaining](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Chaining)

Quick Links to Wiki Information
-------------------------------
- [Installation](https://github.com/gwen-interpreter/gwen-web/wiki/Installation)
- [Getting Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)
- [Gwen-Web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html)

Mail Group
----------

All announcements and discussions are posted and broadcast to all members in
the following mail group. You are welcome to visit and subscribe to receive
notifications or get involved.

- [Our mail group](https://groups.google.com/d/forum/gwen-interpreter)

Credits
-------
- Selenium
- Cucumber/Gherkin

Contributions
-------------

New capabilities, improvements, and fixes are all valid candidates for
contribution. Submissions can be made using pull requests. Each submission
is reviewed and verified by the project committers before being integrated
and released to the community. We ask that all code submissions include unit
tests or sample test features providing relevant coverage.

By submitting contributions, you agree to release your work under the
license that covers this software.

How to contribute:
1. Fork this repository
2. Create a branch on your forked repository
3. Commit your changes to your branch
4. Push your branch to your forked repository
5. Create a pull request from your branch to here

License
-------

Copyright 2014-2017 Brady Wood, Branko Juric

This software is open sourced under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

See also: [LICENSE](LICENSE).

This project has dependencies on [gwen](https://github.com/gwen-interpreter/gwen)
and other open source projects. All distributed third party depdendencies and
their licenses are listed in the [LICENSE-THIRDPARTY](LICENSE-THIRDPARTY)
file.

Open sourced 28 June 2014 03:27 pm AEST
