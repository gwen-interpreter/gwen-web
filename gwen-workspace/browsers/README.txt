Gwen workspace
==============

browsers directory
------------------

This directory contains config files for all the browsers that Gwen
supports. When Gwen is launched, the settings contained in the associated
<browser>.conf file are loaded into Gwen as system properties.
This enables you to configure system properties for the embedded
WebDriverManager and the native web driver at the browser level.

For example:
- When chrome is the target browser, then all settings in the
  chrome.conf file are loaded into Gwen as system properties.

If no config file exists in this directory for a given browser, then 
Gwen will automatically create one for that browser for you with default 
values in it and load those values.

You add or change settings for a browser at any time by editing that
browser's <browser>.conf file. Gwen will then pick up your changes
the next time it is launched.
