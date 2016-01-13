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

Why gwen-web?
-------------

With gwen-web you can instantly automate web pages by writing plain text 
features like this:
```gherkin
   Feature: Google search
   
  @StepDef
  Scenario: I search for "<query>"
            
            This is a custom step definition that accepts a query string as 
            input and performs a google search on it and checks for a returned 
            match. It is not immediately executed but rather loaded into 
            memory and executed only when referenced in another step by name.
             
      Given I navigate to "http://www.google.com"
        And the search field can be located by name "q"
       When I enter "$<query>" in the search field
       Then the page title should start with "$<query>"
        And the first result can be located by class name "r"
        And the first result should contain "$<query>"
        
  Scenario: Perform a google search
            
            This scenario calls the above step definition to perform a google 
            search for "gwen-web". It then clicks the first link in the 
            returned results and checks the URL of the page that is loaded.
            
      Given I search for "gwen-web"
       When I click the first result
       Then the current URL should be "https://github.com/gwen-interpreter/gwen-web"
```
..instead of developing code like this:
```java
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

public class GoogleSearch  {
    /** 
     * Performs a google search and returns the first element in the results.
     * @param query the query string to search
     * @param driver the web driver
     */
    private static WebElement searchFor(String query, WebDriver driver) {
        driver.get("http://www.google.com");
        WebElement searchField = driver.findElement(By.name("q"));
        searchField.sendKeys(query);
        searchField.submit();
        new WebDriverWait(driver, 10).until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver d) {
                return d.getTitle().startsWith(query);
            }
        });
        WebElement firstResult = driver.findElement(By.className("r"));
        if(!firstResult.getText().contains(query)) {
            throw new AssertionError("Unexpected result");
        }
        return firstResult;
    }
    /** 
     * Main method that creates a new web driver instance and calls the 
     * above method to perform a google search for "gwen-web" before clicking 
     * the first link in the results and checking the URL of the loaded page.
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        WebDriver driver = new FirefoxDriver();
        try {
            WebElement firstResult = searchFor("gwen-web", driver);
            firstResult.click();
            String url = driver.getCurrentUrl(); 
            if (!url.equals("https://github.com/gwen-interpreter/gwen-web")) {
                throw new AssertionError("Unexepcted URL");
            }
        } finally {
            driver.quit();
        }
    }
}
```

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
- [Interchangeable Selenium](doc/CHEATSHEET.md#changing-the-selenium-version) implementation
- See also: [CHANGELOG](CHANGELOG)

Core Requirements
-----------------

- Java JRE 7+
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
    necessary (using a bottom-up approach)
  - [Automation By Meta](http://warpedjavaguy.wordpress.com/2015/01/12/automation-by-meta/) - 
    How to write a feature file and make it executable (using a top-down approach)
  - [gwen-web and Remote Webdriver](https://quietachievingtester.wordpress.com/2015/04/23/remote-webdriver-feature-now-available-in-gwen-web/) - 
    How to enable remote webdriver in gwen-web
- [FAQ](doc/FAQ.md)

Installation
------------

### Installing a Binary Release

Perform the following to download and install the latest binary build:

1. Download and install [Java (JRE) 7+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
2. Download one of the following zips:
   - [latest version 1.0.0 snapshot](https://oss.sonatype.org/content/repositories/snapshots/org/gweninterpreter/gwen-web/1.0.0-SNAPSHOT/gwen-web-1.0.0-SNAPSHOT.zip)
   - version 1.0.0 release (coming soon) 
3. Extract the zip to a desired location on your local drive:
   - A folder named _gwen-web-[version]_ will be created
4. [Install native web driver](doc/CHEATSHEET.md#native-web-drivers) for Chrome or IE, 
   or a browser extension for Safari
   - Skip this step if you are using firefox (no native driver required)
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
is reviewed and verified by the project committers before being integrated 
and released to the community. We ask that all code submissions include unit 
tests or sample test features providing relevant coverage.

By sending a pull request, we assume that you agree to release your work under 
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
