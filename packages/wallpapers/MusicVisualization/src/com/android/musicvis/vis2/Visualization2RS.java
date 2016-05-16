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

package com.android.musicvis.vis2;

import com.android.musicvis.GenericWaveRS;
import com.android.musicvis.R;
import com.android.musicvis.AudioCapture;
import android.util.Log;

import android.media.MediaPlayer;

class Visualization2RS extends GenericWaveRS {

    Visualization2RS(int width, int height) {
        super(width, height, R.drawable.fire);
    }

    @Override
    public void start() {
        if (mAudioCapture == null) {
            mAudioCapture = new AudioCapture(AudioCapture.TYPE_PCM, 1024);
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (mAudioCapture != null) {
            mAudioCapture.release();
            mAudioCapture = null;
        }
    }

    @Override
    public void update() {
        int len = 0;
        if (mAudioCapture != null) {
            mVizData = mAudioCapture.getFormattedData(1,1);
            len = mVizData.length;
        }

        if (len == 0) {
            if (mWorldState.idle == 0) {
                mWorldState.idle = 1;
                //mState.data(mWorldState);
                updateWorldState();
            }
            return;
        }

        int outlen = mPointData.length / 8;
        if (len > outlen) len = outlen;

        if (mWorldState.idle != 0) {
            mWorldState.idle = 0;
            //mState.data(mWorldState);
            updateWorldState();
        }
        // TODO: might be more efficient to push this in to renderscript
        for(int i = 0; i < len; i++) {
            int amp = mVizData[i];
            mPointData[i*8+1] = amp;
            mPointData[i*8+5] = -amp;
        }
        mPointAlloc.copyFromUnchecked(mPointData);
    }

}
