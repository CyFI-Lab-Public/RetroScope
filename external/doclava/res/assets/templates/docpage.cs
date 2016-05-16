<?cs include:"doctype.cs" ?>
<?cs include:"macros.cs" ?>
<html>
<?cs include:"head_tag.cs" ?>
<body class="gc-documentation" itemscope itemtype="http://schema.org/Article">
<?cs include:"header.cs" ?>

<div class="g-unit" id="doc-content"><a name="top"></a>

<div id="jd-header" class="guide-header">
  <span class="crumb" itemprop="breadcrumb">
    <?cs if:parent.link ?>
      <a href="<?cs var:parent.link ?>"><?cs var:parent.title ?></a> >
    <?cs else ?>&nbsp;
    <?cs /if ?>
  </span>
<h1 itemprop="name"><?cs var:page.title ?></h1>
</div>

  <div id="jd-content">

    <div class="jd-descr" itemprop="articleBody">
    <?cs call:tag_list(root.descr) ?>
    </div>

  <a href="#top" style="float:right">&uarr; Go to top</a>
  <?cs if:parent.link ?>
    <p><a href="<?cs var:parent.link ?>">&larr; Back to <?cs var:parent.title ?></a></p>
  <?cs /if ?>
  </div>

<?cs include:"footer.cs" ?>
</div><!-- end doc-content -->

<?cs include:"trailer.cs" ?>

</body>
</html>



