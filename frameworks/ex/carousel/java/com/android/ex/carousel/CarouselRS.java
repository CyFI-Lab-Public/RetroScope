/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.ex.carousel;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.renderscript.*;
import static android.renderscript.Element.*;
import android.renderscript.Program.TextureType;
import android.renderscript.RenderScript.RSMessageHandler;
import android.util.Log;

/**
 * This is a support class for Carousel renderscript.  It handles most of the low-level interactions
 * with Renderscript as well as dispatching events.
 *
 */
public class CarouselRS  {
    private static final int DEFAULT_VISIBLE_SLOTS = 1;
    private static final int DEFAULT_CARD_COUNT = 0;
    private static final int DEFAULT_ROW_COUNT = 1;

    // Client messages *** THIS LIST MUST MATCH THOSE IN carousel.rs ***
    public static final int CMD_CARD_SELECTED = 100;
    public static final int CMD_DETAIL_SELECTED = 105;
    public static final int CMD_CARD_LONGPRESS = 110;
    public static final int CMD_REQUEST_TEXTURE = 200;
    public static final int CMD_INVALIDATE_TEXTURE = 210;
    public static final int CMD_REQUEST_GEOMETRY = 300;
    public static final int CMD_INVALIDATE_GEOMETRY = 310;
    public static final int CMD_ANIMATION_STARTED = 400;
    public static final int CMD_ANIMATION_FINISHED = 500;
    public static final int CMD_REQUEST_DETAIL_TEXTURE = 600;
    public static final int CMD_INVALIDATE_DETAIL_TEXTURE = 610;
    public static final int CMD_PING = 1000; // for debugging

    // Drag models *** THIS LIST MUST MATCH THOSE IN carousel.rs ***
    public static final int DRAG_MODEL_SCREEN_DELTA = 0;
    public static final int DRAG_MODEL_PLANE = 1;
    public static final int DRAG_MODEL_CYLINDER_INSIDE = 2;
    public static final int DRAG_MODEL_CYLINDER_OUTSIDE = 3;

    public static final int FILL_DIRECTION_CCW = +1;
    public static final int FILL_DIRECTION_CW = -1;

    private static final String TAG = "CarouselRS";
    private static final int DEFAULT_SLOT_COUNT = 10;
    private static final Allocation.MipmapControl MIPMAP =
        Allocation.MipmapControl.MIPMAP_NONE;
    private static final boolean DBG = false;

    private RenderScriptGL mRS;
    private Resources mRes;
    private ScriptC_carousel mScript;
    private ScriptField_Card mCards;
    private ScriptField_FragmentShaderConstants_s mFSConst;
    private ScriptField_ProgramStore_s mProgramStoresCard;
    private ProgramFragment mSingleTextureFragmentProgram;
    private ProgramFragment mSingleTextureBlendingFragmentProgram;
    private ProgramFragment mMultiTextureFragmentProgram;
    private ProgramFragment mMultiTextureBlendingFragmentProgram;
    private ProgramVertex mVertexProgram;
    private ProgramRaster mRasterProgram;
    private Allocation[] mAllocationPool;
    private boolean mForceBlendCardsWithZ;
    private int mVisibleSlots;
    private int mRowCount;
    private int mPrefetchCardCount;
    private CarouselCallback mCallback;
    private float[] mEyePoint = new float[] { 2.0f, 0.0f, 0.0f };
    private float[] mAtPoint = new float[] { 0.0f, 0.0f, 0.0f };
    private float[] mUp = new float[] { 0.0f, 1.0f, 0.0f };

    private static final String mSingleTextureShader = new String(
            "varying vec2 varTex0;" +
            "void main() {" +
            "vec2 t0 = varTex0.xy;" +
            "vec4 col = texture2D(UNI_Tex0, t0);" +
            "gl_FragColor = col; " +
            "}");

    private static final String mSingleTextureBlendingShader = new String(
            "varying vec2 varTex0;" +
            "void main() {" +
            "vec2 t0 = varTex0.xy;" +
            "vec4 col = texture2D(UNI_Tex0, t0);" +
            "gl_FragColor = col * UNI_overallAlpha; " +
            "}");

