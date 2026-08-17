@echo off
rem Runs duplicate-decl-stress.scala against a Flix compiler jar. See
rem scripts/duplicate-decl-stress (the POSIX wrapper) for why this exists: a
rem `using jar` directive is a static literal, so duplicate-decl-stress.scala
rem alone has no classpath.
rem
rem This never goes through flixw.cmd, on purpose, same reason as the other lab scripts:
rem flixw.cmd holds only one pinned jar at a time, and checking an arbitrary build (to see
rem whether a frontend change affected duplicate-declaration diagnostics) is the point. Jar
rem resolution, in order, first match wins:
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
rem   scripts\duplicate-decl-stress.cmd
rem   scripts\duplicate-decl-stress.cmd --count 500
rem   scripts\duplicate-decl-stress.cmd --flix-jar path\to\flix.jar
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
echo Usage: scripts\duplicate-decl-stress.cmd [--flix-jar path\to\flix.jar]
echo                                          [--count n] [--compiler-version string]
echo(
echo Generates `count` byte-identical top-level declarations of the same def, compiles
echo them, and checks that every one is still cited at its own source line across the
echo reported errors -- i.e. that identical content never gets merged, dropped, or
echo misattributed just because two declarations read the same.
echo(
echo Options:
echo   --flix-jar ^<path^>        Flix compiler jar to compile against.
echo                             Overrides every other source below.
echo   --count ^<n^>               How many identical duplicate declarations
echo                             to generate (default: 50^).
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
rem -- so a self-computed digest (a short prefix, git-style) is always appended too, computed
rem from the jar's own bytes via certutil (built into Windows, no extra install).
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

rem -Xss128m, defensively: unlike scripts/collision-stress.cmd, this doesn't generate one
rem deeply nested expression (each duplicate is its own flat top-level def), so the risk is
rem lower -- but the namer's own duplicate-checking recursion depth as --count grows isn't
rem verified either way, and the cost of the larger stack up front is negligible.
scala-cli run "%~dp0duplicate-decl-stress.scala" --jvm 21 --jar "%JAR%" -J -Xss128m -- --compiler-version "%COMPILER_VERSION%" %ARGS%
