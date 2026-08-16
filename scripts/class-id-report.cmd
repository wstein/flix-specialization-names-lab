@echo off
rem Runs class-id-report.scala. See scripts/class-id-report (the POSIX
rem wrapper) for why this exists rather than running the .scala file
rem directly: every script under scripts/ is invoked by its extensionless
rem name, never by calling scala-cli on the .scala file itself.
rem
rem Usage:
rem   scripts\class-id-report.cmd [class-directory] [--output-dir directory] [--json]
setlocal
cd /d "%~dp0.."

scala-cli run scripts\class-id-report.scala -- %*
