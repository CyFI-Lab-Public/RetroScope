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

import com.android.ex.carousel.CarouselRS.CarouselCallback;
import com.android.ex.carousel.CarouselView.DetailAlignment;

import android.graphics.Bitmap;
import android.renderscript.Float4;
import android.renderscript.Mesh;
import android.renderscript.RenderScriptGL;
import android.util.Log;

/**
 * <p>
 * This class represents the basic building block for using a 3D Carousel. The Carousel is
 * basically a scene of cards and slots.  The spacing between cards is dictated by the number
 * of slots and the radius. The number of visible cards dictates how far the Carousel can be moved.
 * If the number of cards exceeds the number of slots, then the Carousel will continue to go
 * around until the last card can be seen.
 */
public class CarouselController {
    private final int DEFAULT_SLOT_COUNT = 10;
    private final float DEFAULT_RADIUS = 20.0f;
    private final int DEFAULT_VISIBLE_DETAIL_COUNT = 3;
    private final int DEFAULT_PREFETCH_CARD_COUNT = 2;
    private final int DEFAULT_ROW_COUNT = 1;
    private final float DEFAULT_OVERSCROLL_SLOTS = 1.0f;
    private final float DEFAULT_ROW_SPACING = 0.0f;
    private final float DEFAULT_SWAY_SENSITIVITY = 0.0f;
    private final float DEFAULT_FRICTION_COEFFICIENT = 10.0f;
    private final float DEFAULT_DRAG_FACTOR = 0.25f;
    private final int DEFAULT_DETAIL_ALIGNMENT =
            DetailAlignment.VIEW_TOP | DetailAlignment.LEFT;
    private CarouselRS mRenderScript;
    private RenderScriptGL mRS;
    private static final String TAG = "CarouselController";
    private static final boolean DBG = false;

    // These shadow the state of the renderer in case the surface changes so the surface
    // can be restored to its previous state.
    private Bitmap mDefaultBitmap;
    private Bitmap mLoadingBitmap;
    private Bitmap mBackgroundBitmap;
    private Bitmap mDefaultLineBitmap = Bitmap.createBitmap(
            new int[] {0x00000000, 0xffffffff, 0x00000000}, 0, 3, 3, 1, Bitmap.Config.ARGB_4444);
    private int mDefaultGeometry;
    private int mLoadingGeometry;
    private float[] mDefaultCardMatrix;
    private int mCardCount = 0;
    private int mVisibleSlots = 0;
    private int mVisibleDetails = DEFAULT_VISIBLE_DETAIL_COUNT;
    private int mPrefetchCardCount = DEFAULT_PREFETCH_CARD_COUNT;
    private int mDetailTextureAlignment = DEFAULT_DETAIL_ALIGNMENT;
    private boolean mForceBlendCardsWithZ = false;
    private boolean mDrawRuler = true;
    private float mStartAngle;
    private float mCarouselRotationAngle;
    private float mRadius = DEFAULT_RADIUS;
    private float mCardRotation = 0.0f;
    private boolean mCardsFaceTangent = false;
    private float mOverscrollSlots = DEFAULT_OVERSCROLL_SLOTS;
    private float mSwaySensitivity = DEFAULT_SWAY_SENSITIVITY;
    private float mFrictionCoefficient = DEFAULT_FRICTION_COEFFICIENT;
    private float mDragFactor = DEFAULT_DRAG_FACTOR;
    private int mSlotCount = DEFAULT_SLOT_COUNT;
    private int mRowCount = DEFAULT_ROW_COUNT;
    private float mRowSpacing = DEFAULT_ROW_SPACING;
    private float mEye[] = { 20.6829f, 2.77081f, 16.7314f };
    private float mAt[] = { 14.7255f, -3.40001f, -1.30184f };
    private float mUp[] = { 0.0f, 1.0f, 0.0f };
    private Float4 mBackgroundColor = new Float4(0.0f, 0.0f, 0.0f, 1.0f);
    private CarouselCallback mCarouselCallback;
    private float mRezInCardCount = 0.0f;
    private long mFadeInDuration = 250L;
    private long mCardCreationFadeDuration = 0L;
    private Bitmap mDetailLoadingBitmap = Bitmap.createBitmap(
            new int[] {0}, 0, 1, 1, 1, Bitmap.Config.ARGB_4444);
    private int mDragModel = CarouselRS.DRAG_MODEL_SCREEN_DELTA;
    private int mFillDirection = CarouselRS.FILL_DIRECTION_CCW;
    private boolean mFirstCardTop = false;
    private int[] mStoreConfigs;

