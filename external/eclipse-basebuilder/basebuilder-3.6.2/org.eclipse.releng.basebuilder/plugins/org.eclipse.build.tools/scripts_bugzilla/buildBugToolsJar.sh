#!/bin/sh

export JAVA_HOME=/opt/sun-java2-5.0;
export ANT_HOME=/opt/apache-ant-1.6;
$ANT_HOME/bin/ant -f buildBugToolsJar.xml;
