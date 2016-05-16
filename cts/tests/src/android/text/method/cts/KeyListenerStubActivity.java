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

package android.text.method.cts;

import com.android.cts.stub.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.BaseKeyListener;
import android.text.method.DateKeyListener;
import android.text.method.DateTimeKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.MultiTapKeyListener;
import android.text.method.NumberKeyListener;
import android.text.method.QwertyKeyListener;
import android.text.method.TextKeyListener;
import android.text.method.TimeKeyListener;
import android.util.Log;

/**
 * This Activity is used for testing:
 * {@link DigitsKeyListener}
 * {@link BaseKeyListener}
 * {@link MultiTapKeyListener}
 * {@link NumberKeyListener}
 * {@link QwertyKeyListener}
 * {@link TextKeyListener}
 * {@link DateKeyListener}
 * {@link DateTimeKeyListener}
 * {@link TimeKeyListener}
 *
 * @see DigitsKeyListener
 * @see BaseKeyListener
 * @see MultiTapKeyListener
 * @see NumberKeyListener
 * @see QwertyKeyListener
 * @see TextKeyListener
 * @see DateKeyListener
 * @see DateTimeKeyListener
 * @see TimeKeyListener
 */

public class KeyListenerStubActivity extends Activity {
    private boolean mHasWindowFocus = false;
    private Object mHasWindowFocusLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keylistener_layout);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            Log.w("KeyListenerStubActivity", "KeyListenerStubActivity lost window focus");
        }
        synchronized(mHasWindowFocusLock) {
            mHasWindowFocus = hasFocus;
            mHasWindowFocusLock.notify();
        }
    }

    /**
     * Blocks the calling thread until the {@link KeyListenerStubActivity} has window focus or the
     * specified duration (in milliseconds) has passed.
     */
    public boolean waitForWindowFocus(long durationMillis) {
        long elapsedMillis = SystemClock.elapsedRealtime();
        synchronized(mHasWindowFocusLock) {
            mHasWindowFocus = hasWindowFocus();
            while (!mHasWindowFocus && durationMillis > 0) {
                long newElapsedMillis = SystemClock.elapsedRealtime();
                durationMillis -= (newElapsedMillis - elapsedMillis);
                elapsedMillis = newElapsedMillis;
                if (durationMillis > 0) {
                    try {
                        mHasWindowFocusLock.wait(durationMillis);
                    } catch (InterruptedException e) {
                    }
                }
            }
            return mHasWindowFocus;
        }
    }
}
