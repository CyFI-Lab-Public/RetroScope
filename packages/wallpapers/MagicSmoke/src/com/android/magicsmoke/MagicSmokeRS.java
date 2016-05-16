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

package com.android.magicsmoke;

import static android.renderscript.Sampler.Value.LINEAR;
import static android.renderscript.Sampler.Value.WRAP;

import com.android.magicsmoke.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Handler;
import android.renderscript.*;
import android.renderscript.Element.Builder;
import android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.ProgramStore.BlendSrcFunc;
import android.util.Log;
import android.view.MotionEvent;
import android.os.Bundle;

import java.util.TimeZone;

class MagicSmokeRS extends RenderScriptScene implements OnSharedPreferenceChangeListener {

    static class WorldState {
        public float mXOffset;
        public float mYOffset;
        public int   mPreset;
        public int   mTextureMask;
        public int   mRotate;
        public int   mTextureSwap;
        public int   mProcessTextureMode;
        public int   mBackCol;
        public int   mLowCol;
        public int   mHighCol;
        public float mAlphaMul;
        public int   mPreMul;
    }
    WorldState mWorldState = new WorldState();
    //private Type mStateType;
    //private Allocation mState;

    private ProgramStore mPStore;
    private ProgramFragment mPF5tex;
    private ProgramFragment mPF4tex;
    private Sampler[] mSampler;
    private Allocation[] mSourceTextures;
    private Allocation[] mRealTextures;

    private ScriptC_clouds mScript;

    private ScriptField_VertexShaderConstants_s mVSConst;
    private ScriptField_FragmentShaderConstants_s mFSConst;

    private ProgramVertex mPV5tex;
    private ProgramVertex mPV4tex;
    private ProgramVertexFixedFunction.Constants mPVAlloc;

    private static final int RSID_STATE = 0;
    //private static final int RSID_PROGRAMVERTEX = 3;
    private static final int RSID_NOISESRC1 = 1;
    private static final int RSID_NOISESRC2 = 2;
    private static final int RSID_NOISESRC3 = 3;
    private static final int RSID_NOISESRC4 = 4;
    private static final int RSID_NOISESRC5 = 5;
    private static final int RSID_NOISEDST1 = 6;
    private static final int RSID_NOISEDST2 = 7;
    private static final int RSID_NOISEDST3 = 8;
    private static final int RSID_NOISEDST4 = 9;
    private static final int RSID_NOISEDST5 = 10;

    private Context mContext;
    private SharedPreferences mSharedPref;

    static class Preset {
        Preset(int processmode, int backcol, int locol, int hicol, float mul, int mask,
                 boolean rot, boolean texswap, boolean premul) {
            mProcessTextureMode = processmode;
            mBackColor = backcol;
            mLowColor = locol;
            mHighColor = hicol;
            mAlphaMul = mul;
            mTextureMask = mask;
            mRotate = rot;
            mTextureSwap = texswap;
            mPreMul = premul;
        }
        public int mProcessTextureMode;
        public int mBackColor;
        public int mLowColor;
        public int mHighColor;
        public float mAlphaMul;
        public int mTextureMask;
        public boolean mRotate;
        public boolean mTextureSwap;
        public boolean mPreMul;
    }

