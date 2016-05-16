<html>
<head>

<?php
	$parts = explode("/", getcwd());
	$parts2 = explode("-", $parts[count($parts) - 1]);
	$buildName = $parts2[1];

	// Get build type names

	$fileHandle = fopen("../../dlconfig.txt", "r");
	while (!feof($fileHandle)) {

		$aLine = fgets($fileHandle, 4096); // Length parameter only optional after 4.2.0
		$parts = explode(",", $aLine);
		$dropNames[trim($parts[0])] = trim($parts[1]);
 	}
	fclose($fileHandle);

	$buildType = $dropNames[$parts2[0]];

	echo "<title>Test Results for $buildType $buildName </title>";
?>

<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="http://dev.eclipse.org/default_style.css" type="text/css">
</head>
<body>
<p><b><font face="Verdana" size="+3">Test Results</font></b> </p>
<table border=0 cellspacing=5 cellpadding=2 width="100%" >
  <tr>
    <td align=LEFT valign=TOP colspan="3" bgcolor="#0080C0"><b><font color="#FFFFFF" face="Arial,Helvetica">Unit
      Test Results for <?php echo "$buildType $buildName"; ?> </font></b></td>
  </tr>
</table>
<p></p><table border="0">
</table>

<table width="77%" border="1">
  <tr>
    <td width="81%"><b>Tests Performed</b></td>
    <td width="19%"><b>Errors &amp; Failures</b></td>
  </tr>

  %testresults%

</table>
<p></p>
<br>
<table border=0 cellspacing=5 cellpadding=2 width="100%" >
  <tr>
    <td align=LEFT valign=TOP colspan="3" bgcolor="#0080C0"><b><font color="#FFFFFF" face="Arial,Helvetica">
      Console output logs
      <?php echo "$buildType $buildName"; ?>
      </font></b></td>
  </tr>
</table>
<br>
These <a href="consoleLogs.php">logs</a> contain the console output captured while
running the JUnit automated tests. <br>
<br>
<table border=0 cellspacing=5 cellpadding=2 width="100%" >
  <tr>
    <td align=LEFT valign=TOP colspan="3" bgcolor="#0080C0"><b><font color="#FFFFFF" face="Arial,Helvetica">Plugins
      containing compile errors </font></b></td>
  </tr>
</table>

<table width="77%" border="1">
  <tr>
    <td><b>Compile Logs (Jar Files)</b></td>
    <td><b>Errors</b></td>
    <td><b>Warnings</b></td>
  </tr>

  %compilelogs%

</table>

<table width="77%" border="1">
  <tr>
    <td><b>Compile Logs (Jar Files)</b></td>
    <td><b>Forbidden Access Warnings</b></td>
    <td><b>Discouraged Access Warnings</b></td>
  </tr>

  %accesseslogs%

</table>

</body>
</html>
