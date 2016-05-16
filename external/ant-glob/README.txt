Ant Glob support

Apache Ant
Copyright 1999-2012 The Apache Software Foundation
-------------------------------------

This project is a fork of a subset of Ant 1.8.3:
http://ant.apache.com/

Specifically, it contains the subset of Ant related to matching glob patterns
to paths

The fork was modified as follows:
* Started with version 1.8.3

* Extracted
    org.apache.tools.ant.types.selectors.SelectorUtils
  and then everything it transitively references that was truly needed:
    org.apache.tools.ant.util.FileUtils,
    org.apache.tools.ant.taskdefs.condition.Condition,
    org.apache.tools.ant.taskdefs.condition.OS,
    org.apache.tools.ant.BuildException

* FileUtils was pruned to keep only 2 methods, which brought OS (which
  brought Condition). In turn this brought in BuildException which was trimmed
  up a bit (removing references to Location)

