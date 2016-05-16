<?cs # The default API filter selector that goes in the header ?><?cs
def:default_api_filter() ?>
  <div id="api-level-toggle">
    <input type="checkbox" id="apiLevelCheckbox" onclick="toggleApiLevelSelector(this)" />
    <label for="apiLevelCheckbox" class="disabled">Filter by API Level: </label>
    <select id="apiLevelSelector">
      <!-- option elements added by buildApiLevelSelector() -->
    </select>
  </div>
  <script>
    var SINCE_DATA = [ <?cs 
      each:since = since ?>'<?cs 
        var:since.name ?>'<?cs 
        if:!last(since) ?>, <?cs /if ?><?cs
      /each 
    ?> ];
    buildApiLevelSelector();
  </script>
<?cs /def ?>