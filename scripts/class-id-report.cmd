@echo off
rem Runs class-id-report.scala. See scripts/class-id-report (the POSIX
rem wrapper) for why this exists rather than running the .scala file
rem directly: every script under scripts/ is invoked by its extensionless
rem name, never by calling scala-cli on the .scala file itself.
rem
rem Usage:
rem   scripts\class-id-report.cmd [class-directory-or-jar] [--output-dir directory] [--json]
rem                                [--stable-width n]
rem
rem Deliberately does not `cd` anywhere: a class-directory-or-jar argument, or
rem --output-dir, is a path the caller gave relative to wherever they are, and
rem resolving it against this script's own directory instead -- as `cd`-ing
rem here first would -- silently censuses (or writes) the wrong thing when
rem this is invoked via a relative path from outside the project.
setlocal

scala-cli run "%~dp0class-id-report.scala" -- %*
