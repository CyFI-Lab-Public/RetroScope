<?cs def:custom_masthead() ?>
<div id="header">
    <div id="headerLeft">
      <?cs if:reference && reference.apilevels ?>
        <?cs call:default_api_filter() ?>
      <?cs /if ?>
    </div>
    <div id="headerRight">
        <?cs call:default_search_box() ?>
    </div><!-- headerRight -->
</div><!-- header -->
<?cs /def ?>