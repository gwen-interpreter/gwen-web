[![Gwen-web](https://github.com/gwen-interpreter/gwen/blob/master/doc/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/blob/master/doc/LOGO.md)
Gwen-web
========

Gwen-web is a web automation engine that runs inside the 
[Gwen](https://github.com/gwen-interpreter/gwen) interpreter. 
It allows teams to automate front end web tests by writing 
[Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin) feature 
specifications instead of code. 
A [prescribed DSL](doc/CHEATSHEET.md#supported-dsl) delegates to 
[Selenium WebDriver](http://www.seleniumhq.org/projects/webdriver) 
under the covers for you, but you can also declaratively compose your own 
custom DSL with annotated 
[@StepDef](https://github.com/gwen-interpreter/gwen#composable-steps) 
Scenarios that can accept parameters and call other steps. 
[Meta features](https://github.com/gwen-interpreter/gwen#meta-features) 
can help eliminate redundancies and give you the flexibility to be as 
imperative or as declarative as you like in your approach to writing features.

A [REPL console](https://github.com/gwen-interpreter/gwen#repl-console) 
provides users with the ability to interactively write and debug tests 
before committing them to an automation cycle. Once the tests have been 
written, gwen-web is able to run them sequentially or in parallel.  It 
can also target different types of browsers including Firefox, Chrome, 
IE and Safari. Rich HTML reports showing the status of each test execution, 
including timings, screenshots, slideshows, and captured runtime data 
can also be generated. XML JUnit style reports can be generated too. 

#### Current Status

[![Build Status](https://travis-ci.org/gwen-interpreter/gwen-web.svg?branch=master)](https://travis-ci.org/gwen-interpreter/gwen-web)

- See also [Gwen Wiki](https://github.com/gwen-interpreter/gwen-web/wiki)


Core Requirements
-----------------

- Java JRE 7+
- A web browser (Chrome, Firefox, Safari, or IE)
- A native web driver (required for Chrome and IE only)

Quick Links to Wiki Information
-----------
- [Installation](/gwen-interpreter/gwen-web/wiki/Installation) 
- [Getting Started](/gwen-interpreter/gwen-web/wiki/Getting-Started)
- [Supported DSL](/gwen-interpreter/gwen-web/wiki/Supported-DSL)
- [REPL Console](/gwen-interpreter/gwen-web/wiki/REPL-Console)

Mail Group
----------

All announcements and discussions are posted and broadcast to all members of 
the following mail group. You are welcome to visit and subscribe to receive 
notifications or get involved.

- [gwen-interpreter](https://groups.google.com/d/forum/gwen-interpreter) 

Development Guide
-----------------

See the [Dev Guide](/gwen-interpreter/gwen-web/wiki/Development-Guide) if you would like to work with the code 
or build the project from source.

Contributions
-------------

New capabilities, improvements, and fixes are all valid candidates for 
contribution. Submissions can be made using pull requests. Each submission 
is reviewed and verified by the project committers before being integrated 
and released to the community. We ask that all code submissions include unit 
tests or sample test features providing relevant coverage.

By submitting contributions, we assume that you agree to release your work under 
the license that covers this software.

License
-------

Copyright 2014-2016 Brady Wood, Branko Juric

This software is open sourced under the 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

See also: [LICENSE](LICENSE).

This project has dependencies on [gwen](https://github.com/gwen-interpreter/gwen) 
and other open source projects. All distributed third party depdendencies and 
their licenses are listed in the [LICENSE-Third-Party.txt](LICENSE-Third-Party.txt) 
file.

Open sourced 28 June 2014 03:27 pm AEST

Mentions
--------
- 2013+,  [Selenium WebDriver](http://docs.seleniumhq.org/docs/03_webdriver.jsp) which gwen web uses
  to interact and control the browsers.

(The following mentions are on the gwen (the core interpreter) page and have been included here as they also 
played a part in gwen web)

- 2013-2014, [Mick Grigg](http://au.linkedin.com/in/mickgrigg) for 
  involving us in your side project and supporting us in open sourcing this 
  interpreter which we built as part of that. 
- 2014, [Arun Datta](http://au.linkedin.com/in/arundatta) for reviewing our 
  pre-release documentation and providing valuable feedback.

Thanks again also to Nascent Creative for the awesome logo.

***
