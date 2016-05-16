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

import static com.android.cts.verifier.nls.MockListener.JSON_FLAGS;
import static com.android.cts.verifier.nls.MockListener.JSON_ICON;
import static com.android.cts.verifier.nls.MockListener.JSON_ID;
import static com.android.cts.verifier.nls.MockListener.JSON_PACKAGE;
import static com.android.cts.verifier.nls.MockListener.JSON_TAG;
import static com.android.cts.verifier.nls.MockListener.JSON_WHEN;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.TagVerifierActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class NotificationListenerVerifierActivity extends PassFailButtons.Activity
implements Runnable {
    static final String TAG = TagVerifierActivity.class.getSimpleName();
    private static final String STATE = "state";
    private static final String LISTENER_PATH = "com.android.cts.verifier/" + 
            "com.android.cts.verifier.nls.MockListener";
    private static final int SETUP = 0;
    private static final int PASS = 1;
    private static final int FAIL = 2;
    private static final int WAIT_FOR_USER = 3;
    private static final int CLEARED = 4;
    private static final int READY = 5;
    private static final int RETRY = 6;
    private static final int NOTIFICATION_ID = 1001;
    private static LinkedBlockingQueue<String> sDeletedQueue = new LinkedBlockingQueue<String>();

    private int mState;
    private int[] mStatus;
    private LayoutInflater mInflater;
    private ViewGroup mItemList;
    private PackageManager mPackageManager;
    private String mTag1;
    private String mTag2;
    private String mTag3;
    private NotificationManager mNm;
    private Context mContext;
    private Runnable mRunner;
    private View mHandler;
    private String mPackageString;
    private int mIcon1;
    private int mIcon2;
    private int mIcon3;
    private int mId1;
    private int mId2;
    private int mId3;
    private long mWhen1;
    private long mWhen2;
    private long mWhen3;
    private int mFlag1;
    private int mFlag2;
    private int mFlag3;

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
        mContext = this;
        mRunner = this;
        mNm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mPackageManager = getPackageManager();
        mInflater = getLayoutInflater();
        View view = mInflater.inflate(R.layout.nls_main, null);
        mItemList = (ViewGroup) view.findViewById(R.id.nls_test_items);
        mHandler = mItemList;
        createTestItems();
        mStatus = new int[mItemList.getChildCount()];
        setContentView(view);

        setPassFailButtonClickListeners();
        setInfoResources(R.string.nls_test, R.string.nls_info, -1);

        getPassButton().setEnabled(false);
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
        createUserItem(R.string.nls_enable_service);
        createAutoItem(R.string.nls_service_started);
        createAutoItem(R.string.nls_note_received);
        createAutoItem(R.string.nls_payload_intact);
        createAutoItem(R.string.nls_clear_one);
        createAutoItem(R.string.nls_clear_all);
        createUserItem(R.string.nls_disable_service);
        createAutoItem(R.string.nls_service_stopped);
        createAutoItem(R.string.nls_note_missed);
    }

    private void setItemState(int index, boolean passed) {
        ViewGroup item = (ViewGroup) mItemList.getChildAt(index);
        ImageView status = (ImageView) item.findViewById(R.id.nls_status);
        status.setImageResource(passed ? R.drawable.fs_good : R.drawable.fs_error);
        View button = item.findViewById(R.id.nls_launch_settings);
        button.setClickable(false);
        button.setEnabled(false);
        status.invalidate();
    }

    private void markItemWaiting(int index) {
        ViewGroup item = (ViewGroup) mItemList.getChildAt(index);
        ImageView status = (ImageView) item.findViewById(R.id.nls_status);
        status.setImageResource(R.drawable.fs_warning);
        status.invalidate();
    }

    private View createUserItem(int stringId) {
        View item = mInflater.inflate(R.layout.nls_item, mItemList, false);
        TextView instructions = (TextView) item.findViewById(R.id.nls_instructions);
        instructions.setText(stringId);
        mItemList.addView(item);
        return item;
    }

    private View createAutoItem(int stringId) {
        View item = mInflater.inflate(R.layout.nls_item, mItemList, false);
        TextView instructions = (TextView) item.findViewById(R.id.nls_instructions);
        instructions.setText(stringId);
        View button = item.findViewById(R.id.nls_launch_settings);
        button.setVisibility(View.GONE);
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
                testIsEnabled(0);
                break;
            case 1:
                testIsStarted(1);
                break;
            case 2:
                testNotificationRecieved(2);
                break;
            case 3:
                testDataIntact(3);
                break;
            case 4:
                testDismissOne(4);
                break;
            case 5:
                testDismissAll(5);
                break;
            case 6:
                testIsDisabled(6);
                break;
            case 7:
                testIsStopped(7);
                break;
            case 8:
                testNotificationNotRecieved(8);
                break;
            case 9:
                getPassButton().setEnabled(true);
                mNm.cancelAll();
                break;
        }
    }

    public void launchSettings(View button) {
        startActivity(
                new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
    }

    private PendingIntent makeIntent(int code, String tag) {
        Intent intent = new Intent(tag);
        intent.setComponent(new ComponentName(mContext, DismissService.class));
        PendingIntent pi = PendingIntent.getService(mContext, code, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }

    @SuppressLint("NewApi")
    private void sendNotificaitons() {
        mTag1 = UUID.randomUUID().toString();
        mTag2 = UUID.randomUUID().toString();
        mTag3 = UUID.randomUUID().toString();

        mNm.cancelAll();

        mWhen1 = System.currentTimeMillis() + 1;
        mWhen2 = System.currentTimeMillis() + 2;
        mWhen3 = System.currentTimeMillis() + 3;

        mIcon1 = R.drawable.fs_good;
        mIcon2 = R.drawable.fs_error;
        mIcon3 = R.drawable.fs_warning;

        mId1 = NOTIFICATION_ID + 1;
        mId2 = NOTIFICATION_ID + 2;
        mId3 = NOTIFICATION_ID + 3;

        mPackageString = "com.android.cts.verifier";

        Notification n1 = new Notification.Builder(mContext)
        .setContentTitle("ClearTest 1")
        .setContentText(mTag1.toString())
        .setPriority(Notification.PRIORITY_LOW)
        .setSmallIcon(mIcon1)
        .setWhen(mWhen1)
        .setDeleteIntent(makeIntent(1, mTag1))
        .setOnlyAlertOnce(true)
        .build();
        mNm.notify(mTag1, mId1, n1);
        mFlag1 = Notification.FLAG_ONLY_ALERT_ONCE;

        Notification n2 = new Notification.Builder(mContext)
        .setContentTitle("ClearTest 2")
        .setContentText(mTag2.toString())
        .setPriority(Notification.PRIORITY_HIGH)
        .setSmallIcon(mIcon2)
        .setWhen(mWhen2)
        .setDeleteIntent(makeIntent(2, mTag2))
        .setAutoCancel(true)
        .build();
        mNm.notify(mTag2, mId2, n2);
        mFlag2 = Notification.FLAG_AUTO_CANCEL;

        Notification n3 = new Notification.Builder(mContext)
        .setContentTitle("ClearTest 3")
        .setContentText(mTag3.toString())
        .setPriority(Notification.PRIORITY_LOW)
        .setSmallIcon(mIcon3)
        .setWhen(mWhen3)
        .setDeleteIntent(makeIntent(3, mTag3))
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .build();
        mNm.notify(mTag3, mId3, n3);
        mFlag3 = Notification.FLAG_ONLY_ALERT_ONCE | Notification.FLAG_AUTO_CANCEL;
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

    boolean checkEquals(long expected, long actual, String message) {
        if (expected == actual) {
            return true;
        }
        logWithStack(String.format(message, expected, actual));
        return false;
    }

    boolean checkEquals(String expected, String actual, String message) {
        if (expected.equals(actual)) {
            return true;
        }
        logWithStack(String.format(message, expected, actual));
        return false;
    }

    boolean checkFlagSet(int expected, int actual, String message) {
        if ((expected & actual) != 0) {
            return true;
        }
        logWithStack(String.format(message, expected, actual));
        return false;
    };

    private void logWithStack(String message) {
        Throwable stackTrace = new Throwable();
        stackTrace.fillInStackTrace();
        Log.e(TAG, message, stackTrace);
    }

    // Tests

    private void testIsEnabled(int i) {
        // no setup required
        Intent settings = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        if (settings.resolveActivity(mPackageManager) == null) {
            logWithStack("failed testIsEnabled: no settings activity");
            mStatus[i] = FAIL;
        } else {
            // TODO: find out why Secure.ENABLED_NOTIFICATION_LISTENERS is hidden
            String listeners = Secure.getString(getContentResolver(),
                    "enabled_notification_listeners");
            if (listeners != null && listeners.contains(LISTENER_PATH)) {
                mStatus[i] = PASS;
            } else {
                mStatus[i] = WAIT_FOR_USER;
            }
        }
        next();
    }

    private void testIsStarted(final int i) {
        if (mStatus[i] == SETUP) {
            mStatus[i] = READY;
            // wait for the service to start
            delay();
        } else {
            MockListener.probeListenerStatus(mContext,
                    new MockListener.IntegerResultCatcher() {
                @Override
                public void accept(int result) {
                    if (result == Activity.RESULT_OK) {
                        mStatus[i] = PASS;
                    } else {
                        logWithStack("failed testIsStarted: " + result);
                        mStatus[i] = FAIL;
                    }
                    next();
                }
            });
        }
    }

    private void testNotificationRecieved(final int i) {
        if (mStatus[i] == SETUP) {
            MockListener.resetListenerData(this);
            mStatus[i] = CLEARED;
            // wait for intent to move through the system
            delay();
        } else if (mStatus[i] == CLEARED) {
            sendNotificaitons();
            mStatus[i] = READY;
            // wait for notifications to move through the system
            delay();
        } else {
            MockListener.probeListenerPosted(mContext,
                    new MockListener.StringListResultCatcher() {
                @Override
                public void accept(List<String> result) {
                    if (result.size() > 0 && result.contains(mTag1)) {
                        mStatus[i] = PASS;
                    } else {
                        logWithStack("failed testNotificationRecieved");
                        mStatus[i] = FAIL;
                    }
                    next();
                }});
        }
    }

    private void testDataIntact(final int i) {
        // no setup required
        MockListener.probeListenerPayloads(mContext,
                new MockListener.StringListResultCatcher() {
            @Override
            public void accept(List<String> result) {
                boolean pass = false;
                Set<String> found = new HashSet<String>();
                if (result.size() > 0) {
                    pass = true;
                    for(String payloadData : result) {
                        try {
                            JSONObject payload = new JSONObject(payloadData);
                            pass &= checkEquals(mPackageString, payload.getString(JSON_PACKAGE),
                                    "data integrity test fail: notificaiton package (%s, %s)");
                            String tag = payload.getString(JSON_TAG);
                            if (mTag1.equals(tag)) {
                                found.add(mTag1);
                                pass &= checkEquals(mIcon1, payload.getInt(JSON_ICON),
                                        "data integrity test fail: notificaiton icon (%d, %d)");
                                pass &= checkFlagSet(mFlag1, payload.getInt(JSON_FLAGS),
                                        "data integrity test fail: notificaiton flags (%d, %d)");
                                pass &= checkEquals(mId1, payload.getInt(JSON_ID),
                                        "data integrity test fail: notificaiton ID (%d, %d)");
                                pass &= checkEquals(mWhen1, payload.getLong(JSON_WHEN),
                                        "data integrity test fail: notificaiton when (%d, %d)");
                            } else if (mTag2.equals(tag)) {
                                found.add(mTag2);
                                pass &= checkEquals(mIcon2, payload.getInt(JSON_ICON),
                                        "data integrity test fail: notificaiton icon (%d, %d)");
                                pass &= checkFlagSet(mFlag2, payload.getInt(JSON_FLAGS),
                                        "data integrity test fail: notificaiton flags (%d, %d)");
                                pass &= checkEquals(mId2, payload.getInt(JSON_ID),
                                        "data integrity test fail: notificaiton ID (%d, %d)");
                                pass &= checkEquals(mWhen2, payload.getLong(JSON_WHEN),
                                        "data integrity test fail: notificaiton when (%d, %d)");
                            } else if (mTag3.equals(tag)) {
                                found.add(mTag3);
                                pass &= checkEquals(mIcon3, payload.getInt(JSON_ICON),
                                        "data integrity test fail: notificaiton icon (%d, %d)");
                                pass &= checkFlagSet(mFlag3, payload.getInt(JSON_FLAGS),
                                        "data integrity test fail: notificaiton flags (%d, %d)");
                                pass &= checkEquals(mId3, payload.getInt(JSON_ID),
                                        "data integrity test fail: notificaiton ID (%d, %d)");
                                pass &= checkEquals(mWhen3, payload.getLong(JSON_WHEN),
                                        "data integrity test fail: notificaiton when (%d, %d)");
                            } else {
                                pass = false;
                                logWithStack("failed on unexpected notification tag: " + tag);
                            }
                        } catch (JSONException e) {
                            pass = false;
                            Log.e(TAG, "failed to unpack data from mocklistener", e);
                        }
                    }
                }
                pass &= found.size() == 3;
                mStatus[i] = pass ? PASS : FAIL;
                next();
            }});
    }

    private void testDismissOne(final int i) {
        if (mStatus[i] == SETUP) {
            MockListener.resetListenerData(this);
            mStatus[i] = CLEARED;
            // wait for intent to move through the system
            delay();
        } else if (mStatus[i] == CLEARED) {
            MockListener.clearOne(mContext, mTag1, NOTIFICATION_ID + 1);
            mStatus[i] = READY;
            delay();
        } else {
            MockListener.probeListenerRemoved(mContext,
                    new MockListener.StringListResultCatcher() {
                @Override
                public void accept(List<String> result) {
                    if (result.size() > 0 && result.contains(mTag1)) {
                        mStatus[i] = PASS;
                        next();
                    } else {
                        if (mStatus[i] == RETRY) {
                            logWithStack("failed testDismissOne");
                            mStatus[i] = FAIL;
                            next();
                        } else {
                            logWithStack("failed testDismissOne, once: retrying");
                            mStatus[i] = RETRY;
                            delay();
                        }
                    }
                }});
        }
    }

    private void testDismissAll(final int i) {
        if (mStatus[i] == SETUP) {
            MockListener.resetListenerData(this);
            mStatus[i] = CLEARED;
            // wait for intent to move through the system
            delay();
        } else if (mStatus[i] == CLEARED) {
            MockListener.clearAll(mContext);
            mStatus[i] = READY;
            delay();
        } else {
            MockListener.probeListenerRemoved(mContext,
                    new MockListener.StringListResultCatcher() {
                @Override
                public void accept(List<String> result) {
                    if (result.size() == 2 && result.contains(mTag2) && result.contains(mTag3)) {
                        mStatus[i] = PASS;
                        next();
                    } else {
                        if (mStatus[i] == RETRY) {
                            logWithStack("failed testDismissAll");
                            mStatus[i] = FAIL;
                            next();
                        } else {
                            logWithStack("failed testDismissAll, once: retrying");
                            mStatus[i] = RETRY;
                            delay();
                        }
                    }
                }
            });
        }
    }

    private void testIsDisabled(int i) {
        // no setup required
        // TODO: find out why Secure.ENABLED_NOTIFICATION_LISTENERS is hidden
        String listeners = Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (listeners == null || !listeners.contains(LISTENER_PATH)) {
            mStatus[i] = PASS;
            next();
        } else {
            mStatus[i] = WAIT_FOR_USER;
            delay();
        }
    }

    private void testIsStopped(final int i) {
        if (mStatus[i] == SETUP) {
            mStatus[i] = READY;
            // wait for the service to start
            delay();
        } else {
            MockListener.probeListenerStatus(mContext,
                    new MockListener.IntegerResultCatcher() {
                @Override
                public void accept(int result) {
                    if (result == Activity.RESULT_OK) {
                        logWithStack("failed testIsStopped");
                        mStatus[i] = FAIL;
                    } else {
                        mStatus[i] = PASS;
                    }
                    next();
                }
            });
        }
    }

    private void testNotificationNotRecieved(final int i) {
        if (mStatus[i] == SETUP) {
            MockListener.resetListenerData(this);
            mStatus[i] = CLEARED;
            // wait for intent to move through the system
            delay();
        } else if (mStatus[i] == CLEARED) {
            // setup for testNotificationRecieved
            sendNotificaitons();
            mStatus[i] = READY;
            delay();
        } else {
            MockListener.probeListenerPosted(mContext,
                    new MockListener.StringListResultCatcher() {
                @Override
                public void accept(List<String> result) {
                    if (result == null || result.size() == 0) {
                        mStatus[i] = PASS;
                    } else {
                        logWithStack("failed testNotificationNotRecieved");
                        mStatus[i] = FAIL;
                    }
                    next();
                }});
        }
    }
}
