[![Gwen-web](https://github.com/gwen-interpreter/gwen/blob/master/doc/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/blob/master/doc/LOGO.md)

gwen-web
========

Gwen-web is a web automation engine that runs inside the 
[gwen](https://github.com/gwen-interpreter/gwen) interpreter. 
It allows teams to automate front end web tests by writing 
specifications instead of code. It also provides 
users with the ability to interactively debug tests before 
committing them to an automation cycle.  
 
Once the tests have been written, gwen-web is able to run them 
sequentially or in parallel.  It can also target different types
of browsers including Firefox, Chrome, IE and Safari.

Reports showing the status of each test execution, including timings, 
screenshots, slideshows, and captured runtime data can also be generated.

Key Features
------------
- Tests are plain text specifications
- Tests can be run in batch mode or interactively
- Tests can be run sequentially or in parallel
- Tests can be data driven (using csv data feeds)
- REPL console allows verifying before running
- Cross browser support
- Remote web driver support
- Screenshot capture and slideshow playback
 
Core Requirements
-----------------

- Java 7+
- A web browser (Chrome, Firefox, Safari, or IE)
- A native web driver (required for Chrome and IE only)

User Guides
-----------

- Introduction
  - [Keynote](doc/gwen-web-intro.key?raw=true)
  - [Powerpoint](doc/gwen-web-intro.pptx?raw=true)
- [Installation](#installation) 
- [Getting Started](doc/START.md)
- [Cheat Sheet](doc/CHEATSHEET.md)
- Blogs and Articles
  - [Page Objects Begone](http://warpedjavaguy.wordpress.com/2014/08/27/page-objects-begone/) - 
    See how gwen-web does away with page objects and why coding them is no longer 
    necessary.
  - [Automation By Meta](http://warpedjavaguy.wordpress.com/2015/01/12/automation-by-meta/) - 
    How to write a feature file and make it executable.
  - [gwen-web and Remote Webdriver](https://quietachievingtester.wordpress.com/2015/04/23/remote-webdriver-feature-now-available-in-gwen-web/) - 
    How to enable remote webdriver in gwen-web
- [FAQ](doc/FAQ.md)

Installation
------------

### Installing a Binary Release

Perform the following to download and install the latest binary build:

1. Download and install [Java (JRE) 7+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
2. Download one of the following zips:
   - [latest version 1.0.0 snapshot](https://oss.sonatype.org/content/repositories/snapshots/org/gweninterpreter/gwen-web_2.11/1.0.0-SNAPSHOT/gwen-web_2.11-1.0.0-SNAPSHOT.zip)
   - version 1.0.0 release (coming soon) 
3. Extract the zip to a desired location on your local drive:
   - A folder named _gwen-web-[version]_ will be created
4. [Install native web driver](doc/CHEATSHEET.md#native-web-drivers) for Chrome, IE, or Safari
   - Skip this step if you are using firefox (you're good to go)
5. [Configure settings](doc/CHEATSHEET.md#configuration-settings) (optional)
6. [Start using gwen-web](doc/START.md) 

Mail Group
----------

All announcements and discussions are posted and broadcast to all members of 
the following mail group. You are welcome to visit and subscribe to receive 
notifications or get involved.

- [gwen-interpreter](https://groups.google.com/d/forum/gwen-interpreter) 

Development Guide
-----------------

See the [Dev Guide](doc/DEVGUIDE.md) if you would like to work with the code 
or build the project from source.

Contributions
-------------

New capabilities, improvements, and fixes are all valid candidates for 
contribution. Submissions can be made using pull requests. Each submission 
is reviewed and verified by the project's committer's before being integrated 
and released to the community. We ask that all code submissions include unit 
tests or sample test features providing relevant coverage.

By sending a pull request, we assume that you agree to release your work under 
the license that covers this software.

License
-------

Copyright 2014-2015 Brady Wood, Branko Juric

This software is open sourced under the 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

See also: [LICENSE](LICENSE).

This project has dependencies on other open source projects, all of which are 
listed in the [NOTICE](NOTICE) file.

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
