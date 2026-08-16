@echo off
rem Runs edit-resistance.scala against a Flix compiler jar. See
rem scripts/edit-resistance (the POSIX wrapper) for why this exists: a
rem `using jar` directive is a static literal, so edit-resistance.scala alone
rem has no classpath.
rem
rem flixw.cmd is preferred when available -- its jar is the one pinned in
rem .flixw\lock.toml and digest-verified before use -- but nothing here
rem requires it. Jar resolution, in order, first match wins:
rem   1. --flix-jar <path>  passed on this script's own command line
rem   2. %FLIX_JAR%         same override convention flixw itself uses for
rem                         running a compiler it did not download (see
rem                         .envrc.example); unverified, same as there
rem   3. flixw.cmd info     the jar flixw.cmd already downloaded and
rem                         digest-verified for .flixw\lock.toml, if
rem                         flixw.cmd is present and succeeds
rem   4. .\flix.jar         a jar placed at the project root by hand
rem   5. `flix` on %PATH%   whatever a system-installed Flix resolves to.
rem                         Only works if that is itself a jar (or a
rem                         polyglot self-executing one) -- scala-cli's
rem                         --jar needs an actual jar, not a launcher
rem                         script, and this is unverified either way
rem
rem Usage:
rem   scripts\edit-resistance.cmd [source.flix]
rem   scripts\edit-resistance.cmd --flix-jar path\to\flix.jar [source.flix]
setlocal enabledelayedexpansion

rem Parsed first, ahead of everything else below, so -h/--help short-circuits
rem before this changes directory, wipes build\, or resolves a jar.
set "JAR=%FLIX_JAR%"
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
set "ARGS=!ARGS! %1"
shift
goto parse

:usage
echo Usage: scripts\edit-resistance.cmd [--flix-jar path\to\flix.jar] [source.flix]
echo(
echo Measures how many generated class names, and how many generated classes,
echo survive a set of edits to source.flix (default: src\Main.flix^).
echo(
echo Options:
echo   --flix-jar ^<path^>  Flix compiler jar to compile against. Overrides
echo                       every other source below.
echo   -h, --help          Show this help and exit.
echo(
echo Jar resolution when --flix-jar is not given, first match wins:
echo   1. %%FLIX_JAR%%        unverified, as in .envrc.example
echo   2. flixw.cmd info    the jar flixw.cmd downloaded and digest-verified
echo                        for .flixw\lock.toml, if flixw.cmd is present
echo                        and succeeds
echo   3. .\flix.jar        a jar placed at the project root by hand
echo   4. `flix` on %%PATH%%  a system-installed Flix, if it resolves to a jar
exit /b 0

:afterparse
cd /d "%~dp0.."

rem This tool compiles in-process and never reads or writes build\ itself, but
rem a stale build\ from an earlier flixw.cmd build or flixw.cmd test can
rem linger alongside it. Wiping it here keeps a run's disk footprint to just
rem what this invocation produces, and keeps a parallel class-id-report
rem census (which does read build\class) from ever picking up classes from a
rem source version that has since moved on.
if exist build rmdir /s /q build

if "%JAR%"=="" (
  where flixw.cmd >nul 2>nul && (
    for /f "tokens=1,*" %%A in ('flixw.cmd info 2^>nul') do (
      if "%%A"=="jar" set "JAR=%%B"
    )
  )
)

if "%JAR%"=="" if exist flix.jar set "JAR=flix.jar"

if "%JAR%"=="" (
  for /f "delims=" %%F in ('where flix 2^>nul') do (
    if "!JAR!"=="" set "JAR=%%F"
  )
)

if "%JAR%"=="" (
  echo error: no Flix compiler jar found ^(tried %%FLIX_JAR%%, flixw.cmd info, .\flix.jar, and 'flix' on %%PATH%%^) 1>&2
  echo run flixw.cmd check once to download and verify it, place a jar at .\flix.jar, or pass --flix-jar 1>&2
  exit /b 1
)
if not exist "%JAR%" (
  echo error: Flix compiler jar not found at '%JAR%' 1>&2
  exit /b 1
)

scala-cli run scripts\edit-resistance.scala --jvm 21 --jar "%JAR%" -- %ARGS%
