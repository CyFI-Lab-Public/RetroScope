<?php
require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/app.class.php"); require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/nav.class.php"); require_once($_SERVER['DOCUMENT_ROOT'] . "/eclipse.org-common/system/menu.class.php"); $App = new App(); $Nav = new Nav(); $Menu = new Menu(); include($App->getProjectCommon());
ob_start();

include("includes/db.php");

$pagesize = 25; //results per page
$scroll = 5; //+- pages to show in nav
$days = 7;
$page = (preg_match("/^\d+$/", $_GET["p"]) ? $_GET["p"] : 1);
$offset = ($page - 1) * $pagesize;

$where = "WHERE `date` >= DATE_SUB(CURDATE(), INTERVAL $days DAY)";
$order = "ORDER BY `date` DESC";

$extraf = array(
	array("regex" => "/author: ?(\S+)/", "sql" => "`author` LIKE '%%%s%%'", "sqlpart" => "where"),
	array("regex" => "/file: ?(\S+)/", "sql" => "`cvsname` LIKE '%%%s%%'", "sqlpart" => "where"),
	array("regex" => "/days: ?(\d+)/", "sql" => "`date` >= DATE_SUB(CURDATE(), INTERVAL %d DAY)", "sqlpart" => "where"),
	array("regex" => "/(?:project|module): ?(\S+)/", "sql" => "`project` LIKE '%s'", "sqlpart" => "where"),
	array("regex" => "/branch: ?(\S+)/", "sql" => "`branch` LIKE '%%%s%%'", "sqlpart" => "having") //is a calculated value, won't work in WHERE
);

$q = (get_magic_quotes_gpc() ? $_GET["q"] : addslashes($_GET["q"]));
$extra = array("where" => array(), "having" => array());
foreach ($extraf as $z)
{
	while (preg_match($z["regex"], $q, $regs))
	{
		array_push($extra[$z["sqlpart"]], sprintf($z["sql"], $regs[1]));
		$q = preg_replace($z["regex"], "", $q);
	}
}

$regs = array();
/* this *could* be put into $extraf, but it would change the semantics slightly, in that any number searched for would be treated as a bug #, which i think is undesirable */
if (preg_match("/^\s*\[?(\d+)\]?\s*$/", $_GET["q"], $regs))
{
	$_GET["q"] = $regs[1]; 
	$where = "WHERE `bugid` = $regs[1]";
	$et = "Bug #";
}
else if (preg_match("/(\S)/", $q, $regs) || sizeof($extra["where"]) + sizeof($extra["having"]) > 0)
{
	$match = "'1'";
	if (sizeof($regs) > 0)
	{
		$match = "MATCH(`message`) AGAINST('$q'" . (preg_match("/\".+\"/", $q) ? " IN BOOLEAN MODE" : "") . ")";
	}
	$where = "WHERE " . ($match ? $match : "1");
	$where .= (sizeof($extra["where"]) > 0 ? " AND " . join($extra["where"], " AND ") : "");
	$having = (sizeof($extra["having"]) > 0 ? " HAVING " . join($extra["having"], " AND ") : "");
	$ec = ", $match AS `relevance`";
	$order = "ORDER BY `relevance` DESC, `date` DESC";
}
?>
<div id="midcolumn">
<div class="homeitem3col">
	<h3>Search</h3>
	<div id="searchdiv">
		<form action="" method="get">
			<input type="text" size="60" id="q" name="q"<?php print ($_GET["q"] ? " value=\"" . sanitize($_GET["q"], "text") . "\"" : ""); ?>/>
			<input type="submit" value="Go!"/>
		</form>
	</div>
</div>
<?php

/* 1.1.2.x <- 1.1.0.2 = branch tag, likewise, 1.1.4.x <- 1.1.0.4 = branch tag, so dynamically rewrite a.b.c.d to a.b.0.c to find the branch tag */
$branch = "IF(`revision` LIKE '%.%.%.%', (SELECT `tagname` FROM `tags` NATURAL JOIN `filetags` WHERE `fid` = `ofid` AND `revision` = CONCAT(SUBSTRING_INDEX(`orev`, '.', 2), '.0.', SUBSTRING_INDEX(SUBSTRING_INDEX(`orev`, '.', -2), '.', 1))), 'HEAD')";
$sql = "SELECT SQL_CALC_FOUND_ROWS `cvsname`, `revision`, `date`, `author`, `message`, `keyword_subs`, `bugid`, `revision` AS `orev`, `fid` AS `ofid`, $branch AS `branch`$ec FROM `cvsfiles` NATURAL JOIN `commits` NATURAL LEFT JOIN `bugs` $where GROUP BY `fid`, `revision`, `bugid` $having $order LIMIT $offset, $pagesize";
$result = wmysql_query($sql);