    public static final int DEFAULT_PRESET = 16;
    public static final Preset [] mPreset = new Preset[] {
        //       proc    back     low       high     alph  mask  rot    swap   premul
        new Preset(1,  0x000000, 0x000000, 0xffffff, 2.0f, 0x0f, true,  false, false),
        new Preset(1,  0x0000ff, 0x000000, 0xffffff, 2.0f, 0x0f, true,  false, false),
        new Preset(1,  0x00ff00, 0x000000, 0xffffff, 2.0f, 0x0f, true,  false, false),
        new Preset(1,  0x00ff00, 0x000000, 0xffffff, 2.0f, 0x0f, true,  false, true),
        new Preset(1,  0x00ff00, 0x00ff00, 0xffffff, 2.5f, 0x1f, true,  true,  true),
        new Preset(1,  0x800000, 0xff0000, 0xffffff, 2.5f, 0x1f, true,  true,  false),
        new Preset(0,  0x000000, 0x000000, 0xffffff, 0.0f, 0x1f, true,  false, false),
        new Preset(1,  0x0000ff, 0x00ff00, 0xffff00, 2.0f, 0x1f, true,  true,  false),
        new Preset(1,  0x008000, 0x00ff00, 0xffffff, 2.5f, 0x1f, true,  true,  false),
        new Preset(1,  0x800000, 0xff0000, 0xffffff, 2.5f, 0x1f, true,  true,  true),
        new Preset(1,  0x808080, 0x000000, 0xffffff, 2.0f, 0x0f, true,  false, true),
        new Preset(1,  0x0000ff, 0x000000, 0xffffff, 2.0f, 0x0f, true,  false, true),
        new Preset(1,  0x0000ff, 0x00ff00, 0xffff00, 1.5f, 0x1f, false, false, true),
        new Preset(1,  0x0000ff, 0x00ff00, 0xffff00, 2.0f, 0x1f, true,  true,  true),
        new Preset(1,  0x0000ff, 0x00ff00, 0xffff00, 1.5f, 0x1f, true,  true,  true),
        new Preset(1,  0x808080, 0x000000, 0xffffff, 2.0f, 0x0f, true,  false, false),
        new Preset(1,  0x000000, 0x000000, 0xffffff, 2.0f, 0x0f, true,  true,  false),
        new Preset(2,  0x000000, 0x000070, 0xff2020, 2.5f, 0x1f, true,  false, false),
        new Preset(2,  0x6060ff, 0x000070, 0xffffff, 2.5f, 0x1f, true,  false, false),
        new Preset(3,  0x0000f0, 0x000000, 0xffffff, 2.0f, 0x0f, true,  true,  false),
    };

    private float mTouchY;

    MagicSmokeRS(Context context, int width, int height) {
        super(width, height);
        mWidth = width;
        mHeight = height;
        mContext = context;
        mSharedPref = mContext.getSharedPreferences("magicsmoke", Context.MODE_PRIVATE);
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        makeNewState();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (!mIsStarted) {
            start();
            mRS.finish();
            stop(false);
        } else {
            makeNewState();
        }
    }

    void makeNewState() {
        int p = mSharedPref.getInt("preset", DEFAULT_PRESET);
        if (p >= mPreset.length) {
            p = 0;
        }
        mWorldState.mPreset = p;
        mWorldState.mTextureMask = mPreset[p].mTextureMask;
        mWorldState.mRotate = mPreset[p].mRotate ? 1 : 0;
        mWorldState.mTextureSwap = mPreset[p].mTextureSwap ? 1 : 0;
        mWorldState.mProcessTextureMode = mPreset[p].mProcessTextureMode;
        mWorldState.mBackCol = mPreset[p].mBackColor;
        mWorldState.mLowCol = mPreset[p].mLowColor;
        mWorldState.mHighCol = mPreset[p].mHighColor;
        mWorldState.mAlphaMul = mPreset[p].mAlphaMul;
        mWorldState.mPreMul = mPreset[p].mPreMul ? 1 : 0;

        if(mScript != null) {
            mScript.set_gPreset(mWorldState.mPreset);
            mScript.set_gTextureMask(mWorldState.mTextureMask);
            mScript.set_gRotate(mWorldState.mRotate);
            mScript.set_gTextureSwap(mWorldState.mTextureSwap);
            mScript.set_gProcessTextureMode(mWorldState.mProcessTextureMode);
            mScript.set_gBackCol(mWorldState.mBackCol);
            mScript.set_gLowCol(mWorldState.mLowCol);
            mScript.set_gHighCol(mWorldState.mHighCol);
            mScript.set_gAlphaMul(mWorldState.mAlphaMul);
            mScript.set_gPreMul(mWorldState.mPreMul);
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
    }

    @Override
    public Bundle onCommand(String action, int x, int y, int z, Bundle extras,
            boolean resultRequested) {

        if ("android.wallpaper.tap".equals(action)) {
            mTouchY = y;
        }
        return null;
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
                dy /= 20;
                if (dy > 4) {
                    dy = 4;
                } else if (dy < -4) {
                    dy = -4;
                }
                //mState.data(mWorldState);
        }
    }*/

