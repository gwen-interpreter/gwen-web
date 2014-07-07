Getting Started with gwen-web
=============================

Prerequisites
-------------

> Please ensure that you have [installed](../README.md#user-content-building-and-installing-from-source) 
> gwen-web before proceeding.
 
Introducing the Console
-----------------------

The easiest way to get started with gwen-wb is to launch the console and have 
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
