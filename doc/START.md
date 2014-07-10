Getting Started with gwen-web
=============================

Prerequisites
-------------

> Please ensure that you have [installed](../README.md#user-content-building-and-installing-from-source) 
> gwen-web before proceeding.
 
Introducing the Console
-----------------------

The easiest way to get started with gwen-web is to launch the console and have 
a play.

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

Open a command prompt to the bin directory where you installed gwen-web and 
type `gwen-web` (or `./gwen-web` on unix systems) and hit enter. This will 
launch the console REPL which will prompt you to start entering steps.

```
   __ ___      _____ _ __     _    
  / _` \ \ /\ / / _ \ '_ \   { \," 
 | (_| |\ V  V /  __/ | | | {_`/   
  \__, | \_/\_/ \___|_| |_|   `    
  |___/                            

Welcome to gwen-web!

INFO - Initialising environment context
INFO - Chrome web driver configured
INFO - WebEnvContext initialised

REPL Console

Enter steps to evaluate or type exit to quit..

gwen>_
```

### Flood IO Challenge
Before walking you through gwen's web engine in detail, we have included a 
feature and a meta file allowing you to see a gwen in action whilst it automates
the [Flood IO script challenge](https://challengers.flood.io/start)

The flood io challenge will put any automation script through a series of 
tasks to determine its capabilities.  From clicking buttons, selecting 
dropdowns, calculating maximum radio option and finally to its ability to 
wait for an ajax call and copy text from one dom element to another.

Before starting this challenge, please take the time to click through the 
challenge manually to familiarize yourself with the task at hand.

We have included the feature and its meta file for you in the zip (both of 
which will be explained later) 
  - [floodio.feature](../src/test/resources/features/floodio/FloodIO.feature)
  - [floodio.meta](../src/test/resources/features/floodio/FloodIO.meta) 

Next issue the following command in your command prompt / shell

```
gwen-web ../features/floodio/FloodIO.feature
```

This has now started the flood io automation.

When you get to the end of this challenge you may find that the You're Done 
page actually mentions that you have just walked through the test manually.  
This step has also been setup to verify the capabilities of the robot.  How 
do we get around it?  Gwen has a setting in the user.home/gwen.properties 
that allows you to control what user-agent you are.  For this test we need 
to specify I AM ROBOT in order for the test to pass as a robot.

```
gwen.web.useragent = I AM ROBOT
```

That is it, you have now run your first automated test using gwen.  

In the next update, I will be walking you through the finer details of the web
engine and how meta can be used to setup context.