    public CarouselController() {
        boolean useDepthBuffer = true;
    }

    public void setRS(RenderScriptGL rs, CarouselRS renderScript) {
        mRS = rs;
        mRenderScript = renderScript;
    }

    public void onSurfaceChanged() {
        setSlotCount(mSlotCount);
        setDefaultCardMatrix(mDefaultCardMatrix);
        createCards(mCardCount);
        setVisibleSlots(mVisibleSlots);
        setVisibleDetails(mVisibleDetails);
        setPrefetchCardCount(mPrefetchCardCount);
        setOverscrollSlots(mOverscrollSlots);
        setRowCount(mRowCount);
        setRowSpacing(mRowSpacing);
        setFirstCardTop(mFirstCardTop);
        setDetailTextureAlignment(mDetailTextureAlignment);
        setForceBlendCardsWithZ(mForceBlendCardsWithZ);
        setDrawRuler(mDrawRuler);
        setCallback(mCarouselCallback);
        setDefaultBitmap(mDefaultBitmap);
        setLoadingBitmap(mLoadingBitmap);
        setDefaultGeometry(mDefaultGeometry);
        setLoadingGeometry(mLoadingGeometry);
        setBackgroundColor(mBackgroundColor.x, mBackgroundColor.y, mBackgroundColor.z,
                mBackgroundColor.w);
        setBackgroundBitmap(mBackgroundBitmap);
        setDetailLineBitmap(mDefaultLineBitmap);
        setStartAngle(mStartAngle);
        setCarouselRotationAngle(mCarouselRotationAngle);
        setRadius(mRadius);
        setCardRotation(mCardRotation);
        setCardsFaceTangent(mCardsFaceTangent);
        setSwaySensitivity(mSwaySensitivity);
        setFrictionCoefficient(mFrictionCoefficient);
        setDragFactor(mDragFactor);
        setDragModel(mDragModel);
        setFillDirection(mFillDirection);
        setLookAt(mEye, mAt, mUp);
        setRezInCardCount(mRezInCardCount);
        setFadeInDuration(mFadeInDuration);
        setCardCreationFadeDuration(mCardCreationFadeDuration);
        setDetailLoadingBitmap(mDetailLoadingBitmap);
        setStoreConfigs(mStoreConfigs);
    }

    /**
     * Loads geometry from a resource id.
     *
     * @param resId
     * @return the loaded mesh or null if it cannot be loaded
     */
    public Mesh loadGeometry(int resId) {
        if (mRenderScript != null) {
          return mRenderScript.loadGeometry(resId);
        }
        return null;
    }

    /**
     * Set the geometry to show for a given slot.
     * @param n The card to set the geometry for
     * @param mesh The geometry for that item
     * @see {@link #setDefaultGeometry}
     */
    public void setGeometryForItem(int n, Mesh mesh) {
        if (mRenderScript != null) {
            mRenderScript.setGeometry(n, mesh);
        }
    }

    /**
     * Load A3D file from resource. If resId == 0, will clear geometry for this item.
     * @param n The card to set the geometry for
     * @param resId The resource ID for the geometry for that item
     * @see {@link #setDefaultGeometry}
     */
    public void setGeometryForItem(int n, int resId) {
        if (mRenderScript != null) {
            Mesh mesh = mRenderScript.loadGeometry(resId);
            mRenderScript.setGeometry(n, mesh);
        }
    }

    /**
     * Set the matrix for the specified card
     * @param n The card to set the matrix for
     * @param matrix The matrix to use
     * @see {@link #setDefaultGeometry}
     */
    public void setMatrixForItem(int n, float[] matrix) {
        if (mRenderScript != null) {
            mRenderScript.setMatrix(n, matrix);
        }
    }

    /**
     * Set the number of slots around the Carousel. Basically equivalent to the poles horses
     * might attach to on a real Carousel.
     *
     * @param n the number of slots
     */
    public void setSlotCount(int n) {
        mSlotCount = n;
        if (mRenderScript != null) {
            mRenderScript.setSlotCount(n);
        }
    }

