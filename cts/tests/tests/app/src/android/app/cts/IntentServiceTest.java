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

package android.app.cts;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.cts.util.PollingCheck;
import android.os.IBinder;

import java.util.concurrent.Callable;


public class IntentServiceTest extends ActivityTestsBase {

    private Intent mIntent;
    private static final int TIMEOUT_MSEC = 30000;
    private boolean mConnected;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        IntentServiceStub.reset();
        mIntent = new Intent(mContext, IntentServiceStub.class);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mContext.stopService(mIntent);
    }

    public void testIntents() throws Throwable {
        final int value = 42;
        final int adds = 3;

        Intent addIntent = new Intent(mContext, IntentServiceStub.class);

        addIntent.setAction(IntentServiceStub.ISS_ADD);
        addIntent.putExtra(IntentServiceStub.ISS_VALUE, 42);

        for (int i = 0; i < adds; i++) {
            mContext.startService(addIntent);
        }

        PollingCheck.check("onHandleIntentCalled not called enough", TIMEOUT_MSEC,
                new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return IntentServiceStub.getOnHandleIntentCalledCount() == adds;
            }
        });

        PollingCheck.check("accumulator not correct", TIMEOUT_MSEC, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return IntentServiceStub.getAccumulator() == adds * value;
            }
        });

        PollingCheck.check("onDestroyCalled not called", TIMEOUT_MSEC, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return IntentServiceStub.isOnDestroyCalled();
            }
        });
    }

    public void testIntentServiceLifeCycle() throws Throwable {
        // start service
        mContext.startService(mIntent);
        new PollingCheck(TIMEOUT_MSEC) {
            protected boolean check() {
                return IntentServiceStub.getOnHandleIntentCalledCount() > 0;
            }
        }.run();
        assertTrue(IntentServiceStub.isOnCreateCalled());
        assertTrue(IntentServiceStub.isOnStartCalled());

        // bind service
        ServiceConnection conn = new TestConnection();
        mContext.bindService(mIntent, conn, Context.BIND_AUTO_CREATE);
        new PollingCheck(TIMEOUT_MSEC) {
            protected boolean check() {
                return mConnected;
            }
        }.run();
        assertTrue(IntentServiceStub.isOnBindCalled());

        // unbind service
        mContext.unbindService(conn);
        // stop service
        mContext.stopService(mIntent);
        IntentServiceStub.waitToFinish(TIMEOUT_MSEC);
    }

    private class TestConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName name, IBinder service) {
            mConnected = true;
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    }
}
