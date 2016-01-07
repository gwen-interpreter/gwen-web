Getting Started with gwen-web
=============================

Prerequisites
-------------

> Please ensure that you have [installed](../README.md#installation) 
> gwen-web before proceeding.

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
 
Next, open a command prompt / shell in your gwen installation directory and 
issue the following command if you are using Firefox or Chrome (replace all forward slashes with back slashes if you are on a windows platform):

```
bin/gwen -b features/floodio
```

Otherwise, if you are using Safari or IE, then you will need to issue the 
following command instead to skip over the robot steps since there is no 
programmatic support for setting the user agent header in those drivers.

```
bin/gwen -b -t ~@Robot features/floodio
```

This will start and complete the flood io challenge.

Next Steps
==========

Read the [gwen-web user guides](../README.md#user-guides).

