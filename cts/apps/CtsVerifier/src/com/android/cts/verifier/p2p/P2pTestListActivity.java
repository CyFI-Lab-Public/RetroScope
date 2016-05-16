/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.cts.verifier.p2p;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ListView;

import com.android.cts.verifier.ArrayTestListAdapter;
import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestListAdapter.TestListItem;

/**
 * Activity that lists all the WiFi Direct tests.
 */
public class P2pTestListActivity extends PassFailButtons.TestListActivity {

    /*
     * BroadcastReceiver to check p2p status.
     * If WiFi Direct is disabled, show the dialog message to user.
     */
    private final P2pBroadcastReceiver mReceiver = new P2pBroadcastReceiver();
    private final IntentFilter mIntentFilter = new IntentFilter();
    private boolean mIsP2pEnabled = false;

    /**
     * Constructor
     */
    public P2pTestListActivity() {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_list);
        setInfoResources(R.string.p2p_test, R.string.p2p_test_info, 0);
        setPassFailButtonClickListeners();

        getPassButton().setEnabled(false);

        /**
         * Added WiFiDirect test activity to the list.
         */
        ArrayTestListAdapter adapter = new ArrayTestListAdapter(this);

        adapter.add(TestListItem.newCategory(this, R.string.p2p_group_formation));
        adapter.add(TestListItem.newTest(this,
                R.string.p2p_go_neg_responder_test,
                GoNegResponderTestActivity.class.getName(),
                new Intent(this, GoNegResponderTestActivity.class), null));
        adapter.add(TestListItem.newTest(this,
                R.string.p2p_go_neg_requester_test,
                GoNegRequesterTestListActivity.class.getName(),
                new Intent(this, GoNegRequesterTestListActivity.class), null));

        adapter.add(TestListItem.newCategory(this, R.string.p2p_join));
        adapter.add(TestListItem.newTest(this,
                R.string.p2p_group_owner_test,
                GoTestActivity.class.getName(),
                new Intent(this, GoTestActivity.class), null));
        adapter.add(TestListItem.newTest(this,
                R.string.p2p_group_client_test,
                P2pClientTestListActivity.class.getName(),
                new Intent(this, P2pClientTestListActivity.class), null));

        adapter.add(TestListItem.newCategory(this, R.string.p2p_service_discovery));
        adapter.add(TestListItem.newTest(this,
                R.string.p2p_service_discovery_responder_test,
                ServiceResponderTestActivity.class.getName(),
                new Intent(this, ServiceResponderTestActivity.class), null));
        adapter.add(TestListItem.newTest(this,
                R.string.p2p_service_discovery_requester_test,
                ServiceRequesterTestListActivity.class.getName(),
                new Intent(this, ServiceRequesterTestListActivity.class), null));


        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                updatePassButton();
            }
        });

        setTestListAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onResume();
        unregisterReceiver(mReceiver);
    }

    /**
     * Launch the activity when its {@link ListView} item is clicked.
     * If WiFi Direct is disabled, show the dialog to jump to system setting activity.
     **/
    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        if (!mIsP2pEnabled) {
            showP2pEnableDialog();
            return;
        }
        super.onListItemClick(listView, view, position, id);
    }

    /**
     * Show the dialog to jump to system settings in order to enable
     * WiFi Direct.
     */
    private void showP2pEnableDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle(R.string.p2p_not_enabled);
        builder.setMessage(R.string.p2p_not_enabled_message);
        builder.setPositiveButton(R.string.p2p_settings,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });
        builder.create().show();
    }

    /**
     * Enable Pass Button when the all tests passed.
     */
    private void updatePassButton() {
        getPassButton().setEnabled(mAdapter.allTestsPassed());
    }

    /**
     * Receive the WIFI_P2P_STATE_CHANGED_ACTION action.
     */
    class P2pBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if ((state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)) {
                    mIsP2pEnabled = true;
                }
            }
        }
    }
}
