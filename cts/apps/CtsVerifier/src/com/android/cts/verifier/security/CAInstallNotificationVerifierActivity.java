/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.cts.verifier.security;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;

public class CAInstallNotificationVerifierActivity extends PassFailButtons.Activity
implements Runnable {
    static final String TAG = CAInstallNotificationVerifierActivity.class.getSimpleName();
    private static final String STATE = "state";
    private static final int PASS = 1;
    private static final int FAIL = 2;
    private static final int WAIT_FOR_USER = 3;
    private static LinkedBlockingQueue<String> sDeletedQueue = new LinkedBlockingQueue<String>();

    private int mState;
    private int[] mStatus;
    private LayoutInflater mInflater;
    private ViewGroup mItemList;
    private Runnable mRunner;
    private View mHandler;

    private static final String CERT_ASSET_NAME = "myCA.cer";
    private File certStagingFile = new File("/sdcard/", CERT_ASSET_NAME);

    protected boolean doneInstallingCert = false;
    protected boolean doneCheckingInSettings = false;
    protected boolean doneCheckingNotification = false;
    protected boolean doneDismissingNotification = false;


    public static class DismissService extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onStart(Intent intent, int startId) {
            sDeletedQueue.offer(intent.getAction());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mState = savedInstanceState.getInt(STATE, 0);
        }
        mRunner = this;
        mInflater = getLayoutInflater();
        View view = mInflater.inflate(R.layout.cainstallnotify_main, null);
        mItemList = (ViewGroup) view.findViewById(R.id.ca_notify_test_items);
        mHandler = mItemList;
        createTestItems();
        mStatus = new int[mItemList.getChildCount()];
        setContentView(view);

        setPassFailButtonClickListeners();
        setInfoResources(R.string.cacert_test, R.string.cacert_info, -1);
    }

    @Override
    protected void onSaveInstanceState (Bundle outState) {
        outState.putInt(STATE, mState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        next();
    }

    // Interface Utilities

    private void createTestItems() {
        createUserItem(R.string.cacert_install_cert, new InstallCert());
        createUserItem(R.string.cacert_check_cert_in_settings, new OpenTrustedCredentials());
        createUserItem(R.string.cacert_check_notification,
                new DoneCheckingNotification(), R.string.cacert_done);
        createUserItem(R.string.cacert_dismiss_notification,
                new DoneCheckingDismissed(), R.string.cacert_done);
    }

    private void setItemState(int index, boolean passed) {
        ViewGroup item = (ViewGroup) mItemList.getChildAt(index);
        ImageView status = (ImageView) item.findViewById(R.id.ca_notify_status);
        status.setImageResource(passed ? R.drawable.fs_good : R.drawable.fs_error);
        View button = item.findViewById(R.id.ca_notify_do_something);
        button.setClickable(false);
        button.setEnabled(false);
        status.invalidate();
    }

    private void markItemWaiting(int index) {
        ViewGroup item = (ViewGroup) mItemList.getChildAt(index);
        ImageView status = (ImageView) item.findViewById(R.id.ca_notify_status);
        status.setImageResource(R.drawable.fs_warning);
        status.invalidate();
    }

    private View createUserItem(int stringId, OnClickListener listener) {
        return createUserItem(stringId, listener, 0);
    }

    private View createUserItem(int stringId, OnClickListener listener, int buttonLabel) {
        View item = mInflater.inflate(R.layout.cainstallnotify_item, mItemList, false);
        TextView instructions = (TextView) item.findViewById(R.id.ca_notify_instructions);
        instructions.setText(stringId);
        Button button = (Button) item.findViewById(R.id.ca_notify_do_something);
        if (buttonLabel != 0) {
            button.setText(buttonLabel);
        }
        button.setOnClickListener(listener);
        mItemList.addView(item);
        return item;
    }

    // Test management

    public void run() {
        while (mState < mStatus.length && mStatus[mState] != WAIT_FOR_USER) {
            if (mStatus[mState] == PASS) {
                setItemState(mState, true);
                mState++;
            } else if (mStatus[mState] == FAIL) {
                setItemState(mState, false);
                return;
            } else {
                break;
            }
        }

        if (mState < mStatus.length && mStatus[mState] == WAIT_FOR_USER) {
            markItemWaiting(mState);
        }

        switch (mState) {
            case 0:
                testInstalledCert(0);
                break;
            case 1:
                testCheckedSettings(1);
                break;
            case 2:
                testCheckedNotification(2);
                break;
            case 3:
                testNotificationDismissed(3);
                break;
        }
    }

    /**
     * Return to the state machine to progress through the tests.
     */
    private void next() {
        mHandler.post(mRunner);
    }

    /**
     * Wait for things to settle before returning to the state machine.
     */
    private void delay() {
        mHandler.postDelayed(mRunner, 2000);
    }

    // Listeners

    class InstallCert implements OnClickListener {
        @Override
        public void onClick(View v) {
            InputStream is = null;
            FileOutputStream os = null;
            try {
                try {
                    is = getAssets().open(CERT_ASSET_NAME);
                    os = new FileOutputStream(certStagingFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = is.read(buffer)) > 0) {
                        os.write(buffer, 0, length);
                    }
                } finally {
                    if (is != null) is.close();
                    if (os != null) os.close();
                    certStagingFile.setReadable(true, false);
                }
            } catch (IOException ioe) {
                Log.w(TAG, "Problem moving cert file to /sdcard/", ioe);
                return;
            }
            try {
                startActivity(new Intent("android.credentials.INSTALL"));
            } catch (ActivityNotFoundException e) {
                // do nothing
            }
            doneInstallingCert = true;
        }
    }

    class OpenTrustedCredentials implements OnClickListener {
        @Override
        public void onClick(View v) {
            try {
                startActivity(new Intent("com.android.settings.TRUSTED_CREDENTIALS_USER"));
            } catch (ActivityNotFoundException e) {
                // do nothing
            }
            doneCheckingInSettings = true;
        }
    }

    class DoneCheckingNotification implements OnClickListener {
        @Override
        public void onClick(View v) {
            doneCheckingNotification = true;
        }
    }

    class DoneCheckingDismissed implements OnClickListener {
        @Override
        public void onClick(View v) {
            doneDismissingNotification = true;
        }
    }

    // Tests

    private void testInstalledCert(final int i) {
        if (doneInstallingCert) {
            mStatus[i] = PASS;
            next();
        } else {
            delay();
        }
    }

    private void testCheckedSettings(final int i) {
        if (doneCheckingInSettings) {
            mStatus[i] = PASS;
            next();
        } else {
            delay();
        }
    }

    private void testCheckedNotification(final int i) {
        if (doneCheckingNotification) {
            mStatus[i] = PASS;
            next();
        } else {
            delay();
        }
    }

    private void testNotificationDismissed(final int i) {
        if (doneDismissingNotification) {
            mStatus[i] = PASS;
            next();
        } else {
            delay();
        }
    }
}
