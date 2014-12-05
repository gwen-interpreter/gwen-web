![Gwen-web](https://github.com/gwen-interpreter/gwen/blob/master/doc/img/gwen-attractor.png)

gwen-web
========

Gwen-web is a web automation engine that runs inside the 
[gwen-interpreter](https://github.com/gwen-interpreter/gwen).
It allows teams to automate web browsers using language instead of code.  
It aims to bring an interactive capability to automation testing by 
providing users with an ability to verify commands and debug tests before 
committing them to an automation cycle.  
 
Once the tests have been confirmed, gwen web is able to run the tests 
sequentially or in parallel.  gwen web can be used to target different types
of browsers including Firefox, Chrome, IE and Safari.

Finally gwen web will create a summary report detailing the status of each 
test (passed and failed), the time taken to execute each step, screenshots 
on failures, current environment variables and meta information.

Blogs and Articles
------------------

- [Page Objects Begone](http://warpedjavaguy.wordpress.com/2014/08/27/page-objects-begone/)

Key Features
------------
- Tests are written in plain English
- Tests can be run in batch mode or interactively
- Tests can be run sequentially or in parallel
- Interactive console allows verifying before running
- Cross Browser Support
- No development
 
Core Requirements
-----------------

- Java 1.6 or later
- A web browser (Chrome, Firefox, Safari, or IE)
- A native web driver (required for Chrome and IE only)

Installation
------------

### Installing a Binary Release

Perform the following to download and install the latest build:

1. Download and extract the following zip to a desired location on your drive:
   - [gwen-web_2.11-0.1.0-SNAPSHOT.zip](https://oss.sonatype.org/content/repositories/snapshots/org/gweninterpreter/gwen-web_2.11/0.1.0-SNAPSHOT/gwen-web_2.11-0.1.0-SNAPSHOT.zip)
   - A folder named _gwen-web-[version]_ will be installed 
2. [Start using gwen-web](doc/START.md)

### Building and Installing from Source

Perform the following if you would like to build and install gwen-web from source: 

1. Download and install [Java SDK 1.6 or later](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 
   - Note Java 8 is not recommended at this time of writing
2. Download and install the latest [sbt](http://www.scala-sbt.org/) version
3. Either:
   - Download a [Git client](http://git-scm.com/downloads) and clone this 
     repository using one of the following (SSH or HTTPS) URLs: 
     - `git@github.com:gwen-interpreter/gwen-web.git`
     - `https://github.com/gwen-interpreter/gwen-web.git`
   - Or click the Download ZIP link on this project page to download 
     the source archive and extract it to a local folder  
4. Change to the directory where you cloned/downloaded the source and run 
   `sbt universal:package-bin` to build the distributable ZIP
   - This will create a _gwen-web-[version].zip_ file in the 
     _target/universal_ folder relative to your current directory
5. Extract the generated ZIP to a desired location on your local drive
   - This will create a project folder named _gwen-web-[version]_ in that 
     location
6. [Start using gwen-web](doc/START.md)

Usage
-----

### Getting Started

See our [getting started](doc/START.md) user guide for a quick introduction 
and tutorial.

### Cheat Sheet

Please see our [cheat sheet](doc/CHEATSHEET.md) for a list of commands and sample dsl statements

Mail Group
----------

All announcements and discussions are posted and broadcast to all members of 
the following mail group. You are welcome to visit and subscribe to receive 
notifications or get involved.

- [gwen-interpreter](https://groups.google.com/d/forum/gwen-interpreter) 

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

Copyright 2014 Brady Wood, Branko Juric

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
