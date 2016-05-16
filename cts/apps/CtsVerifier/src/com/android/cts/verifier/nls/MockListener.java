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
package com.android.cts.verifier.nls;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MockListener extends NotificationListenerService {
    static final String TAG = "MockListener";

    static final String SERVICE_CHECK = "android.service.notification.cts.SERVICE_CHECK";
    static final String SERVICE_POSTED = "android.service.notification.cts.SERVICE_POSTED";
    static final String SERVICE_PAYLOADS = "android.service.notification.cts.SERVICE_PAYLOADS";
    static final String SERVICE_REMOVED = "android.service.notification.cts.SERVICE_REMOVED";
    static final String SERVICE_RESET = "android.service.notification.cts.SERVICE_RESET";
    static final String SERVICE_CLEAR_ONE = "android.service.notification.cts.SERVICE_CLEAR_ONE";
    static final String SERVICE_CLEAR_ALL = "android.service.notification.cts.SERVICE_CLEAR_ALL";

    static final String EXTRA_PAYLOAD = "TAGS";
    static final String EXTRA_TAG = "TAG";
    static final String EXTRA_CODE = "CODE";

    static final int RESULT_TIMEOUT = Activity.RESULT_FIRST_USER;
    static final int RESULT_NO_SERVER = Activity.RESULT_FIRST_USER + 1;

    public static final String JSON_FLAGS = "flag";
    public static final String JSON_ICON = "icon";
    public static final String JSON_ID = "id";
    public static final String JSON_PACKAGE = "pkg";
    public static final String JSON_WHEN = "when";
    public static final String JSON_TAG = "tag";

    private ArrayList<String> mPosted = new ArrayList<String>();
    private ArrayList<String> mPayloads = new ArrayList<String>();
    private ArrayList<String> mRemoved = new ArrayList<String>();
    private BroadcastReceiver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "created");

        mPosted = new ArrayList<String>();
        mRemoved = new ArrayList<String>();

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (SERVICE_CHECK.equals(action)) {
                    Log.d(TAG, "SERVICE_CHECK");
                    setResultCode(Activity.RESULT_OK);
                } else if (SERVICE_POSTED.equals(action)) {
                    Log.d(TAG, "SERVICE_POSTED");
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(EXTRA_PAYLOAD, mPosted);
                    setResultExtras(bundle);
                    setResultCode(Activity.RESULT_OK);
                } else if (SERVICE_PAYLOADS.equals(action)) {
                    Log.d(TAG, "SERVICE_PAYLOADS");
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(EXTRA_PAYLOAD, mPayloads);
                    setResultExtras(bundle);
                    setResultCode(Activity.RESULT_OK);
                } else if (SERVICE_REMOVED.equals(action)) {
                    Log.d(TAG, "SERVICE_REMOVED");
                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList(EXTRA_PAYLOAD, mRemoved);
                    setResultExtras(bundle);
                    setResultCode(Activity.RESULT_OK);
                } else if (SERVICE_CLEAR_ONE.equals(action)) {
                    Log.d(TAG, "SERVICE_CLEAR_ONE");
                    MockListener.this.cancelNotification(
                            context.getApplicationInfo().packageName,
                            intent.getStringExtra(EXTRA_TAG),
                            intent.getIntExtra(EXTRA_CODE, 0));
                } else if (SERVICE_CLEAR_ALL.equals(action)) {
                    Log.d(TAG, "SERVICE_CLEAR_ALL");
                    MockListener.this.cancelAllNotifications();
                } else if (SERVICE_RESET.equals(action)) {
                    Log.d(TAG, "SERVICE_RESET");
                    resetData();
                } else {
                    Log.w(TAG, "unknown action");
                    setResultCode(Activity.RESULT_CANCELED);
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICE_CHECK);
        filter.addAction(SERVICE_POSTED);
        filter.addAction(SERVICE_PAYLOADS);
        filter.addAction(SERVICE_REMOVED);
        filter.addAction(SERVICE_CLEAR_ONE);
        filter.addAction(SERVICE_CLEAR_ALL);
        filter.addAction(SERVICE_RESET);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mReceiver = null;
        Log.d(TAG, "destroyed");
    }

    public void resetData() {
        mPosted.clear();
        mPayloads.clear();
        mRemoved.clear();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "posted: " + sbn.getTag());
        mPosted.add(sbn.getTag());
        JSONObject payload = new JSONObject();
        try {
            payload.put(JSON_TAG, sbn.getTag());
            payload.put(JSON_ID, sbn.getId());
            payload.put(JSON_PACKAGE, sbn.getPackageName());
            payload.put(JSON_WHEN, sbn.getNotification().when);
            payload.put(JSON_ICON, sbn.getNotification().icon);
            payload.put(JSON_FLAGS, sbn.getNotification().flags);
            mPayloads.add(payload.toString());
        } catch (JSONException e) {
            Log.e(TAG, "failed to pack up notification payload", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "removed: " + sbn.getTag());
        mRemoved.add(sbn.getTag());
    }

    public static void resetListenerData(Context context) {
        sendCommand(context, SERVICE_RESET, null, 0);
    }

    public static void probeListenerStatus(Context context, IntegerResultCatcher catcher) {
        requestIntegerResult(context, SERVICE_CHECK, catcher);
    }

    public static void probeListenerPosted(Context context, StringListResultCatcher catcher) {
        requestStringListResult(context, SERVICE_POSTED, catcher);
    }

    public static void probeListenerPayloads(Context context, StringListResultCatcher catcher) {
        requestStringListResult(context, SERVICE_PAYLOADS, catcher);
    }

    public static void probeListenerRemoved(Context context, StringListResultCatcher catcher) {
        requestStringListResult(context, SERVICE_REMOVED, catcher);
    }

    public static void clearOne(Context context, String tag, int code) {
        sendCommand(context, SERVICE_CLEAR_ONE, tag, code);
    }

    public static void clearAll(Context context) {
        sendCommand(context, SERVICE_CLEAR_ALL, null, 0);
    }

    private static void sendCommand(Context context, String action, String tag, int code) {
        Intent broadcast = new Intent(action);
        if (tag != null) {
            broadcast.putExtra(EXTRA_TAG, tag);
            broadcast.putExtra(EXTRA_CODE, code);
        }
        context.sendBroadcast(broadcast);
    }

    public abstract static class IntegerResultCatcher extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            accept(Integer.valueOf(getResultCode()));
        }

        abstract public void accept(int result);
    }

    private static void requestIntegerResult(Context context, String action,
            IntegerResultCatcher catcher) {
        Intent broadcast = new Intent(action);
        context.sendOrderedBroadcast(broadcast, null, catcher, null, RESULT_NO_SERVER, null, null);
    }

    public abstract static class StringListResultCatcher extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            accept(getResultExtras(true).getStringArrayList(EXTRA_PAYLOAD));
        }

        abstract public void accept(List<String> result);
    }

    private static void requestStringListResult(Context context, String action,
            StringListResultCatcher catcher) {
        Intent broadcast = new Intent(action);
        context.sendOrderedBroadcast(broadcast, null, catcher, null, RESULT_NO_SERVER, null, null);
    }
}
