Gwen workspace
==============

browsers directory
------------------

This directory contains properties files for all the browsers that Gwen
supports. When Gwen is launched, the properties contained in the associated
<browser>.properties file are loaded into Gwen as system properties.
This enables you to configure system properties for the embedded
WebDriverManager and the native web driver at the browser level.

For example:
- When chrome is the target browser, then all properties in the
  chrome.properties file are loaded into Gwen as system properties.

If no properties file exists in this directory for a given browser, then 
Gwen will automatically create one for that browser for you with default 
values in it and load those values.

You add or change properties for a browser at any time by editing that
browser's <browser>.properties file. Gwen will then pick up your changes
the next time it is launched.
