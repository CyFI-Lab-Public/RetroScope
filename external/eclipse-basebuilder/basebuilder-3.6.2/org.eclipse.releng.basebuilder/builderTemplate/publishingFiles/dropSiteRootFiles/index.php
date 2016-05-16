<html><head>
<link rel="stylesheet" href="http://dev.eclipse.org/default_style.css">
<title>Eclipse Project Downloads</title></head>
<body>
<?php

        $serverName = $_SERVER["SERVER_NAME"];
		 
		 if (strstr($serverName, ".oti.com")) {
		 		 $warning = '<br><font color="#FF0000" size="+2">Internal OTI Mirror. Live external site is <a href="http://download.eclipse.org/eclipse/downloads" target="_top">here</a>. </font>';
                $serverName = $_SERVER["SERVER_NAME"];
		 } else {
		 		 $warning = '';
		 }
?> <table border=0 cellspacing=5 cellpadding=2 width="100%" > <tr> <td align=left width="72%"> 
<font class=indextop> &lt;your project&gt;<br>downloads</font> <br> <font class=indexsub> 
latest downloads from the &lt;your project</font>&gt;<br> <?php echo $warning; ?> 
</td><td width="28%"><img src="http://dev.eclipse.org/images/Idea.jpg" height=86 width=120></td></tr> 
</table><table border=0 cellspacing=5 cellpadding=2 width="100%" > <tr> <td align=LEFT valign=TOP colspan="2" bgcolor="#0080C0"><b><font color="#FFFFFF" face="Arial,Helvetica">Latest 
Downloads</font></b></td></tr> <!-- The Eclipse Projects --> <tr> <td> <p>On this 
page you can find the latest <a href="build_types.html">builds</a> produced by 
the &lt;your project&gt;. To get started run the program and and go through the 
user and developer documentation provided in the online help system. If you have 
problems downloading the drops, contact the <font size="-1" face="arial,helvetica,geneva"><a href="mailto:webmaster@eclipse.org">webmaster</a></font>. 
All downloads are provided under the terms and conditions of the <a href="http://www.eclipse.org/legal/notice.html">Eclipse.org 
Software User Agreement</a> unless otherwise specified. </p><p> For information 
about different kinds of builds look <a href="build_types.html">here</a>.</p></td></tr> 
</table><?php
		 
		 $fileHandle = fopen("dlconfig.txt", "r");
		 while (!feof($fileHandle)) {
		 		 
		 		 $aLine = fgets($fileHandle, 4096);
		 		 parse_str($aLine);

		 		 
		 }
		 fclose($fileHandle);
		 
		 for ($i = 0; $i < count($dropType); $i++) {
		 		 $typeToPrefix[$dropType[$i]] = $dropPrefix[$i];
		 }
		 
		 $aDirectory = dir("drops");
		 while ($anEntry = $aDirectory->read()) {

		 		 // Short cut because we know aDirectory only contains other directories.
		 		 if ($anEntry != "." && $anEntry!="..") {
		 		 		 $aDropDirectory = dir("drops/".$anEntry);
		 		 		 $fileCount = 0;
		 		 		 while ($aDropEntry = $aDropDirectory->read()) {
		 		 		 		 if ((stristr($aDropEntry, ".tar.gz"))||(stristr($aDropEntry, ".zip")))  {
		 		 		 		 		 // Count the files in the directory
		 		 		 		 		 $fileCount = $fileCount + 1;
		 		 		 		 }
		 		 		 }
		 		 		 $aDropDirectory.closedir();
		 		 		 // Read the count file
		 		 		 $countFile = "drops/".$anEntry."/files.count";
		 		 		 $indexFile = "drops/".$anEntry."/index.html";
		 		 		 if (file_exists($countFile) && file_exists($indexFile)) {
		 		 		 		 $anArray = file($countFile);
		 
		 		 		 		 // If a match - process the directory
		 		 		 		 if ($anArray[0] == $fileCount) {
		 		 		 		 		 $parts = explode("-", $anEntry);
		 		 		 		 		 if (count($parts) == 3) {
		 		 		 
		 		 		 		 		 		 $buckets[$parts[0]][] = $anEntry;
		 		 		 
		 		 		 		 		 		 $timePart = $parts[2];
		 		 		 		 		 		 $year = substr($timePart, 0, 4);
		 		 		 		 		 		 $month = substr($timePart, 4, 2);
		 		 		 		 		 		 $day = substr($timePart, 6, 2);
		 		 		 		 		 		 $hour = substr($timePart,8,2);
		 		 		 		 		 		 $minute = substr($timePart,10,2);
		 		 		 		 		 		 $timeStamp = mktime($hour, $minute, 0, $month, $day, $year);
		 		 		 		 		 
		 		 		 		 		 		 $timeStamps[$anEntry] = date("D, j M Y -- H:i (O)", $timeStamp);
		 		 		 
		 		 		 		 		 		 if ($timeStamp > $latestTimeStamp[$parts[0]]) {
		 		 		 		 		 		 		 $latestTimeStamp[$parts[0]] = $timeStamp;
		 		 		 		 		 		 		 $latestFile[$parts[0]] = $anEntry;
		 		 		 		 		 		 }
		 		 		 		 		 }
		 		 		 		 }
		 		 		 }
		 		 }
		 }
		 // $aDirectory.closedir();
 ?> <table width="100%" cellspacing=0 cellpadding=3 align=center> <td align=left> 
