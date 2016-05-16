package com.google.android.apps.common.testing.ui.testapp;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

/**
 * An activity displaying a long list.
 */
public class LongListActivity extends Activity {
  public static final String STR = "STR";
  public static final String LEN = "LEN";
  private List<Map<String, Object>> data = Lists.newArrayList();

  @Override
  public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    populateData();
    setContentView(R.layout.list_activity);
    ((TextView) findViewById(R.id.selection_pos)).setText("");
    ((TextView) findViewById(R.id.selection_class)).setText("");

    ListView listView = (ListView) findViewById(R.id.list);
    String[] from = new String[] {STR, LEN};
    int[] to = new int[] {R.id.item_content, R.id.item_size};

    listView.setAdapter(new SimpleAdapter(this, data, R.layout.list_item, from, to));
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(
          AdapterView<?> unusedParent, View clickedView, int position, long id) {
        ((TextView) findViewById(R.id.selection_pos)).setText(String.valueOf(position));
        ((TextView) findViewById(R.id.selection_class)).setText(
            clickedView.getClass().getSimpleName());
      }
    });
  }

  private void populateData() {
    for (int i = 0; i < 100; i++) {
      Map<String, Object> dataRow = Maps.newHashMap();
      dataRow.put(STR, "item: " + i);
      dataRow.put(LEN, ((String) dataRow.get(STR)).length());
      data.add(dataRow);
    }
  }

}