    /**
     * Sets the number of visible slots around the Carousel.  This is primarily used as a cheap
     * form of clipping. The Carousel will never show more than this many cards.
     * @param n the number of visible slots
     */
    public void setVisibleSlots(int n) {
        mVisibleSlots = n;
        if (mRenderScript != null) {
            mRenderScript.setVisibleSlots(n);
        }
    }

    /**
     * Set the number of detail textures that can be visible at one time.
     *
     * @param n the number of slots
     */
    public void setVisibleDetails(int n) {
        mVisibleDetails = n;
        if (mRenderScript != null) {
            mRenderScript.setVisibleDetails(n);
        }
    }

    /**
     * Set the number of cards to pre-load that are outside of the visible region, as determined by
     * setVisibleSlots(). This number gets added to the number of visible slots and used to
     * determine when resources for cards should be loaded. This number should be small (n <= 4)
     * for systems with limited texture memory or views that show more than half dozen cards in the
     * view.
     *
     * @param n the number of cards; should be even, so the count is the same on each side
     */
    public void setPrefetchCardCount(int n) {
        mPrefetchCardCount = n;
        if (mRenderScript != null) {
            mRenderScript.setPrefetchCardCount(n);
        }
    }

    /**
     * Sets the number of rows of cards to show in each slot.
     */
    public void setRowCount(int n) {
        mRowCount = n;
        if (mRenderScript != null) {
            mRenderScript.setRowCount(n);
        }
    }

    /**
     * Sets the spacing between each row of cards when rowCount > 1.
     */
    public void setRowSpacing(float s) {
        mRowSpacing = s;
        if (mRenderScript != null) {
            mRenderScript.setRowSpacing(s);
        }
    }

     /**
     * Sets the position of the first card when rowCount > 1 .
     */
    public void setFirstCardTop(boolean f) {
        mFirstCardTop = f;
        if (mRenderScript != null) {
            mRenderScript.setFirstCardTop(f);
        }
    }

    /**
     * Sets the amount of allowed overscroll (in slots)
     */
    public void setOverscrollSlots(float slots) {
        mOverscrollSlots = slots;
        if (mRenderScript != null) {
            mRenderScript.setOverscrollSlots(slots);
        }
    }

    /**
     * Sets how detail textures are aligned with respect to the card.
     *
     * @param alignment a bitmask of DetailAlignment flags.
     */
    public void setDetailTextureAlignment(int alignment) {
        int xBits = alignment & DetailAlignment.HORIZONTAL_ALIGNMENT_MASK;
        if (xBits == 0 || ((xBits & (xBits - 1)) != 0)) {
            throw new IllegalArgumentException(
                    "Must specify exactly one horizontal alignment flag");
        }
        int yBits = alignment & DetailAlignment.VERTICAL_ALIGNMENT_MASK;
        if (yBits == 0 || ((yBits & (yBits - 1)) != 0)) {
            throw new IllegalArgumentException(
                    "Must specify exactly one vertical alignment flag");
        }

        mDetailTextureAlignment = alignment;
        if (mRenderScript != null) {
            mRenderScript.setDetailTextureAlignment(alignment);
        }
    }

    /**
     * Set whether depth is enabled while blending. Generally, this is discouraged because
     * it causes bad artifacts. Careful attention to geometry and alpha transparency of
     * textures can mitigate much of this. Geometry for an individual item must be drawn
     * back-to-front, for example.
     *
     * @param enabled True to enable depth while blending, and false to disable it.
     */
    public void setForceBlendCardsWithZ(boolean enabled) {
        mForceBlendCardsWithZ = enabled;
        if (mRenderScript != null) {
            mRenderScript.setForceBlendCardsWithZ(enabled);
        }
    }

    /**
     * Set whether to draw a ruler from the card to the detail texture
     *
     * @param drawRuler True to draw a ruler, false to draw nothing where the ruler would go.
     */
    public void setDrawRuler(boolean drawRuler) {
        mDrawRuler = drawRuler;
        if (mRenderScript != null) {
            mRenderScript.setDrawRuler(drawRuler);
        }
    }

    /**
     * This dictates how many cards are in the deck.  If the number of cards is greater than the
     * number of slots, then the Carousel goes around n / slot_count times.
     *
     * Can be called again to increase or decrease the number of cards.
     *
     * @param n the number of cards to create.
     */
    public void createCards(int n) {
        mCardCount = n;
        if (mRenderScript != null) {
            mRenderScript.createCards(n);
        }
    }

