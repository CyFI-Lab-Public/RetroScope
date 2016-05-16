#!/bin/sh

export JAVA_HOME=/opt/sun-java2-1.4;
export ANT_HOME=/opt/apache-ant-1.6;

#aasemble the command and its classpath
CLASSPATH="$JAVA_HOME/lib/rt.jar";
# note that com.sun.org.apache.xerces (Sun JDK 1.5, rt.jar) != org.apache.xerces (Ant 1.6.5, xercesImpl.jar) so must remove from classpath
#for f in `find $ANT_HOME/lib  -maxdepth 1 -name "*.jar" -type f -not -name "xercesImpl.jar"`; do CLASSPATH=$CLASSPATH":"$f; done
CLASSPATH=$CLASSPATH":"$ANT_HOME/lib/ant.jar":"$ANT_HOME/lib/ant-launcher.jar;
cmd="$JAVA_HOME/bin/java \
  -Dant.home=$ANT_HOME \
  -Dant.library.dir=$JAVA_HOME/lib \
  -classpath $CLASSPATH:../bugTools.jar \
  org.apache.tools.ant.launch.Launcher \
  -buildfile updateBugState.xml";

if [[ $debug -gt 0 ]]; then 
	echo "Running ..."; echo ""; echo $cmd | sed -e "s/ \-/#  \-/g" -e "s/:/#    :/g" | tr "#" "\n"; echo "";
fi

# run the command
$cmd;
