/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.cts.verifier.bluetooth;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectionAccessServerActivity extends PassFailButtons.Activity {

    private static final String ACTION_CONNECTION_ACCESS_REQUEST =
            "android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST";

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private ConnectionAccessRequestReceiver mConnectionAccessRequestReceiver;
    private BluetoothChatService mChatService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.bt_connection_access);
        setInfoResources(R.string.bt_connection_access_server,
                R.string.bt_connection_access_server_info, 0);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);

        View settings = findViewById(R.id.bt_settings);
        settings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            }
        });

        Button makeDiscoverableButton = (Button) findViewById(R.id.bt_make_discoverable_button);
        makeDiscoverableButton.setVisibility(View.VISIBLE);
        makeDiscoverableButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                makeDiscoverable();
            }
        });

        mConnectionAccessRequestReceiver = new ConnectionAccessRequestReceiver();
        IntentFilter intentFilter = new IntentFilter(ACTION_CONNECTION_ACCESS_REQUEST);
        registerReceiver(mConnectionAccessRequestReceiver, intentFilter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            startChatService();
        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ENABLE_BLUETOOTH_REQUEST:
                if (resultCode == RESULT_OK) {
                    startChatService();
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                break;
        }
    }

    private void startChatService() {
        mChatService = new BluetoothChatService(this, new ChatHandler(),
                BluetoothChatService.HANDSFREE_INSECURE_UUID);
        boolean secure = false;
        mChatService.start(secure);
    }

    private void makeDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
            startActivity(intent);
        }
    }

    private class ChatHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BluetoothChatService.MESSAGE_STATE_CHANGE:
                    handleStateChange(msg);
                    break;

                case BluetoothChatService.MESSAGE_TOAST:
                    handleToast(msg);
                    break;
            }
        }
    }

    private void handleStateChange(Message msg) {
        int state = msg.arg1;
        switch (state) {
            case BluetoothChatService.STATE_LISTEN:
                setProgressBarIndeterminateVisibility(true);
                Toast.makeText(this, R.string.bt_listening, Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void handleToast(Message msg) {
        String toast = msg.getData().getString(BluetoothChatService.TOAST);
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
    }

    class ConnectionAccessRequestReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            new AlertDialog.Builder(ConnectionAccessServerActivity.this)
                    .setMessage(R.string.bt_ca_dialog)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getPassButton().setEnabled(true);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getPassButton().setEnabled(false);
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
        unregisterReceiver(mConnectionAccessRequestReceiver);
    }
}
