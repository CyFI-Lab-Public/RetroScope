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

package com.android.phone.tests;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Service which receives instant text responses from in-call UI and shows the message using
 * {@link Toast}.
 */
public class SendInstantTextTestService extends IntentService {
    private static final String TAG = SendInstantTextTestService.class.getSimpleName();

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showMessageWithToast((Intent) msg.obj);
        }
    };

    public SendInstantTextTestService() {
        super("send_instant_text_test_service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Uri uri = intent.getData();
        String message = intent.getStringExtra(Intent.EXTRA_TEXT);
        Log.i(TAG, "Received instant response message. action=" + action
                + ", uri=" + uri + ", message=\"" + message + "\"");
        // Phone app uses Toast to show confirmation, which overlaps with this message.
        // Let's delay showing this one for 5sec.
        mHandler.sendMessageDelayed(Message.obtain(mHandler, 0, intent), 5 * 1000);
    }

    private void showMessageWithToast(Intent intent) {
        Uri uri = intent.getData();
        String message = intent.getStringExtra(Intent.EXTRA_TEXT);
        String toastText = getString(R.string.instant_text_back_content, message, uri.toString());
        Toast.makeText(this, toastText, Toast.LENGTH_LONG).show();
    }
}