    private static final String mMultiTextureShader = new String(
            "varying vec2 varTex0;" +
            "void main() {" +
            "vec2 t0 = varTex0.xy;" +
            "vec4 col = texture2D(UNI_Tex0, t0);" +
            "vec4 col2 = texture2D(UNI_Tex1, t0);" +
            "gl_FragColor = mix(col, col2, UNI_fadeAmount);}");

    private static final String mMultiTextureBlendingShader = new String(
            "varying vec2 varTex0;" +
            "void main() {" +
            "vec2 t0 = varTex0.xy;" +
            "vec4 col = texture2D(UNI_Tex0, t0);" +
            "vec4 col2 = texture2D(UNI_Tex1, t0);" +
            "gl_FragColor = mix(col, col2, UNI_fadeAmount) * UNI_overallAlpha;" +
            "}"
    );

    public static interface CarouselCallback {
        /**
         * Called when a card is selected
         * @param n the id of the card
         */
        void onCardSelected(int n);

        /**
         * Called when the detail texture for a card is tapped
         * @param n the id of the card
         * @param x how far the user tapped from the left edge of the card, in pixels
         * @param y how far the user tapped from the top edge of the card, in pixels
         */
        void onDetailSelected(int n, int x, int y);

        /**
         * Called when a card is long-pressed
         * @param n the id of the card
         * @param touchPosition position of where the user pressed, in screen coordinates
         * @param detailCoordinates position of detail texture, in screen coordinates
         */
        void onCardLongPress(int n, int touchPosition[], Rect detailCoordinates);

        /**
         * Called when texture is needed for card n.  This happens when the given card becomes
         * visible.
         * @param n the id of the card
         */
        void onRequestTexture(int n);

        /**
         * Called when a texture is no longer needed for card n.  This happens when the card
         * goes out of view.
         * @param n the id of the card
         */
        void onInvalidateTexture(int n);

        /**
         * Called when detail texture is needed for card n.  This happens when the given card
         * becomes visible.
         * @param n the id of the card
         */
        void onRequestDetailTexture(int n);

        /**
         * Called when a detail texture is no longer needed for card n.  This happens when the card
         * goes out of view.
         * @param n the id of the card
         */
        void onInvalidateDetailTexture(int n);

        /**
         * Called when geometry is needed for card n.
         * @param n the id of the card.
         */
        void onRequestGeometry(int n);

        /**
         * Called when geometry is no longer needed for card n. This happens when the card goes
         * out of view.
         * @param n the id of the card
         */
        void onInvalidateGeometry(int n);

        /**
         * Called when card animation (e.g. a fling) has started.
         */
        void onAnimationStarted();

        /**
         * Called when card animation has stopped.
         * @param carouselRotationAngle the angle of rotation, in radians, at which the animation
         * stopped.
         */
        void onAnimationFinished(float carouselRotationAngle);
    };

    private RSMessageHandler mRsMessage = new RSMessageHandler() {
        public void run() {
            if (mCallback == null) return;
            switch (mID) {
                case CMD_CARD_SELECTED:
                    mCallback.onCardSelected(mData[0]);
                    break;

                case CMD_DETAIL_SELECTED:
                    mCallback.onDetailSelected(mData[0], mData[1], mData[2]);
                    break;

                case CMD_CARD_LONGPRESS:
                    int touchPosition[] = { mData[1], mData[2] };
                    Rect detailCoordinates = new Rect(mData[3], mData[4], mData[5], mData[6]);
                    mCallback.onCardLongPress(mData[0], touchPosition, detailCoordinates);
                    break;

                case CMD_REQUEST_TEXTURE:
                    mCallback.onRequestTexture(mData[0]);
                    break;

                case CMD_INVALIDATE_TEXTURE:
                    setTexture(mData[0], null);
                    mCallback.onInvalidateTexture(mData[0]);
                    break;

                case CMD_REQUEST_DETAIL_TEXTURE:
                    mCallback.onRequestDetailTexture(mData[0]);
                    break;

                case CMD_INVALIDATE_DETAIL_TEXTURE:
                    setDetailTexture(mData[0], 0.0f, 0.0f, 0.0f, 0.0f, null);
                    mCallback.onInvalidateDetailTexture(mData[0]);
                    break;

                case CMD_REQUEST_GEOMETRY:
                    mCallback.onRequestGeometry(mData[0]);
                    break;

                case CMD_INVALIDATE_GEOMETRY:
                    setGeometry(mData[0], null);
                    mCallback.onInvalidateGeometry(mData[0]);
                    break;

                case CMD_ANIMATION_STARTED:
                    mCallback.onAnimationStarted();
                    break;

                case CMD_ANIMATION_FINISHED:
                    mCallback.onAnimationFinished(Float.intBitsToFloat(mData[0]));
                    break;

                case CMD_PING:
                    if (DBG) Log.v(TAG, "PING...");
                    break;

                default:
                    Log.e(TAG, "Unknown RSMessage: " + mID);
            }
        }
    };

