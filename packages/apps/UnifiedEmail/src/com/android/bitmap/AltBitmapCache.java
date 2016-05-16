/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.bitmap;

import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

/**
 * This subclass provides custom pool behavior. The pool can be set to block on {@link #poll()} if
 * nothing can be returned. This is useful if you know you will incur high costs upon receiving
 * nothing from the pool, and you do not want to incur those costs at the critical moment when the
 * UI is animating.
 */
public class AltBitmapCache extends AltPooledCache<DecodeTask.Request, ReusableBitmap>
        implements BitmapCache {
    private boolean mBlocking = false;
    private final Object mLock = new Object();

    private final static boolean DEBUG = false;
    private final static String TAG = LogTag.getLogTag();

    public AltBitmapCache(final int targetSizeBytes, final float nonPooledFraction) {
        super(targetSizeBytes, nonPooledFraction);
    }

    /**
     * Declare that {@link #poll()} should now block until it can return something.
     */
    public void setBlocking(final boolean blocking) {
        synchronized (mLock) {
            if (DEBUG) LogUtils.d(TAG, "AltBitmapCache: block %b", blocking);
            mBlocking = blocking;
            if (!mBlocking) {
                // no longer blocking. Notify every thread.
                mLock.notifyAll();
            }
        }
    }

    @Override
    protected int sizeOf(final ReusableBitmap value) {
        return value.getByteCount();
    }

    /**
     * If {@link #setBlocking(boolean)} has been called with true, this method will block until a
     * resource is available.
     * @return an available resource, or null if none are available. Null will never be returned
     * until blocking is set to false.
     */
    @Override
    public ReusableBitmap poll() {
        ReusableBitmap bitmap;
        synchronized (mLock) {
            while ((bitmap = super.poll()) == null && mBlocking) {
                if (DEBUG) {
                    LogUtils.d(TAG, "AltBitmapCache: %s waiting", Thread.currentThread().getName());
                }
                Trace.beginSection("sleep");
                try {
                    // block
                    mLock.wait();
                    if (DEBUG) {
                        LogUtils.d(TAG, "AltBitmapCache: %s notified",
                                Thread.currentThread().getName());
                    }
                } catch (InterruptedException e) {
                }
                Trace.endSection();
            }
        }
        return bitmap;
    }

    @Override
    public void offer(final ReusableBitmap value) {
        synchronized (mLock) {
            super.offer(value);
            if (DEBUG) LogUtils.d(TAG, "AltBitmapCache: offer +1");
            // new resource gained. Notify one thread.
            mLock.notify();
        }
    }
}
