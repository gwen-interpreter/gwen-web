Getting Started with gwen-web
=============================

Prerequisites
-------------

Please ensure that you have the following installed before proceeding:

 - Java 1.6 or later
 - A web browser (Firefox, Safari, Chrome, or IE)
 
Introducing the Console
-----------------------

The easiest way to get started with Gwen is to launch the console and have a 
play. In this guide we will be using the gwen-web interpreter. Download 
the latest gwen-web distributable and extract it to a location on 
your local drive.

### Multiple browser support

Gwen-web uses Firefox as the default browser. If you would like to use a 
different browser, then create a gwen.properties file in your user home 
directory and set the following property to one of the values shown: 

    gwen.web.browser = Firefox | Safari | Chrome | IE

- If you set this property to Chrome or IE, then you will need to download the 
  [Chrome web driver](http://code.google.com/p/selenium/wiki/ChromeDriver) 
  or 
  [IE web driver](https://code.google.com/p/selenium/wiki/InternetExplorerDriver) 
  respectively and install it on your system path.

### Gwen properties

You can also store this property in any properties file you wish and pass it 
as a parameter to Gwen using the `-p` option (but you'll have to do it every 
time you invoke Gwen). Putting it in the gwen.properties file in your user 
home directory is much more convenient since Gwen knows where to find it. 

### Launching gwen-web

Open a command prompt to the bin directory where you extracted gwen-web and 
type `gwen-web` (or `./gwen-web` on unix systems) and hit enter. This will 
launch the console REPL which will prompt you to start entering steps.

```
   __ ___      _____ _ __     _    
  / _` \ \ /\ / / _ \ '_ \   { \," 
 | (_| |\ V  V /  __/ | | | {_`/   
  \__, | \_/\_/ \___|_| |_|   `    
  |___/                            

Welcome to gwen-web! v0.1.0-SNAPSHOT

INFO - Initialising environment context
INFO - Chrome web driver configured
INFO - WebEnvContext initialised

REPL Console

Enter steps to evaluate or type exit to quit..

gwen>_
```

Now enter the following at the gwen prompt (hit enter after typing):

    Given I navigate to "http://google.com"

This will launch google in your browser.

### Web element bindings

Now we will tell Gwen how to locate the search field. If you inspect the HTML 
of the google page you will find that the search field has a name attribute 
with value "q". Now enter the following:

    Then the search field can be located by name "q"

Similarly the DIV that will contain the search results has an id attribute 
value "ires". Now enter the following:

    And the search results can be located by id "ires"

What we have done above is associated the name 'the search field' to the 
search HTML input element on the google page and the name 'the search results' 
to the HTML DIV element that will contain the search results.

### Double quotes for literals

Notice too that we did not surround 'the search field' or 'the search results' 
with double quotes. That is because these are web element bindings and not 
string literals. Double quotes are only required for string literals.

### Environment context dump

The above information is stored in the environment context of the interpreter. 
If you type `env` at the gwen prompt you will see it in the console. You can 
do this any time. Try it now. You should see the following output showing you 
what is currently in memory: 

```
{
  "env" : {
    "data" : [ {
      "page" : [ {
        "scope" : "global",
        "atts" : [ ]
      }, {
        "scope" : "http://google.com",
        "atts" : [ {
          "the search field/locator" : "name"
        }, {
          "the search field/locator/name" : "q"
        }, {
          "the search results/locator" : "id"
        }, {
          "the search results/locator/id" : "ires"
        } ]
      } ]
    } ]
  }
}
```

### Evaluating steps

Now let's perform a search. Enter the following steps, one after the other, 
and observe the browser.

    When I enter "gwen-interpreter" in the search field
     And I submit the search field
    Then the search results should be displayed
     And the page title should be "gwen-interpreter - Google Search"

Type exit when done to close the console.

Congratulations! You have just used gwen-web to perform a google search 
without doing any programming :)

Submitting Features
-------------------

The above steps can also be saved into a Gherkin feature file and submitted 
directly to Gwen for instant evaluation. Lets do that now. Save the 
following into a text file called _Google.feature_ in the current directory 
(the bin directory of your unpacked distribution).

```
Feature: Google search
    
Scenario: Submit a simple google search
    Given I navigate to "http://google.com"
      And the search field can be located by name "q"
      And the search results can be located by id "ires"
     When I enter "gwen-interpreter" in the search field
      And I submit the search field
     Then the search results should be displayed
      And the page title should be "gwen-interpreter - Google Search"
```

Now make sure you have exited the previous console session and then enter the 
following at the command prompt:

    gwen-web Google.feature

This will launch gwen-web, execute the feature file, and then prompt you to enter 
steps. Again, you can type `exit` to quit. 

### Batch mode

Alternatively, you can invoke Gwen in batch mode by passing in the `-b` 
parameter switch. This switch tells Gwen to exit and close the browser after 
executing the feature. Try it out now.

    gwen-web -b Google.feature

You will notice that the console and browser will exit as soon as the feature 
execution completes. Don't blink or you'll miss it! This mode is typically 
used to run Gwen on a server. We will continue running in the interactive REPL 
mode for the remainder of this guide. You will need to remember to exit the 
console between launches.

Submitting Features with Meta
-----------------------------

### Meta features

If you look closely at the feature file we just wrote, you will see that it 
contains some configuration. In particular, steps 2 and 3 are used to 
dynamically configure web element bindings in the interpreter. This is where 
meta features are useful. Create a new file in the same directory called 
_Google.meta_ and save the following feature text:

```
 Feature: Google meta
     
Scenario: Configure google home page
     When I am on the "http://google.com" page
     Then the search field can be located by name "q"
      And the search results can be located by id "ires" 
```

Now delete steps 2 and 3 from the _Google.feature_ file that you ran 
earlier so that the scenario now looks like this:

```
Scenario: Submit a simple google search
    Given I navigate to "http://google.com"
     When I enter "gwen-interpreter" in the search field
      And I submit the search field
     Then the search results should be displayed
      And the page title should be "gwen-interpreter - Google Search"
```

### Loading meta

Now execute the feature again:

    gwen-web Google.feature

This time Gwen will load the meta into the interpreter first and then execute 
the feature. It knows how to find the meta since it is in the same directory. 

### Configuration by meta

The feature file is starting to look more cleaner now with the configuration 
steps removed. But lets now pull the google url out of the feature file and 
into the meta, since a hard coded URL is also considered configuration. 
Replace the first step in the meta scenario we created earlier in the 
_Google.meta_ file with these two steps:

    When I am on the google home page
    Then the url will be "http://google.com"

Then change the 'Then' of the step that follows to 'And' for fluency. Your 
_Google.meta_ file should look like this:

```
 Feature: Google meta
     
Scenario: Configure google home page
     When I am on the google home page
     Then the url will be "http://google.com"
      And the search field can be located by name "q"
      And the search results can be located by id "ires"
```

Now replace the 1st step in your _Google.feature_ file with this one:

    Given I navigate to the google home page

Your _Google.feature_ should look like this:

```
 Feature: Google search
    
Scenario: Submit a simple google search
    Given I navigate to the google home page
     When I enter "gwen-interpreter" in the search field
      And I submit the search field
     Then the search results should be displayed
      And the page title should be "gwen-interpreter - Google Search"
```

Now execute the feature again (don't forget to exit if you still have the 
previous session open):

    gwen-web Google.feature

Reporting
---------

Gwen can also generate evaluation reports if you pass in a `-r` option with a 
target directory. To see the report, execute the feature again, but this time 
in batch mode and with the report option:

    gwen-web -b -r report Google.feature
    
The generated report will be saved in a `feature-summary.html` file in the 
_report_ directory (relative to the current directory): 

Composing Steps
---------------

### Step definitions

The feature file is simpler and reads more like a behavioral specification 
now. But lets simplify it some more by combining two steps into one using a 
step definition. Append the following to the _Google.meta_ file:

```
@StepDef
Scenario: I search for "gwen-interpreter"
    Given I enter "gwen-interpreter" in the search field
      And I submit the search field
```

Your _Google.meta_ file should now look like this:
```
 Feature: Google meta
     
Scenario: Configure google home page
     When I am on the google home page
     Then the url will be "http://google.com"
      And the search field can be located by name "q"
      And the search results can be located by id "ires"
        
@StepDef
Scenario: I search for "gwen-interpreter"
    Given I enter "gwen-interpreter" in the search field
      And I submit the search field
```

Now replace steps 2 and 3 in the _Google.feature_ with:

    When I search for "gwen-interpreter"

Your _Google.feature_ file should now look like this:

```
 Feature: Google search
 
Scenario: Submit a simple google search
    Given I navigate to the google home page
     When I search for "gwen-interpreter"
     Then the search results should be displayed
      And the page title should be "gwen-interpreter - Google Search"
```

Execute the feature again:

    gwen-web Google.feature

### Reducing Steps

We've reduced the feature scenario down to 4 steps. We could stop here, but 
lets instead reduce it right down to just a single step. Copy the entire 
Scenario to the Google.meta file and annotate it with the @StepDef tag and 
change the name to 'I do a google search for "gwen-interpreter"'. Your 
_Google.meta_ file should now look like this:
```
 Feature: Google meta
     
Scenario: Configure google home page
     When I am on the google home page
     Then the url will be "http://google.com"
      And the search field can be located by name "q"
      And the search results can be located by id "ires"
        
@StepDef
Scenario: I search for "gwen-interpreter"
    Given I enter "gwen-interpreter" in the search field
      And I submit the search field
      
@StepDef
Scenario: I do a google search for "gwen-interpreter"
    Given I navigate to the google home page
     When I search for "gwen-interpreter"
     Then the search results should be displayed
      And the page title should be "gwen-interpreter - Google Search"

Now start the console with the meta only:

    gwen-web -m Google.meta

When prompted, enter:

    Given I do a google search for "gwen-interpreter"

Gwen will now open the browser, navigate to the google home page, enter 
"gwen-interpreter" in the search field, submit the search, verify that the 
results are displayed, and check the title.

### Dynamic data binding

The above performs a google search for "gwen-interpreter" only. As a final 
exercise, we will compose another step that will allow us to google anything. 
To do this, add the following step definition to the _Google.meta_ file:

```
@StepDef 
Scenario: google it
    Given I navigate to the google home page
     When I enter my search term attribute in the search field
      And I submit the search field
     Then the search results should be displayed
      And the page title should contain my search term attribute
```

Note that steps 2 and 5 in this step definition makes reference to an attribute 
binding named 'search term'. This is a named attribute, and it can be 
anything you like. We settled on 'search term' because it conveys meaning and 
reads well in this example. Note too that this name is not statically bound to 
any value. This is because the intention is to bind it dynamically. So let's do 
that now. Load up the meta in the console:

    gwen-web -m Google.meta

Now enter the following steps to perform a google search for "scala":

    Given my search term attribute is "scala"
     Then google it

Now we can do a google search for anything we like using these two steps. Try 
it again with a different search term. There's no need to exit the console and 
start over unless you really want to. Then type `env` at the prompt if you're 
interested in seeing how this is managed in memory.