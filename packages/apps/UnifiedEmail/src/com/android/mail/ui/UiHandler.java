/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.ui;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Handler;

import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A thin wrapper of {@link Handler} to run a callback in UI thread. Any callback posted to this
 * handler is guarantee to run inside {@link Activity} life cycle. However, it can be dropped if the
 * {@link Activity} has been stopped. This handler is safe to use with {@link FragmentTransaction}.
 *
 * @author phamm
 */
public class UiHandler {
    private final Handler mHandler = new Handler();
    private boolean mEnabled = true;
    private final static String LOG_TAG = LogTag.getLogTag();

    /** Number of {@link Runnable} in the queue. */
    private AtomicInteger mCount = new AtomicInteger(0);

    public void post(final Runnable r) {
        if (mEnabled) {
            mCount.incrementAndGet();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCount.decrementAndGet();
                    r.run();
                }
            });
        } else {
            LogUtils.d(LOG_TAG, "UiHandler is disabled in post(). Dropping Runnable.");
        }
    }

    public void postDelayed(final Runnable r, long delayMillis) {
        if (mEnabled) {
            mCount.incrementAndGet();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCount.decrementAndGet();
                    r.run();
                }
            }, delayMillis);
        } else {
            LogUtils.d(LOG_TAG, "UiHandler is disabled in postDelayed(). Dropping Runnable.");
        }
    }

    public void removeCallbacks(Runnable r) {
        mHandler.removeCallbacks(r);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        if (!mEnabled) {
            int count = mCount.getAndSet(0);
            if (count > 0) {
                LogUtils.e(LOG_TAG, "Disable UiHandler. Dropping %d Runnables.", count);
            }
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * @return whether the {@link UiHandler} is enabled. It's safe to edit UI if the
     *         {@link UiHandler} is enabled.
     */
    public boolean isEnabled() {
        return mEnabled;
    }
}