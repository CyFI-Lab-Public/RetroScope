Search CVS is a tool for converting cvs commits into a mysql database, which 
can be updated without worry of data duplication. The database is then searchable 
via a web page run on www.eclipse.org.

This folder is a copy of the contents of another cvs repository for example purposes only,
and should not be considered up to date. It is provided here simply to make this code base 
more complete and to provide a self-contained runnable example. 

For the latest version of this folder's contents, see:

:pserver:anonymous@dev.eclipse.org:/cvsroot/org.eclipse/www/emf/
    searchcvs.php
    includes/searchcvs.css
    includes/db.php

Additionally, there is a non-public file called includes/searchcvs-dbaccess.php (referenced by
includes/db.php) which reads something like this:

<?php
    $dbhost = "mysqlserver";
    $dbuser = "dbaccessro";
    $dbpass = "dbaccessropassword";
?>

** WARNING: DO NOT COMMIT THIS FILE INTO YOUR PUBLIC CVS REPOSITORY! **

To put this file on the www.eclipse.org server without it being available in CVS for public view, 
place a copy of the file in your home directory on dev.eclipse.org, then open a bug 
(https://bugs.eclipse.org/bugs/enter_bug.cgi?product=Community) or send a note to webmaster@eclipse.org
and ask to have the file placed for you. Example: https://bugs.eclipse.org/bugs/show_bug.cgi?id=156451#c8

----

Additional details on setup and on using this tool can be found here:

http://wiki.eclipse.org/index.php/Search_CVS
