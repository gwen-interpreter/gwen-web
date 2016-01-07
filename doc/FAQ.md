gwen-web FAQ
============

> See also: [gwen FAQ](https://github.com/gwen-interpreter/gwen/blob/master/doc/FAQ.md)

How do I target a specific web browser?
---------------------------------------
By default, gwen-web will target the Firefox browser (as it requires no native 
web driver to be installed).  To target a different browser, you will need to 
[configure](CHEATSHEET.md#configuration-settings) the `gwen.web.browser` setting 
and install a [native web driver](CHEATSHEET.md#native-web-drivers).

Do I need to develop page objects or compile any code?
------------------------------------------------------
No, gwen-web uses page scopes to emulate page objects and does away with 
development and compilation altogether through a [prescribed DSL](CHEATSHEET.md#supported-dsl). 
- See also: [Page Objects Begone](https://warpedjavaguy.wordpress.com/2014/08/27/page-objects-begone/)

Can I change the selenium implementation used by gwen-web?
----------------------------------------------------------
Yes, see [Changing the Selenium version](CHEATSHEET.md#changing-the-selenium-version).
