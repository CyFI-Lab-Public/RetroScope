@ECHO OFF

REM Start-up script for ProGuard -- free class file shrinker, optimizer,
REM obfuscator, and preverifier for Java bytecode.

IF EXIST "%PROGUARD_HOME%" GOTO home
SET PROGUARD_HOME=..
:home

java -jar "%PROGUARD_HOME%"\lib\proguard.jar %*
