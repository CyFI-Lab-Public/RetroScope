Eclipse basebuilder is used to build Eclipse plugins. These are only used during the build and are not released.
For more info, consult: http://wiki.eclipse.org/Platform-releng-basebuilder

--- /basebuilder-3.6.2 directory:

The contents of the basebuilder-3-6.2. directory were retrieved as follows:
$ cvs -d :pserver:anonymous@dev.eclipse.org/cvsroot/eclipse/ login
$ cvs -d :pserver:anonymous@dev.eclipse.org/cvsroot/eclipse/ checkout -r R3_6_2 org.eclipse.releng.basebuilder
$ find . -name "CVS" -exec rm -rf {} \;

--- /src directory:

To find the Eclipse sources matching basebuilder-3.6.2:
- open the Eclipse 3.6.2 download page:
  http://download.eclipse.org/eclipse/downloads/drops/R-3.6.2-201102101200/index.php
- fetch "Source Build (Source in .zip)" eclipse-sourceBuild-srcIncluded-3.6.2.zip [135 MB]

