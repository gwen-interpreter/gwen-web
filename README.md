gwen-web
========

A [Gwen](https://github.com/gwen-interpreter/gwen) web engine for 
automating web activities.

Core Requirements
-----------------

- Java 1.6 or later

Building and Installing from Source
-----------------------------------

Perform the following to build and install gwen-web: 

1. Either:
   - Download a [Git client](http://git-scm.com/downloads) and clone this 
     repository using one of the following (SSH or HTTPS) URLs: 
     - `git@github.com:gwen-interpreter/gwen-web.git`
     - `https://github.com/gwen-interpreter/gwen-web.git`
   - Or click the Download ZIP link on this GitHub project page to download 
     the source archive and extract it to a local folder 
2. Download and install [Java SDK 1.6 or later](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 
   - Note Java 8 is not recommended at this time of writing
3. Download and install the latest [sbt](http://www.scala-sbt.org/) version 
4. Change to the directory where your cloned/downloaded the source
5. Run `sbt test` to compile and run all tests and verify that all is OK
6. Run `sbt universal:packageBin` to build the distributable ZIP
   - This will create a _gwen-web-[version].zip_ file in the 
     _target/universal_ folder relative to your current directory
7. Extract the generated ZIP to a desired location on your local drive
   - This will create a project folder named _gwen-web-[version]_ in that 
     location
8. You are now ready to start using gwen-web!

Getting Started
---------------

See our [getting started](doc/START.md) user guide for a quick introduction 
and tutorial.