$count = wmysql_query("SELECT FOUND_ROWS()"); //mysql_num_rows() doesn't do what we want here
$row = mysql_fetch_row($count);
$rows = $row[0];

$title = "<span>$rows results total</span>Showing results " . ($offset + 1) . "-" . ($offset + $pagesize > $rows ? $rows : $offset + $pagesize) . " for " . ($_GET["q"] == "" ? "last $days days of commits" : "$et" . sanitize($_GET["q"], "text"));
$title = ($rows == 0 ? "No results found for " . sanitize($_GET["q"], "text") . "" : $title);

print "<div class=\"homeitem3col\">\n";
print "<h3>$title</h3>\n";

dopager($rows, $page, $pagesize);

print "<ul>\n";

while ($row = mysql_fetch_assoc($result))
{
	$file = basename($row["cvsname"], ",v");
	$row["cvsname"] = preg_replace("#^/cvsroot/[^\/]+/(.+),v$#", "$1", $row["cvsname"]);
	print "<li>\n";
	print "<div>{$row['date']}</div>";
	print ($row["bugid"] ? "[<a href=\"https://bugs.eclipse.org/bugs/show_bug.cgi?id={$row['bugid']}\">{$row['bugid']}</a>] " : "");
	print "<a href=\"" . cvsfile($row["cvsname"]) . "\"><abbr title=\"{$row['cvsname']}\">$file</abbr></a> ({$row['branch']} " . showrev($row['revision'], $row["cvsname"]) . ")";
	print "<ul>\n";
	print "<li><div>{$row['author']}</div>" . pretty_comment($row["message"], $q) . "</li>";
	print "</ul>\n";
	print "</li>\n";
}
print "</ul>\n";

dopager($rows, $page, $pagesize);

print "</div>\n";
print "</div>\n";
mysql_close($connect);
?>
<div id="rightcolumn">
	<div class="sideitem">
		<h6>Help</h6>
		<p><a href="http://wiki.eclipse.org/index.php/Search_CVS">Consult the wiki</a>, or try these examples:</p>
		<ul>
			<li><a href="?q=%5B155286%5D">[155286]</a></li>
			<li><a href="?q=98877+file%3A+ChangeAdapter">98877 file: ChangeAdapter</a></li>
			<li><a href="?q=file%3A+org.eclipse.emf%2F+days%3A+7">file: org.eclipse.emf/ days: 7</a></li>
			<li><a href="?q=days%3A200+author%3Amerks">days:200 author:merks</a></li>
			<li><a href="?q=branch%3A+R2_1_+file%3A+.xml">branch: R2_1_ file: .xml</a></li>
			<li><a href="?q=static+dynamic+project%3A+org.eclipse.emf">static dynamic project: org.eclipse.emf</a></li>
			<li><a href="?q=%22package+protected%22">"package protected"</a></li>
			<li><a href="?q=Neil+Skrypuch">Neil Skrypuch</a></li>
		</ul>
        <p>See also the complete <a href="http://wiki.eclipse.org/index.php/Search_CVS#Parameter_List">Parameter List</a>.</p> 
	</div>
</div>
<?php
$html = ob_get_contents();
ob_end_clean();

$pageTitle = "Eclipse Tools - Search CVS";
$pageKeywords = ""; 
$pageAuthor = "Neil Skrypuch";

$App->AddExtraHtmlHeader('<link rel="stylesheet" type="text/css" href="/emf/includes/searchcvs.css"/>' . "\n");
if (!isset($_GET["totalonly"]))
{
	ob_start();
	$App->generatePage($theme, $Menu, $Nav, $pageAuthor, $pageKeywords, $pageTitle, $html);
	$html = ob_get_contents();
	ob_end_clean();
	print preg_replace("/<body>/", "<body onload=\"document.getElementById('q').focus()\">", $html);
}
else
{
	header("Content-Type: text/plain");
	print $rows;
}

