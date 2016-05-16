UpdateBugStateTask is an ant task used for updating the status of 1 or 
more bugs that have been resolved with a particular build of any Eclipse 
project. It can also be used for updating a specified list of bugs which 
are all in a specific state.

Before you can use this tool, you must run the UpdateBugStateTask.sh script 
to generate a Bugzilla login session. This should take less than a minute, 
and is a one time thing (although it is possible that the Bugzilla server 
will clear the stored sessions, in this case simply run the 
UpdateBugStateTask.sh script again).

UpdateBugStateTask requires JDK 1.4 or greater.

See the sample UpdateBugStateTask.xml build script for a full list and 
description of the task options.
