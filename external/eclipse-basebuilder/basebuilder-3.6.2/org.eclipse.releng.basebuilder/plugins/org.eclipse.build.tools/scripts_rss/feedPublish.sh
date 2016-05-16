#*******************************************************************************
# Copyright (c) 2005, 2006 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************
#!/bin/sh

###
### NOTE: this script is ONLY an example, and needs to be customized for your personal use. ###
###

export JAVA_HOME=/opt/sun-java2-5.0;
export ANT_HOME=/opt/apache-ant-1.6;
CLASSPATH="$JAVA_HOME/lib/rt.jar";
CLASSPATH=$CLASSPATH":"$ANT_HOME/lib/ant.jar":"$ANT_HOME/lib/ant-launcher.jar;
CLASSPATH=$CLASSPATH":"../feedTools.jar;

projectName="emf";

# define/override variables not set in properties file (can also be passed in querystring, etc.)
# this allows a static set of properties + some dynamic ones to be mixed in together
debug=2;
branch="HEAD"; # optional
version="2.2.1";
buildID="S200609210005";
buildAlias="2.2.1RC2";
dependencyURLs="http://www.eclipse.org/downloads/download.php?file=/eclipse/downloads/drops/M20060919-1045/eclipse-SDK-M20060919-1045-linux-gtk.tar.gz"; # comma-separated if more than one
# ...

cmd="$JAVA_HOME/bin/java -debug -Dant.home=$ANT_HOME -Dant.library.dir=$JAVA_HOME/lib -classpath $CLASSPATH org.apache.tools.ant.launch.Launcher"
cmd=$cmd" -buildfile feedPublish.xml -propertyfile ../properties/feedPublish.$projectName.properties"
cmd=$cmd" -Dbranch=$branch -Dversion=$version -DbuildID=$buildID -DbuildAlias=$buildAlias -DbuildType="${buildID:0:1};
cmd=$cmd" -DdependencyURLs=$dependencyURLs -Ddebug=$debug";
echo ""; echo $cmd | sed -e "s/ \-/#  \-/g" -e "s/.jar:/.jar#    :/g" | tr "#" "\n"; echo "";
$cmd;
