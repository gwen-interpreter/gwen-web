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
