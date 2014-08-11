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

| Command      | Description	|
| ------------- |-------------|
|--version	| gwen version information |
|--help | this help menu |
| -b, --batch <dir> | batch mode, will load both features and meta non-interactively |
| -p, --properties <file> |	configurable gwen properties |
| -r, --report <dir> | generated reporting location |
| -m, --metafeature <file> | overriding metedata during test execution |
| -t, --tags @ or ~@ inc/exc | ability to filter test selection. |
| feature file, files, and or dir | Individually listed features execute in order |
|	| Directory
|   | execute all features in sub folders additionally sources all meta	specific to the directory scope |
   								
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

| DSL Sample       | Purpose 	|
| ------------- |-------------|
| I navigate to the "text" page 		 					| Will lookup the navigation/url in the meta against a specified page, and actually set the url in the browser window |
| I navigate to "http://abc" url							| Will use the literal in the browser window |
| I am on the "text" page									| Will set the current scope to the literal.  If the current scope is not the target then a new scope is created. |
| <scoped elem> can be located by <locator> "xx"			| Will set the <scoped elem> to be found by a locator in the page scope |
| the page title should (be|contain) my <scoped var>		| |
| the page title should be "text"							| Will verify that the page title is set to the literal |
| I switch to "frmsrc" frame by source						| using a css selector will switch the webdriver to the iframe determined by specified iframe[src] |
| I switch to the default frame								| will switch back to the default context in selenium webdriver (required after switching to iframe) |
| <scoped elem> text should (be|contain) my <scoped var>	| |
| I capture <scoped elem> text								| will retrieve the element in the page scope and set its text value in the global scope |
| <scoped elem> text should (be|contain) "text"				| will retrieve the element in the page scope and verify its contents against the literal |
| <scoped elem> should be <actionA>							| will retrieve the element in the page scope and perform on of actionA's |
| my role is "text"											| will create a new feature scope |
| my "text" setting (is|will be) "text"						| sets sys.prop |
| my "text" attribute (is|will be) "text"					| sets a literal in the feature scope to be that of a value |
| the url will be "http://abc"								| sets a url literal in the page scope to be that of a url. (does not action url, just sets scope) |
| I wait for <scoped elem> text for 99 second(s)			| waits for x seconds for the text of the element (found in the page scope) to appear |
| I wait for <scoped elem> text								| waits for the text of the element (found in the page scope) to appear, reads properties for gwen.web.wait.seconds, or defaults to 3 seconds |
| I wait for <scoped elem> for 99 second(s)					| waits for x seconds for the element (found in the page scope) to appear |
| I wait for <scoped elem>									| waits for x seconds for the element (found in the page scope) to appear, reads properties for gwen.web.wait.seconds, or defaults to 3 seconds |
| I wait 99 second(s)										| waits x seconds |
| I enter my <scoped elem> attribute in <scoped elem 2>		| finds a value in feature scope and sets it against the scoped locator |
| I enter <scoped elem> in <scoped elem 2>					| retrieves the text value of an element in page scope and uses that to set scoped elem 2 |
| I enter "text" in <scoped elem>							| enters the literal into the scoped element |
| I select "text" in <scoped elem>							| selects a value in the dropdown scoped element |
| I <actionB> <scoped elem>									| performs actionB on scoped element |
| I highlight <scoped elem>									| highlights in the browser the scoped element |


Key - ActionA, ActionB
Where actionA and actionB are mentioned in the table above, please see below for possible values.

| name       | Action 	|
| ------------- |-------------|
| actionA | visible |
|         | displayed |
|         | invisible |
|         | hidden |
|         | checked |
|         | ticked |
|         | unchecked |
|         | unticked |
|         | enabled |
|         | disabled |
| actionB | click |
|         | submit |
|         | tick |
|         | check |
|         | untick |
|         | uncheck |
