<!doctype html public "-//w3c//dtd html 4.0 frameset//en""http://www.w3.org/tr/rec-html40/frameset.dtd">
<html>
<head>
$Common_meta()$
<title>
Android API Differences Report
</title>
<body>

<div class="body">

$Header()$

<div class="content">

<h2>
Package $Link(url={http://code.google.com/android/reference/$package_delta.to.packageFragments; separator="/"$/package-summary.html}, name=package_delta.to.name)$
</h2>


$if(removed_classes)$
$Table(name="Removed Classes and Interfaces", rows=removed_classes:{$it.from:{<tt>$it.name$</tt>}$}:SimpleTableRow())$
<br/>
$endif$


$if(added_classes)$
$Table(name="Added Classes and Interfaces", rows=added_classes:{$it.to:ClassReferenceLink()$}:SimpleTableRow())$
<br/>
$endif$

$if(changed_classes)$
$Table(name="Changed Classes and Interfaces", rows=changed_classes:{$it.to:ClassChangedLink()$}:SimpleTableRow())$
<br/>
$endif$
</div>
</div>
</body>
</html>