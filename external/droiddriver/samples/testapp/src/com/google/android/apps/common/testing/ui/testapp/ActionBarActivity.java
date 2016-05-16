package com.google.android.apps.common.testing.ui.testapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Shows ActionBar with a lot of items to get Action overflow on large displays. Click on item
 * changes text of R.id.textActionBarResult. We have to use third-party ActionBarSherlock, because
 * Android Action is available since API 11 and we support API >= 7.
 */
public class ActionBarActivity extends SherlockActivity {
  private ActionMode mode;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.Theme_Sherlock);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.actionbar_activity);

    mode = startActionMode(new TestActionMode());

    ((Button) findViewById(R.id.show)).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mode = startActionMode(new TestActionMode());
      }
    });
    ((Button) findViewById(R.id.hide)).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mode != null) {
          mode.finish();
        }
      }
    });
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    populate(menu);
    return true;
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem menu) {
    setResult(menu.getTitle());
    return true;
  }

  private void setResult(CharSequence result) {
    TextView text = (TextView) findViewById(R.id.textActionBarResult);
    text.setText(result);
  }

  private void populate(Menu menu) {
    menu.add("Save")
        .setIcon(R.drawable.ic_action_save).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    menu.add("Search")
        .setIcon(R.drawable.ic_action_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    menu.add("World")
        .setIcon(R.drawable.ic_action_world).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    menu.add("Key")
        .setIcon(R.drawable.ic_action_key).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    menu.add("Calendar")
        .setIcon(R.drawable.ic_action_calendar).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    menu.add("Lock")
        .setIcon(R.drawable.ic_action_lock).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
  }
          

  private final class TestActionMode implements ActionMode.Callback {
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      populate(menu);
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      setResult(item.getTitle());
      return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {}
  }
}
