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


import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.test.suitebuilder.annotation.MediumTest;

public class ServiceTest extends ActivityTestsBase {
    private static final int STATE_START_1 = 0;
    private static final int STATE_START_2 = 1;
    private static final int STATE_UNBIND = 2;
    private static final int STATE_DESTROY = 3;
    private static final int STATE_REBIND = 4;
    private static final int STATE_UNBIND_ONLY = 5;
    private static final int DELAY = 5000;
    private static final
        String EXIST_CONN_TO_RECEIVE_SERVICE = "existing connection to receive service";
    private static final String EXIST_CONN_TO_LOSE_SERVICE = "existing connection to lose service";
    private int mExpectedServiceState;
    private Context mContext;
    private Intent mLocalService;
    private Intent mLocalDeniedService;
    private Intent mLocalGrantedService;
    private Intent mLocalService_ApplicationHasPermission;
    private Intent mLocalService_ApplicationDoesNotHavePermission;

    private IBinder mStateReceiver;

    private static class EmptyConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }

    private class TestConnection implements ServiceConnection {
        private final boolean mExpectDisconnect;
        private final boolean mSetReporter;
        private boolean mMonitor;
        private int mCount;

        public TestConnection(boolean expectDisconnect, boolean setReporter) {
            mExpectDisconnect = expectDisconnect;
            mSetReporter = setReporter;
            mMonitor = !setReporter;
        }

        void setMonitor(boolean v) {
            mMonitor = v;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (mSetReporter) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken(LocalService.SERVICE_LOCAL);
                data.writeStrongBinder(mStateReceiver);
                try {
                    service.transact(LocalService.SET_REPORTER_CODE, data, null, 0);
                } catch (RemoteException e) {
                    finishBad("DeadObjectException when sending reporting object");
                }
                data.recycle();
            }

            if (mMonitor) {
                mCount++;
                if (mExpectedServiceState == STATE_START_1) {
                    if (mCount == 1) {
                        finishGood();
                    } else {
                        finishBad("onServiceConnected() again on an object when it "
                                + "should have been the first time");
                    }
                } else if (mExpectedServiceState == STATE_START_2) {
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
            if (mMonitor) {
                if (mExpectedServiceState == STATE_DESTROY) {
                    if (mExpectDisconnect) {
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

    private void startExpectResult(Intent service) {
        startExpectResult(service, new Bundle());
    }

    private void startExpectResult(Intent service, Bundle bundle) {
        bundle.putParcelable(LocalService.REPORT_OBJ_NAME, new IBinderParcelable(mStateReceiver));

        boolean success = false;
        try {
            mExpectedServiceState = STATE_START_1;
            mContext.startService(new Intent(service).putExtras(bundle));
            waitForResultOrThrow(DELAY, "service to start first time");
            mExpectedServiceState = STATE_START_2;
            mContext.startService(new Intent(service).putExtras(bundle));
            waitForResultOrThrow(DELAY, "service to start second time");
            success = true;
        } finally {
            if (!success) {
                mContext.stopService(service);
            }
        }
        mExpectedServiceState = STATE_DESTROY;
        mContext.stopService(service);
        waitForResultOrThrow(DELAY, "service to be destroyed");
    }

    private void startExpectNoPermission(Intent service) {
        try {
            mContext.startService(service);
            fail("Expected security exception when starting " + service);
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * test the service lifecycle, a service can be used in two ways:
     * 1  It can be started and allowed to run until someone stops it or it stops itself.
     *    In this mode, it's started by calling Context.startService()
     *    and stopped by calling Context.stopService().
     *    It can stop itself by calling Service.stopSelf() or Service.stopSelfResult().
     *    Only one stopService() call is needed to stop the service,
     *    no matter how many times startService() was called.
     * 2  It can be operated programmatically using an interface that it defines and exports.
     *    Clients establish a connection to the Service object
     *    and use that connection to call into the service.
     *    The connection is established by calling Context.bindService(),
     *    and is closed by calling Context.unbindService().
     *    Multiple clients can bind to the same service.
     *    If the service has not already been launched, bindService() can optionally launch it.
     */
    private void bindExpectResult(Intent service) {
        TestConnection conn = new TestConnection(true, false);
        TestConnection conn2 = new TestConnection(false, false);
        boolean success = false;
        try {
            // Expect to see the TestConnection connected.
            mExpectedServiceState = STATE_START_1;
            mContext.bindService(service, conn, 0);
            mContext.startService(service);
            waitForResultOrThrow(DELAY, EXIST_CONN_TO_RECEIVE_SERVICE);

            // Expect to see the second TestConnection connected.
            mContext.bindService(service, conn2, 0);
            waitForResultOrThrow(DELAY, "new connection to receive service");

            mContext.unbindService(conn2);
            success = true;
        } finally {
            if (!success) {
                mContext.unbindService(conn);
                mContext.unbindService(conn2);
                mContext.stopService(service);
            }
        }

        // Expect to see the TestConnection disconnected.
        mExpectedServiceState = STATE_DESTROY;
        mContext.stopService(service);
        waitForResultOrThrow(DELAY, EXIST_CONN_TO_LOSE_SERVICE);

        mContext.unbindService(conn);

        conn = new TestConnection(true, true);
        success = false;
        try {
            // Expect to see the TestConnection connected.
            conn.setMonitor(true);
            mExpectedServiceState = STATE_START_1;
            mContext.bindService(service, conn, 0);
            mContext.startService(service);
            waitForResultOrThrow(DELAY, EXIST_CONN_TO_RECEIVE_SERVICE);

            success = true;
        } finally {
            if (!success) {
                mContext.unbindService(conn);
                mContext.stopService(service);
            }
        }

        // Expect to see the service unbind and then destroyed.
        conn.setMonitor(false);
        mExpectedServiceState = STATE_UNBIND;
        mContext.stopService(service);
        waitForResultOrThrow(DELAY, EXIST_CONN_TO_LOSE_SERVICE);

        mContext.unbindService(conn);

        conn = new TestConnection(true, true);
        success = false;
        try {
            // Expect to see the TestConnection connected.
            conn.setMonitor(true);
            mExpectedServiceState = STATE_START_1;
            mContext.bindService(service, conn, 0);
            mContext.startService(service);
            waitForResultOrThrow(DELAY, EXIST_CONN_TO_RECEIVE_SERVICE);

            success = true;
        } finally {
            if (!success) {
                mContext.unbindService(conn);
                mContext.stopService(service);
            }
        }

        // Expect to see the service unbind but not destroyed.
        conn.setMonitor(false);
        mExpectedServiceState = STATE_UNBIND_ONLY;
        mContext.unbindService(conn);
        waitForResultOrThrow(DELAY, "existing connection to unbind service");

        // Expect to see the service rebound.
        mExpectedServiceState = STATE_REBIND;
        mContext.bindService(service, conn, 0);
        waitForResultOrThrow(DELAY, "existing connection to rebind service");

        // Expect to see the service unbind and then destroyed.
        mExpectedServiceState = STATE_UNBIND;
        mContext.stopService(service);
        waitForResultOrThrow(DELAY, EXIST_CONN_TO_LOSE_SERVICE);

        mContext.unbindService(conn);
    }

    /**
     * test automatically create the service as long as the binding exists
     * and disconnect from an application service
     */
    private void bindAutoExpectResult(Intent service) {
        TestConnection conn = new TestConnection(false, true);
        boolean success = false;
        try {
            conn.setMonitor(true);
            mExpectedServiceState = STATE_START_1;
            mContext.bindService(
                    service, conn, Context.BIND_AUTO_CREATE);
            waitForResultOrThrow(DELAY, "connection to start and receive service");
            success = true;
        } finally {
            if (!success) {
                mContext.unbindService(conn);
            }
        }
        mExpectedServiceState = STATE_UNBIND;
        mContext.unbindService(conn);
        waitForResultOrThrow(DELAY, "disconnecting from service");
    }

    private void bindExpectNoPermission(Intent service) {
        TestConnection conn = new TestConnection(false, false);
        try {
            mContext.bindService(service, conn, Context.BIND_AUTO_CREATE);
            fail("Expected security exception when binding " + service);
        } catch (SecurityException e) {
            // expected
        } finally {
            mContext.unbindService(conn);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mLocalService = new Intent(mContext, LocalService.class);
        mLocalDeniedService = new Intent(mContext, LocalDeniedService.class);
        mLocalGrantedService = new Intent(mContext, LocalGrantedService.class);
        mLocalService_ApplicationHasPermission = new Intent(LocalService.SERVICE_LOCAL_GRANTED);
        mLocalService_ApplicationDoesNotHavePermission = new Intent(LocalService.SERVICE_LOCAL_DENIED);
        mStateReceiver = new MockBinder();
    }

    private class MockBinder extends Binder {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply,
                int flags) throws RemoteException {
            if (code == LocalService.STARTED_CODE) {
                data.enforceInterface(LocalService.SERVICE_LOCAL);
                int count = data.readInt();
                if (mExpectedServiceState == STATE_START_1) {
                    if (count == 1) {
                        finishGood();
                    } else {
                        finishBad("onStart() again on an object when it "
                                + "should have been the first time");
                    }
                } else if (mExpectedServiceState == STATE_START_2) {
                    if (count == 2) {
                        finishGood();
                    } else {
                        finishBad("onStart() the first time on an object when it "
                                + "should have been the second time");
                    }
                } else {
                    finishBad("onStart() was called when not expected (state="
                            + mExpectedServiceState + ")");
                }
                return true;
            } else if (code == LocalService.DESTROYED_CODE) {
                data.enforceInterface(LocalService.SERVICE_LOCAL);
                if (mExpectedServiceState == STATE_DESTROY) {
                    finishGood();
                } else {
                    finishBad("onDestroy() was called when not expected (state="
                            + mExpectedServiceState + ")");
                }
                return true;
            } else if (code == LocalService.UNBIND_CODE) {
                data.enforceInterface(LocalService.SERVICE_LOCAL);
                if (mExpectedServiceState == STATE_UNBIND) {
                    mExpectedServiceState = STATE_DESTROY;
                } else if (mExpectedServiceState == STATE_UNBIND_ONLY) {
                    finishGood();
                } else {
                    finishBad("onUnbind() was called when not expected (state="
                            + mExpectedServiceState + ")");
                }
                return true;
            } else if (code == LocalService.REBIND_CODE) {
                data.enforceInterface(LocalService.SERVICE_LOCAL);
                if (mExpectedServiceState == STATE_REBIND) {
                    finishGood();
                } else {
                    finishBad("onRebind() was called when not expected (state="
                            + mExpectedServiceState + ")");
                }
                return true;
            } else {
                return super.onTransact(code, data, reply, flags);
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mContext.stopService(mLocalService);
        mContext.stopService(mLocalGrantedService);
        mContext.stopService(mLocalService_ApplicationHasPermission);
    }

    public void testLocalStartClass() throws Exception {
        startExpectResult(mLocalService);
    }

    public void testLocalStartAction() throws Exception {
        startExpectResult(new Intent(LocalService.SERVICE_LOCAL));
    }

    public void testLocalBindClass() throws Exception {
        bindExpectResult(mLocalService);
    }

    @MediumTest
    public void testLocalBindAction() throws Exception {
        bindExpectResult(new Intent(LocalService.SERVICE_LOCAL));
    }

    @MediumTest
    public void testLocalBindAutoClass() throws Exception {
        bindAutoExpectResult(mLocalService);
    }

    @MediumTest
    public void testLocalBindAutoAction() throws Exception {
        bindAutoExpectResult(new Intent(LocalService.SERVICE_LOCAL));
    }

    @MediumTest
    public void testLocalStartClassPermissions() throws Exception {
        startExpectResult(mLocalGrantedService);
        startExpectResult(mLocalDeniedService);
    }

    @MediumTest
    public void testLocalStartActionPermissions() throws Exception {
        startExpectResult(mLocalService_ApplicationHasPermission);
        startExpectResult(mLocalService_ApplicationDoesNotHavePermission);
    }

    @MediumTest
    public void testLocalBindClassPermissions() throws Exception {
        bindExpectResult(mLocalGrantedService);
        bindExpectResult(mLocalDeniedService);
    }

    @MediumTest
    public void testLocalBindActionPermissions() throws Exception {
        bindExpectResult(mLocalService_ApplicationHasPermission);
        bindExpectResult(mLocalService_ApplicationDoesNotHavePermission);
    }

    @MediumTest
    public void testLocalBindAutoClassPermissionGranted() throws Exception {
        bindAutoExpectResult(mLocalGrantedService);
    }

    @MediumTest
    public void testLocalBindAutoActionPermissionGranted() throws Exception {
        bindAutoExpectResult(mLocalService_ApplicationHasPermission);
    }

    @MediumTest
    public void testLocalUnbindTwice() throws Exception {
        EmptyConnection conn = new EmptyConnection();
        mContext.bindService(
                mLocalService_ApplicationHasPermission, conn, 0);
        mContext.unbindService(conn);
        try {
            mContext.unbindService(conn);
            fail("No exception thrown on the second unbind");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
