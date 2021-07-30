Gwen Working Directory
======================

This is the Gwen working directory for your project where you can launch and manage all your Gherkin features, Gwen meta and settings files.
- [Get Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)

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
    +--/features          # Your feature/meta files go here 
    |     README.md
    +--/reports           # Report output directory
    +--/samples           # Sample features
```

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

Links and Resources
----------------------

- [Gwen Home](http://gweninterpreter.org)
- [Web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL)
- [FAQ](https://github.com/gwen-interpreter/gwen-web/wiki/FAQ)
- [Blog](https://gweninterpreter.wordpress.com)
- [Twitter](https://twitter.com/gweninterpreter)
- [Support](https://gwenify.com)
