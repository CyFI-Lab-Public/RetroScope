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

import android.content.Context;
import android.os.Vibrator;
import android.test.AndroidTestCase;
import android.util.Log;

public class VibratorTest extends AndroidTestCase {

    private Vibrator mVibrator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void testVibratorCancel() {
        try {
            mVibrator.vibrate(1000);
        } catch (Exception e) {
            fail("testVibratorCancel failed!");
        }
        sleep();
        try {
            mVibrator.cancel();
        } catch (Exception e) {
            fail("testVibratorCancel failed!");
        }
    }

    public void testVibratePattern() {
        long[] pattern = {100, 200, 400, 800, 1600};
        try {
            mVibrator.vibrate(pattern, 3);
        } catch (Exception e) {
            fail("vibrate failed!");
        }
        try {
            mVibrator.vibrate(pattern, 10);
            fail("Should throw ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        sleep();
    }

    public void testVibrateMultiThread() {
        Log.d("*******VibratorTest", "MultiTreadTest");
        new Thread(new Runnable() {
            public void run() {
                Log.d("*******VibratorTest", "Thread 1");
                try {
                    mVibrator.vibrate(100);
                } catch (Exception e) {
                    fail("MultiThread fail1");
                }
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                Log.d("*******VibratorTest", "Thread 2");
                try {
                    // This test only get two threads to run vibrator at the same time
                    // for a functional test,
                    // but it can not verify if the second thread get the precedence.
                    mVibrator.vibrate(1000);
                } catch (Exception e) {
                    fail("MultiThread fail2");
                }
            }
        }).start();
        sleep();
    }

    private void sleep() {
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
        }
    }
}
