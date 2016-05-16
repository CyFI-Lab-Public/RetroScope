/*
 * Copyright (C) 2009 The Android Open Source Project
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
package android.telephony.cts;

import android.content.Context;
import android.os.Looper;
import android.os.cts.TestThread;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.test.AndroidTestCase;

public class PhoneStateListenerTest extends AndroidTestCase {

    public static final long WAIT_TIME = 1000;

    private boolean mOnCallForwardingIndicatorChangedCalled;
    private boolean mOnCallStateChangedCalled;
    private boolean mOnCellLocationChangedCalled;
    private boolean mOnDataActivityCalled;
    private boolean mOnDataConnectionStateChangedCalled;
    private boolean mOnMessageWaitingIndicatorChangedCalled;
    private boolean mOnServiceStateChangedCalled;
    private boolean mOnSignalStrengthChangedCalled;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mListener;
    private final Object mLock = new Object();
    private Looper mLooper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Context context = getContext();
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mLooper != null) {
            mLooper.quit();
        }
        if (mListener != null) {
            // unregister the listener
            mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    public void testPhoneStateListener() {
        new PhoneStateListener();
    }

    /*
     * The tests below rely on the framework to immediately call the installed listener upon
     * registration. There is no simple way to emulate state changes for testing the listeners.
     */

    public void testOnServiceStateChanged() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mListener = new PhoneStateListener() {
                    @Override
                    public void onServiceStateChanged(ServiceState serviceState) {
                        synchronized(mLock) {
                            mOnServiceStateChangedCalled = true;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_SERVICE_STATE);

                Looper.loop();
            }
        });

        assertFalse(mOnServiceStateChangedCalled);
        t.start();

        synchronized (mLock) {
            while(!mOnServiceStateChangedCalled){
                mLock.wait();
            }
        }
        quitLooper();
        t.checkException();
        assertTrue(mOnServiceStateChangedCalled);
    }

    private void quitLooper() {
        mLooper.quit();
        mLooper = null;
    }

    public void testOnSignalStrengthChanged() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mLooper = Looper.myLooper();
                mListener = new PhoneStateListener() {
                    @Override
                    public void onSignalStrengthChanged(int asu) {
                        synchronized(mLock) {
                            mOnSignalStrengthChangedCalled = true;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTH);

                Looper.loop();
            }
        });

        assertFalse(mOnSignalStrengthChangedCalled);
        t.start();

        synchronized (mLock) {
            while(!mOnSignalStrengthChangedCalled){
                mLock.wait();
            }
        }
        quitLooper();
        t.checkException();
        assertTrue(mOnSignalStrengthChangedCalled);
    }

    public void testOnMessageWaitingIndicatorChanged() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mLooper = Looper.myLooper();
                mListener = new PhoneStateListener() {
                    @Override
                    public void onMessageWaitingIndicatorChanged(boolean mwi) {
                        synchronized(mLock) {
                            mOnMessageWaitingIndicatorChangedCalled = true;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(
                        mListener, PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR);

                Looper.loop();
            }
        });

        assertFalse(mOnMessageWaitingIndicatorChangedCalled);
        t.start();

        synchronized (mLock) {
            while(!mOnMessageWaitingIndicatorChangedCalled){
                mLock.wait();
            }
        }
        quitLooper();
        t.checkException();
        assertTrue(mOnMessageWaitingIndicatorChangedCalled);
    }

    public void testOnCallForwardingIndicatorChanged() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mLooper = Looper.myLooper();
                mListener = new PhoneStateListener() {
                    @Override
                    public void onCallForwardingIndicatorChanged(boolean cfi) {
                        synchronized(mLock) {
                            mOnCallForwardingIndicatorChangedCalled = true;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(
                        mListener, PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR);

                Looper.loop();
            }
        });

        assertFalse(mOnCallForwardingIndicatorChangedCalled);
        t.start();

        synchronized (mLock) {
            while(!mOnCallForwardingIndicatorChangedCalled){
                mLock.wait();
            }
        }
        quitLooper();
        t.checkException();
        assertTrue(mOnCallForwardingIndicatorChangedCalled);
    }

    public void testOnCellLocationChanged() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mLooper = Looper.myLooper();
                mListener = new PhoneStateListener() {
                    @Override
                    public void onCellLocationChanged(CellLocation location) {
                        synchronized(mLock) {
                            mOnCellLocationChangedCalled = true;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CELL_LOCATION);

                Looper.loop();
            }
        });

        assertFalse(mOnCellLocationChangedCalled);
        t.start();

        synchronized (mLock) {
            while(!mOnCellLocationChangedCalled){
                mLock.wait();
            }
        }
        quitLooper();
        t.checkException();
        assertTrue(mOnCellLocationChangedCalled);
    }

    public void testOnCallStateChanged() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mLooper = Looper.myLooper();
                mListener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(int state, String incomingNumber) {
                        synchronized(mLock) {
                            mOnCallStateChangedCalled = true;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_CALL_STATE);

                Looper.loop();
            }
        });

        assertFalse(mOnCallStateChangedCalled);
        t.start();

        synchronized (mLock) {
            while(!mOnCallStateChangedCalled){
                mLock.wait();
            }
        }
        quitLooper();
        t.checkException();
        assertTrue(mOnCallStateChangedCalled);
    }

    public void testOnDataConnectionStateChanged() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mLooper = Looper.myLooper();
                mListener = new PhoneStateListener() {
                    @Override
                    public void onDataConnectionStateChanged(int state) {
                        synchronized(mLock) {
                            mOnDataConnectionStateChangedCalled = true;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(
                        mListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

                Looper.loop();
            }
        });

        assertFalse(mOnDataConnectionStateChangedCalled);
        t.start();

        synchronized (mLock) {
            while(!mOnDataConnectionStateChangedCalled){
                mLock.wait();
            }
        }
        quitLooper();
        t.checkException();
        assertTrue(mOnDataConnectionStateChangedCalled);
    }

    public void testOnDataActivity() throws Throwable {
        TestThread t = new TestThread(new Runnable() {
            public void run() {
                Looper.prepare();

                mLooper = Looper.myLooper();
                mListener = new PhoneStateListener() {
                    @Override
                    public void onDataActivity(int direction) {
                        synchronized(mLock) {
                            mOnDataActivityCalled = true;
                            mLock.notify();
                        }
                    }
                };
                mTelephonyManager.listen(mListener, PhoneStateListener.LISTEN_DATA_ACTIVITY);

                Looper.loop();
            }
        });

        assertFalse(mOnDataActivityCalled);
        t.start();

        synchronized (mLock) {
            while(!mOnDataActivityCalled){
                mLock.wait();
            }
        }
        quitLooper();
        t.checkException();
        assertTrue(mOnDataActivityCalled);
    }
}
