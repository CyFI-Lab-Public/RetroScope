#!/bin/sh
#
# Start-up script for Retrace -- companion tool for ProGuard, free class file
# shrinker, optimizer, obfuscator, and preverifier for Java bytecode.

PROGUARD_HOME=`dirname "$0"`
PROGUARD_HOME=`dirname "$PROGUARD_HOME"`

java -jar $PROGUARD_HOME/lib/retrace.jar "$@"
