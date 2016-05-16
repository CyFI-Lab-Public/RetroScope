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
export ECLIPSE_HOME=/home/eclipse/eclipse;

#ant with Eclipse - requires org.eclipse.ant.ui*.jar
tmpfolder="/tmp/antrunner-"`date +%Y%m%d_%H%M%S`;
for f in `find $ECLIPSE_HOME/plugins -maxdepth 1 -name "org.eclipse.ant.ui*.jar" -type f`; do unzip $f lib/*.jar -d $tmpfolder; done

CLASSPATH="$JAVA_HOME/lib/rt.jar";
for d in "$ANT_HOME/lib" "$tmpfolder/lib"; do
  # note that com.sun.org.apache.xerces (Sun JDK 1.5, rt.jar) != org.apache.xerces (Ant 1.6.5, xercesImpl.jar) so must remove from classpath
  for f in `find $d  -maxdepth 1 -name "*.jar" -type f -not -name "xercesImpl.jar"`; do CLASSPATH=$CLASSPATH":"$f; done
done

cmd="$JAVA_HOME/bin/java \
  -Dant.home=$ANT_HOME \
  -Dant.library.dir=$ANT_HOME/lib \
  -classpath $CLASSPATH \
  org.eclipse.ant.internal.ui.antsupport.InternalAntRunner \
  -buildfile feedManipulation.xml";

echo "Running ...";

#prettyprint the command
echo $cmd | sed -e 's/ \-/\n  \-/g' -e 's/:/\n    :/g'; echo "";

# run the command
$cmd;

rm -fr $tmpfolder;