    public int getCardCount() {
        return mCardCount;
    }

    /**
     * This sets the texture on card n.  It should only be called in response to
     * {@link CarouselCallback#onRequestTexture(int)}.  Since there's no guarantee
     * that a given texture is still on the screen, replacing this texture should be done
     * by first setting it to null and then waiting for the next
     * {@link CarouselCallback#onRequestTexture(int)} to swap it with the new one.
     *
     * @param n the card given by {@link CarouselCallback#onRequestTexture(int)}
     * @param bitmap the bitmap image to show
     */
    public void setTextureForItem(int n, Bitmap bitmap) {
        // Also check against mRS, to handle the case where the result is being delivered by a
        // background thread but the sender no longer exists.
        if (mRenderScript != null && mRS != null) {
            if (DBG) Log.v(TAG, "setTextureForItem(" + n + ")");
            mRenderScript.setTexture(n, bitmap);
            if (DBG) Log.v(TAG, "done");
        }
    }

    /**
     * This sets the detail texture that floats above card n. It should only be called in response
     * to {@link CarouselCallback#onRequestDetailTexture(int)}.  Since there's no guarantee
     * that a given texture is still on the screen, replacing this texture should be done
     * by first setting it to null and then waiting for the next
     * {@link CarouselCallback#onRequestDetailTexture(int)} to swap it with the new one.
     *
     * @param n the card to set detail texture for
     * @param offx an optional offset to apply to the texture (in pixels) from top of detail line
     * @param offy an optional offset to apply to the texture (in pixels) from top of detail line
     * @param loffx an optional offset to apply to the line (in pixels) from left edge of card
     * @param loffy an optional offset to apply to the line (in pixels) from top of screen
     * @param bitmap the bitmap to show as the detail
     */
    public void setDetailTextureForItem(int n, float offx, float offy, float loffx, float loffy,
            Bitmap bitmap) {
        if (mRenderScript != null && mRS != null) {
            if (DBG) Log.v(TAG, "setDetailTextureForItem(" + n + ")");
            mRenderScript.setDetailTexture(n, offx, offy, loffx, loffy, bitmap);
            if (DBG) Log.v(TAG, "done");
        }
    }

    /**
     * Sets the specified texture as invalid. If {@code eraseCurrent} is true,
     * the texture will be immediately cleared from view and an invalidate
     * handler will be called. If {@code eraseCurrent} is false, a replacement
     * texture will be requested, and the old texture will be left in place in
     * the meantime.
     *
     * @param n the card to invalidate the detail texture for
     * @param eraseCurrent whether to erase the current texture
     */
    public void invalidateTexture(int n, boolean eraseCurrent) {
        if (mRenderScript != null && mRS != null) {
            if (DBG) Log.v(TAG, "invalidateTexture(" + n + ", " + eraseCurrent + ")");
            mRenderScript.invalidateTexture(n, eraseCurrent);
            if (DBG) Log.v(TAG, "done");
        }
    }

    /**
     * Sets the specified detail texture as invalid. If eraseCurrent is true, the texture will be
     * immediately cleared from view and an invalidate handler will be called. If eraseCurrent is
     * false, a replacement texture will be requested, and the old texture will be left in place
     * in the meantime.
     * @param n the card to invalidate the detail texture for
     * @param eraseCurrent whether to erase the current texture
     */
    public void invalidateDetailTexture(int n, boolean eraseCurrent) {
        if (mRenderScript != null && mRS != null) {
            if (DBG) Log.v(TAG, "invalidateDetailTexture(" + n + ", " + eraseCurrent + ")");
            mRenderScript.invalidateDetailTexture(n, eraseCurrent);
            if (DBG) Log.v(TAG, "done");
        }
    }

    /**
     * Sets the bitmap to show on a card when the card draws the very first time.
     * Generally, this bitmap will only be seen during the first few frames of startup
     * or when the number of cards are changed.  It can be ignored in most cases,
     * as the cards will generally only be in the loading or loaded state.
     *
     * @param bitmap
     */
    public void setDefaultBitmap(Bitmap bitmap) {
        mDefaultBitmap = bitmap;
        if (mRenderScript != null) {
            mRenderScript.setDefaultBitmap(bitmap);
        }
    }

