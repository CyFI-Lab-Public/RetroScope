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

package com.android.musicvis.vis5;

import com.android.musicvis.R;
import com.android.musicvis.RenderScriptScene;
import com.android.musicvis.ScriptField_Vertex;
import com.android.musicvis.AudioCapture;

import android.os.Handler;
import android.renderscript.*;
import android.renderscript.Element.Builder;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;
import android.renderscript.Sampler.Value;
import android.util.Log;
import android.view.MotionEvent;

import java.util.TimeZone;

class Visualization5RS extends RenderScriptScene {

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
        public float mRotate;
        public float mTilt;
        public int   mIdle;
        public int   mWaveCounter;
    }
    WorldState mWorldState = new WorldState();

    ScriptC_many mScript;
    private com.android.musicvis.vis5.ScriptField_Vertex mVertexBuffer;

    private ProgramStore mPfsBackground;
    private ProgramFragment mPfBackgroundMip;
    private ProgramFragment mPfBackgroundNoMip;
    private ProgramRaster mPr;
    private Sampler mSamplerMip;
    private Sampler mSamplerNoMip;
    private Allocation[] mTextures;

    private ProgramVertex mPVBackground;
    private ProgramVertexFixedFunction.Constants mPVAlloc;

    private Mesh mCubeMesh;

    protected Allocation mPointAlloc;
    // 256 lines, with 4 points per line (2 space, 2 texture) each consisting of x and y,
    // so 8 floats per line.
    protected float [] mPointData = new float[256*8];

    private Allocation mLineIdxAlloc;
    // 2 indices per line
    private short [] mIndexData = new short[256*2];

    private AudioCapture mAudioCapture = null;
    private int [] mVizData = new int[1024];

    private static final int RSID_STATE = 0;
    private static final int RSID_POINTS = 1;
    private static final int RSID_LINES = 2;
    private static final int RSID_PROGRAMVERTEX = 3;

    private float mTouchY;

    Visualization5RS(int width, int height) {
        super(width, height);
        mWidth = width;
        mHeight = height;
        // the x, s and t coordinates don't change, so set those now
        int outlen = mPointData.length / 8;
        int half = outlen / 2;
        for(int i = 0; i < outlen; i++) {
            mPointData[i*8]   = i - half;          // start point X (Y set later)
            mPointData[i*8+2] = 0;                 // start point S
            mPointData[i*8+3] = 0;                 // start point T
            mPointData[i*8+4] = i - half;          // end point X (Y set later)
            mPointData[i*8+6] = 1.0f;              // end point S
            mPointData[i*8+7] = 0f;                // end point T
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (mPVAlloc != null) {
            Matrix4f proj = new Matrix4f();
            proj.loadProjectionNormalized(width, height);
            mPVAlloc.setProjection(proj);
        }
        mWorldState.mTilt = -20;
    }

    /*@Override
    public void onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = event.getY() - mTouchY;
                mTouchY += dy;
                dy /= 10;
                dy += mWorldState.mTilt;
                if (dy > 0) {
                    dy = 0;
                } else if (dy < -45) {
                    dy = -45;
                }
                mWorldState.mTilt = dy;
                mState.data(mWorldState);
                //updateWorldState();
        }
    }*/

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        // update our state, then push it to the renderscript
        mWorldState.mRotate = (xOffset - 0.5f) * 90;
        updateWorldState();
    }

    @Override
    protected ScriptC createScript() {
        mScript = new ScriptC_many(mRS, mResources, R.raw.many);

        // First set up the coordinate system and such
        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        mPVBackground = pvb.create();
        mPVAlloc = new ProgramVertexFixedFunction.Constants(mRS);
        ((ProgramVertexFixedFunction)mPVBackground).bindConstants(mPVAlloc);
        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(mWidth, mHeight);
        mPVAlloc.setProjection(proj);

        mScript.set_gPVBackground(mPVBackground);

        mTextures = new Allocation[8];
        mTextures[0] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.background,
                                                           Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_background(mTextures[0]);
        mTextures[1] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.frame,
                                                           Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_frame(mTextures[1]);
        mTextures[2] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.peak_on,
                                                           Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_peak_on(mTextures[2]);
        mTextures[3] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.peak_off,
                                                           Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_peak_off(mTextures[3]);
        mTextures[4] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.needle,
                                                           Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_needle(mTextures[4]);
        mTextures[5] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.black,
                                                           Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_black(mTextures[5]);
        mTextures[6] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.albumart,
                                                           Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTvumeter_album(mTextures[6]);
        mTextures[7] = Allocation.createFromBitmapResource(mRS, mResources, R.drawable.fire,
                                                           Allocation.MipmapControl.MIPMAP_ON_SYNC_TO_TEXTURE,
                                                           Allocation.USAGE_GRAPHICS_TEXTURE);
        mScript.set_gTlinetexture(mTextures[7]);

        {
            Sampler.Builder builder = new Sampler.Builder(mRS);
            builder.setMinification(Value.LINEAR);
            builder.setMagnification(Value.LINEAR);
            builder.setWrapS(Value.WRAP);
            builder.setWrapT(Value.WRAP);
            mSamplerNoMip = builder.create();
        }

        {
            Sampler.Builder builder = new Sampler.Builder(mRS);
            builder.setMinification(Value.LINEAR_MIP_LINEAR);
            builder.setMagnification(Value.LINEAR);
            builder.setWrapS(Value.WRAP);
            builder.setWrapT(Value.WRAP);
            mSamplerMip = builder.create();
        }

        {
            ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
            builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                               ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
            mPfBackgroundNoMip = builder.create();
            mPfBackgroundNoMip.bindSampler(mSamplerNoMip, 0);
            mScript.set_gPFBackgroundNoMip(mPfBackgroundNoMip);
        }

        {
            ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
            builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                               ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
            mPfBackgroundMip = builder.create();
            mPfBackgroundMip.bindSampler(mSamplerMip, 0);
            mScript.set_gPFBackgroundMip(mPfBackgroundMip);
        }

        {
            ProgramRaster.Builder builder = new ProgramRaster.Builder(mRS);
            builder.setCullMode(ProgramRaster.CullMode.NONE);
            mPr = builder.create();
            mScript.set_gPR(mPr);
        }

        {
            ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
            builder.setDepthFunc(ProgramStore.DepthFunc.EQUAL);
            //builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE_MINUS_SRC_ALPHA);
            builder.setBlendFunc(BlendSrcFunc.ONE, BlendDstFunc.ONE_MINUS_SRC_ALPHA);
            builder.setDitherEnabled(true); // without dithering there is severe banding
            builder.setDepthMaskEnabled(false);
            mPfsBackground = builder.create();

            mScript.set_gPFSBackground(mPfsBackground);
        }

        // Start creating the mesh
        mVertexBuffer = new com.android.musicvis.vis5.ScriptField_Vertex(mRS, mPointData.length / 4);

        final Mesh.AllocationBuilder meshBuilder = new Mesh.AllocationBuilder(mRS);
        meshBuilder.addVertexAllocation(mVertexBuffer.getAllocation());
        // Create the Allocation for the indices
        mLineIdxAlloc = Allocation.createSized(mRS, Element.U16(mRS), mIndexData.length,
                                               Allocation.USAGE_SCRIPT |
                                               Allocation.USAGE_GRAPHICS_VERTEX);
        // This will be a line mesh
        meshBuilder.addIndexSetAllocation(mLineIdxAlloc, Mesh.Primitive.LINE);

        // Create the Allocation for the vertices
        mCubeMesh = meshBuilder.create();

        mPointAlloc = mVertexBuffer.getAllocation();

        mScript.bind_gPoints(mVertexBuffer);
        mScript.set_gPointBuffer(mPointAlloc);
        mScript.set_gCubeMesh(mCubeMesh);

        // put the vertex and index data in their respective buffers
        updateWave();
        for(int i = 0; i < mIndexData.length; i ++) {
            mIndexData[i] = (short) i;
        }

        //  upload the vertex and index data
        mPointAlloc.copyFromUnchecked(mPointData);
        mLineIdxAlloc.copyFrom(mIndexData);
        mLineIdxAlloc.syncAll(Allocation.USAGE_SCRIPT);

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

        // downsample 1024 samples in to 256

        if (len == 0) {
            if (mWorldState.mIdle == 0) {
                mWorldState.mIdle = 1;
            }
        } else {
            if (mWorldState.mIdle != 0) {
                mWorldState.mIdle = 0;
            }
            // TODO: might be more efficient to push this in to renderscript
            int outlen = mPointData.length / 8;
            len /= 4;
            if (len > outlen) len = outlen;
            for(int i = 0; i < len; i++) {
                int amp = (mVizData[i*4]  + mVizData[i*4+1] + mVizData[i*4+2] + mVizData[i*4+3]);
                mPointData[i*8+1] = amp;
                mPointData[i*8+5] = -amp;
            }
            mPointAlloc.copyFromUnchecked(mPointData);
            mWorldState.mWaveCounter++;
        }

        updateWorldState();
    }

    protected void updateWorldState() {
        mScript.set_gAngle(mWorldState.mAngle);
        mScript.set_gPeak(mWorldState.mPeak);
        mScript.set_gRotate(mWorldState.mRotate);
        mScript.set_gTilt(mWorldState.mTilt);
        mScript.set_gIdle(mWorldState.mIdle);
        mScript.set_gWaveCounter(mWorldState.mWaveCounter);
    }
}
