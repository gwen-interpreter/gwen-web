Gwen workspace
==============

env directory
-------------

This directory is used for defining environment specific settings. When 
you target an environment with Gwen, the settings in the associated 
<environment>.conf file are loaded into Gwen as system properties
which can be injected throughout features using the ${setting.name} syntax.

Add a config file for any specific environment that you require here. 
Then set the settings in that file that you require for that environment.
Settings defined here have precedence over any settings defined in the 
root gwen.conf file in this workspace. You can therefore override 
any of those settings at the enviornment level if you need to.

Examples:
- dev.conf (for dev environment settings)
- test.conf (for test environment settings)

To have Gwen load the settings for an environment, pass the environment 
name (<name>.conf) as the first or second parameter to gwen.
If you do not specify and environment parameter then local will be used by
default.

For example:
- gwen
  - to launch gwen REPL with local environment settings using the default browser
- gwen dev
  - to launch gwen REPL with dev environment settings using the default browser
- gwen firefox dev
  - to launch gwen REPL with dev environment settings using the firefox browser
- gwen test -b features
  - to run all features in the features folder with test environment settings
    using the default browser
- gwen firefox test -b features
  - to run all features in the features folder with test environment settings
    using the firefox browser

Any number of config files can be defined in this folder (one for each
environment that you require).