    /**
     * Sets the bitmap to show on the card while the texture is loading. It is set to this
     * value just before {@link CarouselCallback#onRequestTexture(int)} is called and changed
     * when {@link CarouselView#setTextureForItem(int, Bitmap)} is called. It is shared by all
     * cards.
     *
     * @param bitmap
     */
    public void setLoadingBitmap(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
        if (mRenderScript != null) {
            mRenderScript.setLoadingBitmap(bitmap);
        }
    }

    /**
     * Sets background to specified color.  If a background texture is specified with
     * {@link CarouselView#setBackgroundBitmap(Bitmap)}, then this call has no effect.
     *
     * @param red the amount of red
     * @param green the amount of green
     * @param blue the amount of blue
     * @param alpha the amount of alpha
     */
    public void setBackgroundColor(float red, float green, float blue, float alpha) {
        mBackgroundColor = new Float4(red, green, blue, alpha);
        if (mRenderScript != null) {
            mRenderScript.setBackgroundColor(mBackgroundColor);
        }
    }

    /**
     * Can be used to optionally set the background to a bitmap. When set to something other than
     * null, this overrides {@link CarouselController#setBackgroundColor(Float4)}.
     *
     * @param bitmap
     */
    public void setBackgroundBitmap(Bitmap bitmap) {
        mBackgroundBitmap = bitmap;
        if (mRenderScript != null) {
            mRenderScript.setBackgroundTexture(bitmap);
        }
    }

    /**
     * Can be used to optionally set a "loading" detail bitmap. Typically, this is just a black
     * texture with alpha = 0 to allow details to slowly fade in.
     *
     * @param bitmap
     */
    public void setDetailLoadingBitmap(Bitmap bitmap) {
        mDetailLoadingBitmap = bitmap;
        if (mRenderScript != null) {
            mRenderScript.setDetailLoadingTexture(bitmap);
        }
    }

    /**
     * This texture is used to draw a line from the card alongside the texture detail. The line
     * will be as wide as the texture. It can be used to give the line glow effects as well as
     * allowing other blending effects. It is typically one dimensional, e.g. 3x1.
     *
     * @param bitmap
     */
    public void setDetailLineBitmap(Bitmap bitmap) {
        mDefaultLineBitmap = bitmap;
        if (mRenderScript != null) {
            mRenderScript.setDetailLineTexture(bitmap);
        }
    }

    /**
     * This geometry will be shown when no geometry has been loaded for a given slot. If not set,
     * a quad will be drawn in its place. It is shared for all cards. If something other than
     * simple planar geometry is used, consider enabling depth test with
     * {@link CarouselController#setForceBlendCardsWithZ(boolean)}
     *
     * @param mesh
     */
    public void setDefaultGeometry(int resId) {
        mDefaultGeometry = resId;
        if (mRenderScript != null) {
            Mesh mesh = mRenderScript.loadGeometry(resId);
            mRenderScript.setDefaultGeometry(mesh);
        }
    }

    /**
     * Sets the matrix used to transform card geometries.  By default, this
     * is the identity matrix, but you can specify a different matrix if you
     * want to scale, translate and / or rotate the card before drawing.
     *
     * @param matrix array of 9 or 16 floats representing a 3x3 or 4x4 matrix,
     * or null as a shortcut for an identity matrix.
     */
    public void setDefaultCardMatrix(float[] matrix) {
        mDefaultCardMatrix = matrix;
        if (mRenderScript != null) {
           mRenderScript.setDefaultCardMatrix(matrix);
        }
    }

    /**
     * This is an intermediate version of the object to show while geometry is loading. If not set,
     * a quad will be drawn in its place.  It is shared for all cards. If something other than
     * simple planar geometry is used, consider enabling depth test with
     * {@link CarouselView#setForceBlendCardsWithZ(boolean)}
     *
     * @param resId
     */
    public void setLoadingGeometry(int resId) {
        mLoadingGeometry = resId;
        if (mRenderScript != null) {
            Mesh mesh = mRenderScript.loadGeometry(resId);
            mRenderScript.setLoadingGeometry(mesh);
        }
    }

    /**
     * Sets the callback for receiving events from RenderScript.
     *
     * @param callback
     */
    public void setCallback(CarouselCallback callback)
    {
        mCarouselCallback = callback;
        if (mRenderScript != null) {
            mRenderScript.setCallback(callback);
        }
    }

    /**
     * Gets the callback for receiving events from Renderscript.
     */
    public CarouselCallback getCallback() {
        return mCarouselCallback;
    }

