#!/usr/bin/php

<?php
/*
RCS file: /cvsroot/tools/org.eclipse.emf/.cvsignore,v //store
Working file: .cvsignore //don't need to store, basename(rcs file), minus ,v?
head: 1.1 //maybe store, could be calculated based on tags table?
branch: //probably store, what does this actually mean?, never used (always empty)?
locks: strict //store, though not used (always strict)?
access list: //store, though not used (always empty)?
symbolic names: //do we actually need to store these? we'll see
        build_200608030000: 1.1
	...
keyword substitution: kv //store, 'b', 'k', 'kv', 'kvl', 'v' are (currently) used
total revisions: 2;     selected revisions: 2 //don't store, is count of commits to the file
description: //is this a title for the revisions? probably
----------------------------
revision 1.1
date: 2004/03/06 18:22:28;  author: marcelop;  state: Exp;
branches:  1.1.2;
Move the EMF, XSD and SDO source code to the Eclipse.org repository
----------------------------
revision 1.1.2.1
date: 2005/06/02 16:09:17;  author: nickb;  state: Exp;  lines: +0 -0
*** empty log message ***
=============================================================================
*/

$db = "modeling"; /* database name. change this to match your project or can leave as is */

$perfile_regex = "/^RCS\ file:\ (.+?$)\\n
	^Working\ file:\ (.+?$)\\n
	^head:\ (.+?$)\\n
	^branch:\ ?(.*?$)\\n
	^locks:\ (.+?$)\\n
	^access\ list:\ ?(.*?$)\\n
	(?:^symbolic\ names:\ ?(.*?$)\\n
	((?:^\\t\S+:\ [0-9\.]+$\\n)+))?
	^keyword\ substitution:\ (.+?$)\\n
	^total\ revisions:\ (\d+);\\tselected\ revisions:\ (\d+)$\\n
	^description:\ ?(.*?$)\\n
	^((?:\-{28}.+$\\n)?
	^={77})$/smx";

$percommit_regex = "#^\-{28}$\\n
	^revision\ ([0-9\.]+)$\\n
	^date:\ (\d{4}/\d\d/\d\d\ \d\d:\d\d:\d\d);\ \ author:\ (\S+);\ \ state:\ (\S+);(?:\ \ lines:\ \+(\d+)\ \-(\d+))?$\\n
	(?:^branches:(?:\ \ ([0-9\.]+);)+$\\n)?
	^(.+?)$\\n
	^(?:\-{28}|={77})#smx";

$bugs_regex = "@(?:
        \[\#?(\d+)\]
        |
        (?:Bugzilla)?\#(\d+)
        |
        https?\Q://bugs.eclipse.org/bugs/show_bug.cgi?id=\E(\d+)
        )@x";

include_once "includes/parsecvs-dbaccess.php";
$connect = mysql_connect($dbhost, $dbuser, $dbpass) or die("Couldn't connect to database!\n");
mysql_select_db($db, $connect) or die(mysql_error());
$file = file_get_contents(($argv[1] ? $argv[1] : "php://stdin"));

wmysql_query("CREATE TEMPORARY TABLE `tmptags` (`tagname` VARBINARY(255), `revision` VARCHAR(20), PRIMARY KEY (`tagname`)) ENGINE = memory");

