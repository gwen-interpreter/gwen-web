Gwen workspace
==============

This is your Gwen workspace where you can launch Gwen and manage all your 
feature, meta, and settings files.

The structure of this workspace is as follows:

gwen-workspace
|--browsers               : Browser properties folder
|--env                    : Environment properties folder
|--features               : Gherkin features folder
|  gwen                   : Gwen launch script for Linux/Mac environments
|  gwen.bat               : Gwen launch script for Windows environments
|  gwen.properties        : Global workspace properties
|  log4j.properties       : Log4j properties file for Gwen
|  gwen-gpm.jar           : Gwen package manager JAR
|  .gitignore             : Git ignore file

Before using this workspace, it is recommended that you read and work through 
the Getting Started with Gwen guide here:
  - https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started

See also:
- The README.txt files in each subfolder of this workspace

Proxy Configuration
-------------------

If you are behind a firewall and need to go through a proxy, you will need to 
ensure that the HTTPS_PROXY environment variable in your environment is set 
to one of the following formats:
- host:port
- username:password@host:port
  - Or use 1st option and set username and password in the HTTPS_PROXY_USER 
    and HTTPS_PROXY_PASS variables respectively

