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

package com.android.musicvis.vis3;

import com.android.musicvis.GenericWaveRS;
import com.android.musicvis.R;
import com.android.musicvis.RenderScriptScene;
import com.android.musicvis.AudioCapture;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Mesh.Primitive;
import android.renderscript.ProgramVertex;
import android.renderscript.ScriptC;
import android.renderscript.Type;
import android.renderscript.Element.Builder;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.TimeZone;

class Visualization3RS extends GenericWaveRS {

    private short [] mAnalyzer = new short[512];

    float lastOffset;

    Visualization3RS(int width, int height) {
        super(width, height, R.drawable.ice);
        lastOffset = 0;
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        mWorldState.yRotation = (xOffset * 4) * 360;
        updateWorldState();
    }

    @Override
    public void start() {
        if (mAudioCapture == null) {
            mAudioCapture = new AudioCapture(AudioCapture.TYPE_FFT, 512);
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
            mVizData = mAudioCapture.getFormattedData(1, 1);
            // the really high frequencies aren't that interesting for music,
            // so just chop those off and use only the lower half of the spectrum
            len = mVizData.length / 2;
        }
        if (len == 0) {
            if (mWorldState.idle == 0) {
                mWorldState.idle = 1;
                updateWorldState();
            }
            return;
        }

        len /= 2; // the bins are comprised of 2 values each

        if (len > mAnalyzer.length) len = mAnalyzer.length;

        if (mWorldState.idle != 0) {
            mWorldState.idle = 0;
            updateWorldState();
        }

        for (int i = 1; i < len - 1; i++) {
            int val1 = mVizData[i * 2];
            int val2 = mVizData[i * 2 + 1];
            int val = val1 * val1 + val2 * val2;
            short newval = (short)(val * (i/16+1));
            short oldval = mAnalyzer[i];
            if (newval >= oldval - 800) {
                // use new high value
            } else {
                newval = (short)(oldval - 800);
            }
            mAnalyzer[i] = newval;
        }

        // distribute the data over mWidth samples in the middle of the mPointData array
        final int outlen = mPointData.length / 8;
        final int width = mWidth > outlen ? outlen : mWidth;
        final int skip = (outlen - width) / 2;

        int srcidx = 0;
        int cnt = 0;
        for (int i = 0; i < width; i++) {
            float val = mAnalyzer[srcidx] / 8;
            if (val < 1f && val > -1f) val = 1;
            mPointData[(i + skip) * 8 + 1] = val;
            mPointData[(i + skip) * 8 + 5] = -val;
            cnt += len;
            if (cnt > width) {
                srcidx++;
                cnt -= width;
            }
        }
        mPointAlloc.copyFromUnchecked(mPointData);
    }

}
