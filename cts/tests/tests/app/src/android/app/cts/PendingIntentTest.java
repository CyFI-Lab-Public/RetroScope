/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.app.cts;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.test.AndroidTestCase;

public class PendingIntentTest extends AndroidTestCase {

    private static final int WAIT_TIME = 5000;
    private PendingIntent mPendingIntent;
    private Intent mIntent;
    private Context mContext;
    private boolean mFinishResult;
    private boolean mHandleResult;
    private String mResultAction;
    private PendingIntent.OnFinished mFinish;
    private boolean mLooperStart;
    private Looper mLooper;
    private Handler mHandler;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mFinish = new PendingIntent.OnFinished() {
            public void onSendFinished(PendingIntent pi, Intent intent, int resultCode,
                    String resultData, Bundle resultExtras) {
                mFinishResult = true;
                if (intent != null) {
                    mResultAction = intent.getAction();
                }
            }
        };

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLooperStart = true;
                Looper.loop();
            }
        }.start();
        while (!mLooperStart) {
            Thread.sleep(50);
        }
        mHandler = new Handler(mLooper) {
            @Override
            public void dispatchMessage(Message msg) {
                mHandleResult = true;
                super.dispatchMessage(msg);
            }

            @Override
            public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
                mHandleResult = true;
                return super.sendMessageAtTime(msg, uptimeMillis);
            }

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                mHandleResult = true;
            }
        };
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mLooper.quit();
    }

    public void testGetActivity() throws InterruptedException, CanceledException {
        PendingIntentStubActivity.status = PendingIntentStubActivity.INVALIDATE;
        mPendingIntent = null;
        mIntent = new Intent();

        mIntent.setClass(mContext, PendingIntentStubActivity.class);
        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        assertEquals(mContext.getPackageName(), mPendingIntent.getTargetPackage());

        mPendingIntent.send();

        Thread.sleep(WAIT_TIME);
        assertNotNull(mPendingIntent);
        assertEquals(PendingIntentStubActivity.status, PendingIntentStubActivity.ON_CREATE);

        // test getActivity return null
        mPendingIntent.cancel();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_NO_CREATE);
        assertNull(mPendingIntent);

        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_ONE_SHOT);

        pendingIntentSendError(mPendingIntent);
    }

    private void pendingIntentSendError(PendingIntent pendingIntent) {
        try {
            // From the doc send function will throw CanceledException if the PendingIntent
            // is no longer allowing more intents to be sent through it. So here call it twice then
            // a CanceledException should be caught.
            mPendingIntent.send();
            mPendingIntent.send();
            fail("CanceledException expected, but not thrown");
        } catch (PendingIntent.CanceledException e) {
            // expected
        }
    }

    public void testGetBroadcast() throws InterruptedException, CanceledException {
        MockReceiver.sAction = null;
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mPendingIntent.send();

        Thread.sleep(WAIT_TIME);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

        // test getBroadcast return null
        mPendingIntent.cancel();
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_NO_CREATE);
        assertNull(mPendingIntent);

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_ONE_SHOT);

        pendingIntentSendError(mPendingIntent);
    }

    public void testGetService() throws InterruptedException, CanceledException {
        MockService.result = false;
        mIntent = new Intent();
        mIntent.setClass(mContext, MockService.class);
        mPendingIntent = PendingIntent.getService(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mPendingIntent.send();

        Thread.sleep(WAIT_TIME);
        assertTrue(MockService.result);

        // test getService return null
        mPendingIntent.cancel();
        mPendingIntent = PendingIntent.getService(mContext, 1, mIntent,
                PendingIntent.FLAG_NO_CREATE);
        assertNull(mPendingIntent);

        mPendingIntent = PendingIntent.getService(mContext, 1, mIntent,
                PendingIntent.FLAG_ONE_SHOT);

        pendingIntentSendError(mPendingIntent);
    }

    public void testCancel() throws CanceledException {
        mIntent = new Intent();
        mIntent.setClass(mContext, MockService.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mPendingIntent.send();

        mPendingIntent.cancel();
        pendingIntentSendShouldFail(mPendingIntent);
    }

    private void pendingIntentSendShouldFail(PendingIntent pendingIntent) {
        try {
            pendingIntent.send();
            fail("CanceledException expected, but not thrown");
        } catch (CanceledException e) {
            // expected
        }
    }

    public void testSend() throws InterruptedException, CanceledException {
        MockReceiver.sAction = null;
        MockReceiver.sResultCode = -1;
        mIntent = new Intent();
        mIntent.setAction(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mPendingIntent.send();

        Thread.sleep(WAIT_TIME);

        // send function to send default code 0
        assertEquals(0, MockReceiver.sResultCode);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);
        mPendingIntent.cancel();

        pendingIntentSendShouldFail(mPendingIntent);
    }

    public void testSendWithParamInt() throws InterruptedException, CanceledException {
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        MockReceiver.sResultCode = 0;
        MockReceiver.sAction = null;
        // send result code 1.
        mPendingIntent.send(1);
        Thread.sleep(WAIT_TIME);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

        // assert the result code
        assertEquals(1, MockReceiver.sResultCode);
        assertEquals(mResultAction, null);

        mResultAction = null;
        MockReceiver.sResultCode = 0;
        // send result code 2
        mPendingIntent.send(2);
        Thread.sleep(WAIT_TIME);

        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

        // assert the result code
        assertEquals(2, MockReceiver.sResultCode);
        assertEquals(MockReceiver.sAction, MockReceiver.MOCKACTION);
        assertNull(mResultAction);
        mPendingIntent.cancel();
        pendingIntentSendShouldFail(mPendingIntent);
    }

    public void testSendWithParamContextIntIntent() throws InterruptedException, CanceledException {
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);

        MockReceiver.sAction = null;
        MockReceiver.sResultCode = 0;

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);

        mPendingIntent.send(mContext, 1, null);
        Thread.sleep(WAIT_TIME);

        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);
        assertEquals(1, MockReceiver.sResultCode);
        mPendingIntent.cancel();

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        MockReceiver.sAction = null;
        MockReceiver.sResultCode = 0;

        mPendingIntent.send(mContext, 2, mIntent);
        Thread.sleep(WAIT_TIME);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);
        assertEquals(2, MockReceiver.sResultCode);
        mPendingIntent.cancel();
    }

    public void testSendWithParamIntOnFinishedHandler() throws InterruptedException,
            CanceledException {
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setClass(mContext, MockReceiver.class);

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        mFinishResult = false;
        mHandleResult = false;
        MockReceiver.sAction = null;
        MockReceiver.sResultCode = 0;

        mPendingIntent.send(1, null, null);
        Thread.sleep(WAIT_TIME);
        assertFalse(mFinishResult);
        assertFalse(mHandleResult);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

        // assert result code
        assertEquals(1, MockReceiver.sResultCode);
        mPendingIntent.cancel();

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        mFinishResult = false;
        MockReceiver.sAction = null;
        MockReceiver.sResultCode = 0;
        mHandleResult = false;

        mPendingIntent.send(2, mFinish, null);
        Thread.sleep(WAIT_TIME);
        assertTrue(mFinishResult);
        assertFalse(mHandleResult);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

        // assert result code
        assertEquals(2, MockReceiver.sResultCode);
        mPendingIntent.cancel();

        mHandleResult = false;
        mFinishResult = false;
        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        MockReceiver.sAction = null;
        mPendingIntent.send(3, mFinish, mHandler);
        Thread.sleep(WAIT_TIME);
        assertTrue(mHandleResult);
        assertTrue(mFinishResult);
        assertEquals(MockReceiver.MOCKACTION, MockReceiver.sAction);

        // assert result code
        assertEquals(3, MockReceiver.sResultCode);
        mPendingIntent.cancel();
    }

    public void testSendWithParamContextIntIntentOnFinishedHandler() throws InterruptedException,
            CanceledException {
        mIntent = new Intent(MockReceiver.MOCKACTION);
        mIntent.setAction(MockReceiver.MOCKACTION);

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        mFinishResult = false;
        mResultAction = null;
        mHandleResult = false;
        mPendingIntent.send(mContext, 1, mIntent, null, null);
        Thread.sleep(WAIT_TIME);
        assertFalse(mFinishResult);
        assertFalse(mHandleResult);
        assertNull(mResultAction);
        mPendingIntent.cancel();

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        mFinishResult = false;
        mResultAction = null;
        mHandleResult = false;
        mPendingIntent.send(mContext, 1, mIntent, mFinish, null);
        Thread.sleep(WAIT_TIME);
        assertTrue(mFinishResult);
        assertEquals(mResultAction, MockReceiver.MOCKACTION);
        assertFalse(mHandleResult);
        mPendingIntent.cancel();

        mPendingIntent = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        mFinishResult = false;
        mResultAction = null;
        mHandleResult = false;
        mPendingIntent.send(mContext, 1, mIntent, mFinish, mHandler);
        Thread.sleep(WAIT_TIME);
        assertTrue(mHandleResult);
        assertEquals(mResultAction, MockReceiver.MOCKACTION);
        assertTrue(mFinishResult);
        mPendingIntent.cancel();
    }

    public void testGetTargetPackage() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        assertEquals(mContext.getPackageName(), mPendingIntent.getTargetPackage());
    }

    public void testEquals() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        PendingIntent target = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        assertFalse(mPendingIntent.equals(target));
        assertFalse(mPendingIntent.hashCode() == target.hashCode());
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent, 1);

        target = PendingIntent.getActivity(mContext, 1, mIntent, 1);
        assertTrue(mPendingIntent.equals(target));

        mIntent = new Intent(MockReceiver.MOCKACTION);
        target = PendingIntent.getBroadcast(mContext, 1, mIntent, 1);
        assertFalse(mPendingIntent.equals(target));
        assertFalse(mPendingIntent.hashCode() == target.hashCode());

        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent, 1);
        target = PendingIntent.getActivity(mContext, 1, mIntent, 1);

        assertTrue(mPendingIntent.equals(target));
        assertEquals(mPendingIntent.hashCode(), target.hashCode());
    }

    public void testDescribeContents() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        final int expected = 0;
        assertEquals(expected, mPendingIntent.describeContents());
    }

    public void testWriteToParcel() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        Parcel parcel = Parcel.obtain();

        mPendingIntent.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        PendingIntent pendingIntent = PendingIntent.CREATOR.createFromParcel(parcel);
        assertTrue(mPendingIntent.equals(pendingIntent));
    }

    public void testReadAndWritePendingIntentOrNullToParcel() {
        mIntent = new Intent();
        mPendingIntent = PendingIntent.getActivity(mContext, 1, mIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        assertNotNull(mPendingIntent.toString());

        Parcel parcel = Parcel.obtain();
        PendingIntent.writePendingIntentOrNullToParcel(mPendingIntent, parcel);
        parcel.setDataPosition(0);
        PendingIntent target = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        assertEquals(mPendingIntent, target);
        assertEquals(mPendingIntent.getTargetPackage(), target.getTargetPackage());

        mPendingIntent = null;
        parcel = Parcel.obtain();
        PendingIntent.writePendingIntentOrNullToParcel(mPendingIntent, parcel);
        target = PendingIntent.readPendingIntentOrNullFromParcel(parcel);
        assertNull(target);
    }

}
