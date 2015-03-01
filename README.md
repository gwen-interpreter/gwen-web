[![Gwen-web](https://github.com/gwen-interpreter/gwen/blob/master/doc/img/gwen-attractor.png)](https://github.com/gwen-interpreter/gwen/blob/master/doc/LOGO.md)

gwen-web
========

Gwen-web is a web automation engine that runs inside the 
[gwen](https://github.com/gwen-interpreter/gwen) interpreter. 
It allows teams to automate front end web tests by writing 
acceptance specifications instead of code. It aims to bring 
an interactive capability to automation testing by providing 
users with an ability to verify commands and debug tests before 
committing them to an automation cycle.  
 
Once the tests have been confirmed, gwen-web is able to run the tests 
sequentially or in parallel.  It can also target different types
of browsers including Firefox, Chrome, IE and Safari.

Reports showing the status of each test execution (passed and failed), 
the time taken to execute each step, screenshots 
on failures, current environment variables and meta information can 
also be generated.

User Guides
-----------

- [Installation](#installation) 
- [Getting Started](doc/START.md)
- [Cheat Sheet](doc/CHEATSHEET.md)
- Blogs and Articles
  - [Page Objects Begone](http://warpedjavaguy.wordpress.com/2014/08/27/page-objects-begone/) - 
    See how gwen-web does away with page objects and why coding them is no longer 
    necessary.
  - [Automation By Meta](http://warpedjavaguy.wordpress.com/2015/01/12/automation-by-meta/) - 
    How to write a feature file and make it executable.

Key Features
------------
- Tests are plain text acceptance specifications
- Tests can be run in batch mode or interactively
- Tests can be run sequentially or in parallel
- Interactive console allows verifying before running
- Cross browser support
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

1. Download one of the following zips:
   - [latest version 1.0.0 snapshot](https://oss.sonatype.org/content/repositories/snapshots/org/gweninterpreter/gwen-web_2.11/1.0.0-SNAPSHOT/gwen-web_2.11-1.0.0-SNAPSHOT.zip)
   - version 1.0.0 release (coming soon) 
2. Extract the zip to a desired location on your local drive:
   - A folder named _gwen-web-[version]_ will be created
3. [Set your target browser ](#set-target-browser)

### Building and Installing from Source

Perform the following if you would like to build and install gwen-web from source: 

1. Download and install [Java SDK 1.6 or later](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 
2. Download and install the latest [sbt](http://www.scala-sbt.org/) version
3. Download a [Git client](http://git-scm.com/downloads).
4. Clone the gwen source repository using one of the following (SSH or HTTPS) URLs: 
     - `git@github.com:gwen-interpreter/gwen.git`
     - `https://github.com/gwen-interpreter/gwen.git`
4. Change to the directory where you cloned the gwen source and run 
   `sbt publish-local` to build the gwen binary and publish it to 
   your local repository
5. Clone the gwen-web source repository using one of the following (SSH or HTTPS) URLs: 
     - `git@github.com:gwen-interpreter/gwen-web.git`
     - `https://github.com/gwen-interpreter/gwen-web.git`
6. Change to the directory where you cloned the gwen-web source and run 
   `sbt universal:package-bin` to build the distributable ZIP
   - This will create a _gwen-web-[version].zip_ file in the 
     _target/universal_ folder relative to your current directory
7. Extract the generated ZIP to a desired location on your local drive
   - This will create a project folder named _gwen-web-[version]_ in that 
     location
8. [Set your target browser ](#set-target-browser)

### Set Target Browser

Gwen-web uses Firefox as the default browser. If you would like to use a 
different browser, then create a _gwen.properties_ file in your user home 
directory and set the following property to one of the values shown: 

    gwen.web.browser = firefox | safari | chrome | ie

- If you use Chrome or IE, then you will need to download the 
  [chrome web driver](http://chromedriver.storage.googleapis.com/index.html) 
  or [IE web driver server](http://selenium-release.storage.googleapis.com/index.html) 
  respectively and install it on your system path. If you do not have permission to 
  install the driver on your system path, then you can set the path to your 
  downloaded driver location in your _gwen.properties_ file as shown below:
  
```
    (chrome)  webdriver.chrome.driver = /path/to/chromedriver
    (ie)      webdriver.ie.driver = c:/path/to/IEDriverServer.exe
```

- If you want to use Safari, then you will need to install the latest  
  [safari driver extension](http://selenium-release.storage.googleapis.com/index.html) 
  in your browser.

You can also store settings in any properties file you wish and pass that 
file to Gwen using the `-p` option (but you'll have to do it every 
time you invoke Gwen). Putting the _gwen.properties_ file in your user 
home directory is much more convenient since Gwen knows where to find it. 

You are now ready to [start using gwen-web](doc/START.md)

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