    /**
     * Sets the startAngle for the Carousel. The start angle is the first position of the first
     * slot draw.  Cards will be drawn from this angle in a counter-clockwise manner around the
     * Carousel.
     *
     * @param angle the angle, in radians.
     */
    public void setStartAngle(float angle)
    {
        mStartAngle = angle;
        if (mRenderScript != null) {
            mRenderScript.setStartAngle(angle);
        }
    }

    /**
     * Set the current carousel rotation angle, in card units.
     * This is measured in card positions, not in radians or degrees.
     *
     * A value of 0.0 means that card 0 is in the home position.
     * A value of 1.0 means that card 1 is in the home position, and so on.
     * The maximum value will be somewhat less than the total number of cards.
     *
     * @param angle
     */
    public void setCarouselRotationAngle(float angle) {
        mCarouselRotationAngle = angle;
        if (mRenderScript != null) {
            mRenderScript.setCarouselRotationAngle(angle);
        }
    }

    /**
     * Triggers a rotation of the carousel. All angles are in card units, see:
     * {@link CarouselController#setCarouselRotationAngle(float)}) for more details.
     *
     * @param endAngle the card unit to which the carousel should rotate to
     * @param milliseconds the length of the animation
     * @param interpolationMode three modes are currently supported :
     * {@link CarouselView.InterpolationMode#LINEAR}
     * {@link CarouselView.InterpolationMode#DECELERATE_QUADRATIC}
     * {@link CarouselView.InterpolationMode#ACCELERATE_DECELERATE_CUBIC}
     * @param maxAnimatedArc the maximum angular distance over which the transition will be
     * animated.
     * If the current position is further away, it is set at maxAnimatedArc from endAngle.
     * This parameter is ignored when <= 0.
     */
    public void setCarouselRotationAngle(float endAngle, int milliseconds, int interpolationMode,
            float maxAnimatedArc) {
        if (mRenderScript != null) {
            mRenderScript.setCarouselRotationAngle(endAngle, milliseconds,
                    interpolationMode, maxAnimatedArc);
        }
    }

    public void setRadius(float radius) {
        mRadius = radius;
        if (mRenderScript != null) {
            mRenderScript.setRadius(radius);
        }
    }

    /**
     * Sets the current model for dragging. There are currently four drag models:
     * {@link CarouselView#DRAG_MODEL_SCREEN_DELTA}
     * {@link CarouselView#DRAG_MODEL_PLANE}
     * {@link CarouselView#DRAG_MODEL_CYLINDER_INSIDE}
     * {@link CarouselView#DRAG_MODEL_CYLINDER_OUTSIDE}
     *
     * @param model
     */
    public void setDragModel(int model) {
        mDragModel  = model;
        if (mRenderScript != null) {
            mRenderScript.setDragModel(model);
        }
    }

    /** Sets the direction to fill in cards around the carousel.
     *
     * @param direction Either {@link CarouselRS#FILL_DIRECTION_CCW} or
     * {@link CarouselRS#FILL_DIRECTION_CW}.
     */
    public void setFillDirection(int direction) {
        mFillDirection = direction;
        if (mRenderScript != null) {
            mRenderScript.setFillDirection(direction);
        }
    }

    public void setCardRotation(float cardRotation) {
        mCardRotation = cardRotation;
        if (mRenderScript != null) {
            mRenderScript.setCardRotation(cardRotation);
        }
    }

    public void setCardsFaceTangent(boolean faceTangent) {
        mCardsFaceTangent = faceTangent;
        if (mRenderScript != null) {
            mRenderScript.setCardsFaceTangent(faceTangent);
        }
    }

    public void setSwaySensitivity(float swaySensitivity) {
        mSwaySensitivity = swaySensitivity;
        if (mRenderScript != null) {
            mRenderScript.setSwaySensitivity(swaySensitivity);
        }
    }

    public void setFrictionCoefficient(float frictionCoefficient) {
        mFrictionCoefficient = frictionCoefficient;
        if (mRenderScript != null) {
            mRenderScript.setFrictionCoefficient(frictionCoefficient);
        }
    }

    public void setDragFactor(float dragFactor) {
        mDragFactor = dragFactor;
        if (mRenderScript != null) {
            mRenderScript.setDragFactor(dragFactor);
        }
    }

