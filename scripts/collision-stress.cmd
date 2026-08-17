@echo off
rem Runs collision-stress.scala against a Flix compiler jar. See
rem scripts/collision-stress (the POSIX wrapper) for why this exists: a
rem `using jar` directive is a static literal, so collision-stress.scala alone
rem has no classpath.
rem
rem This never goes through flixw.cmd, on purpose, same reason as scripts/edit-resistance:
rem flixw.cmd holds only one pinned jar at a time, and pointing this at an arbitrary build
rem (to check whether a fix to the collision guard actually changed anything) is the point.
rem Jar resolution, in order, first match wins:
rem   1. --flix-jar <path>  passed on this script's own command line
rem   2. %FLIX_JAR%         same override convention flixw itself uses for
rem                         running a compiler it did not download (see
rem                         .envrc.example); unverified, same as there
rem   3. .\flix.jar         a jar placed at the project root by hand
rem   4. `flix` on %PATH%   whatever a system-installed Flix resolves to.
rem                         Only works if that is itself a jar (or a
rem                         polyglot self-executing one) -- scala-cli's
rem                         --jar needs an actual jar, not a launcher
rem                         script, and this is unverified either way
rem
rem Usage:
rem   scripts\collision-stress.cmd
rem   scripts\collision-stress.cmd --width 2 --count 200
rem   scripts\collision-stress.cmd --flix-jar path\to\flix.jar
rem
rem Deliberately does not `cd` anywhere: --flix-jar is a path the caller gave
rem relative to wherever they are, and resolving it against this script's own
rem directory instead would silently look for a jar in the wrong place when
rem invoked via a relative path from outside the project. .\flix.jar is still
rem anchored to the project root explicitly below, since that fallback means
rem "a jar placed at the project root by hand", not "in the caller's cwd".
setlocal enabledelayedexpansion

rem Parsed first, ahead of everything else below, so -h/--help short-circuits
rem before resolving a jar.
set "JAR=%FLIX_JAR%"
set "COMPILER_VERSION="
set "ARGS="

:parse
if "%~1"=="" goto afterparse
if "%~1"=="-h" goto usage
if "%~1"=="--help" goto usage
if "%~1"=="--flix-jar" (
  set "JAR=%~2"
  shift
  shift
  goto parse
)
if "%~1"=="--compiler-version" (
  set "COMPILER_VERSION=%~2"
  shift
  shift
  goto parse
)
set "ARGS=!ARGS! %1"
shift
goto parse

:usage
echo Usage: scripts\collision-stress.cmd [--flix-jar path\to\flix.jar]
echo                                     [--width n] [--count n] [--compiler-version string]
echo(
echo Generates `count` distinct specializations of one generic def, compiles them at
echo stable-id width `n`, and reports whether two of them collided into the same class
echo name -- and if so, whether the compiler caught it or let it through silently.
echo(
echo Options:
echo   --flix-jar ^<path^>        Flix compiler jar to compile against.
echo                             Overrides every other source below.
echo   --width ^<n^>               Stable-id width to compile at (default: 2^).
echo                             Narrow enough that a collision is likely with
echo                             a modest --count; the default 12-character
echo                             width would need billions of specializations
echo                             to reach the same odds.
echo   --count ^<n^>               How many distinct specializations to generate.
echo                             Defaults to whatever the birthday bound says
echo                             gives a 99.9%% collision probability at --width.
echo   --compiler-version ^<str^> Label the report with what produced it. If
echo                             not given, this asks the resolved jar
echo                             directly (java -jar ... --version^) and
echo                             appends a short digest of its own bytes.
echo   -h, --help                Show this help and exit.
echo(
echo Jar resolution when --flix-jar is not given, first match wins. This never
echo consults flixw.cmd, even if present: it deliberately holds only one
echo pinned jar, and this needs to point at whatever build is being checked.
echo   1. %%FLIX_JAR%%        unverified, as in .envrc.example
echo   2. .\flix.jar        a jar placed at the project root by hand
echo   3. `flix` on %%PATH%%  a system-installed Flix, if it resolves to a jar
exit /b 0

:afterparse

if "%JAR%"=="" if exist "%~dp0..\flix.jar" set "JAR=%~dp0..\flix.jar"

if "%JAR%"=="" (
  for /f "delims=" %%F in ('where flix 2^>nul') do (
    if "!JAR!"=="" set "JAR=%%F"
  )
)

if "%JAR%"=="" (
  echo error: no Flix compiler jar found ^(tried %%FLIX_JAR%%, %~dp0..\flix.jar, and 'flix' on %%PATH%%^) 1>&2
  echo place a jar at %~dp0..\flix.jar, or pass --flix-jar 1>&2
  exit /b 1
)
if not exist "%JAR%" (
  echo error: Flix compiler jar not found at '%JAR%' 1>&2
  exit /b 1
)

rem Labeling the report: an explicit --compiler-version always wins. Otherwise this asks the
rem jar itself (java -jar ... --version) rather than parsing its cache filename -- that
rem works no matter what the file is named or where it came from. The version alone is not
rem enough: two builds can print the identical version string while differing in every byte
rem -- e.g. a fix to the collision guard itself might not change the version number -- so a
rem self-computed digest (a short prefix, git-style) is always appended too, computed from
rem the jar's own bytes via certutil (built into Windows, no extra install).
if "%COMPILER_VERSION%"=="" (
  set "VERSION="
  for /f "tokens=*" %%L in ('java -jar "%JAR%" --version 2^>nul') do set "VERSION_LINE=%%L"
  if defined VERSION_LINE (
    for %%T in (!VERSION_LINE!) do set "VERSION=%%T"
  )

  set "DIGEST="
  for /f "skip=1 delims=" %%H in ('certutil -hashfile "%JAR%" SHA256 2^>nul') do (
    if not defined DIGEST (
      set "HLINE=%%H"
      echo !HLINE! | findstr /c:"CertUtil" >nul || set "DIGEST=!HLINE: =!"
    )
  )

  rem Independent, non-chained conditions rather than if/else: an else here would bind to
  rem the nearest enclosing if, not the outer one, and silently leave COMPILER_VERSION
  rem empty in the not-defined-VERSION case instead of falling back to %JAR%.
  if defined VERSION if defined DIGEST set "COMPILER_VERSION=!VERSION! ^(!DIGEST:~0,12!^)"
  if defined VERSION if not defined DIGEST set "COMPILER_VERSION=!VERSION!"
  if not defined VERSION set "COMPILER_VERSION=%JAR%"
)

rem -Xss128m: main()'s generated body chains every use_i() result into one expression, so a
rem large --count is a deeply nested AST that some recursive compiler phase walks non-tail-
rem recursively. The default thread stack overflows well before --count reaches the
rem hundreds a real stress run needs; 128m has run comfortably past 200.
scala-cli run "%~dp0collision-stress.scala" --jvm 21 --jar "%JAR%" -J -Xss128m -- --compiler-version "%COMPILER_VERSION%" %ARGS%