    @Override
    public void setOffset(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
        // update our state, then push it to the renderscript
        mWorldState.mXOffset = xOffset;
        mWorldState.mYOffset = yOffset;
        mScript.set_gXOffset(mWorldState.mXOffset);
        mScript.set_gYOffset(mWorldState.mYOffset);
    }

    @Override
    public void stop(boolean forReal) {
        if (forReal) {
            mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.stop(forReal);
    }

    @Override
    public void start() {
        makeNewState();
        super.start();
    }

    float alphafactor;
    Type mTextureType;

    void loadBitmap(int id, int index, String name, float alphamul, int lowcol, int highcol) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap in = BitmapFactory.decodeResource(mResources, id, opts);

        // Bitmaps are stored in memory in premultiplied form. We want non-premultiplied,
        // which is what getPixels gives us.
        int pixels[] = new int[65536];
        in.getPixels(pixels, 0, 256, 0, 0, 256, 256);
        mRealTextures[index] = Allocation.createTyped(mRS, mTextureType,
                                                      Allocation.MipmapControl.MIPMAP_NONE,
                                                      Allocation.USAGE_SCRIPT |
                                                      Allocation.USAGE_GRAPHICS_TEXTURE);
        mSourceTextures[index] = Allocation.createTyped(mRS, mTextureType,
                                                      Allocation.MipmapControl.MIPMAP_NONE,
                                                      Allocation.USAGE_SCRIPT);

        // copyFrom needs a byte[], not an int[], so we need to copy the data first
        byte bpixels[] = new byte[65536*4];
        for (int i = 0; i < 65536; i++) {
            bpixels[i * 4 + 0] = (byte)(pixels[i] & 0xff);
            bpixels[i * 4 + 1] = (byte)((pixels[i] >> 8) & 0xff);
            bpixels[i * 4 + 2] = (byte)((pixels[i] >>16) & 0xff);
            bpixels[i * 4 + 3] = (byte)((pixels[i] >> 24) & 0xff);
        }
        mSourceTextures[index].copyFrom(bpixels);
        in.recycle();
    }

    void loadBitmaps() {
        alphafactor = 1f;
        float alphamul = mPreset[mWorldState.mPreset].mAlphaMul;
        int lowcol = mPreset[mWorldState.mPreset].mLowColor;
        int highcol = mPreset[mWorldState.mPreset].mHighColor;
        //Log.i("@@@@", "preset " + mWorldState.mPreset + ", mul: " + alphamul +
        //        ", colors: " + Integer.toHexString(lowcol) + "/" + Integer.toHexString(highcol));

        // TODO: using different high and low colors for each layer offers some cool effects too
        loadBitmap(R.drawable.noise1, 0, "Tnoise1", alphamul, lowcol, highcol);
        loadBitmap(R.drawable.noise2, 1, "Tnoise2", alphamul, lowcol, highcol);
        loadBitmap(R.drawable.noise3, 2, "Tnoise3", alphamul, lowcol, highcol);
        loadBitmap(R.drawable.noise4, 3, "Tnoise4", alphamul, lowcol, highcol);
        loadBitmap(R.drawable.noise5, 4, "Tnoise5", alphamul, lowcol, highcol);

        mScript.set_gTnoise1(mRealTextures[0]);
        mScript.set_gTnoise2(mRealTextures[1]);
        mScript.set_gTnoise3(mRealTextures[2]);
        mScript.set_gTnoise4(mRealTextures[3]);
        mScript.set_gTnoise5(mRealTextures[4]);

        mScript.bind_gNoisesrc1(mSourceTextures[0]);
        mScript.bind_gNoisesrc2(mSourceTextures[1]);
        mScript.bind_gNoisesrc3(mSourceTextures[2]);
        mScript.bind_gNoisesrc4(mSourceTextures[3]);
        mScript.bind_gNoisesrc5(mSourceTextures[4]);

        mScript.bind_gNoisedst1(mRealTextures[0]);
        mScript.bind_gNoisedst2(mRealTextures[1]);
        mScript.bind_gNoisedst3(mRealTextures[2]);
        mScript.bind_gNoisedst4(mRealTextures[3]);
        mScript.bind_gNoisedst5(mRealTextures[4]);
    }

