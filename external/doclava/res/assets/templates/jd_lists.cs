var JD_DATA = [
<?cs
each:page = docs.pages
?>
  { label:"<?cs var:page.label ?>", link:"<?cs var:page.link ?>",
    tags:[<?cs var:page.tags ?>], type:"<?cs var:page.type ?>" }<?cs if:!last(page) ?>,<?cs /if ?><?cs
/each ?>
];
