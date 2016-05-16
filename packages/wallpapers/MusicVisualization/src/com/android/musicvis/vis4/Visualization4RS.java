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

package com.android.musicvis.vis4;

import static android.renderscript.ProgramStore.DepthFunc.ALWAYS;
import static android.renderscript.Sampler.Value.LINEAR;
import static android.renderscript.Sampler.Value.WRAP;

import com.android.musicvis.R;
import com.android.musicvis.RenderScriptScene;
import com.android.musicvis.AudioCapture;

import android.os.Handler;
import android.renderscript.*;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;

import java.util.TimeZone;
import android.util.Log;

class Visualization4RS extends RenderScriptScene {

    private final Handler mHandler = new Handler();
    private final Runnable mDrawCube = new Runnable() {
        public void run() {
            updateWave();
        }
    };
    private boolean mVisible;

    private int mNeedlePos = 0;
    private int mNeedleSpeed = 0;
    // tweak this to get quicker/slower response
    private int mNeedleMass = 10;
    private int mSpringForceAtOrigin = 200;

    static class WorldState {
        public float mAngle;
        public int   mPeak;
    }
    WorldState mWorldState = new WorldState();

    ScriptC_vu mScript;

    private ProgramStore mPfsBackground;
    private ProgramFragment mPfBackground;
    private Sampler mSampler;
    private Allocation[] mTextures;

    private ProgramVertex mPVBackground;
    private ProgramVertexFixedFunction.Constants mPVAlloc;

    private AudioCapture mAudioCapture = null;
    private int [] mVizData = new int[1024];

    private static final int RSID_STATE = 0;
    private static final int RSID_POINTS = 1;
    private static final int RSID_LINES = 2;
    private static final int RSID_PROGRAMVERTEX = 3;


    Visualization4RS(int width, int height) {
        super(width, height);
        mWidth = width;
        mHeight = height;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (mPVAlloc != null) {
            Matrix4f proj = new Matrix4f();
            proj.loadProjectionNormalized(width, height);
            mPVAlloc.setProjection(proj);
        }
    }

    @Override
    protected ScriptC createScript() {

        mScript = new ScriptC_vu(mRS, mResources, R.raw.vu);

        // First set up the coordinate system and such
        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        mPVBackground = pvb.create();
        mPVAlloc = new ProgramVertexFixedFunction.Constants(mRS);
        ((ProgramVertexFixedFunction)mPVBackground).bindConstants(mPVAlloc);
        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(mWidth, mHeight);
        mPVAlloc.setProjection(proj);

        mScript.set_gPVBackground(mPVBackground);

        updateWave();

        mTextures = new Allocation[6];
        mTextures[0] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.background,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_background(mTextures[0]);
        mTextures[1] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.frame,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_frame(mTextures[1]);
        mTextures[2] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.peak_on,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_peak_on(mTextures[2]);
        mTextures[3] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.peak_off,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_peak_off(mTextures[3]);
        mTextures[4] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.needle,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_needle(mTextures[4]);
        mTextures[5] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.black,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_black(mTextures[5]);

        Sampler.Builder samplerBuilder = new Sampler.Builder(mRS);
        samplerBuilder.setMinification(LINEAR);
        samplerBuilder.setMagnification(LINEAR);
        samplerBuilder.setWrapS(WRAP);
        samplerBuilder.setWrapT(WRAP);
        mSampler = samplerBuilder.create();

        {
            ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
            builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                               ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
            mPfBackground = builder.create();
            mPfBackground.bindSampler(mSampler, 0);

            mScript.set_gPFBackground(mPfBackground);
        }

        {
            ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
            builder.setDepthFunc(ALWAYS);
            //builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE_MINUS_SRC_ALPHA);
            builder.setBlendFunc(BlendSrcFunc.ONE, BlendDstFunc.ONE_MINUS_SRC_ALPHA);
            builder.setDitherEnabled(true); // without dithering there is severe banding
            builder.setDepthMaskEnabled(false);
            mPfsBackground = builder.create();

            mScript.set_gPFSBackground(mPfsBackground);
        }

        mScript.setTimeZone(TimeZone.getDefault().getID());

        return mScript;
    }

    @Override
    public void start() {
        super.start();
        mVisible = true;
        if (mAudioCapture == null) {
            mAudioCapture = new AudioCapture(AudioCapture.TYPE_PCM, 1024);
        }
        mAudioCapture.start();
        updateWave();
    }

    @Override
    public void stop() {
        super.stop();
        mVisible = false;
        if (mAudioCapture != null) {
            mAudioCapture.stop();
            mAudioCapture.release();
            mAudioCapture = null;
        }
    }

    void updateWave() {
        mHandler.removeCallbacks(mDrawCube);
        if (!mVisible) {
            return;
        }
        mHandler.postDelayed(mDrawCube, 20);

        int len = 0;
        if (mAudioCapture != null) {
            // arbitrary scalar to get better range: 512 = 2 * 256 (256 for 8 to 16 bit)
            mVizData = mAudioCapture.getFormattedData(512, 1);
            len = mVizData.length;
        }

        // Simulate running the signal through a rectifier by
        // taking the average of the absolute sample values.
        int volt = 0;
        if (len > 0) {
            for (int i = 0; i < len; i++) {
                int val = mVizData[i];
                if (val < 0) {
                    val = -val;
                }
                volt += val;
            }
            volt = volt / len;
        }
        // There are several forces working on the needle: a force applied by the
        // electromagnet, a force applied by the spring,  and friction.
        // The force from the magnet is proportional to the current flowing
        // through its coil. We have to take in to account that the coil is an
        // inductive load, which means that an immediate change in applied voltage
        // will result in a gradual change in current, but also that current will
        // be induced by the movement of the needle.
        // The force from the spring is proportional to the position of the needle.
        // The friction force is a function of the speed of the needle, but so is
        // the current induced by the movement of the needle, so we can combine
        // them.


        // Add up the various forces, with some multipliers to make the movement
        // of the needle more realistic
        // 'volt' is for the applied voltage, which causes a current to flow through the coil
        // mNeedleSpeed * 3 is for the movement of the needle, which induces an opposite current
        // in the coil, and is also proportional to the friction
        // mNeedlePos + mSpringForceAtOrigin is for the force of the spring pushing the needle back
        int netforce = volt - mNeedleSpeed * 3 - (mNeedlePos + mSpringForceAtOrigin) ;
        int acceleration = netforce / mNeedleMass;
        mNeedleSpeed += acceleration;
        mNeedlePos += mNeedleSpeed;
        if (mNeedlePos < 0) {
            mNeedlePos = 0;
            mNeedleSpeed = 0;
        } else if (mNeedlePos > 32767) {
            if (mNeedlePos > 33333) {
                 mWorldState.mPeak = 10;
            }
            mNeedlePos = 32767;
            mNeedleSpeed = 0;
        }
        if (mWorldState.mPeak > 0) {
            mWorldState.mPeak--;
        }

        mWorldState.mAngle = 131f - (mNeedlePos / 410f); // ~80 degree range
        mScript.set_gAngle(mWorldState.mAngle);
        mScript.set_gPeak(mWorldState.mPeak);
    }
}