    public CarouselRS(RenderScriptGL rs, Resources res, int resId) {
        mRS = rs;
        mRes = res;

        // create the script object
        mScript = new ScriptC_carousel(mRS, mRes, resId);
        mRS.setMessageHandler(mRsMessage);
        initProgramStore();
        initFragmentProgram();
        initRasterProgram();
        initVertexProgram();
        setSlotCount(DEFAULT_SLOT_COUNT);
        setVisibleSlots(DEFAULT_VISIBLE_SLOTS);
        setRowCount(DEFAULT_ROW_COUNT);
        createCards(DEFAULT_CARD_COUNT);
        setStartAngle(0.0f);
        setCarouselRotationAngle(0.0f);
        setRadius(1.0f);
        setLookAt(mEyePoint, mAtPoint, mUp);
        setRadius(20.0f);
        // Fov: 25
    }

    public void setLookAt(float[] eye, float[] at, float[] up) {
        for (int i = 0; i < 3; i++) {
            mEyePoint[i] = eye[i];
            mAtPoint[i] = at[i];
            mUp[i] = up[i];
        }
        mScript.invoke_lookAt(eye[0], eye[1], eye[2], at[0], at[1], at[2], up[0], up[1], up[2]);
    }

    public void setRadius(float radius) {
        mScript.invoke_setRadius(radius);
    }

    public void setCardRotation(float cardRotation) {
        mScript.set_cardRotation(cardRotation);
    }

    public void setCardsFaceTangent(boolean faceTangent) {
        mScript.set_cardsFaceTangent(faceTangent);
    }

    public void setSwaySensitivity(float swaySensitivity) {
        mScript.set_swaySensitivity(swaySensitivity);
    }

    public void setFrictionCoefficient(float frictionCoeff) {
        mScript.set_frictionCoeff(frictionCoeff);
    }

    public void setDragFactor(float dragFactor) {
        mScript.set_dragFactor(dragFactor);
    }

    public void setDragModel(int model) {
        mScript.set_dragModel(model);
    }

    public void setFillDirection(int direction) {
        mScript.set_fillDirection(direction);
    }

    private Matrix4f matrixFromFloat(float[] matrix) {
        int dimensions;
        if (matrix == null || matrix.length == 0) {
          dimensions = 0;
        } else if (matrix.length == 16) {
          dimensions = 4;
        } else if (matrix.length == 9) {
          dimensions = 3;
        } else {
          throw new IllegalArgumentException("matrix length not 0,9 or 16");
        }

        Matrix4f rsMatrix = new Matrix4f();  // initialized as identity.
        for (int i = 0; i < dimensions; i++) {
            for (int j = 0; j < dimensions; j++) {
                rsMatrix.set(i, j, matrix[i*dimensions + j]);
            }
        }

        return rsMatrix;
    }

    public void setDefaultCardMatrix(float[] matrix) {
        mScript.set_defaultCardMatrix(matrixFromFloat(matrix));
    }

    private void initVertexProgram() {
        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        mVertexProgram = pvb.create();
        ProgramVertexFixedFunction.Constants pva = new ProgramVertexFixedFunction.Constants(mRS);
        ((ProgramVertexFixedFunction)mVertexProgram).bindConstants(pva);
        Matrix4f proj = new Matrix4f();
        proj.loadProjectionNormalized(1, 1);
        pva.setProjection(proj);
        mScript.set_vertexProgram(mVertexProgram);
    }

    private void initRasterProgram() {
        ProgramRaster.Builder programRasterBuilder = new ProgramRaster.Builder(mRS);
        mRasterProgram = programRasterBuilder.create();
        //mRasterProgram.setCullMode(CullMode.NONE);
        mScript.set_rasterProgram(mRasterProgram);
    }

