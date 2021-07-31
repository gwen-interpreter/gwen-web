Gwen Working Directory
======================

This is the Gwen working directory for your project where you can launch and manage all your Gherkin features, Gwen meta and settings files.

```
./                        # Your project root
 |  gwen.conf             # Common/default Gwen settings
 +--/gwen
    |  .gitignore         # Git ignore file
    |  README.md
    +--/browsers          # Browser settings
    |     chrome.conf     # - default is chrome
    |     edge.conf
    |     firefox.conf
    |     safari.conf
    |     ie.conf
    |     remote.conf     # Remote web driver settings
    |     README.md
    +--/env               # Environment settings
    |     local.conf      # - default is local
    |     dev.conf
    |     test.conf
    |     README.md
    +--/features          # Features and associative meta
    |     README.md
    +--/meta              # Common/reusable meta
    |     README.md
    +--/reports           # Report output directory
    +--/samples           # Sample features
```

By default, Gwen will run all features on your local chrome browser. You can reconfigure the CLI defaults (`gwen.cli.*` settings in `gwen.conf`) and rearrange the directory structure to your linking. The default setup is just a starting point.

Help
----

For command-line help, run the following in your project root:
- `./gwen --help`

See also:
- [browsers/README.md](browsers/README.md)
- [env/README.md](env/README.md)
- [features/README.md](features/README.md)
- [reports/README.md](reports/README.md)
- [samples/README.md](samples/README.md)

Learn more:
- [Geting Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)

Links and Resources:
----------------------

- [Gwen Home](http://gweninterpreter.org)
- [Web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL)
- [FAQ](https://github.com/gwen-interpreter/gwen-web/wiki/FAQ)
- [Blog](https://gweninterpreter.wordpress.com)
- [Twitter](https://twitter.com/gweninterpreter)
- [Support](https://gwenify.com)