function pretty_comment($str, $hl)
{
	$str = preg_replace("/\n/", "<br/>", $str);
	$hl = words($hl);

	for ($i = 0; $i < sizeof($hl); $i++)
	{
		$str = preg_replace("/\b(\Q$hl[$i]\E)\b([^=]|\Z)/i", "<span class=\"hl$i\">$1</span>$2", $str);
	}

	$str = preg_replace("/^(\Q*** empty log message ***\E)$/", "<span class=\"empty\">$1</span>", $str);

	return $str;
}

function cvsminus($rev)
{
	if (preg_match("/^1\.1$/", $rev)) // "1.10" == "1.1" returns true, curiously enough
	{
		return $rev;
	}
	else
	{
		if (preg_match("/\.1$/", $rev))
		{
			return preg_replace("/^(\d+\.\d+)\..+$/", "$1", $rev);
		}
		else
		{
			return preg_replace("/^(.+\.)(\d+)$/e", "\"$1\" . ($2 - 1);", $rev);
		}
	}
}

function showrev($rev, $file)
{
	$link = "<a href=\"" . cvsfile($file) . "\">$rev</a>";
	if (!preg_match("/^1\.1$/", $rev)) // "1.10" == "1.1" returns true, curiously enough
	{
		$oldrev = cvsminus($rev);
		$link = "<a href=\"" . cvsfile($file, $rev, $oldrev) . "\">$rev &gt; $oldrev</a>";
	}

        return $link;
}

function cvsfile($file, $rev = "", $oldrev = "")
{
	if ($rev && $oldrev)
	{
		$ext = ".diff";
		$params = "r1=$oldrev&amp;r2=$rev&amp;";
	}
	$params .= (preg_match("/\.php$/", $file) && $ext != ".diff" ? "content-type=text/plain&amp;" : "");

	if (preg_match("/^www/", $file))
	{
		return "http://dev.eclipse.org/viewcvs/index.cgi/~checkout~/$file$ext?${params}cvsroot=Eclipse_Website";
	}
	else
	{
		return "http://dev.eclipse.org/viewcvs/indextools.cgi/~checkout~/$file$ext?$params";
	}
}

function sanitize($str, $type = "url")
{
	$tmp = urlencode(urldecode((get_magic_quotes_gpc() ? stripslashes($str) : $str)));
	return ($type == "url" ? $tmp : htmlspecialchars(urldecode($tmp)));
}

function pagelink($page, $selected, $linktext = "")
{
	$innertext = ($linktext ? $linktext : $page);
	$text = (!$selected ? "<a href=\"?q=" . sanitize($_GET["q"]) . "&amp;p=$page\">$innertext</a>" : $innertext);
	return "<span" . ($selected ? " class=\"selected\"" : "") . ">$text</span>";
}

function dopager($rows, $page, $pagesize)
{
	$startpage = ($page - 5 < 1 ? 1 : $page - 5);
	$endpage = ($page + 5 > $rows/$pagesize ? ceil($rows/$pagesize) : $page + 5);

	if ($rows > 0)
	{
		print "<div class=\"pager\">\n";
		print ($page > 1 ? pagelink($page - 1, false, "Previous") : "");
		for ($i = $startpage; $i <= $endpage; $i++)
		{
			print pagelink($i, $i == $page);
		}
		print ($page < ceil($rows/$pagesize) ? pagelink($page + 1, false, "Next") : "");
		print "</div>\n";
	}
}

function words($str)
{
	$str = stripslashes($str);
        $list = array();

        preg_match_all("/\"([^\"]+)\"/", $str, $regs);
        foreach ($regs[1] as $word)
        {
		$word = addslashes($word);
                $list[] = $word;
                $str = preg_replace("/\Q$word\E/", "", $str);
        }

        $regs = null;
        preg_match_all("/(\w+)/", $str, $regs);
        foreach ($regs[1] as $word)
        {
                $list[] = addslashes($word);
        }

        return $list;
}
?>