    private void initFragmentProgram() {
        //
        // Single texture program
        //
        ProgramFragment.Builder pfbSingle = new ProgramFragment.Builder(mRS);
        // Specify the resource that contains the shader string
        pfbSingle.setShader(mSingleTextureShader);
        // Tell the builder how many textures we have
        pfbSingle.addTexture(Program.TextureType.TEXTURE_2D);
        mSingleTextureFragmentProgram = pfbSingle.create();
        // Bind the source of constant data
        mSingleTextureFragmentProgram.bindSampler(Sampler.CLAMP_LINEAR(mRS), 0);

        //
        // Single texture program, plus blending
        //
        mFSConst = new ScriptField_FragmentShaderConstants_s(mRS, 1);
        mScript.bind_shaderConstants(mFSConst);
        ProgramFragment.Builder pfbSingleBlend = new ProgramFragment.Builder(mRS);
        // Specify the resource that contains the shader string
        pfbSingleBlend.setShader(mSingleTextureBlendingShader);
        // Tell the builder how many textures we have
        pfbSingleBlend.addTexture(Program.TextureType.TEXTURE_2D);
        // Define the constant input layout
        pfbSingleBlend.addConstant(mFSConst.getAllocation().getType());
        mSingleTextureBlendingFragmentProgram = pfbSingleBlend.create();
        // Bind the source of constant data
        mSingleTextureBlendingFragmentProgram.bindConstants(mFSConst.getAllocation(), 0);
        mSingleTextureBlendingFragmentProgram.bindSampler(Sampler.CLAMP_LINEAR(mRS), 0);

        //
        // Multi texture program
        //
        ProgramFragment.Builder pfbMulti = new ProgramFragment.Builder(mRS);
        // Specify the resource that contains the shader string
        pfbMulti.setShader(mMultiTextureShader);
        // Tell the builder how many textures we have
        pfbMulti.addTexture(Program.TextureType.TEXTURE_2D);
        pfbMulti.addTexture(Program.TextureType.TEXTURE_2D);
        // Define the constant input layout
        pfbMulti.addConstant(mFSConst.getAllocation().getType());
        mMultiTextureFragmentProgram = pfbMulti.create();
        // Bind the source of constant data
        mMultiTextureFragmentProgram.bindConstants(mFSConst.getAllocation(), 0);
        mMultiTextureFragmentProgram.bindSampler(Sampler.CLAMP_LINEAR(mRS), 0);
        mMultiTextureFragmentProgram.bindSampler(Sampler.CLAMP_LINEAR(mRS), 1);

        //
        // Multi texture program, plus blending
        //
        ProgramFragment.Builder pfbMultiBlend = new ProgramFragment.Builder(mRS);
        // Specify the resource that contains the shader string
        pfbMultiBlend.setShader(mMultiTextureBlendingShader);
        // Tell the builder how many textures we have
        pfbMultiBlend.addTexture(Program.TextureType.TEXTURE_2D);
        pfbMultiBlend.addTexture(Program.TextureType.TEXTURE_2D);
        // Define the constant input layout
        pfbMultiBlend.addConstant(mFSConst.getAllocation().getType());
        mMultiTextureBlendingFragmentProgram = pfbMultiBlend.create();
        // Bind the source of constant data
        mMultiTextureBlendingFragmentProgram.bindConstants(mFSConst.getAllocation(), 0);
        mMultiTextureBlendingFragmentProgram.bindSampler(Sampler.CLAMP_LINEAR(mRS), 0);
        mMultiTextureBlendingFragmentProgram.bindSampler(Sampler.CLAMP_LINEAR(mRS), 1);

        mScript.set_linearClamp(Sampler.CLAMP_LINEAR(mRS));
        mScript.set_singleTextureFragmentProgram(mSingleTextureFragmentProgram);
        mScript.set_singleTextureBlendingFragmentProgram(mSingleTextureBlendingFragmentProgram);
        mScript.set_multiTextureFragmentProgram(mMultiTextureFragmentProgram);
        mScript.set_multiTextureBlendingFragmentProgram(mMultiTextureBlendingFragmentProgram);
    }

