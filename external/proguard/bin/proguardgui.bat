@ECHO OFF

REM Start-up script for the GUI of ProGuard -- free class file shrinker,
REM optimizer, obfuscator, and preverifier for Java bytecode.

IF EXIST "%PROGUARD_HOME%" GOTO home
SET PROGUARD_HOME=..
:home

java -jar "%PROGUARD_HOME%"\lib\proguardgui.jar %1 %2 %3 %4 %5 %6 %7 %8 %9