preg_match_all("/^(RCS file:.+?^={77}$)/sm", $file, $regs) or die("Couldn't find any cvs logs!\n");
foreach ($regs[0] as $z)
{
	/* parse each file's info */
	if (preg_match($perfile_regex, $z, $props))
	{
		$esc = array(1, 3, 9);
		foreach ($esc as $y)
		{
			$props[$y] = mysql_real_escape_string($props[$y], $connect);
		}
		preg_match("/^\/cvsroot\/[^\/]+\/([^\/]+)\//", $props[1], $proj);
		$q = "`project` = '$proj[1]', `head` = '$props[3]', `keyword_subs` = '$props[9]'";
		wmysql_query("INSERT INTO `cvsfiles` SET `cvsname` = '$props[1]', $q ON DUPLICATE KEY UPDATE $q");
		/* mysql_insert_id() won't work if we updated rather than inserted */
		$result = wmysql_query("SELECT `fid` FROM `cvsfiles` WHERE `cvsname` = '$props[1]'");
		$row = mysql_fetch_row($result);

		/* parse symbolic names */
		$tags = array();
		$filetags = array();
		$count = preg_match_all("/^\t(\S+): ([0-9\.]+)$\n/m", $props[8], $syms);
		for ($i=0;$i<$count;$i++)
		{
			array_push($filetags, "('{$syms[1][$i]}', '{$syms[2][$i]}')");
		}
		if ($count > 0)
		{
			$syms[1] = preg_replace("/^(.+)$/e", "fixup('$1')", $syms[1]);
			wmysql_query("INSERT INTO `tags` (`tagname`, `tagdate`) VALUES " . join($syms[1], ",") . " ON DUPLICATE KEY UPDATE `tid` = `tid`");
			wmysql_query("INSERT INTO `tmptags` (`tagname`, `revision`) VALUES " . join($filetags, ","));
			wmysql_query("INSERT INTO `filetags` SELECT $row[0], `tid`, `revision` FROM `tmptags` NATURAL JOIN `tags` ON DUPLICATE KEY UPDATE `filetags`.`revision` = `tmptags`.`revision`");
			wmysql_query("TRUNCATE TABLE `tmptags`");
		}

		$commits = $props[13];
		/* parse commits */
		while (preg_match($percommit_regex, $commits, $revs))
		{
			$commits = substr($commits, strlen($revs[0]) - 28); //leave the \-{28} in tact
			$revs[8] = mysql_real_escape_string($revs[8], $connect);
			$q = "`date` = STR_TO_DATE('$revs[2]', '%Y/%m/%d %T'), `author` = '$revs[3]', `state` = '$revs[4]', `linesplus` = '$revs[5]', `linesminus` = '$revs[6]', `message` = '$revs[8]'";
			wmysql_query("INSERT INTO `commits` SET `fid` = '$row[0]', `revision` = '$revs[1]', $q ON DUPLICATE KEY UPDATE $q");

			/* parse bug numbers */
			if (preg_match_all($bugs_regex, $revs[8], $ubugs))
			{
				unset($ubugs[0]);
				$bugs = extract_bugs($ubugs);
				$bugs = preg_replace("/^(.+)$/", "('$row[0]', '$revs[1]', '$1')", $bugs);
				wmysql_query("INSERT INTO `bugs` (`fid`, `revision`, `bugid`) VALUES " . join($bugs, ",") . " ON DUPLICATE KEY UPDATE `bugid` = `bugid`");
			}
		}
	}
}
wmysql_query("DROP TEMPORARY TABLE `tmptags`");

$tables = array();
mysql_select_db("INFORMATION_SCHEMA") or die(mysql_error());
$result = wmysql_query("SELECT TABLE_NAME FROM TABLES WHERE `TABLE_SCHEMA` = '$db' AND `TABLE_TYPE` = 'BASE TABLE'");
while ($row = mysql_fetch_row($result))
{
	array_push($tables, $row[0]);
}

mysql_select_db($db) or die(mysql_error());
wmysql_query("OPTIMIZE TABLE " . join($tables, ","));
wmysql_query("ANALYZE TABLE " . join($tables, ","));
mysql_close($connect);

function fixup($str)
{
	return "('$str', " . (preg_match("/^build_(\d{12})$/", $str, $regs) ? "STR_TO_DATE('$regs[1]', '%Y%m%d%k%i')" : "NULL") . ")";
}

function wmysql_query($sql)
{
	$res = mysql_query($sql) or die("$sql\n" . mysql_error() . "\n");
	return $res;
}

function extract_bugs($regs)
{
        foreach ($regs as $z)
        {
                foreach ($z as $y)
                {
                        if (preg_match("/^\d+$/", $y))
                        {
                                $bugs[] = $y;
                        }
                }
        }

        return $bugs;
}
?>
