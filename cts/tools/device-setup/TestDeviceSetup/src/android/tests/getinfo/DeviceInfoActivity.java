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

package android.tests.getinfo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * Collect device information on target device.
 */
public class DeviceInfoActivity extends Activity {

    // work done should be reported in GLES..View
    private CountDownLatch mDone = new CountDownLatch(1);
    private HashSet<String> mFormats = new HashSet<String>();
    private String mGraphicsVendor;
    private String mGraphicsRenderer;

    /**
     * Other classes can call this function to wait for this activity
     * to finish. */
    public void waitForAcitityToFinish() {
        try {
            mDone.await();
        } catch (InterruptedException e) {
            // just move on
        }
    }

    private void runIterations(int glVersion) {
        for (int i = 1; i <= glVersion; i++) {
            final CountDownLatch done = new CountDownLatch(1);
            final int version = i;
            DeviceInfoActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    setContentView(new GLESSurfaceView(DeviceInfoActivity.this, version, done));
                }
            });
            try {
                done.await();
            } catch (InterruptedException e) {
                // just move on
            }
        }

        StringBuilder builder = new StringBuilder();
        for (String format: mFormats) {
            builder.append(format);
            builder.append(";");
        }
        DeviceInfoInstrument.addResult(
                DeviceInfoConstants.OPEN_GL_COMPRESSED_TEXTURE_FORMATS,
                builder.toString());
        DeviceInfoInstrument.addResult(
                DeviceInfoConstants.GRAPHICS_VENDOR,
                mGraphicsVendor);
        DeviceInfoInstrument.addResult(
                DeviceInfoConstants.GRAPHICS_RENDERER,
                mGraphicsRenderer);
        mDone.countDown();
    }

    public void addCompressedTextureFormat(String format) {
        mFormats.add(format);
    }

    public void setGraphicsInfo(String vendor, String renderer) {
        mGraphicsVendor = vendor;
        mGraphicsRenderer = renderer;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        ActivityManager am =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        final int glVersion = (info.reqGlEsVersion & 0xffff0000) >> 16;
        new Thread() {
            public void run() {
                runIterations(glVersion);
            }
        }.start();

        Configuration con = getResources().getConfiguration();
        String touchScreen = null;
        if (con.touchscreen == Configuration.TOUCHSCREEN_UNDEFINED) {
            touchScreen = "undefined";
        } else if (con.touchscreen == Configuration.TOUCHSCREEN_NOTOUCH) {
            touchScreen = "notouch";
        } else if (con.touchscreen == Configuration.TOUCHSCREEN_STYLUS) {
            touchScreen = "stylus";
        } else if (con.touchscreen == Configuration.TOUCHSCREEN_FINGER) {
            touchScreen = "finger";
        }
        if (touchScreen != null) {
            DeviceInfoInstrument.addResult(DeviceInfoConstants.TOUCH_SCREEN,
                    touchScreen);
        }

        String navigation = null;
        if (con.navigation == Configuration.NAVIGATION_UNDEFINED) {
            navigation = "undefined";
        } else if (con.navigation == Configuration.NAVIGATION_NONAV) {
            navigation = "nonav";
        } else if (con.navigation == Configuration.NAVIGATION_DPAD) {
            navigation = "drap";
        } else if (con.navigation == Configuration.NAVIGATION_TRACKBALL) {
            navigation = "trackball";
        } else if (con.navigation == Configuration.NAVIGATION_WHEEL) {
            navigation = "wheel";
        }

        if (navigation != null) {
            DeviceInfoInstrument.addResult(DeviceInfoConstants.NAVIGATION,
                    navigation);
        }

        String keypad = null;
        if (con.keyboard == Configuration.KEYBOARD_UNDEFINED) {
            keypad = "undefined";
        } else if (con.keyboard == Configuration.KEYBOARD_NOKEYS) {
            keypad = "nokeys";
        } else if (con.keyboard == Configuration.KEYBOARD_QWERTY) {
            keypad = "qwerty";
        } else if (con.keyboard == Configuration.KEYBOARD_12KEY) {
            keypad = "12key";
        }
        if (keypad != null) {
            DeviceInfoInstrument.addResult(DeviceInfoConstants.KEYPAD, keypad);
        }

        String[] locales = getAssets().getLocales();
        StringBuilder localeList = new StringBuilder();
        for (String s : locales) {
            if (s.length() == 0) { // default locale
                localeList.append(new Locale("en", "US").toString());
            } else {
                localeList.append(s);
            }
            localeList.append(";");
        }
        DeviceInfoInstrument.addResult(DeviceInfoConstants.LOCALES,
                localeList.toString());
    }
}
