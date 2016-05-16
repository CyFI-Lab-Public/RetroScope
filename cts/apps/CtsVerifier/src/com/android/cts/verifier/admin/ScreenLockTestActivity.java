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

package com.android.cts.verifier.admin;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ScreenLockTestActivity extends PassFailButtons.Activity {

    private static final int ADD_DEVICE_ADMIN_REQUEST_CODE = 1;

    private ScreenOffReceiver mReceiver;

    private Button mForceLockButton;

    private DevicePolicyManager mDevicePolicyManager;

    private KeyguardManager mKeyguardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.da_screen_lock_main);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.da_screen_lock_test, R.string.da_screen_lock_info, -1);

        mDevicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        mKeyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        getPassButton().setEnabled(false);

        mForceLockButton = (Button) findViewById(R.id.da_force_lock_button);
        mForceLockButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAddDeviceAdminIntent();
            }
        });

        mReceiver = new ScreenOffReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
    }

    private void sendAddDeviceAdminIntent() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                TestDeviceAdminReceiver.getComponent(this));
        startActivityForResult(intent, ADD_DEVICE_ADMIN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ADD_DEVICE_ADMIN_REQUEST_CODE:
                handleAddDeviceAdminResult(resultCode, data);
                break;
        }
    }

    private void handleAddDeviceAdminResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            mDevicePolicyManager.lockNow();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private class ScreenOffReceiver extends BroadcastReceiver {

        private static final int LOCK_CHECK_DELAY = 1000;

        @Override
        public void onReceive(Context context, Intent intent) {
            mForceLockButton.postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean lockSuccess = mKeyguardManager.inKeyguardRestrictedInputMode();
                    getPassButton().setEnabled(lockSuccess);

                    int iconId = lockSuccess
                            ? android.R.drawable.ic_dialog_info
                            : android.R.drawable.ic_dialog_alert;
                    int messageId = lockSuccess
                            ? R.string.da_lock_success
                            : R.string.da_lock_error;
                    new AlertDialog.Builder(ScreenLockTestActivity.this)
                        .setTitle(R.string.da_screen_lock_test)
                        .setMessage(messageId)
                        .setIcon(iconId)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                }
            }, LOCK_CHECK_DELAY);
        }
    }
}
