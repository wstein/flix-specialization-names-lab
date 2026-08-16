@echo off
rem Runs edit-resistance.scala against a Flix compiler jar. See
rem scripts/edit-resistance (the POSIX wrapper) for why this exists: a
rem `using jar` directive is a static literal, so edit-resistance.scala alone
rem has no classpath.
rem
rem This never goes through flixw.cmd, on purpose: the whole point of the lab is comparing
rem two different compiler builds (counter-suffix vs. hash-suffix) against the same source,
rem and flixw.cmd only ever holds one pinned jar at a time. Jar resolution, in order, first
rem match wins:
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
rem   scripts\edit-resistance.cmd [source.flix]
rem   scripts\edit-resistance.cmd --flix-jar path\to\flix.jar [source.flix]
rem   scripts\edit-resistance.cmd --compiler-version string [source.flix]
rem
rem Deliberately does not `cd` anywhere: a source.flix argument, or
rem --flix-jar, is a path the caller gave relative to wherever they are, and
rem resolving it against this script's own directory instead -- as `cd`-ing
rem here first would -- silently measures (or looks for a jar at) the wrong
rem thing when this is invoked via a relative path from outside the project.
rem What must stay anchored to the project root regardless of caller cwd --
rem the default source, the project-root flix.jar fallback, this tool's own
rem build\ -- is qualified with %~dp0..\ explicitly below instead.
setlocal enabledelayedexpansion

rem Parsed first, ahead of everything else below, so -h/--help short-circuits
rem before this changes directory, wipes build\, or resolves a jar.
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
echo Usage: scripts\edit-resistance.cmd [--flix-jar path\to\flix.jar] [source.flix]
echo                                    [--compiler-version string]
echo(
echo Measures how many generated class names, and how many generated classes,
echo survive a set of edits to source.flix (default: src\Main.flix^).
echo(
echo Options:
echo   --flix-jar ^<path^>        Flix compiler jar to compile against.
echo                             Overrides every other source below.
echo   --compiler-version ^<str^> Label the report with what produced it. If
echo                             not given, this asks the resolved jar
echo                             directly (java -jar ... --version^) and
echo                             appends a short digest of its own bytes,
echo                             since two builds can share a version
echo                             string while differing in every byte.
echo   -h, --help                Show this help and exit.
echo(
echo Jar resolution when --flix-jar is not given, first match wins. This never
echo consults flixw.cmd, even if present: it deliberately holds only one
echo pinned jar, and this needs to compare arbitrary builds against each other.
echo   1. %%FLIX_JAR%%        unverified, as in .envrc.example
echo   2. .\flix.jar        a jar placed at the project root by hand
echo   3. `flix` on %%PATH%%  a system-installed Flix, if it resolves to a jar
exit /b 0

:afterparse

rem This tool compiles in-process and never reads or writes build\ itself, but
rem a stale build\ from an earlier flixw.cmd build or flixw.cmd test can
rem linger alongside it. Wiping it here keeps a run's disk footprint to just
rem what this invocation produces, and keeps a parallel class-id-report
rem census (which does read build\class) from ever picking up classes from a
rem source version that has since moved on.
if exist "%~dp0..\build" rmdir /s /q "%~dp0..\build"

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

rem No positional source.flix was given: fall back to this lab's own demo file
rem explicitly, rather than leaving it to edit-resistance.scala's relative
rem default -- which would otherwise resolve against the caller's cwd now
rem that this never `cd`s.
if "%ARGS%"=="" set ARGS= "%~dp0..\src\Main.flix"

rem Labeling the report: an explicit --compiler-version always wins. Otherwise this asks the
rem jar itself (java -jar ... --version) rather than parsing its cache filename -- that
rem works no matter what the file is named or where it came from.
rem
rem The version alone is not enough to label with: two builds can print the identical
rem version string while differing in every byte -- comparing exactly that is what this lab
rem is for -- so a self-computed digest (a short prefix, git-style) is always appended,
rem computed from the jar's own bytes via certutil (built into Windows, no extra install).
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

scala-cli run "%~dp0edit-resistance.scala" --jvm 21 --jar "%JAR%" -- --compiler-version "%COMPILER_VERSION%" %ARGS%
