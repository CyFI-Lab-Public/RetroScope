Search CVS is a tool for converting cvs commits into a mysql database, which 
can be updated without worry of data duplication. The database is then searchable 
via a web page run on www.eclipse.org.

MySQL Server Quick Setup:

1. Extract the searchcvs/ folder from cvs and place it in your $HOME folder 
on some server running MySQL 5 and PHP 4 or 5 with the MySQL module for MySQL 5 compiled in. 

2. Customize setup.sh for your needs and run it to extract your code from cvs.

3. Load database tables using .dump files provided.

4. Edit includes/parsecvs-dbaccess.php. Run parsecvs.sh.

5. To make nightly updates to the data (reflecting the day's cvs commits), run parsecvs.sh as a cron like this:

#Min    Hr      Mday    Month   Wday    Cmd
00 22 * * * $HOME/searchcvs/parsecvs.sh 2>&1 1> $HOME/searchcvs/parsecvs.log.txt

----

Additional details on setup and on using this tool can be found here:

http://wiki.eclipse.org/index.php/Search_CVS