    private void initProgramStore() {
        resizeProgramStoresCard(1);

        final boolean dither = true;
        final ProgramStore.DepthFunc depthFunc = mForceBlendCardsWithZ ?
                ProgramStore.DepthFunc.LESS : ProgramStore.DepthFunc.ALWAYS;

        // Background: Alpha disabled, depth optional
        mScript.set_programStoreBackground(new ProgramStore.Builder(mRS)
            .setBlendFunc(ProgramStore.BlendSrcFunc.ONE, ProgramStore.BlendDstFunc.ZERO)
            .setDitherEnabled(dither)
            .setDepthFunc(depthFunc)
            .setDepthMaskEnabled(mForceBlendCardsWithZ)
            .create());

        // Card: Alpha enabled, depth optional
        setProgramStoreCard(0, new ProgramStore.Builder(mRS)
            .setBlendFunc(ProgramStore.BlendSrcFunc.ONE,
                ProgramStore.BlendDstFunc.ONE_MINUS_SRC_ALPHA)
            .setDitherEnabled(dither)
            .setDepthFunc(depthFunc)
            .setDepthMaskEnabled(mForceBlendCardsWithZ)
            .create());

        // Detail: Alpha enabled, depth disabled
        mScript.set_programStoreDetail(new ProgramStore.Builder(mRS)
            .setBlendFunc(ProgramStore.BlendSrcFunc.ONE,
                ProgramStore.BlendDstFunc.ONE_MINUS_SRC_ALPHA)
            .setDitherEnabled(dither)
            .setDepthFunc(ProgramStore.DepthFunc.ALWAYS)
            .setDepthMaskEnabled(false)
            .create());
    }

    public void createCards(int count)
    {
        // Because RenderScript can't have allocations with 0 dimensions, we always create
        // an allocation of at least one card. This relies on invoke_createCards() to keep
        // track of when the allocation is not valid.
        if (mCards != null && count > 0) {
            // resize the array
            int oldSize = mCards.getAllocation().getType().getX();
            mCards.resize(count);
            mScript.invoke_createCards(oldSize, count);
        } else {
            // create array from scratch
            mCards = new ScriptField_Card(mRS, count > 0 ? count : 1);
            mScript.bind_cards(mCards);
            mScript.invoke_createCards(0, count);
        }
    }

    public void setVisibleSlots(int count)
    {
        mVisibleSlots = count;
        mScript.set_visibleSlotCount(count);
    }

    public void setVisibleDetails(int count) {
        mScript.set_visibleDetailCount(count);
    }

    public void setRowCount(int count) {
        mRowCount = count;
        mScript.set_rowCount(count);
    }

    public void setRowSpacing(float spacing) {
        mScript.set_rowSpacing(spacing);
    }

    public void setOverscrollSlots(float slots) {
        mScript.set_overscrollSlots(slots);
    }

    public void setFirstCardTop(boolean first) {
        mScript.set_firstCardTop(first);
    }

    public void setPrefetchCardCount(int count) {
        mPrefetchCardCount = count;
        mScript.set_prefetchCardCount(count);
    }

    public void setDetailTextureAlignment(int alignment) {
        mScript.set_detailTextureAlignment(alignment);
    }

    private void resizeProgramStoresCard(int count) {
        // enableResize works around a Renderscript bug that keeps resizes from being propagated.
        // TODO(jshuma): Remove enableResize once the Renderscript bug is fixed
        final boolean enableResize = false;

        if (mProgramStoresCard != null && enableResize) {
            int newSize = count > 0 ? count : 1;
            mProgramStoresCard.resize(newSize);
        } else {
            mProgramStoresCard = new ScriptField_ProgramStore_s(mRS, count > 0 ? count : 1);
            mScript.bind_programStoresCard(mProgramStoresCard);
        }
    }

    private void setProgramStoreCard(int n, ProgramStore programStore) {
        ScriptField_ProgramStore_s.Item item = mProgramStoresCard.get(n);
        if (item == null) {
            item = new ScriptField_ProgramStore_s.Item();
        }
        item.programStore = programStore;
        mProgramStoresCard.set(item, n, false);
        mScript.invoke_setProgramStoresCard(n, programStore);
    }

