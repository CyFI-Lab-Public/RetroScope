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

public class SharedPreset {

    private volatile ImagePreset mProducerPreset = null;
    private volatile ImagePreset mConsumerPreset = null;
    private volatile ImagePreset mIntermediatePreset = null;
    private volatile boolean mHasNewContent = false;

    public synchronized void enqueuePreset(ImagePreset preset) {
        if (mProducerPreset == null || (!mProducerPreset.same(preset))) {
            mProducerPreset = new ImagePreset(preset);
        } else {
            mProducerPreset.updateWith(preset);
        }
        ImagePreset temp = mIntermediatePreset;
        mIntermediatePreset = mProducerPreset;
        mProducerPreset = temp;
        mHasNewContent = true;
    }

    public synchronized ImagePreset dequeuePreset() {
        if (!mHasNewContent) {
            return mConsumerPreset;
        }
        ImagePreset temp = mConsumerPreset;
        mConsumerPreset = mIntermediatePreset;
        mIntermediatePreset = temp;
        mHasNewContent = false;
        return mConsumerPreset;
    }
}
