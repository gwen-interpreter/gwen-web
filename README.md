[![Gwen-web](https://github.com/gwen-interpreter/gwen/wiki/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/wiki/The-Gwen-Logo)
Gwen-web
========

Gwen-Web is a web automation engine that runs inside the [Gwen](https://github.com/gwen-interpreter/gwen) interpreter. It allows teams to automate front end web tests by writing 
[Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin) feature specifications instead of code.
A [prescribed DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html) delegates to [Selenium WebDriver](http://www.seleniumhq.org/projects/webdriver) under the covers for you and frees you from development concerns. You can also declaratively compose your own custom DSL with annotated 
[@StepDef](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features#compostable-steps) Scenarios that can accept parameters and call other steps. [Meta features](https://github.com/gwen-interpreter/gwen/wiki/Meta-Features) can help eliminate redundancies and give you the flexibility to be as imperative or as declarative as you like in your approach to writing features.

- See also:
  - [Wiki](https://github.com/gwen-interpreter/gwen-web/wiki)
  - [FAQ](https://github.com/gwen-interpreter/gwen-web/wiki/FAQ)
  - [Blog](https://gweninterpreter.wordpress.com)

### Current Status

[![Build Status](https://travis-ci.org/gwen-interpreter/gwen-web.svg?branch=master)](https://travis-ci.org/gwen-interpreter/gwen-web)

- [Latest release](https://github.com/gwen-interpreter/gwen-web/releases/latest)
- [Change log](CHANGELOG)

Key Features
------------

* Tests are plain text [Gherkin](https://github.com/cucumber/cucumber/wiki/Gherkin) specifications
  * See [Gwen-Web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html)
* Tests can be run in [batch mode](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#batch-execution) or [interactively](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#interactive-repl-execution)
* Tests can be run [sequentially](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#serial-execution) or in [parallel](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#parallel-execution)
* Tests can be [data driven](https://github.com/gwen-interpreter/gwen/wiki/Execution-Modes#csv-data-feeds) (using csv data feeds)
* [REPL console](https://github.com/gwen-interpreter/gwen/wiki/REPL-Console) allows verifying before running
* Cross browser support
* Remote web driver support
* Screenshot capture and slideshow playback
* [Interchangeable Selenium](https://github.com/gwen-interpreter/gwen-web/wiki/Runtime-Settings#changing-the-selenium-version) implementation
* [Locator Chaining](https://github.com/gwen-interpreter/gwen-web/wiki/Locator-Chaining)

Core Requirements
-----------------

- Java JRE 7+
- A web browser (Chrome, Firefox, Safari, or IE)
- A native web driver (required for Chrome and IE only)

Quick Links to Wiki Information
-----------
- [Installation](https://github.com/gwen-interpreter/gwen-web/wiki/Installation) 
- [Getting Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)
- [Gwen-Web DSL](http://htmlpreview.github.io/?https://github.com/gwen-interpreter/gwen-web/blob/master/docs/dsl/gwen-web-dsl.html)
- [REPL Console](https://github.com/gwen-interpreter/gwen-web/wiki/REPL-Console)

Mail Group
----------

All announcements and discussions are posted and broadcast to all members in 
the following mail group. You are welcome to visit and subscribe to receive 
notifications or get involved.

- [gwen-interpreter](https://groups.google.com/d/forum/gwen-interpreter) 

Development Guide
-----------------

See the [Dev Guide](https://github.com/gwen-interpreter/gwen-web/wiki/Development-Guide) if you would like to work with the code 
or build the project from source.

Contributions
-------------

New capabilities, improvements, and fixes are all valid candidates for 
contribution. Submissions can be made using pull requests. Each submission 
is reviewed and verified by the project committers before being integrated 
and released to the community. We ask that all code submissions include unit 
tests or sample test features providing relevant coverage.

By submitting contributions, you agree to release your work under the 
license that covers this software.

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