    public void setStoreConfigs(int configs[]) {
        if (configs == null) {
            initProgramStore();
            return;
        }

        final int count = configs.length;

        resizeProgramStoresCard(count);
        for (int i=0; i<count; ++i) {
            final int config = configs[i];

            final boolean alpha = (config & CarouselController.STORE_CONFIG_ALPHA) != 0;
            final boolean depthReads = (config & CarouselController.STORE_CONFIG_DEPTH_READS) != 0;
            final boolean depthWrites =
                    (config & CarouselController.STORE_CONFIG_DEPTH_WRITES) != 0;

            final boolean dither = true;
            final ProgramStore.BlendDstFunc dstFunc = alpha ?
                    ProgramStore.BlendDstFunc.ONE_MINUS_SRC_ALPHA :
                    ProgramStore.BlendDstFunc.ZERO;
            final ProgramStore.DepthFunc depthFunc = depthReads ?
                    ProgramStore.DepthFunc.LESS :
                    ProgramStore.DepthFunc.ALWAYS;

            final ProgramStore ps = new ProgramStore.Builder(mRS)
                    .setBlendFunc(ProgramStore.BlendSrcFunc.ONE, dstFunc)
                    .setDitherEnabled(dither)
                    .setDepthFunc(depthFunc)
                    .setDepthMaskEnabled(depthWrites)
                    .create();

            setProgramStoreCard(i, ps);
        }
    }

    /**
     * Sets whether the background texture and default card geometry are to be drawn with respect
     * to the depth buffer (both reading from it and writing to it).
     *
     * This method is a specialization of functionality that can be done with greater flexibility
     * by setStoreConfigs. Calling setForceBlendCardsWithZ() after calling setStoreConfigs()
     * results in the values set in setStoreConfigs() being discarded.
     *
     * @param enabled true to read from and write to the depth buffer, false to ignore it
     */
    public void setForceBlendCardsWithZ(boolean enabled) {
        mForceBlendCardsWithZ = enabled;
        initProgramStore();
    }

    public void setDrawRuler(boolean drawRuler) {
        mScript.set_drawRuler(drawRuler);
    }

    public void setDefaultBitmap(Bitmap bitmap)
    {
        mScript.set_defaultTexture(allocationFromBitmap(bitmap, MIPMAP));
    }

    public void setLoadingBitmap(Bitmap bitmap)
    {
        mScript.set_loadingTexture(allocationFromBitmap(bitmap, MIPMAP));
    }

    public void setDefaultGeometry(Mesh mesh)
    {
        mScript.set_defaultGeometry(mesh);
    }

    public void setLoadingGeometry(Mesh mesh)
    {
        mScript.set_loadingGeometry(mesh);
    }

    public void setStartAngle(float theta)
    {
        mScript.set_startAngle(theta);
    }

    public void setCarouselRotationAngle(float theta) {
        mScript.invoke_setCarouselRotationAngle(theta);
    }

    public void setCarouselRotationAngle(float endAngle, int milliseconds, int interpolationMode,
            float maxAnimatedArc) {
        mScript.invoke_setCarouselRotationAngle2(endAngle, milliseconds, interpolationMode,
                maxAnimatedArc);
    }

    public void setCallback(CarouselCallback callback)
    {
        mCallback = callback;
    }

    private Allocation allocationFromBitmap(Bitmap bitmap, Allocation.MipmapControl mipmap)
    {
        if (bitmap == null) return null;
        Allocation allocation = Allocation.createFromBitmap(mRS, bitmap,
                mipmap, Allocation.USAGE_GRAPHICS_TEXTURE);
        return allocation;
    }

    private Allocation allocationFromPool(int n, Bitmap bitmap, Allocation.MipmapControl mipmap)
    {
        int count = (mVisibleSlots + 2*mPrefetchCardCount) * mRowCount;
        if (mAllocationPool == null || mAllocationPool.length != count) {
            Allocation[] tmp = new Allocation[count];
            int oldsize = mAllocationPool == null ? 0 : mAllocationPool.length;
            for (int i = 0; i < Math.min(count, oldsize); i++) {
                tmp[i] = mAllocationPool[i];
            }
            mAllocationPool = tmp;
        }
        Allocation allocation = mAllocationPool[n % count];
        if (allocation == null) {
            allocation = allocationFromBitmap(bitmap, mipmap);
            mAllocationPool[n % count]  = allocation;
        } else if (bitmap != null) {
            if (bitmap.getWidth() == allocation.getType().getX()
                && bitmap.getHeight() == allocation.getType().getY()) {
                allocation.copyFrom(bitmap);
            } else {
                Log.v(TAG, "Warning, bitmap has different size. Taking slow path");
                allocation = allocationFromBitmap(bitmap, mipmap);
                mAllocationPool[n % count]  = allocation;
            }
        }
        return allocation;
    }