    public void setLookAt(float[] eye, float[] at, float[] up) {
        mEye = eye;
        mAt = at;
        mUp = up;
        if (mRenderScript != null) {
            mRenderScript.setLookAt(eye, at, up);
        }
    }

    /**
     * This sets the number of cards in the distance that will be shown "rezzing in".
     * These alpha values will be faded in from the background to the foreground over
     * 'n' cards.  A floating point value is used to allow subtly changing the rezzing in
     * position.
     *
     * @param n the number of cards to rez in.
     */
    public void setRezInCardCount(float n) {
        mRezInCardCount = n;
        if (mRenderScript != null) {
            mRenderScript.setRezInCardCount(n);
        }
    }

    /**
     * This sets the duration (in ms) that a card takes to fade in when loaded via a call
     * to {@link CarouselView#setTextureForItem(int, Bitmap)}. The timer starts the
     * moment {@link CarouselView#setTextureForItem(int, Bitmap)} is called and continues
     * until all of the cards have faded in.  Note: using large values will extend the
     * animation until all cards have faded in.
     *
     * @param t The time, in milliseconds
     */
    public void setFadeInDuration(long t) {
        mFadeInDuration = t;
        if (mRenderScript != null) {
            mRenderScript.setFadeInDuration(t);
        }
    }

    /**
     * This sets the duration (in ms) that a card takes to fade in when it is initially created,
     * such as when it is added or when the application starts. The timer starts at the moment
     * when the card is first created. Replacing a card's contents does not affect the timer.
     * @param t The time, in milliseconds
     */
    public void setCardCreationFadeDuration(long t) {
        mCardCreationFadeDuration = t;
        if (mRenderScript != null) {
            mRenderScript.setCardCreationFadeDuration(t);
        }
    }

    /**
     * Tells the carousel that a touch event has started at the designated location.
     * @param x The number of pixels from the left edge that the event occurred
     * @param y The number of pixels from the top edge that the event occurred
     * @param t The time stamp of the event
     */
    public void onTouchStarted(float x, float y, long t) {
        mRenderScript.doStart(x, y, t);
    }

    /**
     * Tells the carousel that a touch event has moved to the designated location.
     * @param x The number of pixels from the left edge that the event occurred
     * @param y The number of pixels from the top edge that the event occurred
     * @param t The time stamp of the event
     */
    public void onTouchMoved(float x, float y, long t) {
        mRenderScript.doMotion(x, y, t);
    }

    /**
     * Tells the carousel that the user has long-pressed.
     */
    public void onLongPress() {
        mRenderScript.doLongPress();
    }

    /**
     * Tells the carousel that a touch event has stopped at the designated location.
     * @param x The number of pixels from the left edge that the event occurred
     * @param y The number of pixels from the top edge that the event occurred
     * @param t The time stamp of the event
     */
    public void onTouchStopped(float x, float y, long t) {
        mRenderScript.doStop(x, y, t);
    }

    /**
     * Whether to use alpha when drawing a primitive: on for translucent, off for opaque.
     */
    public static final int STORE_CONFIG_ALPHA = 1;

    /**
     * Whether to read from the depth buffer when rendering. Determines with glDepthFunc()
     * is given GL_LESS or GL_ALWAYS. On for GL_LESS, off for GL_ALWAYS.
     */
    public static final int STORE_CONFIG_DEPTH_READS = 2;

    /**
     * Whether to write to the depth buffer when rendering. Passed to glDepthMask().
     */
    public static final int STORE_CONFIG_DEPTH_WRITES = 4;

    /**
     * Set the StoreConfig parameters that will be used for each mesh primitive.
     *
     * Each integer in the array is a bitfield composed of
     * {@link CarouselController#STORE_CONFIG_ALPHA},
     * {@link CarouselController#STORE_CONFIG_DEPTH_READS}, and
     * {@link CarouselController#STORE_CONFIG_DEPTH_WRITES}.
     *
     * These parameters MUST correspond to primitives in geometry previously set in
     * {@link CarouselController#setDefaultGeometry(int)} or
     * {@link CarouselController#setLoadingGeometry(int)} or
     * {@link CarouselController#setGeometryForItem(int,Mesh)}.
     *
     * @param configs An array, each element of which corresponds to an ordered mesh primitive
     */
    public void setStoreConfigs(int configs[]) {
        mStoreConfigs = configs;
        if (mRenderScript != null) {
            mRenderScript.setStoreConfigs(configs);
        }
    }
}
