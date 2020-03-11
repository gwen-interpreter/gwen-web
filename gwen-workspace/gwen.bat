@ECHO OFF

REM Gwen workspace
REM ==============

REM ---------------------------------------------------------------------------
REM  Gwen workspace script for windows environments
REM  ---------------------------------------------------------------------------

REM
SETLOCAL enabledelayedexpansion

REM Defaults
SET DEFAULT_BROWSER=chrome
SET DEFAULT_ENV=local

SET GWEN_ARGS=%*
SET ARG1=%1
SET ARG2=%2

REM Display usage if first argument is help
IF "%1%" == "help" (
  CALL :HELPGWEN
  EXIT /B 1
)
IF "%1%" == "--help" (
  CALL :HELPGWEN
  EXIT /B 1
)
IF "%1%" == "-h" (
  CALL :HELPGWEN
  EXIT /B 1
)

REM Set target browser or default to chrome if no target browser was provided
IF EXIST "browsers\%1.properties" (
  SET TARGET_BROWSER=%1
  SHIFT
) ELSE (
  SET TARGET_BROWSER=%DEFAULT_BROWSER%
)

REM Set target environment
IF NOT "%1" == "%DEFAULT_ENV%" (
  IF NOT EXIST "env\%1.properties" (
    SET TARGET_ENV=%DEFAULT_ENV%
  ) ELSE (
    SET TARGET_ENV=%1
  )
) ELSE (
  SET TARGET_ENV=%1
)

ECHO Target browser is %TARGET_BROWSER%
ECHO Target environment is %TARGET_ENV%

REM Prepare gwen JVM arguments and properties
SET GWEN_JVM_ARGS=-Dgwen.web.browser=%TARGET_BROWSER%
SET GWEN_PROPS=gwen.properties,browsers\%TARGET_BROWSER%.properties,env\%TARGET_ENV%.properties

REM Install gwen-web
SET GWEN_WEB_HOME=target\gwen-web
java -jar gwen-gpm.jar -p "%GWEN_PROPS%" update gwen-web gwen.gwen-web.version %GWEN_WEB_HOME%
SET EXITCODE=!ERRORLEVEL!
IF !EXITCODE! EQU 1 (
  ECHO Failed to auto update/install gwen-web ^(no internet connection maybe^)
)

REM If gwen.selenium.version is set to a specific version, then install that
REM selenium-java API version and set SELENIUM_HOME to the installed location
REM Otherwise do nothing if gwen.selenium.version=provided (exit code 2).
java -jar gwen-gpm.jar -p "%GWEN_PROPS%" update selenium gwen.selenium.version target\selenium
SET EXITCODE=!ERRORLEVEL!
IF !EXITCODE! EQU 0 (
    SET SELENIUM_HOME=target\selenium
)
IF !EXITCODE! NEQ 0 (
  SET "SELENIUM_HOME="
)
IF !EXITCODE! EQU 1 (
  ECHO Failed to auto update/install selenium Java package ^(no internet connection maybe^)
)

REM Remove browser & env from Gwen program args
IF "%ARG1%" == "%TARGET_BROWSER%" (
  IF "%ARG2%" == "%TARGET_ENV%" (
    FOR /F "tokens=2* delims= " %%A in ("%*") DO SET GWEN_ARGS=%%B
  ) ELSE (
    FOR /F "tokens=1* delims= " %%A in ("%*") DO SET GWEN_ARGS=%%B
  )
) ELSE (
  IF "%ARG1%" == "%TARGET_ENV%" (
    FOR /F "tokens=1* delims= " %%A in ("%*") DO SET GWEN_ARGS=%%B
  )
)

REM Launch Gwen
ECHO
ECHO Launching Gwen
CALL %GWEN_WEB_HOME%\bin\gwen-web %GWEN_JVM_ARGS% -r target/reports -p "%GWEN_PROPS%" %GWEN_ARGS%
EXIT /B !ERRORLEVEL!

:HELPGWEN
ECHO Usage^:
ECHO gwen ^[browser^] ^[env^] ^[options^] ^[features^]
ECHO     ^[browser^] =
ECHO       chrome  ^: to use chrome browser (default)
ECHO       firefox ^: to use firefox browser
ECHO       safari  ^: to use safari browser
ECHO       edge    ^: to use Edge browser
ECHO       ie      ^: to use IE browser
ECHO       other   ^: name of browser properties file in browsers directory
ECHO      ^[env^]    =
ECHO         local ^: to use local user environment
ECHO          name ^: name of environment to use
ECHO            ^(dev will load env\dev.properties^)
ECHO      ^[options^] =
ECHO      --version
ECHO               ^: Prints the implementation version
ECHO      -h, --help, help
ECHO               ^: Prints this usage text
ECHO      -b --batch
ECHO               ^: Batch/server mode
ECHO      --parallel
ECHO               ^: Run features or scenarios in parallel depending
ECHO                  on state level
ECHO      --parallel-features
ECHO               ^: Run features in parallel regardless of state level
ECHO      -f, --formats ^<formats^>
ECHO               ^: Comma separated report formats to produce.
ECHO                 Supported formats include: html slideshow junit json
ECHO                 ^(default is html^)
ECHO      -t, --tags ^<tags^>
ECHO               ^: Comma separated list of @include or ~@exclude tags
ECHO      -n, --dry-run
ECHO               ^: Check syntax and validate only
ECHO      -i, --input-data ^<input data file^>
ECHO               ^: Input data ^(CSV file with column headers^)
ECHO      -m, --meta ^<meta files^>
ECHO               ^: Comma separated list of meta files and directories
ECHO    ^<features^> = Space separated list of feature files/directories
EXIT /B 0
