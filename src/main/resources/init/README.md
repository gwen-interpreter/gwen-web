Gwen Working Directory
======================

This is the Gwen working directory for your project where you can launch and manage all your Gherkin features, Gwen meta and settings files.
- [Get Started](https://github.com/gwen-interpreter/gwen-web/wiki/Getting-Started)

```
./                        # Your project root
 |  gwen.conf             # Common/default Gwen settings
 +--/gwen                 # Gwen working directory
    |  README.md
    +--/browsers          # Browser settings
    |     chrome.conf
    |     edge.conf
    |     firefox.conf
    |     safari.conf
    |     ie.conf
    |     remote.conf     # Remote web driver settings
    |     README.md
    +--/env               # Environment settings
    |     localhost.conf
    |     dev.conf
    |     test.conf
    |     README.md
    +--/features          # Your feature/meta files go here 
    |     README.md
    +--/samples           # Sample features
```

## Samples
----------

To run all the provided samples on the default browser (chrome), run the follwing in your project root:
- `./gwen -b samples`
  - Note: the `-b` switch instructs Gwen to exit once done (otherwise REPL mode opens)

> If you don't want chrome to be the default browser, you can change it by updating the `gwen.web.browser.target` setting in the `gwen.conf` file in your project root.

Help
----

For command-line help, run the following in your project root:
- `./gwen --help`

See also:
- [browsers/README.md](browsers/README.md)
- [env/README.md](env/README.md)
- [features/README.md](features/README.md)

### Links and Resources
----------------------

- [Gwen Home](http://gweninterpreter.org)
- [Web DSL](https://github.com/gwen-interpreter/gwen-web/wiki/Supported-DSL)
- [FAQ](https://github.com/gwen-interpreter/gwen-web/wiki/FAQ)
- [Blog](https://gweninterpreter.wordpress.com)
- [Twitter](https://twitter.com/gweninterpreter)
- [Support](https://gwenify.com)
