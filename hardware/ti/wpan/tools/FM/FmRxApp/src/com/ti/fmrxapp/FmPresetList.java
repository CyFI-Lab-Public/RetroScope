/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.fmrxapp;

import java.util.HashMap;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import java.util.HashMap;
import android.widget.Button;
import android.content.Intent;
import android.content.IntentFilter;

public class FmPresetList extends ListActivity implements FmRxAppConstants,
       View.OnClickListener {

    public static SimpleAdapter adapter;

    // ===========================================================
    // Final Fields
    // ===========================================================
    protected static final int MENU_SET_STATION = 0;
    protected static final int MENU_UNSET_STATION = 1;
    protected static final int MENU_RENAME_STATION = 2;
    public static final String TAG = "FmPresetList";

    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.preset);

       // Now build the list adapter
       this.adapter = new SimpleAdapter(
       // the Context
             this,
             // the data to display
             FmRxApp.stations,
             // The layout to use for each item
             R.layout.row,
             // The list item attributes to display
             new String[] { ITEM_KEY, ITEM_VALUE, ITEM_NAME },
             // And the ids of the views where they should be displayed (same
             // order)
             new int[] { R.id.list_key, R.id.list_value, R.id.list_name });

       setListAdapter(this.adapter);
       adapter.notifyDataSetChanged();
       /* Add Context-Menu listener to the ListView. */
       registerForContextMenu(getListView());
       initControls();
             Log.i(TAG, "onCreate: Presets....");
    }

    private void initControls() {
       Button btnBack = (Button) findViewById(R.id.btnBack);
             btnBack.setOnClickListener(this);

    }

    public void onClick(View v) {
       int id = v.getId();
       switch (id) {
       case R.id.btnBack:
          setResult(RESULT_OK);
                    Log.i(TAG, "RESULT_OK");
          finish();
          break;
       }
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {

    switch (keyCode) {

         case KeyEvent.KEYCODE_BACK:
         {
            setResult(RESULT_OK);
            return true;
         }
    }
    return true;
    }

    private void refreshFavListItems() {
       this.setListAdapter(this.adapter);

    }

    public void onCreateContextMenu(ContextMenu menu, View v,
          ContextMenuInfo menuInfo) {
       super.onCreateContextMenu(menu, v, menuInfo);
       menu.setHeaderTitle("ContextMenu");
       menu.add(0, MENU_SET_STATION, 0, "Set Station");
       menu.add(0, MENU_UNSET_STATION, 0, "Unset Station");
       menu.add(0, MENU_RENAME_STATION, 0, "Rename Station");

    }

    // ===========================================================
    // Methods from SuperClass/Interfaces
    // ===========================================================

    public boolean onContextItemSelected(MenuItem aItem) {
       AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) aItem
             .getMenuInfo();
       /* Get the selected item out of the Adapter by its position. */
       final Integer index = menuInfo.position;
       final Integer i = new Integer(index.intValue() + 1);

       /* Switch on the ID of the item, to get what the user selected. */
       switch (aItem.getItemId()) {

       case MENU_RENAME_STATION:

          AlertDialog.Builder alert = new AlertDialog.Builder(this);

          alert.setTitle("Rename");
          alert.setMessage("Rename Station?");

          // Set an EditText view to get user input
          final EditText input = new EditText(this);
          alert.setView(input);

          alert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                          int whichButton) {
                       String value = input.getText().toString();
                       FmRxApp.UpdateRenameStation(index, value);
                       refreshFavListItems();
                    }
                });

          alert.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                          int whichButton) {
                       // Canceled.
                    }
                });

          alert.show();

          return true; /* true means: "we handled the event". */

       case MENU_SET_STATION:
          OnClickListener okButtonListener = new OnClickListener() {

             public void onClick(DialogInterface arg0, int arg1) {
                FmRxApp.updateSetStation(index, FmRxApp.txtFmRxTunedFreq
                       .getText().toString(), "");
                refreshFavListItems();

             }
          };

          new AlertDialog.Builder(this).setTitle("Set Station").setIcon(
                android.R.drawable.ic_dialog_alert).setMessage(
                "Set Station?").setNegativeButton(android.R.string.cancel,
                null).setPositiveButton(android.R.string.ok,
                okButtonListener).show();
          return true; /* true means: "we handled the event". */

       case MENU_UNSET_STATION:

          OnClickListener okUnsetButtonListener = new OnClickListener() {

             public void onClick(DialogInterface arg0, int arg1) {
                FmRxApp.updateUnSetStation(index);
                refreshFavListItems();

             }
          };

          new AlertDialog.Builder(this).setTitle("Unseet Station").setIcon(
                android.R.drawable.ic_dialog_alert).setMessage(
                "Unset Station?").setNegativeButton(
                android.R.string.cancel, null).setPositiveButton(
                android.R.string.ok, okUnsetButtonListener).show();
          return true;
       }
       return false;

    }
}
