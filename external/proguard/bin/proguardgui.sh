#!/bin/sh
#
# Start-up script for the GUI of ProGuard -- free class file shrinker,
# optimizer, obfuscator, and preverifier for Java bytecode.

PROGUARD_HOME=`dirname "$0"`
PROGUARD_HOME=`dirname "$PROGUARD_HOME"`

java -jar $PROGUARD_HOME/lib/proguardgui.jar "$@"
