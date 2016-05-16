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

package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;

public class SharedBuffer {

    private static final String LOGTAG = "SharedBuffer";

    private volatile Buffer mProducer = null;
    private volatile Buffer mConsumer = null;
    private volatile Buffer mIntermediate = null;

    private volatile boolean mNeedsSwap = false;
    private volatile boolean mNeedsRepaint = true;

    public synchronized void setProducer(Bitmap producer) {
        if (mProducer != null
                && !mProducer.isSameSize(producer)) {
            mProducer.remove();
            mProducer = null;
        }
        if (mProducer == null) {
            mProducer = new Buffer(producer);
        } else {
            mProducer.useBitmap(producer);
        }
    }

    public synchronized Buffer getProducer() {
        return mProducer;
    }

    public synchronized Buffer getConsumer() {
        return mConsumer;
    }

    public synchronized void swapProducer() {
        Buffer intermediate = mIntermediate;
        mIntermediate = mProducer;
        mProducer = intermediate;
        mNeedsSwap = true;
    }

    public synchronized void swapConsumerIfNeeded() {
        if (!mNeedsSwap) {
            return;
        }
        Buffer intermediate = mIntermediate;
        mIntermediate = mConsumer;
        mConsumer = intermediate;
        mNeedsSwap = false;
    }

    public synchronized void invalidate() {
        mNeedsRepaint = true;
    }

    public synchronized boolean checkRepaintNeeded() {
        if (mNeedsRepaint) {
            mNeedsRepaint = false;
            return true;
        }
        return false;
    }

}

