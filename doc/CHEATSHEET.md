Gwen Web Manual
===============

Invoking Gwen
-------------

Usage: `gwen-web-install-dir/bin/gwen.sh|gwen.bat` `[options]` `[<feature paths>]`

| Option                         | Description |
| ------------------------------ |-------------|
| `--version  `                  | Prints the implementation version |
| `--help`                       | Prints the usage text |
| `-b`, `--batch`                | batch mode, will exit gwen session after executing features. Absence of this switch launches REPL mode. |
| `-p`, `--properties` `<files>` | Comma separated list of properties file paths |
| `-r`, `--report` `<dir>`       | generated reporting location |
| `-m`, `--meta` `<files>`       | Comma separated list of meta file paths |
| `-t`, `--tags` `<tags>`        | Comma separated list of @include or ~@exclude tags |
| `-|`, `--parallel`             | Executes specified `<feature paths>` in parallel |
| `<feature paths>`              | Space separated list of feature files and/or directories (feature suites) to evaluate |

REPL Commands
-------------

The following commands are available within the gwen REPL console.

| Command                          | Description |
| -------------------------------- | ----------- |
| `env`, `env -v`, `env --visible` | dump the currently visible environment scope to the console |
| `env -a`, `env --all`            | dump the entire environment scope to the console |
| `env -f`, `env --feature`        | dump the global feature scope to the console |
| `exit`,`quit`,`bye`              | will close the current browser (if open) and exit the console. |

Configuration Settings:
-----------------------

The following settings can be used to configure certain runtime aspects. These can be defined in 
your local `gwen.properties` files (located in your user home directory) or in a separate properties 
file provided through the `-p|--properties` command line option.

| Property/Setting               | Default value  | Supported Values | Description |
| ------------------------------ | -------------- | ---------------- | ----------- |
| `gwen.feature.failfast`        | true           | `true` to enable, `false` otherwise | Enables or disables fail fast mode at the feature level. If enabled, will fail a feature as soon as the first scenario in that feature fails before resuming with the next feature (if provided). Accepted values include:  |
| `gwen.web.browser`             | firefox        | `firefox`, `chrome`, `safari`, or `ie` | Configures the target browser to use. |
| `gwen.web.useragent`           |                | Any string literal | Set the user agent header in the browser to the literal specified. |
| `gwen.web.authorize.plugins`   | false          | `true` to allow, `false` otherwise | Controls whether or to allow browser plugins to run (for example: Java applets). |
| `gwen.web.wait.seconds`        | 10             | Number of seconds | Sets the maximum number of seconds the web driver should wait for web elements to become available before timing out. |
| `gwen.web.maximize`            | false          | `true` to maximize, `false` otherwise | Controls whether or not the web driver should maximize the browser window. |
| `gwen.web.throttle.msecs`      | 200            | Number of milliseconds | Sets the number of milliseconds to wait for the web driver and web browser to synchronize with each other between interactions. Also directly controls element highlighting duration. |
| `gwen.web.highlight.style`     | background: yellow; border: 2px solid gold; | HTML style string | Sets the look and feel of element highlighting. |
| `gwen.web.capture.screenshots` | false          | `true` to capture screenshots, `false` otherwise | Controls whether or not the web driver should capture screenshots for all steps that change page states. |

