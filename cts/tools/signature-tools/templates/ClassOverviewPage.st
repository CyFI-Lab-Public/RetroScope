<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Frameset//EN""http://www.w3.org/TR/REC-html40/frameset.dtd">
<html>
<head>
$Common_meta()$
<title>
$class_delta.to.packageName$.$class_delta.to.name$
</title>
</head>
<body>

<div class="body">

$Header()$

<div class="content">
<h2>Class $class_delta.to.packageName$.$class_delta.to:ClassReferenceLink()$</h2>
<br/>

$if(has_class_signature_delta)$
<table summary="Signature Difference" border="1" width="100%">
  <tr>
    <th colspan=3>Signature Difference</th>
  </tr>
  <tbody>
    <tr class="default">
      <td>$SigClassDefinition(element=class_delta.to)$</td>
      <td>
        $if(class_delta.annotationDeltas)$   $MultiSubDelta(desc="Annotations Changed:", deltas=class_delta.annotationDeltas)$      $endif$
        $if(class_delta.modifierDeltas)$     $MultiSubDelta(desc="Modifiers Changed:", deltas=class_delta.modifierDeltas)$          $endif$
        $if(class_delta.typeVariableDeltas)$ $MultiSubDelta(desc="Type Variables Changed:", deltas=class_delta.typeVariableDeltas)$ $endif$
        $if(class_delta.superClassDelta)$    $SingleSubDelta(desc="Superclass Changed", delta=class_delta.superClassDelta)$               $endif$
        $if(class_delta.interfaceDeltas)$    $MultiSubDelta(desc="Interfaces Changed:", deltas=class_delta.interfaceDeltas)$        $endif$
      </td>
    </tr>
  </tbody>
</table>
<br/>
$endif$

$! Annotation Fields !$

$if(removed_annotation_fields)$
$Table(name="Removed Annotation Fields", rows=removed_annotation_fields:{$it.from:ModelElementRow()$})$
<br/>
$endif$

$if(added_annotation_fields)$
$Table(name="Added Annotation Fields", rows=added_annotation_fields:{$it.to:ModelElementRow()$})$
<br/>
$endif$

$if(changed_annotation_fields)$
$Table(name="Changed Annotation Fields", rows=changed_annotation_fields:DeltaTableRow())$
<br/>
$endif$


$! --------Enum Constants-------- !$

$if(removed_enum_constants)$
$Table(name="Removed Enum Constants", rows=removed_enum_constants:{$it.from:ModelElementRow()$})$
<br/>
$endif$

$if(added_enum_constants)$
$Table(name="Added Enum Constants", rows=added_enum_constants:{$it.to:ModelElementRow()$})$
<br/>
$endif$

$if(changed_enum_constants)$
$Table(name="Changed Enum Constants", rows=changed_enum_constants:DeltaTableRow())$
<br/>
$endif$


$! Fields !$

$if(removed_fields)$
$Table(name="Removed Fields", rows=removed_fields:{$it.from:ModelElementRow()$})$
<br/>
$endif$

$if(added_fields)$
$Table(name="Added Fields", rows=added_fields:{$it.to:ModelElementRow()$})$
<br/>
$endif$

$if(changed_fields)$
$Table(name="Changed Fields", rows=changed_fields:DeltaTableRow())$
<br/>
$endif$


$! Constructors !$

$if(removed_constructors)$
$Table(name="Removed Constructors", rows=removed_constructors:{$it.from:ModelElementRow()$})$
<br/>
$endif$

$if(added_constructors)$
$Table(name="Added Constructors", rows=added_constructors:{$it.to:ModelElementRow()$})$
<br/>
$endif$

$if(changed_constructors)$
$Table(name="Changed Constructors", rows=changed_constructors:DeltaTableRow())$
<br/>
$endif$


$! Methods !$

$if(removed_methods)$
$Table(name="Removed Methods", rows=removed_methods:{$it.from:ModelElementRow()$})$
<br/>
$endif$

$if(added_methods)$
$Table(name="Added Methods", rows=added_methods:{$it.to:ModelElementRow()$})$
<br/>
$endif$

$if(changed_methods)$
$Table(name="Changed Methods", rows=changed_methods:DeltaTableRow())$
<br/>
$endif$
</div>
</div>
</body>
</html>