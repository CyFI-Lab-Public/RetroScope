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

import com.android.cts.verifier.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.Set;

/**
 * {@link Activity} that shows a list of paired and new devices and returns the device selected
 * by the user. When the user selects a paired device, it forwards them to the Bluetooth settings
 * page, so that they can unpair it for the test.
 */
public class DevicePickerActivity extends Activity {

    public static final String EXTRA_DEVICE_ADDRESS = "deviceAddress";

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;

    private BluetoothAdapter mBluetoothAdapter;

    private DiscoveryReceiver mReceiver;

    private ArrayAdapter<Device> mNewDevicesAdapter;

    private ArrayAdapter<Device> mPairedDevicesAdapter;

    private TextView mEmptyNewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.bt_device_picker);

        mPairedDevicesAdapter = new ArrayAdapter<Device>(this, R.layout.bt_device_name);
        ListView pairedDevicesListView = (ListView) findViewById(R.id.bt_paired_devices);
        pairedDevicesListView.setAdapter(mPairedDevicesAdapter);
        pairedDevicesListView.setOnItemClickListener(new PairedDeviceClickListener());

        View emptyPairedView = findViewById(R.id.bt_empty_paired_devices);
        pairedDevicesListView.setEmptyView(emptyPairedView);

        mNewDevicesAdapter = new ArrayAdapter<Device>(this, R.layout.bt_device_name);
        ListView newDevicesListView = (ListView) findViewById(R.id.bt_new_devices);
        newDevicesListView.setAdapter(mNewDevicesAdapter);
        newDevicesListView.setOnItemClickListener(new NewDeviceClickListener());

        mEmptyNewView = (TextView) findViewById(R.id.bt_empty_new_devices);
        newDevicesListView.setEmptyView(mEmptyNewView);

        mReceiver = new DiscoveryReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        Button scanButton = (Button) findViewById(R.id.bt_scan_button);
        scanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            scan();
        } else {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_OK) {
                scan();
            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    private void scan() {
        populatePairedDevices();
        mNewDevicesAdapter.clear();
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private void populatePairedDevices() {
        mPairedDevicesAdapter.clear();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            mPairedDevicesAdapter.add(Device.fromBluetoothDevice(device));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }

    class NewDeviceClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent data = new Intent();
            Device device = (Device) parent.getItemAtPosition(position);
            data.putExtra(EXTRA_DEVICE_ADDRESS, device.mAddress);
            setResult(RESULT_OK, data);
            finish();
        }
    }

    class PairedDeviceClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            new AlertDialog.Builder(DevicePickerActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.bt_unpair)
                .setPositiveButton(R.string.bt_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mBluetoothAdapter != null) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(intent);
                    }
                })
                .show();
        }
    }

    class DiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                mEmptyNewView.setText(R.string.bt_scanning);
                setProgressBarIndeterminateVisibility(true);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                mEmptyNewView.setText(R.string.bt_no_devices);
                setProgressBarIndeterminateVisibility(false);
            } else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesAdapter.add(Device.fromBluetoothDevice(device));
                }
            }
        }
    }

    static class Device {

        String mName;

        String mAddress;

        Device(String name, String address) {
            mName = name;
            mAddress = address;
        }

        @Override
        public String toString() {
            return mName + "\n" + mAddress;
        }

        static Device fromBluetoothDevice(BluetoothDevice device) {
            return new Device(device.getName() != null ? device.getName() : "",
                    device.getAddress());
        }
    }
}