    private ScriptField_Card.Item getCard(int n) {
        ScriptField_Card.Item item;
        try {
            item = mCards.get(n);
        }
        catch (ArrayIndexOutOfBoundsException e) {
            if (DBG) Log.v(TAG, "getCard(): no item at index " + n);
            item = null;
        }
        return item;
    }

    private ScriptField_Card.Item getOrCreateCard(int n) {
        ScriptField_Card.Item item = getCard(n);
        if (item == null) {
            if (DBG) Log.v(TAG, "getOrCreateCard(): no item at index " + n + "; creating new");
            item = new ScriptField_Card.Item();
        }
        return item;
    }

    private void setCard(int n, ScriptField_Card.Item item) {
        try {
            mCards.set(item, n, false); // This is primarily used for reference counting.
        }
        catch (ArrayIndexOutOfBoundsException e) {
            // The specified index didn't exist. This can happen when a stale invalidate
            // request outlived an array resize request. Something might be getting dropped,
            // but there's not much we can do about this at this point to recover.
            Log.w(TAG, "setCard(" + n + "): Texture " + n + " doesn't exist");
        }
    }

    public void setTexture(int n, Bitmap bitmap)
    {
        if (n < 0) throw new IllegalArgumentException("Index cannot be negative");

        synchronized(this) {
            ScriptField_Card.Item item = getOrCreateCard(n);
            if (bitmap != null) {
                item.texture = allocationFromPool(n, bitmap, MIPMAP);
            } else {
                if (item.texture != null) {
                    if (DBG) Log.v(TAG, "unloading texture " + n);
                    item.texture = null;
                }
            }
            setCard(n, item);
            mScript.invoke_setTexture(n, item.texture);
        }
    }

    void setDetailTexture(int n, float offx, float offy, float loffx, float loffy, Bitmap bitmap)
    {
        if (n < 0) throw new IllegalArgumentException("Index cannot be negative");

        synchronized(this) {
            ScriptField_Card.Item item = getOrCreateCard(n);
            float width = 0.0f;
            float height = 0.0f;
            if (bitmap != null) {
                item.detailTexture = allocationFromBitmap(bitmap, MIPMAP);
                width = bitmap.getWidth();
                height = bitmap.getHeight();
            } else {
                if (item.detailTexture != null) {
                    if (DBG) Log.v(TAG, "unloading detail texture " + n);
                    // Don't wait for GC to free native memory.
                    // Only works if textures are not shared.
                    item.detailTexture.destroy();
                    item.detailTexture = null;
                }
            }
            setCard(n, item);
            mScript.invoke_setDetailTexture(n, offx, offy, loffx, loffy, item.detailTexture);
        }
    }

    void invalidateTexture(int n, boolean eraseCurrent)
    {
        if (n < 0) throw new IllegalArgumentException("Index cannot be negative");

        synchronized(this) {
            ScriptField_Card.Item item = getCard(n);
            if (item == null) {
                // This card was never created, so there's nothing to invalidate.
                return;
            }
            if (eraseCurrent && item.texture != null) {
                if (DBG) Log.v(TAG, "unloading texture " + n);
                // Don't wait for GC to free native memory.
                // Only works if textures are not shared.
                item.texture.destroy();
                item.texture = null;
            }
            setCard(n, item);
            mScript.invoke_invalidateTexture(n, eraseCurrent);
        }
    }

    void invalidateDetailTexture(int n, boolean eraseCurrent)
    {
        if (n < 0) throw new IllegalArgumentException("Index cannot be negative");

        synchronized(this) {
            ScriptField_Card.Item item = getCard(n);
            if (item == null) {
                // This card was never created, so there's nothing to invalidate.
                return;
            }
            if (eraseCurrent && item.detailTexture != null) {
                if (DBG) Log.v(TAG, "unloading detail texture " + n);
                // Don't wait for GC to free native memory.
                // Only works if textures are not shared.
                item.detailTexture.destroy();
                item.detailTexture = null;
            }
            setCard(n, item);
            mScript.invoke_invalidateDetailTexture(n, eraseCurrent);
        }
    }

