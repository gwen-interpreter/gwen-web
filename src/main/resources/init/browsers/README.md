Browsers
========

Configure all your browser settings here (one file per browser).

To load settings for a browser, pass the associated browser conf file to the Gwen `-c/--config` CLI option.

For example, to run on local firefox:
- `-c gwen/browsers/firefox.conf`
- or `--config gwen/browsers/firefox.conf`

To run on remote firefox via remote webdriver (add the remote config)
- `-c gwen/browsers/firefox.conf,gwen/browsers/remote.conf`
- or `--config gwen/browsers/firefox.conf,gwen/browsers/remote.conf`
