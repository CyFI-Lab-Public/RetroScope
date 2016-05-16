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


package android.permission.cts;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Vibrator;
import android.telephony.gsm.SmsManager;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.TimeZone;

/**
 * Verify the system function require specific permissions.
 */
@SuppressWarnings("deprecation")
public class NoSystemFunctionPermissionTest extends AndroidTestCase {

    /**
     * Verify that ActivityManager.restartPackage() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#RESTART_PACKAGES}.
     */
    @SmallTest
    public void testRestartPackage() {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(
                Context.ACTIVITY_SERVICE);

        try {
            activityManager.restartPackage("packageName");
            fail("ActivityManager.restartPackage() didn't throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that AlarmManager.setTimeZone() requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#SET_TIME_ZONE}.
     */
    @SmallTest
    public void testSetTimeZone() {
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(
                Context.ALARM_SERVICE);
        String[] timeZones = TimeZone.getAvailableIDs();
        String timeZone = timeZones[0];

        try {
            alarmManager.setTimeZone(timeZone);
            fail("AlarmManager.setTimeZone() did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that setting wallpaper relate methods require permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#SET_WALLPAPER}.
     * @throws IOException 
     */
    @SmallTest
    public void testSetWallpaper() throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);

        try {
            mContext.setWallpaper(bitmap);
            fail("Context.setWallpaper(BitMap) did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }

        try {
            mContext.setWallpaper((InputStream) null);
            fail("Context.setWallpaper(InputStream) did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }

        try {
            mContext.clearWallpaper();
            fail("Context.clearWallpaper() did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that Vibrator's vibrating related methods requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#VIBRATE}.
     */
    @SmallTest
    public void testVibrator() {
        Vibrator vibrator = (Vibrator)getContext().getSystemService(Context.VIBRATOR_SERVICE);

        try {
            vibrator.cancel();
            fail("Vibrator.cancel() did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }

        try {
            vibrator.vibrate(1);
            fail("Vibrator.vibrate(long) did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }

        long[] testPattern = {1, 1, 1, 1, 1};

        try {
            vibrator.vibrate(testPattern, 1);
            fail("Vibrator.vibrate(long[], int) not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }
    }

    /**
     * Verify that sending sms requires permissions.
     * <p>Requires Permission:
     *   {@link android.Manifest.permission#SMS}.
     */
    @SmallTest
    public void testSendSms() {
        SmsManager smsManager = SmsManager.getDefault();
        byte[] testData = new byte[10];
        try {
            smsManager.sendDataMessage("1233", "1233", (short) 0, testData, null, null);
            fail("SmsManager.sendDataMessage() did not throw SecurityException as expected.");
        } catch (SecurityException e) {
            // expected
        }
    }
}
