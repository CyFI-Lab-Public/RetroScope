@ECHO OFF

REM Start-up script for Retrace -- companion tool for ProGuard, free class file
REM shrinker, optimizer, obfuscator, and preverifier for Java bytecode.

IF EXIST "%PROGUARD_HOME%" GOTO home
SET PROGUARD_HOME=..
:home

java -jar "%PROGUARD_HOME%"\lib\retrace.jar %1 %2 %3 %4 %5 %6 %7 %8 %9
