Gwen Working Directory
======================

This is the Gwen working directory for your project where you can launch and manage all your Gherkin features, Gwen meta and settings files.

By default, Gwen will run all features on your local chrome browser. You can reconfigure the CLI defaults (`gwen.cli.*` settings in `gwen.conf`) and rearrange the directory structure to your linking. The default setup is just a starting point.

### Reports

HTML reports will be generated in the output directory by default. To change the default report format, either:
- Update the `gwen.report.output.formats` setting in the `gwen.conf` file in your root project to specify your desired formats
- Or invoke Gwen with the `-f/--format` option and explicitly specify your desired formats

Help
----

For command-line help, run the following in your project root:
- `./gwen --help`

See also:
- [browsers/README.md](browsers/README.md)
- [env/README.md](env/README.md)
- [features/README.md](features/README.md)
- [samples/README.md](samples/README.md)

Learn more:
- [Geting Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)
- [Gwen reports](https://github.com/gwen-interpreter/gwen/wiki/Evaluation-Reports)

Links and Resources:
----------------------

- [Gwen Home](http://gweninterpreter.org)
- [Web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL)
- [FAQ](https://github.com/gwen-interpreter/gwen-web/wiki/FAQ)
- [Blog](https://gweninterpreter.wordpress.com)
- [Twitter](https://twitter.com/gweninterpreter)
- [Support](https://gwenify.com)
