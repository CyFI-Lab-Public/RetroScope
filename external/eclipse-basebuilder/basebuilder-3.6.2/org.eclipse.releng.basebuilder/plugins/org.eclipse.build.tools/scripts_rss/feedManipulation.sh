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

export JAVA_HOME=/opt/sun-java2-5.0;
export ANT_HOME=/opt/apache-ant-1.6;

#ant standalone

CLASSPATH="$JAVA_HOME/lib/rt.jar";
# note that com.sun.org.apache.xerces (Sun JDK 1.5, rt.jar) != org.apache.xerces (Ant 1.6.5, xercesImpl.jar) so must remove from classpath
#for f in `find $ANT_HOME/lib  -maxdepth 1 -name "*.jar" -type f -not -name "xercesImpl.jar"`; do CLASSPATH=$CLASSPATH":"$f; done
CLASSPATH=$CLASSPATH":"$ANT_HOME/lib/ant.jar":"$ANT_HOME/lib/ant-launcher.jar;

cmd="$JAVA_HOME/bin/java \
  -Dant.home=$ANT_HOME \
  -Dant.library.dir=$JAVA_HOME/lib \
  -classpath $CLASSPATH \
  org.apache.tools.ant.launch.Launcher \
  -buildfile feedManipulation.xml";

echo "Running ..."; echo "";

#prettyprint the command
echo $cmd | sed -e 's/ \-/\n  \-/g' -e 's/:/\n    :/g'; echo "";

# run the command
$cmd;
