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

package android.os.cts;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.PrintWriter;

import android.app.cts.ActivityTestsBase;
import android.app.cts.LocalService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;

public class BinderTest extends ActivityTestsBase {
    private static final String DESCRIPTOR_GOOGLE = "google";
    private static final String DESCRIPTOR_ANDROID = "android";
    // states of mStartState
    private static final int STATE_START_1 = 0;
    private static final int STATE_START_2 = 1;
    private static final int STATE_UNBIND = 2;
    private static final int STATE_DESTROY = 3;
    private static final int STATE_REBIND = 4;
    private static final int STATE_UNBIND_ONLY = 5;
    private static final int DELAY_MSEC = 5000;
    private MockBinder mBinder;
    private Binder mStartReceiver;
    private int mStartState;
    private Intent mService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mService = new Intent(LocalService.SERVICE_LOCAL);
        mBinder = new MockBinder();
        mStartReceiver = new Binder() {
            @Override
            protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                             throws RemoteException {
                switch (code) {
                    case LocalService.STARTED_CODE:
                        data.enforceInterface(LocalService.SERVICE_LOCAL);
                        int count = data.readInt();

                        switch (mStartState) {
                            case STATE_START_1:
                                if (count == 1) {
                                    finishGood();
                                } else {
                                    finishBad("onStart() again on an object when it "
                                            + "should have been the first time");
                                }
                                break;
                            case STATE_START_2:
                                if (count == 2) {
                                    finishGood();
                                } else {
                                    finishBad("onStart() the first time on an object when it "
                                            + "should have been the second time");
                                }
                                break;
                            default:
                                finishBad("onStart() was called when not expected (state="
                                        + mStartState + ")");
                        }
                        return true;
                    case LocalService.DESTROYED_CODE:
                        data.enforceInterface(LocalService.SERVICE_LOCAL);
                        if (mStartState == STATE_DESTROY) {
                            finishGood();
                        } else {
                            finishBad("onDestroy() was called when not expected (state="
                                    + mStartState + ")");
                        }
                        return true;
                    case LocalService.UNBIND_CODE:
                        data.enforceInterface(LocalService.SERVICE_LOCAL);
                        switch (mStartState) {
                            case STATE_UNBIND:
                                mStartState = STATE_DESTROY;
                                break;
                            case STATE_UNBIND_ONLY:
                                finishGood();
                                break;
                            default:
                                finishBad("onUnbind() was called when not expected (state="
                                        + mStartState + ")");
                        }
                        return true;
                    case LocalService.REBIND_CODE:
                        data.enforceInterface(LocalService.SERVICE_LOCAL);
                        if (mStartState == STATE_REBIND) {
                            finishGood();
                        } else {
                            finishBad("onRebind() was called when not expected (state="
                                    + mStartState + ")");
                        }
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
        };

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mContext.stopService(mService);
    }

    // Mock ServiceConnection
    public class MockServiceConnection implements ServiceConnection {
        private final boolean mIsDisconnect;
        private final boolean mSetReporter;
        private boolean mIsMonitorEnable;
        private int mCount;

        public MockServiceConnection(final boolean isDisconnect, final boolean setReporter) {
            mIsDisconnect = isDisconnect;
            mSetReporter = setReporter;
            mIsMonitorEnable = !setReporter;
        }

        void setMonitor(boolean v) {
            mIsMonitorEnable = v;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (mSetReporter) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken(LocalService.SERVICE_LOCAL);
                data.writeStrongBinder(mStartReceiver);

                try {
                    service.transact(LocalService.SET_REPORTER_CODE, data, null, 0);
                } catch (RemoteException e) {
                    finishBad("DeadObjectException when sending reporting object");
                }

                data.recycle();
            }

            if (mIsMonitorEnable) {
                mCount++;

                if (mStartState == STATE_START_1) {
                    if (mCount == 1) {
                        finishGood();
                    } else {
                        finishBad("onServiceConnected() again on an object when it "
                                + "should have been the first time");
                    }
                } else if (mStartState == STATE_START_2) {
                    if (mCount == 2) {
                        finishGood();
                    } else {
                        finishBad("onServiceConnected() the first time on an object "
                                + "when it should have been the second time");
                    }
                } else {
                    finishBad("onServiceConnected() called unexpectedly");
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            if (mIsMonitorEnable) {
                if (mStartState == STATE_DESTROY) {
                    if (mIsDisconnect) {
                        finishGood();
                    } else {
                        finishBad("onServiceDisconnected() when it shouldn't have been");
                    }
                } else {
                    finishBad("onServiceDisconnected() called unexpectedly");
                }
            }
        }
    }

    public void testTransact() {
        MockServiceConnection conn1 = new MockServiceConnection(true, false);
        MockServiceConnection conn2 = new MockServiceConnection(false, false);
        boolean success = false;

        try {
            // Expect to see the TestConnection connected.
            mStartState = STATE_START_1;
            getContext().bindService(mService, conn1, 0);
            getContext().startService(mService);
            waitForResultOrThrow(DELAY_MSEC, "existing connection to receive service");

            // Expect to see the second TestConnection connected.
            getContext().bindService(mService, conn2, 0);
            waitForResultOrThrow(DELAY_MSEC, "new connection to receive service");

            getContext().unbindService(conn2);
            success = true;
        } finally {
            if (!success) {
                try {
                getContext().stopService(mService);
                getContext().unbindService(conn1);
                getContext().unbindService(conn2);
                } catch (SecurityException e) {
                    fail(e.getMessage());
                }
            }
        }

        // Expect to see the TestConnection disconnected.
        mStartState = STATE_DESTROY;
        getContext().stopService(mService);
        waitForResultOrThrow(DELAY_MSEC, "the existing connection to lose service");

        getContext().unbindService(conn1);

        conn1 = new MockServiceConnection(true, true);
        success = false;

        try {
            // Expect to see the TestConnection connected.
            conn1.setMonitor(true);
            mStartState = STATE_START_1;
            getContext().bindService(mService, conn1, 0);
            getContext().startService(mService);
            waitForResultOrThrow(DELAY_MSEC, "the existing connection to receive service");

            success = true;
        } finally {
            if (!success) {
                try {
                    getContext().stopService(mService);
                    getContext().unbindService(conn1);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }

        // Expect to see the service unbind and then destroyed.
        conn1.setMonitor(false);
        mStartState = STATE_UNBIND;
        getContext().stopService(mService);
        waitForResultOrThrow(DELAY_MSEC, "the existing connection to lose service");

        getContext().unbindService(conn1);

        conn1 = new MockServiceConnection(true, true);
        success = false;

        try {
            // Expect to see the TestConnection connected.
            conn1.setMonitor(true);
            mStartState = STATE_START_1;
            getContext().bindService(mService, conn1, 0);
            getContext().startService(mService);
            waitForResultOrThrow(DELAY_MSEC, "existing connection to receive service");

            success = true;
        } finally {
            if (!success) {
                try {
                    getContext().stopService(mService);
                    getContext().unbindService(conn1);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            }
        }

        // Expect to see the service unbind but not destroyed.
        conn1.setMonitor(false);
        mStartState = STATE_UNBIND_ONLY;
        getContext().unbindService(conn1);
        waitForResultOrThrow(DELAY_MSEC, "existing connection to unbind service");

        // Expect to see the service rebound.
        mStartState = STATE_REBIND;
        getContext().bindService(mService, conn1, 0);
        waitForResultOrThrow(DELAY_MSEC, "existing connection to rebind service");

        // Expect to see the service unbind and then destroyed.
        mStartState = STATE_UNBIND;
        getContext().stopService(mService);
        waitForResultOrThrow(DELAY_MSEC, "existing connection to lose service");

        getContext().unbindService(conn1);
    }

    public void testSimpleMethods() {
        new Binder();

        assertEquals(Process.myPid(), Binder.getCallingPid());
        assertEquals(Process.myUid(), Binder.getCallingUid());

        final String[] dumpArgs = new String[]{"one", "two", "three"};
        mBinder.dump(new FileDescriptor(),
                new PrintWriter(new ByteArrayOutputStream()),
                dumpArgs);

        mBinder.dump(new FileDescriptor(), dumpArgs);
        assertTrue(mBinder.isBinderAlive());

        mBinder.linkToDeath(new MockDeathRecipient(), 0);

        assertTrue(mBinder.unlinkToDeath(new MockDeathRecipient(), 0));

        assertTrue(mBinder.pingBinder());
    }

    public void testFlushPendingCommands() {
        Binder.flushPendingCommands();
    }

    public void testJoinThreadPool() {
        //from java doc this function won't be return until the current process is exiting.
        //so not suitable to test it in unit test
    }

    public void testClearCallingIdentity() {
        long token = Binder.clearCallingIdentity();
        assertTrue(token > 0);
        Binder.restoreCallingIdentity(token);
    }

    public void testInterfaceRelatedMethods() {
        assertNull(mBinder.getInterfaceDescriptor());
        mBinder.attachInterface(new MockIInterface(), DESCRIPTOR_GOOGLE);
        assertEquals(DESCRIPTOR_GOOGLE, mBinder.getInterfaceDescriptor());

        mBinder.attachInterface(new MockIInterface(), DESCRIPTOR_ANDROID);
        assertNull(mBinder.queryLocalInterface(DESCRIPTOR_GOOGLE));
        mBinder.attachInterface(new MockIInterface(), DESCRIPTOR_GOOGLE);
        assertNotNull(mBinder.queryLocalInterface(DESCRIPTOR_GOOGLE));
    }

    private static class MockDeathRecipient implements IBinder.DeathRecipient {
         public void binderDied() {

         }
    }

    private static class MockIInterface implements IInterface {
        public IBinder asBinder() {
            return new Binder();
        }
    }

    private static class MockBinder extends Binder {
        @Override
        public void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
            super.dump(fd, fout, args);
        }
    }

}