<TABLE  width="100%" CELLSPACING=0 CELLPADDING=3> <tr> <td width=\"30%\"><b>Build 
Type</b></td><td><b>Build Name</b></td><td><b>Build Date</b></td></tr> <?php
		 foreach($dropType as $value) {
		 		 $prefix=$typeToPrefix[$value];
		 		 $fileName = $latestFile[$prefix];
		 		 echo "<tr>
		 		 		 <td width=\"30%\">$value</td>";
		 		 
		 		 $parts = explode("-", $fileName);
		 		 
		 		 // Uncomment the line below if we need click through licenses.
		 		 // echo "<td><a href=license.php?license=drops/$fileName>$parts[1]</a></td>";
		 		 
		 		 // Comment the line below if we need click through licenses.
		 		 echo "<td><a href=\"drops/$fileName/index.html\">$parts[1]</a></td>";
		 		 
		 		 echo "<td>$timeStamps[$fileName]</td>";
		 		 echo "</tr>";
		 }
?> </table></table>&nbsp; <?php
		 foreach($dropType as $value) {
		 		 $prefix=$typeToPrefix[$value];
		 		 echo "
		 		 <table width=\"100%\" cellspacing=0 cellpadding=3 align=center>
		 		 <tr bgcolor=\"#999999\">
		 		 <td align=left width=\"30%\"><b><a name=\"$value\"><font color=\"#FFFFFF\" face=\"Arial,Helvetica\">$value";
		 		 echo "s</font></b></a></td>
		 		 </TR>
		 		 <TR>
		 		 <td align=left>
		 		 <TABLE  width=\"100%\" CELLSPACING=0 CELLPADDING=3>
		 		 <tr>
		 		 <td width=\"30%\"><b>Build Name</b></td>
		 		 <td><b>Build Date</b></td>
		 		 </tr>";
		 		 
		 		 $aBucket = $buckets[$prefix];
		 		 if (isset($aBucket)) {
		 		 		 rsort($aBucket);
		 		 		 foreach($aBucket as $innerValue) {
		 		 		 		 $parts = explode("-", $innerValue);
		 		 		 		 echo "<tr>";
		 		 		 		 
		 		 		 		 		 // Uncomment the line below if we need click through licenses.
		 		 		 		 		 // echo "<td><a href=\"license.php?license=drops/$innerValue\">$parts[1]</a></td>";
		 		 		 		 		 
		 		 		 		 		 // Comment the line below if we need click through licenses.
		 		 		 		 		 echo "<td><a href=\"drops/$innerValue/index.html\">$parts[1]</a></td>";

		 		 		 		 		 echo "<td>$timeStamps[$innerValue]</td>
		 		 		 		 		 </tr>";
		 		 		 }
		 		 }
		 		 echo "</table></table>&nbsp;";
		 }
?> &nbsp; 
</body></html>
