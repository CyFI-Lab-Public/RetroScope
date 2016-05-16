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

package com.android.musicvis;

import static android.renderscript.Element.RGB_565;
import static android.renderscript.Sampler.Value.LINEAR;
import static android.renderscript.Sampler.Value.WRAP;

import android.os.Handler;
import android.os.SystemClock;
import android.renderscript.Mesh.Primitive;
import android.renderscript.*;
import android.renderscript.Element.Builder;
import android.util.Log;

import java.util.TimeZone;

public class GenericWaveRS extends RenderScriptScene {

    private final Handler mHandler = new Handler();
    private final Runnable mDrawCube = new Runnable() {
        public void run() {
            updateWave();
        }
    };
    private boolean mVisible;
    private int mTexId;

    protected static class WorldState {
        public float yRotation;
        public int idle;
        public int waveCounter;
        public int width;
    }
    protected WorldState mWorldState = new WorldState();

    ScriptC_waveform mScript;

    private ScriptField_Vertex mVertexBuffer;

    private Mesh mCubeMesh;

    protected Allocation mPointAlloc;
    // 1024 lines, with 4 points per line (2 space, 2 texture) each consisting of x and y,
    // so 8 floats per line.
    protected float [] mPointData = new float[1024*8];

    private ProgramVertex mPVBackground;
    private ProgramVertexFixedFunction.Constants mPVAlloc;

    protected AudioCapture mAudioCapture = null;
    protected int [] mVizData = new int[1024];

    private ProgramFragment mPfBackground;
    private Sampler mSampler;
    private Allocation mTexture;

    private static final int RSID_STATE = 0;
    private static final int RSID_POINTS = 1;
    private static final int RSID_LINES = 2;
    private static final int RSID_PROGRAMVERTEX = 3;

    protected GenericWaveRS(int width, int height, int texid) {
        super(width, height);
        mTexId = texid;
        mWidth = width;
        mHeight = height;
        // the x, s and t coordinates don't change, so set those now
        int outlen = mPointData.length / 8;
        int half = outlen / 2;
        for(int i = 0; i < outlen; i++) {
            mPointData[i*8]   = i - half;          // start point X (Y set later)
            mPointData[i*8+2] = 0;                 // start point S
            mPointData[i*8+3] = 0;                 // start point T
            mPointData[i*8+4]   = i - half;        // end point X (Y set later)
            mPointData[i*8+6] = 1.0f;                 // end point S
            mPointData[i*8+7] = 0f;              // end point T
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        mWorldState.width = width;
        if (mPVAlloc != null) {
            Matrix4f proj = new Matrix4f();
            proj.loadProjectionNormalized(mWidth, mHeight);
            mPVAlloc.setProjection(proj);
        }
    }

    @Override
    protected ScriptC createScript() {

        mScript = new ScriptC_waveform(mRS, mResources, R.raw.waveform);

        // set our java object as the data for the renderscript allocation
        mWorldState.yRotation = 0.0f;
        mWorldState.width = mWidth;
        updateWorldState();

        //  Now put our model in to a form that renderscript can work with:
        //  - create a buffer of floats that are the coordinates for the points that define the cube
        //  - create a buffer of integers that are the indices of the points that form lines
        //  - combine the two in to a mesh

        // First set up the coordinate system and such
        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        mPVBackground = pvb.create();
        mPVAlloc = new ProgramVertexFixedFunction.Constants(mRS);
        ((ProgramVertexFixedFunction)mPVBackground).bindConstants(mPVAlloc);
        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(mWidth, mHeight);
        mPVAlloc.setProjection(proj);

        mScript.set_gPVBackground(mPVBackground);

        mVertexBuffer = new ScriptField_Vertex(mRS, mPointData.length / 4);

        // Start creating the mesh
        final Mesh.AllocationBuilder meshBuilder = new Mesh.AllocationBuilder(mRS);
        meshBuilder.addVertexAllocation(mVertexBuffer.getAllocation());
        // This will be a triangle strip mesh
        meshBuilder.addIndexSetType(Primitive.TRIANGLE_STRIP);

        // Create the Allocation for the vertices
        mCubeMesh = meshBuilder.create();

        mPointAlloc = mVertexBuffer.getAllocation();

        mScript.bind_gPoints(mVertexBuffer);
        mScript.set_gPointBuffer(mPointAlloc);
        mScript.set_gCubeMesh(mCubeMesh);

        //  upload the vertex data
        mPointAlloc.copyFromUnchecked(mPointData);

        // load the texture
        mTexture = Allocation.createFromBitmapResource(mRS, mResources, mTexId,
                                           Allocation.MipmapControl.MIPMAP_NONE,
                                           Allocation.USAGE_GRAPHICS_TEXTURE);

        mScript.set_gTlinetexture(mTexture);

        /*
         * create a program fragment to use the texture
         */
        Sampler.Builder samplerBuilder = new Sampler.Builder(mRS);
        samplerBuilder.setMinification(LINEAR);
        samplerBuilder.setMagnification(LINEAR);
        samplerBuilder.setWrapS(WRAP);
        samplerBuilder.setWrapT(WRAP);
        mSampler = samplerBuilder.create();

        ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                           ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
        mPfBackground = builder.create();
        mPfBackground.bindSampler(mSampler, 0);

        mScript.set_gPFBackground(mPfBackground);

        return mScript;
    }

    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels) {
        mWorldState.yRotation = (xOffset * 4) * 180;
        updateWorldState();
    }

    @Override
    public void start() {
        super.start();
        mVisible = true;
        if (mAudioCapture != null) {
            mAudioCapture.start();
        }
        SystemClock.sleep(200);
        updateWave();
    }

    @Override
    public void stop() {
        super.stop();
        mVisible = false;
        if (mAudioCapture != null) {
            mAudioCapture.stop();
        }
        updateWave();
    }

    public void update() {
    }

    void updateWave() {
        mHandler.removeCallbacks(mDrawCube);
        if (!mVisible) {
            return;
        }
        mHandler.postDelayed(mDrawCube, 20);
        update();
        mWorldState.waveCounter++;
        updateWorldState();
    }

    protected void updateWorldState() {
        mScript.set_gYRotation(mWorldState.yRotation);
        mScript.set_gIdle(mWorldState.idle);
        mScript.set_gWaveCounter(mWorldState.waveCounter);
        mScript.set_gWidth(mWorldState.width);
    }
}
