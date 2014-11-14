Getting Started with gwen-web
=============================

Prerequisites
-------------

> Please ensure that you have [installed](../README.md#user-content-building-and-installing-from-source) 
> gwen-web before proceeding.
 
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

You can also store this property in any properties file you wish and pass that 
file to Gwen using the `-p` option (but you'll have to do it every 
time you invoke Gwen). Putting the gwen.properties file in your user 
home directory is much more convenient since Gwen knows where to find it. 

### Flood IO Challenge

Before walking you through gwen's web engine in detail, we have included a 
feature and a meta file allowing you to see Gwen in action whilst it automates
the [Flood IO script challenge](https://challengers.flood.io/start)

The flood io challenge will put any automation script through a series of 
tasks to determine its capabilities.  From clicking buttons, selecting 
dropdowns, calculating maximum radio option and finally to its ability to 
wait for an ajax call and copy text from one dom element to another.

Before starting this challenge, please take the time to click through the 
challenge manually to familiarize yourself with the task at hand.

We have included the feature and its meta file for you in the zip 
  - [floodio.feature](../features/floodio/FloodIO.feature)
  - [floodio.meta](../features/floodio/FloodIO.meta) 


Note that the floodio challenge requires that the user agent header be set to 
"I AM ROBOT". Gwen currently supports automating the setting of this header for 
the Firefox and Chrome browsers only. 
 
Next, open a command prompt / shell in the _bin_ directory and issue the 
following command if you are using Firefox or Chrome:

```
gwen -b ../features/floodio
```

Otherwise, if you are using Safari or IE, then you will need to issue the 
following command instead to skip over the robot steps since there is no 
programmatic support for setting the user agent header in those drivers.

```
gwen -b -t ~@Robot ../features/floodio
```

This will start and complete the flood io challenge.

