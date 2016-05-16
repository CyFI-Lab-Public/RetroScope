Query : <?cs var:query ?>

Results:
  <?cs each:result = results ?>
    <?cs var:result.title ?> ---> <?cs var:result.url ?>
  <?cs /each ?>
