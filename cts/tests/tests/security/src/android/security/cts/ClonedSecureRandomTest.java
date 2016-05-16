/*
 * Copyright 2013 The Android Open Source Project
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

package android.security.cts;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.security.cts.activity.ISecureRandomService;
import android.security.cts.activity.SecureRandomService;
import android.test.AndroidTestCase;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClonedSecureRandomTest extends AndroidTestCase {
    private static final int ANSWER_TIMEOUT_SECONDS = 60;

    private static final String SEPARATE_PROCESS_NAME = ":secureRandom";

    private static final int MAX_PID = 32768;

    /**
     * Attempt to burn through PIDs faster after this many iterations to reach a
     * wrap-around point faster.
     */
    private static final int PRIMING_ITERATIONS = 128;

    private static final int RANDOM_BYTES_PER_PID = 8;

    private static final int MAX_PIDS_WASTED = 1024;

    private static final int PID_WASTING_SKIP_LOWER = 64;

    private static final int PID_WASTING_SKIP_UPPER = 2048;

    private volatile CountDownLatch mLatch;

    private Intent mSeparateIntent;

    private ISecureRandomService mSecureRandomService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mSecureRandomService = ISecureRandomService.Stub.asInterface(service);
            mLatch.countDown();
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    private boolean mHasDisconnected;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mSeparateIntent = new Intent(getContext(), SecureRandomService.class);
    }

    /**
     * This test spawns a Service in a new process to check the initial state of
     * SecureRandom. It then attempts to make the PID number wrap around so it
     * sees a new process with the same PID twice. The test completes when it
     * sees two newly started processes with the same PID and compares their
     * output.
     */
    public void testCheckForDuplicateOutput() throws Exception {
        assertEquals("Only supports up to " + MAX_PID + " because of memory requirements",
                Integer.toString(MAX_PID), getFirstLineFromFile("/proc/sys/kernel/pid_max"));

        final String packageName = getContext().getPackageName();
        String separateProcessName = packageName + SEPARATE_PROCESS_NAME;

        /*
         * Using a byte[][] and BitSet gives us a fixed upper bound for the
         * memory cost of this test. One could possibly use a SparseArray if the
         * upper bound becomes too large (for instance, if PID_MAX is large),
         * only keep track of a smaller number of outputs, and just cause a
         * wrap-around of PIDs to keep the test working.
         */
        byte[][] outputs = new byte[MAX_PID][RANDOM_BYTES_PER_PID];
        BitSet seenPids = new BitSet(MAX_PID);

        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        int myPid = android.os.Process.myPid();

        /*
         * We're guaranteed to see at least one duplicate if we iterate MAX_PID
         * number of times because of the pigeonhole principle. In an attempt to
         * hit a collision faster, first get a closely-spaced sampling of PIDs
         * then spin up a bunch of threads locally to get us closer to wrapping
         * around to the first PID.
         */
        int firstPid = -1;
        int previousPid = -1;
        for (int i = 0; i < MAX_PID; i++) {
            byte[] output = new byte[RANDOM_BYTES_PER_PID];
            int pid;

            mLatch = new CountDownLatch(1);
            getContext().startService(mSeparateIntent);
            getContext().bindService(mSeparateIntent, mServiceConnection, 0);
            if (!mLatch.await(ANSWER_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                fail("Timeout waiting for answer from SecureRandomService; cannot complete test");
            }

            pid = mSecureRandomService.getRandomBytesAndPid(output);

            getContext().unbindService(mServiceConnection);
            getContext().stopService(mSeparateIntent);
            am.killBackgroundProcesses(packageName);

            /*
             * Make sure the AndroidManifest.xml wasn't altered in a way that
             * breaks the test.
             */
            assertFalse("SecureRandomService must run in a different process. Check "
                    + "AndroidManifest.xml to ensure it has a unique android:process=\"...\"",
                    myPid == pid);

            // We didn't get a new process for some reason. Try again.
            if (previousPid == pid) {
                i--;
                continue;
            } else if (previousPid == -1 && firstPid == -1) {
                /*
                 * The first time around, we'll discard the output. This is
                 * needed because we don't know if the SecureRandomService instance
                 * has been running before or not. To be consistent, we only
                 * want the first outputs from SecureRandom for this test.
                 */
                i--;
                previousPid = pid;
                continue;
            } else {
                previousPid = pid;
            }

            if (seenPids.get(pid)) {
                assertFalse("SecureRandom should not output the same value twice (pid=" + pid
                                + ", output=" + Arrays.toString(output) + ", outputs[pid]="
                                + Arrays.toString(outputs[pid]) + ")",
                        Arrays.equals(output, outputs[pid]));
                return;
            }

            seenPids.set(pid);
            System.arraycopy(output, 0, outputs[pid], 0, output.length);

            if (firstPid == -1) {
                firstPid = pid;
            }

            if (i > PRIMING_ITERATIONS) {
                wastePids(firstPid, previousPid);
            }
        }

        /*
         * This should never be reached unless the test was altered to break it.
         * Since we're looping until we see PID_MAX unique answers, we must have
         * seen a duplicate by the pigeonhole principle.
         */
        fail("Must see a duplicate PID");
    }

    /**
     * This is an attempt to get the PIDs to roll over faster. Threads use up
     * PIDs on Android and spawning a new thread is much faster than having
     * another service spawned as we are doing in this test.
     */
    private static void wastePids(int firstPid, int previousPid) {
        int distance = (firstPid - previousPid + MAX_PID) % MAX_PID;

        // Don't waste PIDs if we're close to wrap-around to improve odds of
        // collision.
        if ((distance < PID_WASTING_SKIP_LOWER) || (MAX_PID - distance < PID_WASTING_SKIP_UPPER)) {
            return;
        }

        for (int i = 0; i < distance; i++) {
            Thread t = new Thread();
            t.start();
        }
    }

    private static String getFirstLineFromFile(String filename) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(filename));
            final String line = in.readLine();
            if (line == null) {
                throw new EOFException("EOF encountered before reading first line of " + filename);
            }
            return line.trim();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
