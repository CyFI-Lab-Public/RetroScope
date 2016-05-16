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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
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

public class ConnectionAccessClientActivity extends PassFailButtons.Activity {

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;
    private static final int PICK_SERVER_DEVICE_REQUEST = 2;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothChatService mChatService;
    private String mDeviceAddress;
    private Button mPickServerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.bt_connection_access);
        setInfoResources(R.string.bt_connection_access_client,
                R.string.bt_connection_access_client_info, 0);
        setPassFailButtonClickListeners();

        View settings = findViewById(R.id.bt_settings);
        settings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            }
        });

        mPickServerButton = (Button) findViewById(R.id.bt_pick_server_button);
        mPickServerButton.setVisibility(View.VISIBLE);
        mPickServerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startDevicePickerActivity();
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            mPickServerButton.setEnabled(false);
            startEnableBluetoothActivity();
        }
    }

    private void startDevicePickerActivity() {
        Intent intent = new Intent(this, DevicePickerActivity.class);
        startActivityForResult(intent, PICK_SERVER_DEVICE_REQUEST);
    }

    private void startEnableBluetoothActivity() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ENABLE_BLUETOOTH_REQUEST:
                if (resultCode == RESULT_OK) {
                    mPickServerButton.setEnabled(true);
                } else {
                    setResult(RESULT_CANCELED);
                    finish();
                }
                break;

            case PICK_SERVER_DEVICE_REQUEST:
                if (resultCode == RESULT_OK) {
                    mDeviceAddress = data.getStringExtra(DevicePickerActivity.EXTRA_DEVICE_ADDRESS);
                    startChartService();
                }
                break;
        }
    }

    private void startChartService() {
        mChatService = new BluetoothChatService(this, new ChatHandler(),
                BluetoothChatService.HANDSFREE_INSECURE_UUID);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        mChatService.connect(device, false);
    }

    private class ChatHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BluetoothChatService.MESSAGE_STATE_CHANGE:
                    handleStateChange(msg);
                    break;

                case BluetoothChatService.MESSAGE_DEVICE_NAME:
                    handleDeviceName(msg);
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
            case BluetoothChatService.STATE_CONNECTING:
                setProgressBarIndeterminateVisibility(true);
                Toast.makeText(this, R.string.bt_connecting, Toast.LENGTH_SHORT).show();
                break;

            case BluetoothChatService.STATE_CONNECTED:
                setProgressBarIndeterminateVisibility(false);
                Toast.makeText(this, R.string.bt_connected, Toast.LENGTH_SHORT).show();
                break;

            case BluetoothChatService.STATE_NONE:
                setProgressBarIndeterminateVisibility(false);
                break;
        }
    }

    private void handleDeviceName(Message msg) {
        mDeviceAddress = msg.getData().getString(BluetoothChatService.DEVICE_NAME);
    }

    private void handleToast(Message msg) {
        String toast = msg.getData().getString(BluetoothChatService.TOAST);
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
    }
}
