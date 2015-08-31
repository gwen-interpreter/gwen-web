Gwen-web Dev Guide
==================

Development
-----------

If you would like to set up a local development environment and work on the 
source, perform the following:

1. Download and install [Java SDK 7+](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 
2. Download a [Git client](http://git-scm.com/downloads)
3. Clone the gwen source repository at https://github.com/gwen-interpreter/gwen 
4. Clone the gwen-web source repository at https://github.com/gwen-interpreter/gwen-web
5. Download and install the latest [sbt](http://www.scala-sbt.org/) version
6. Change to the directory where you cloned the gwen-web source and run `sbt eclipse`
7. Download and install the [Scala IDE](http://scala-ide.org/) for Scala 2.11.x
8. Import the `gwen` and `gwen-web` projects into the Scala IDE

### Building and Installing from Source

Perform the following if you would like to build an installable gwen-web binary from source: 

1. Download and install [Java SDK 7+](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 
2. Download a [Git client](http://git-scm.com/downloads)
3. Clone the gwen source repository at https://github.com/gwen-interpreter/gwen 
4. Clone the gwen-web source repository at https://github.com/gwen-interpreter/gwen-web
5. Download and install the latest [sbt](http://www.scala-sbt.org/) version
6. Change to the directory where you cloned the gwen-web source and run 
   `sbt universal:package-bin` to build the distributable ZIP
   - This will create a _gwen-web-[version].zip_ file in the 
     _target/universal_ folder relative to your current directory
7. Extract the generated ZIP to a desired location on your local drive
   - This will create a project folder named _gwen-web-[version]_ in that 
     location
8. [Configure settings](CHEATSHEET.md#configuration-settings) (optional)
9. [Start using gwen-web](START.md) 