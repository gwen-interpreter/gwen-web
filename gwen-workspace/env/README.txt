Gwen workspace
==============

env directory
-------------

This directory is used for defining environment specific properties. When 
you target an environment with Gwen, the properties in the associated 
<environment>.properties file are loaded into Gwen as system properties
which can be injected throughout features using the ${property.name} syntax.

Add a properties file for any specific environment that you require here. 
Then set the properties in that file that you require for that environment.
Properties defined here have precedence over any properties defind in the 
root gwen.properties file in this workspace. You can therefore override 
any of those properties at the enviornment level if you need to.

Examples:
- local.properties (for local environment settings)
- dev.properties (for dev environment settings)
- test.properties (for test environment settings)

To have Gwen load the properties for an environment, pass the environment 
name (<name>.properties) as the first or second parameter to gwen.
If you do not specify and environment parameter then local will be used by
default.

For example:
- gwen
  - to launch gwen REPL with local environment properties using the default browser
- gwen dev
  - to launch gwen REPL with dev environment properties using the default browser
- gwen firefox dev
  - to launch gwen REPL with dev environment properties using the firefox browser
- gwen test -b features
  - to run all features in the features folder with test environment properties
    using the default browser
- gwen firefox test -b features
  - to run all features in the features folder with test environment properties
    using the firefox browser

Any number of properties files can be defined in this folder (one for each
environment that you require).
