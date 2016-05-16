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

import android.app.cts.ISecondary;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Process;
import android.test.AndroidTestCase;
import android.util.Log;

public class ProcessTest extends AndroidTestCase {

    public static final int THREAD_PRIORITY_HIGHEST = -20;
    private static final String NONE_EXISITENT_NAME = "abcdefcg";
    private static final String WRONG_CACHE_NAME = "cache_abcdefg";
    private static final String PROCESS_SHELL= "shell";
    private static final String PROCESS_CACHE= "cache";
    private static final String REMOTE_SERVICE = "android.app.REMOTESERVICE";
    private static final String TAG = "ProcessTest";
    private ISecondary mSecondaryService = null;
    private Intent mIntent;
    private Object mSync;
    private boolean mHasConnected;
    private boolean mHasDisconnected;
    private ServiceConnection mSecondaryConnection;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSync = new Object();
        mSecondaryConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                    IBinder service) {
                // Connecting to a secondary interface is the same as any
                // other interface.
                android.util.Log.d(TAG, "connected");
                mSecondaryService = ISecondary.Stub.asInterface(service);
                synchronized (mSync) {
                    mHasConnected = true;
                    mSync.notify();
                }
            }
            public void onServiceDisconnected(ComponentName className) {
                Log.d(TAG, "disconnected");
                mSecondaryService = null;
                synchronized (mSync) {
                    mHasDisconnected = true;
                    mSync.notify();
                }
            }
        };
        mIntent = new Intent(REMOTE_SERVICE);
        getContext().startService(mIntent);
        getContext().bindService(new Intent(ISecondary.class.getName()),
                mSecondaryConnection, Context.BIND_AUTO_CREATE);
        synchronized (mSync) {
            if (!mHasConnected) {
                try {
                    mSync.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (mIntent != null) {
            getContext().stopService(mIntent);
        }
        if (mSecondaryConnection != null) {
            getContext().unbindService(mSecondaryConnection);
        }
    }

    public void testMiscMethods() {
        /*
         * Test setThreadPriority(int) and setThreadPriority(int, int)
         * 1.Set the priority of the calling thread, based on Linux priorities level,
         * from -20 for highest scheduling priority to 19 for lowest scheduling priority.
         * 2.Throws IllegalArgumentException if tid does not exist.
         */
        int myTid = Process.myTid();

        int priority = Process.getThreadPriority(myTid);
        assertTrue(priority >= THREAD_PRIORITY_HIGHEST
                && priority <= Process.THREAD_PRIORITY_LOWEST);

        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
        assertEquals(Process.THREAD_PRIORITY_AUDIO, Process.getThreadPriority(myTid));

        Process.setThreadPriority(myTid, Process.THREAD_PRIORITY_LOWEST);
        assertEquals(Process.THREAD_PRIORITY_LOWEST, Process.getThreadPriority(myTid));

        Process.setThreadPriority(myTid, THREAD_PRIORITY_HIGHEST);
        assertEquals(THREAD_PRIORITY_HIGHEST, Process.getThreadPriority(myTid));

        int invalidPriority = THREAD_PRIORITY_HIGHEST - 1;
        Process.setThreadPriority(myTid, invalidPriority);
        assertEquals(THREAD_PRIORITY_HIGHEST, Process.getThreadPriority(myTid));

        try {
            Process.setThreadPriority(-1, Process.THREAD_PRIORITY_DEFAULT);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expect
        } // Hard to address logic of throws SecurityException

        /*
         * Returns the UID assigned to a particular user name, or -1 if there is
         * none.  If the given string consists of only numbers, it is converted
         * directly to a uid.
         */
        assertTrue(Process.getUidForName(PROCESS_SHELL) > 0);
        assertEquals(-1, Process.getUidForName(NONE_EXISITENT_NAME));
        assertEquals(0, Process.getUidForName("0"));

        /*
         * Returns the GID assigned to a particular user name, or -1 if there is
         * none.  If the given string consists of only numbers, it is converted
         * directly to a gid.
         */
        assertTrue(Process.getGidForName(PROCESS_CACHE) > 0);
        assertEquals(-1, Process.getGidForName(WRONG_CACHE_NAME));
        assertEquals(0, Process.getGidForName("0"));

        assertTrue(Process.myUid() >= 0);
    }

    /**
     * Test point of killProcess(int)
     * Only the process running the caller's packages/application
     * and any additional processes created by that app be able to kill each other's processes.
     */
    public void testKillProcess() throws Exception {
        long time = 0;
        int servicePid = 0;
        try {
            servicePid = mSecondaryService.getPid();
            time = mSecondaryService.getElapsedCpuTime();
        } finally {
            getContext().stopService(mIntent);
            mIntent = null;
        }

        assertTrue(time > 0);
        assertTrue(servicePid != Process.myPid());

        Process.killProcess(servicePid);
        synchronized (mSync) {
            if (!mHasDisconnected) {
                try {
                    mSync.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        assertTrue(mHasDisconnected);
    }

    /**
     * Test myPid() point.
     * Returns the identifier of this process, which can be used with
     * {@link #killProcess} and {@link #sendSignal}.
     * Test sendSignal(int) point.
     * Send a signal to the given process.
     */
    public void testSendSignal() throws Exception {
        int servicePid = 0;
        try {
            servicePid = mSecondaryService.getPid();
        } finally {
            getContext().stopService(mIntent);
            mIntent = null;
        }
        assertTrue(servicePid != 0);
        assertTrue(Process.myPid() != servicePid);
        Process.sendSignal(servicePid, Process.SIGNAL_KILL);
        synchronized (mSync) {
            if (!mHasDisconnected) {
                try {
                    mSync.wait();
                } catch (InterruptedException e) {
                }
            }
        }
        assertTrue(mHasDisconnected);
    }
}