    @Override
    protected ScriptC createScript() {

        mScript = new ScriptC_clouds(mRS, mResources, R.raw.clouds);

        mVSConst = new ScriptField_VertexShaderConstants_s(mRS, 1);
        mScript.bind_gVSConstants(mVSConst);

        {
            ProgramVertex.Builder builder = new ProgramVertex.Builder(mRS);
            builder.setShader(mResources, R.raw.pv5tex);
            builder.addConstant(mVSConst.getAllocation().getType());
            builder.addInput(ScriptField_VertexInputs_s.createElement(mRS));

            mPV5tex = builder.create();
            mPV5tex.bindConstants(mVSConst.getAllocation(), 0);

            builder.setShader(mResources, R.raw.pv4tex);
            mPV4tex = builder.create();
            mPV4tex.bindConstants(mVSConst.getAllocation(), 0);
        }
        mScript.set_gPV5tex(mPV5tex);
        mScript.set_gPV4tex(mPV4tex);

        mSourceTextures = new Allocation[5];
        mRealTextures = new Allocation[5];

        Type.Builder tb = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tb.setX(256);
        tb.setY(256);
        mTextureType = tb.create();
        loadBitmaps();

        Sampler.Builder samplerBuilder = new Sampler.Builder(mRS);
        samplerBuilder.setMinification(LINEAR);
        samplerBuilder.setMagnification(LINEAR);
        samplerBuilder.setWrapS(WRAP);
        samplerBuilder.setWrapT(WRAP);
        mSampler = new Sampler[5];
        for (int i = 0; i < 5; i++)
            mSampler[i] = samplerBuilder.create();

        {
            mFSConst = new ScriptField_FragmentShaderConstants_s(mRS, 1);
            mScript.bind_gFSConstants(mFSConst);

            ProgramFragment.Builder builder = new ProgramFragment.Builder(mRS);
            builder.setShader(mResources, R.raw.pf5tex);
            for (int texCount = 0; texCount < 5; texCount ++) {
                builder.addTexture(Program.TextureType.TEXTURE_2D);
            }
            builder.addConstant(mFSConst.getAllocation().getType());

            mPF5tex = builder.create();
            for (int i = 0; i < 5; i++)
                mPF5tex.bindSampler(mSampler[i], i);
            mPF5tex.bindConstants(mFSConst.getAllocation(), 0);

            builder = new ProgramFragment.Builder(mRS);
            builder.setShader(mResources, R.raw.pf4tex);
            for (int texCount = 0; texCount < 4; texCount ++) {
                builder.addTexture(Program.TextureType.TEXTURE_2D);
            }
            builder.addConstant(mFSConst.getAllocation().getType());
            mPF4tex = builder.create();
            for (int i = 0; i < 4; i++)
                mPF4tex.bindSampler(mSampler[i], i);
            mPF4tex.bindConstants(mFSConst.getAllocation(), 0);
        }

        mScript.set_gPF5tex(mPF5tex);
        mScript.set_gPF4tex(mPF4tex);


        {
            ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
            builder.setDepthFunc(ProgramStore.DepthFunc.ALWAYS);
            builder.setBlendFunc(BlendSrcFunc.ONE, BlendDstFunc.ZERO);
            builder.setDitherEnabled(true); // without dithering there is severe banding
            builder.setDepthMaskEnabled(false);
            mPStore = builder.create();
        }

        mScript.set_gPStore(mPStore);

        mScript.set_gPreset(mWorldState.mPreset);
        mScript.set_gTextureMask(mWorldState.mTextureMask);
        mScript.set_gRotate(mWorldState.mRotate);
        mScript.set_gTextureSwap(mWorldState.mTextureSwap);
        mScript.set_gProcessTextureMode(mWorldState.mProcessTextureMode);
        mScript.set_gBackCol(mWorldState.mBackCol);
        mScript.set_gLowCol(mWorldState.mLowCol);
        mScript.set_gHighCol(mWorldState.mHighCol);
        mScript.set_gAlphaMul(mWorldState.mAlphaMul);
        mScript.set_gPreMul(mWorldState.mPreMul);
        mScript.set_gXOffset(mWorldState.mXOffset);

        return mScript;
    }
}
