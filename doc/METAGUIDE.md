Gwen Web Manual
=============================

Feature Guide 
-------------
The following items are available to enter in test features.  Gwen does not distinguish 
between given, when and then's however it does help tests with structure.

[@tag]
Feature:

[Background]

[@tag]
Scenario:
 Given <expression>
  When <expression>
  Then <expression>
   And <expression>
   But <expression>


Console Command Arguments 
-------------------------
The following arguments can be supplied when starting gwen web.

--version					-	gwen version information
--help						-	this help menu
-b, --batch	<dir>			-	batch mode, will load both features and meta non-interactively
-p, --properties <file> 	-	configurable gwen properties
-r, --report <dir>			-	generated reporting location
-m, --metafeature <file>	-	overriding metedata during test execution
-t, --tags @ | ~@ inc/exc	-	ability to filter test selection.
<feature file, files, and 
   or dir>					-	Individually listed features
   								execute in order
   							-	Directory
   								execute all features in sub folders
   								additionally sources all meta
   								specific to the directory scope
   								
Console Command
---------------   								
The following commands are available within the gwen interactive console.

env							-	dump the environment scope to the console
tab							-	when nothing has been entered on the console will display
								And     But     Given   Then    When    env     exit
exit						-	will close the current browser (if open) and exit the console.

WebElement Locators
-----------------
locate by
-	id
-	name
-	tag name
-	css selector
-	xpath
-	class name
-	link text
-	partial link text
-	javascript

Example:		And the Start button can be located by tag name "submit"
				And the Start button can be located by class name "btn"
				And the Start button can be located by id "btn432"


Sample DSLs

| DSL Sample       | Purpose 
| ------------- |:-------------:|
| I navigate to the "text" page 		 					| right-aligned |
| I navigate to "http://abc" url							| Will lookup the navigation/url |
|															| in the meta against a specified page, and actually set | 
|															| the url in the browser window |
| I am on the "text" page									| centered      |
| <scoped elem> can be located by <locator> "xx"			| centered      |
| the page title should (be|contain) my <scoped var>
| the page title should be "text"
| I switch to "frmsrc" frame by source
| I switch to the default frame
| <scoped elem> text should (be|contain) my <scoped var>
| I capture <scoped elem> text
| <scoped elem> text should (be|contain) "text"
| <scoped elem> should be <actionA>
| my role is "text"
| my "text" setting (is|will be) "text"
| my "text" attribute (is|will be) "text"
| the url will be "http://abc"
| I wait for <scoped elem> text for 99 second(s)
| I wait for <scoped elem> text
| I wait for <scoped elem> for 99 second(s)
| I wait for <scoped elem>
| I wait 99 second(s)
| I enter my <scoped elem> attribute in <scoped elem 2>
| I enter <scoped elem> in <scoped elem 2>
| I enter "text" in <scoped elem>
| I select "text" in <scoped elem>
| I <actionB> <scoped elem>
| I highlight <scoped elem>


