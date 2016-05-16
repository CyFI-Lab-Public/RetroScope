<html>
<head>
<title>Eclipse Download Click Through</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" href="http://dev.eclipse.org/default_style.css" type="text/css">
<?php
	$parts = explode("-", $dropFile);
	$clickFile = "clickThroughs/";
	for ($i =0; $i<count($parts); $i++) {
		if ($i != 2) {
			$clickFile = $clickFile.$parts[$i];
		    if ($i < count($parts) - 1) {
        	    $clickFile = $clickFile."-";
            }
 		}
	}
	$clickFile = $clickFile.".txt";

	if (file_exists($clickFile)) {
		$fileHandle = fopen($clickFile, "r");
		while (!feof($fileHandle)) {
			$aLine = fgets($fileHandle, 4096);
			$result = $result.$aLine;
		}
		fclose($fileHandle);
	} else {
		echo '<META HTTP-EQUIV="Refresh" CONTENT="0;URL='.$dropFile.'">';
		echo '<b><font size "+4">Downloading: '.$dropFile.'</font></b>';
		echo '<BR>';
		echo '<BR>';
		echo 'If your download does not begin automatically click <a href="'.$dropFile.'">here</a>.';
	}
?>
</head>

<body bgcolor="#FFFFFF" text="#000000">
  <?php
	if (file_exists($clickFile)) {
	 echo '<p><b><font size="+4">Important Notes<BR>';
	 echo $dropFile;
	 echo '</font></b></font></p>
	<p>It is very important to read the following notes in order to run this version 
	  of Eclipse. Once you have read the notes you can click on the Download link 
	  to download the drop.</p>
	';
	  echo '<textarea name="textfield" cols="80" rows="20" wrap="PHYSICAL">'.$result;
	  echo '</textarea>';
	  echo '<BR>';
	  echo '<BR>';
	  echo '<a href="'.$dropFile.'">Download</a>';
	}
?>
</body>
</html>