    public void setGeometry(int n, Mesh geometry)
    {
        if (n < 0) throw new IllegalArgumentException("Index cannot be negative");

        synchronized(this) {
            final boolean mipmap = false;
            ScriptField_Card.Item item = getOrCreateCard(n);
            if (geometry != null) {
                item.geometry = geometry;
            } else {
                if (DBG) Log.v(TAG, "unloading geometry " + n);
                if (item.geometry != null) {
                    // item.geometry.destroy();
                    item.geometry = null;
                }
            }
            setCard(n, item);
            mScript.invoke_setGeometry(n, item.geometry);
        }
    }

    public void setMatrix(int n, float[] matrix) {
        if (n < 0) throw new IllegalArgumentException("Index cannot be negative");

        synchronized(this) {
            final boolean mipmap = false;
            ScriptField_Card.Item item = getOrCreateCard(n);
            if (matrix != null) {
                item.matrix = matrixFromFloat(matrix);
            } else {
                if (DBG) Log.v(TAG, "unloading matrix " + n);
                item.matrix = null;
            }
            setCard(n, item);
            mScript.invoke_setMatrix(n, item.matrix);
        }
    }

    public void setBackgroundColor(Float4 color) {
        mScript.set_backgroundColor(color);
    }

    public void setBackgroundTexture(Bitmap bitmap) {
        Allocation texture = null;
        if (bitmap != null) {
            texture = Allocation.createFromBitmap(mRS, bitmap,
                    MIPMAP, Allocation.USAGE_GRAPHICS_TEXTURE);
        }
        mScript.set_backgroundTexture(texture);
    }

    public void setDetailLineTexture(Bitmap bitmap) {
        Allocation texture = null;
        if (bitmap != null) {
            texture = Allocation.createFromBitmap(mRS, bitmap,
                    MIPMAP, Allocation.USAGE_GRAPHICS_TEXTURE);
        }
        mScript.set_detailLineTexture(texture);
    }

    public void setDetailLoadingTexture(Bitmap bitmap) {
        Allocation texture = null;
        if (bitmap != null) {
            texture = Allocation.createFromBitmap(mRS, bitmap,
                    MIPMAP, Allocation.USAGE_GRAPHICS_TEXTURE);
        }
        mScript.set_detailLoadingTexture(texture);
    }

    public void pauseRendering() {
        // Used to update multiple states at once w/o redrawing for each.
        mRS.bindRootScript(null);
    }

    public void resumeRendering() {
        mRS.bindRootScript(mScript);
    }

    public void doLongPress() {
        mScript.invoke_doLongPress();
    }

    public void doMotion(float x, float y, long t) {
        mScript.invoke_doMotion(x, y, t);
    }

    public void doStart(float x, float y, long t) {
        mScript.invoke_doStart(x, y, t);
    }

    public void doStop(float x, float y, long t) {
        mScript.invoke_doStop(x, y, t);
    }

    public void setSlotCount(int n) {
        mScript.set_slotCount(n);
    }

    public void setRezInCardCount(float alpha) {
        mScript.set_rezInCardCount(alpha);
    }

    public void setFadeInDuration(long t) {
        mScript.set_fadeInDuration((int)t); // TODO: Remove cast when RS supports exporting longs
    }

    public void setCardCreationFadeDuration(long t) {
        mScript.set_cardCreationFadeDuration((int)t);
    }

    private Element elementForBitmap(Bitmap bitmap, Bitmap.Config defaultConfig) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = defaultConfig;
        }
        if (config == Bitmap.Config.ALPHA_8) {
            return A_8(mRS);
        } else if (config == Bitmap.Config.RGB_565) {
            return RGB_565(mRS);
        } else if (config == Bitmap.Config.ARGB_4444) {
            return RGBA_4444(mRS);
        } else if (config == Bitmap.Config.ARGB_8888) {
            return RGBA_8888(mRS);
        } else {
            throw new IllegalArgumentException("Unknown configuration");
        }
    }

    public Mesh loadGeometry(int resId) {
        if (resId == 0) {
          return null;
        }
        FileA3D model = FileA3D.createFromResource(mRS, mRes, resId);
        if (model == null) {
          return null;
        }
        FileA3D.IndexEntry entry = model.getIndexEntry(0);
        if(entry == null || entry.getEntryType() != FileA3D.EntryType.MESH) {
            return null;
        }
        return (Mesh) entry.getObject();
    }
}