In addition to the above settings, all selenium web driver system properties are also supported. For example, 
all the [FireFox driver properties](https://code.google.com/p/selenium/wiki/FirefoxDriver#Important_System_Properties) 
are supported. 

### Native Web Drivers

If you use Chrome or IE, then you will need to download the 
[chrome web driver](http://chromedriver.storage.googleapis.com/index.html) 
or [IE web driver server](http://selenium-release.storage.googleapis.com/index.html) 
respectively and install it on your system path. If you do not have permission to 
install the driver on your system path, then you can set the path to your 
downloaded driver location in your _gwen.properties_ file as shown below:
  
```
    (chrome)  webdriver.chrome.driver = /path/to/chromedriver
    (ie)      webdriver.ie.driver = c:/path/to/IEDriverServer.exe
```

If you want to use Safari, then you will need to install the latest  
[safari driver extension](http://selenium-release.storage.googleapis.com/index.html) 
in your browser.

Supported DSL
-------------

All feature files must conform to the [Gherkin standard for feature files](https://github.com/cucumber/cucumber/wiki/Feature-Introduction).

The following steps are supported. Each one must be prefixed by one of the keyword literals: `Given`, `When`, `Then`, `And`, or `But`.

| Step | Description | Parameters |
| ---- | ----------- | ---------- |
| I am on the `<page>` | Puts the given page in scope | `<page>` = the name of the page to put in scope |
| the url will be "`<url>`" | Binds the given URL to the current page scope | `<url>` = the URL |
| the url will be defined by `property|setting` "`<name>`" | Binds the URL defined in the named property to the current page scope | `<name>` = name of property containing the URL |
| I navigate to the `<page>` | Opens the browser to the URL bound to the given page | `<page>` = the name of the page to navigate to |
| I navigate to "`<url>`" | Opens the browser to the given URL | `<url>` = the URL to navigate to |
| `<element>` can be located by `id|name|tag name|css selector|xpath|class name|link text|partial link text|javascript` "`<expression>`" | Creates a web element locator binding | `<element>` = name of web element, `<expression>` = the lookup expression |
| the page title `should|should not` `be|contain|match regex|match xpath` "`<expression>`" | Checks that the page title matches or does not match a given expression | `<expression>` = the expression to match against |
| the page title `should|should not` `be|contain|match regex|match xpath` `<attribute>` | Checks that the page title matches or does not match a bound attribute value | `<attribute>` = the attribute to match against | 
| `<element>` `should|should not` be `displayed|hidden|checked|unchecked|enabled|disabled` | Checks that an element should or should not be in a given state | `<element>` = the web element to check |
| `<element|attribute>` `should|should not` `be|contain|match regex|match xpath` "`<expression>`" | Checks that the text value of an element matches or does not match a given expression | `<element|attribute>` = the web element or bound attribute to check, `<expression>` = the expression to match against |
| `<element|attribute>` `should|should not` `be|contain|match regex|match xpath` `<attribute>` | Checks that the text value of an element matches or does not match a bound attribute | `<element|attribute>` = the web element or bound attribute to check, `<attribute>` = the name of the bound attribute containing the value to match against |
| I capture the `text|node|nodeset` in `<element|attribute|property>` by xpath "`<expression>`" as `<attribute>` | Extracts and binds a value by xpath from an element, attribute, or property setting into an attribute |`<element|attribute|property>` = the element, attribute, or property setting to extract the value from, `<expression>` = the extractor expression, `<attribute>` = the attribute to store the captured value into |
| I capture the text in `<element|attribute|property>` by regex "`<expression>`" as `<attribute>` | Extracts and binds a value by regex from an element, attribute, or property setting into an attribute |`<element|attribute|property>` = the element, attribute, or property setting to extract the value from, `<expression>` = the extractor expression, `<attribute>` = the attribute to store the captured value into |
| I capture the current URL | Binds the current browser URL to an attribute named URL in the current page scope |  |
| I capture the current URL as `<attribute>` | Binds the current browser URL to a named attribute in the current page scope | `<attribute>` = the attribute to bind the URL to |
| I capture `<element|attribute|property>` as `<attribute>` | Captures the text value of an element, attribute, or property and binds it to a named attribute | `<element|attribute|property>` = the web element, attribute, or property to capture the text of, `<attribute>` = the name of the attribute to bind the value to |
| I capture `<element|attribute|property>` | Captures the text value of an element and binds it to an attribute of the same name | `<element|attribute|property>` = the web element, attribute, or property to capture the value of and store in the attribute of the same name |
| my `<name>` `property|setting` `is|will be` "`<value>`" | Binds a given value to a system property | `<name>` = the name of the system property to set, `<value>` = the value to set |
| `<attribute>` `is|will be` defined by `javascript|property|setting` "`<expression>`" | Evaluates an expression and binds its value to a named attribute.  The evaluation occurs when the attribute is referenced. | `<attribute>` = the name of the attribute to bind the value to, `<expression>` = the expression that will yield the value to bind |
| `<attribute>` `is|will be` "`<value>`" | Binds a given value to a named attribute | `<attribute>` = the name of the attribute to bind the value to, `<value>` = the value to bind |
| I wait for `<element>` text for `<duration>` `second|seconds` | Waits a given number of seconds for an element's text to be loaded | `<element>` = the element to wait for text on, `<duration>` = the number of seconds to wait before timing out |
| I wait for `<element>` text | Waits for an elements text to be loaded | `<element>` = the element to wait for text on (timeout occurs after configured `gwen.web.wait.seconds`, or 10 seconds by default) |
| I wait for `<element>` for `<duration>` `second|seconds` | Waits a given number of seconds for a web element to be available on the page | `<element>` = the web element to wait for, `<duration>` = the number of seconds to wait before timing out |
| I wait for `<element>` | Waits for a web element to be available on the page | `<element>` = the web element to wait for (timeout occurs after configured `gwen.web.wait.seconds`, or 10 seconds by default) |
| I press enter in `<element>` | Sends the enter key to a web element (emulates user pressing enter in the element) | `<element>` = the element to send the enter key to |
| I `enter|type` "`<value>`" in `<element>` | Types or enters a value into a field (`type` just types the value, `enter` types the value and then sends the enter key to element) | `<value>` = the value to type or enter, `<element>` = the web element to type or enter the value into |
| I `enter|type` `<attribute>` in `<element>` | Types or enters a bound attribute value into a field (`type` just types the value, `enter` types the value and then sends the enter key to element) | `<attribute>` = the name of the attribute containing the value to type or enter, `<element>` = the web element to type or enter the value into |
| I select the `<position>st|nd|rd|th` option in `<element>` | Selects the option in a dropdown at a given position | `<position>` = the position of the option to select (1=first, 2=2nd, etc..), `<element>` = the dropdown element to select |
| I select "`<value>`" in `<element>` | Selects the option (by value) in a dropdown containing the given value | `<value>` = the value to select, `<element>` = the dropdown element to select | 
| I select `<attribute>` in `<element>` | Selects the option (by value) in a dropdown containing a bound attribute value | `<attribute>` = the name of the attribute containing the value to select, `<element>` = the dropdown element to select |
| I `click|submit|check|uncheck` `<element>` | Performs the specified action on an element | `<element>` = the element to perform the action on |
| I wait `<duration>` `second|seconds` when `<element>` is `clicked|submitted|checked|unchecked|selected|typed|entered` | Waits a given number of seconds after an action is performed on an element | `<duration>` = the number of seconds to wait, `<element>` = the element the action was performed on |
| I wait until `<condition>` when `<element>` is `clicked|submitted|checked|unchecked|selected|typed|entered` | Waits for a condition to be true after an action is performed on an element | `<condition>` = the name of the bound attribute containing the javascript predicate expression, `<element>` = the element the action was performed on |
| I wait until "`<javascript>`" | Waits until the given javascript expression returns true on the current page | `<javascript>` = the javascript predicate expression |
| I wait until `<condition>` | Waits until a condition is true on the current page | `<condition>` = the name of the bound attribute containing the javascript predicate expression |
| I wait `<duration>` `second|seconds` | Waits for a given number of seconds to lapse | `<duration>` = the number of seconds to wait |
| I `highlight|locate` `<element>` | Locates and highlights the given element on the current page | `<element>` = the element to highlight |
| I execute system process "`<process>`" | Executes a local system process | `<process>` = the system process to execute |
| I execute a unix system process "`<process>`" | Executes a local unix system process | `<process>` = the unix system process to execute |
