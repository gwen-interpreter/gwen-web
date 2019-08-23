[![Gwen-web](https://github.com/gwen-interpreter/gwen/wiki/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/wiki/The-Gwen-Logo)

Gwen Web Automation
===================

A [Gwen](https://github.com/gwen-interpreter/gwen) interpreter that enables teams to quickly 
automate front end web tests and repetitive online processes with 
[Gherkin](https://docs.cucumber.io/gherkin/reference/) feature specifications. A 
[web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL) interacts with 
[Selenium](http://www.seleniumhq.org/projects/webdriver) under the covers for you so you don't have to do any of that programming or technical development work. All the standard Selenium 
locators are supported and you can additionally inject JavaScript in places where dynamically 
finding elements or running functions on web pages may be necessary.

> [Get Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)
  | [DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL)
  | [Settings](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings)
  | [Blog](https://gweninterpreter.wordpress.com)
  | [Wiki](https://github.com/gwen-interpreter/gwen-web/wiki)
  | [FAQ](https://github.com/gwen-interpreter/gwen-web/wiki/FAQ)
  | [License](https://github.com/gwen-interpreter/gwen-web/blob/master/README.md#license)
  | [Support](https://www.gwenify.com/)
  | [Twitter](https://twitter.com/gweninterpreter)

## Current Status

[![Build Status](https://travis-ci.org/gwen-interpreter/gwen-web.svg?branch=master)](https://travis-ci.org/gwen-interpreter/gwen-web) ![GitHub All Releases](https://img.shields.io/github/downloads/gwen-interpreter/gwen-web/total)

- [Latest release](https://github.com/gwen-interpreter/gwen-web/releases/latest)
- [Change log](CHANGELOG)

### What's New?
- Microsoft Edge browser support
- Integrated [WebDriverManager](https://github.com/bonigarcia/webdrivermanager) now manages all native webdrivers for you
- [Mobile Emulation](https://github.com/gwen-interpreter/gwen-web/wiki/Mobile-Emulation)

Why Gwen?
---------
So you can drive web automation with declarative [Gherkin features](https://docs.cucumber.io/gherkin/reference/) like this ..
```gherkin
 Feature: Google search

Scenario: Perform a google search

    Given I have Google in my browser
     When I do a search for "Gwen automation"
      And I'm feeling lucky
     Then I should land on a Gwen page
```

.. by defining separate and imperative [Meta features](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features) like this ..
```gherkin

 # Gwen meta features are defined in Gherkin too. They define locators 
 # and step definitions that are matched against steps in features to
 # find elements and perform browser operations. They are loaded into 
 # memory before feature execution commences.

 Feature: Google search meta

        # @StepDef annotated Scenarios are step definitions in Gwen. The
        # name of the Scenario becomes the name of the step def. Steps
        # in features that match this name are executed by Gwen by 
        # evaluating the sequence of steps defined in that step def.

@StepDef
Scenario: I have Google in my browser

          Gwen will match the name of this step def to the first step
          in the feature above and evaluate the steps that follow. These
          steps are matched against predefined steps in the Gwen Web DSL
          to perform browser operations for you at runtime.

    Given I start a new browser
     When I navigate to "http://www.google.com"
     Then the page title should be "Google"

@StepDef
Scenario: I do a search for "<query>"

          Gwen will match the name of this step def to the seconond
          step in the feature above and assign the <query> parameter to the
          value provided at the matching location. Gwen will then evaluate the
          steps below and resolve any $<query> references to that value.

    Given the search field can be located by name "q"
     When I enter "$<query>" in the search field
     Then the page title should be "$<query> - Google Search"

@StepDef
Scenario: I'm feeling lucky

          Gwen will match the name of this step def to the third
          step in the feature above and evaluate the steps that follow.

    Given the first match can be located by css selector ".r > a"
     When I click the first match
     Then the page title should not contain "Google"

@StepDef
Scenario: I should land on a Gwen page

          Gwen will match the name of this step def to the last
          step in the feature above and evaluate the step that follows.

     Then the current URL should match regex ".+[G|g]wen.*"
```
.. without having to develop any [page objects or framework](https://gweninterpreter.wordpress.com/2016/03/08/nopageobjects-theres-no-long-way-to-go-were-already-there/) code!

Key Features
------------

- Web automation is driven by [Gherkin](https://docs.cucumber.io/gherkin/reference/)
 specifications
  - Declarative features describe behavioral requirements in Gherkin
  - Imperative [Meta features](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features) describe browser interactions in Gherkin
  - Gwen binds the two at runtime to achieve web automation
- A prescribed [Web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL) performs all browser interactions for you
- Automation across browsers and platforms is consistent
- Chrome, Firefox, Safari, Edge and IE are supported
- An interactive [REPL console](https://github.com/gwen-interpreter/gwen/wiki/REPL-Console) provides a step by step execution environment

Get Started
-----------

### System Requirements

- Linux, Mac or Windows OS
- Chrome, Firefox, Safari, Edge or IE web browser
- Java JRE or JDK 8 (version 1.8) or higher
  - You can verify that you have Java installed by opening a command prompt and typing `java -version`. A version will be displayed if you have Java installed.
  - [Install Java](https://www.java.com/en/download/manual.jsp) if you do not see a version displayed 

### Install Gwen and Go!

Ensure that your system meets the above requirements and then perform the following:

1. Download and extract [gwen-workspace.zip](https://github.com/gwen-interpreter/gwen-web/releases/latest/download/gwen-workspace.zip) into to a folder on your computer
2. [Start automating](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)

Learn More
----------

- Features can execute in [batch mode](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#batch-execution) or [interactively](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#interactive-repl-execution)
- Features can execute [sequentially](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#serial-execution) or in [parallel](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#parallel-execution)
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
- Integrated [WebDriverManager](https://github.com/bonigarcia/webdrivermanager)

License
-------

Copyright 2014-2019 Brady Wood, Branko Juric and [Gwen contributors](#code-contributors).

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

The following [contributors](https://github.com/gwen-interpreter/gwen/graphs/contributors) submitted pull requests
that have been merged:

- [Jacob Juric](https://github.com/TheReturningVoid)
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
