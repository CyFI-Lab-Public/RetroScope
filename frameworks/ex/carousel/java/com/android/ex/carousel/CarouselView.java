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

import android.view.View;
import com.android.ex.carousel.CarouselRS.CarouselCallback;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Float4;
import android.renderscript.Mesh;
import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScriptGL;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * <p>
 * This class represents the basic building block for using a 3D Carousel. The Carousel is
 * basically a scene of cards and slots.  The spacing between cards is dictated by the number
 * of slots and the radius. The number of visible cards dictates how far the Carousel can be moved.
 * If the number of cards exceeds the number of slots, then the Carousel will continue to go
 * around until the last card can be seen.
 */
public abstract class CarouselView extends RSSurfaceView {
    private static final boolean USE_DEPTH_BUFFER = true;
    private static final String TAG = "CarouselView";
    private CarouselRS mRenderScript;
    private RenderScriptGL mRS;
    private Context mContext;
    private boolean mTracking;

    CarouselController mController;

    // Drag relative to x coordinate of motion on screen
    public static final int DRAG_MODEL_SCREEN_DELTA = CarouselRS.DRAG_MODEL_SCREEN_DELTA;
    // Drag relative to projected point on plane of carousel
    public static final int DRAG_MODEL_PLANE = CarouselRS.DRAG_MODEL_PLANE;
    // Drag relative to projected point on inside (far point) of cylinder centered around carousel
    public static final int DRAG_MODEL_CYLINDER_INSIDE = CarouselRS.DRAG_MODEL_CYLINDER_INSIDE;
    // Drag relative to projected point on outside (near point) of cylinder centered around carousel
    public static final int DRAG_MODEL_CYLINDER_OUTSIDE = CarouselRS.DRAG_MODEL_CYLINDER_OUTSIDE;

    // Draw cards counterclockwise around the carousel
    public static final int FILL_DIRECTION_CCW = CarouselRS.FILL_DIRECTION_CCW;
    // Draw cards clockwise around the carousel
    public static final int FILL_DIRECTION_CW = CarouselRS.FILL_DIRECTION_CW;

    // Note: remember to update carousel.rs when changing the values below
    public static class InterpolationMode {
        /** y= x **/
        public static final int LINEAR = 0;
        /** The quadratic curve y= 1 - (1 - x)^2 moves quickly towards the target
         * while decelerating constantly. **/
        public static final int DECELERATE_QUADRATIC = 1;
        /** The cubic curve y= (3-2x)*x^2 gradually accelerates at the origin,
         * and decelerates near the target. **/
        public static final int ACCELERATE_DECELERATE_CUBIC = 2;
    }

    // Note: remember to update carousel.rs when changing the values below
    public static class DetailAlignment {
        /** Detail is centered vertically with respect to the card **/
        public static final int CENTER_VERTICAL = 1;
        /** Detail is aligned with the top edge of the carousel view **/
        public static final int VIEW_TOP = 1 << 1;
        /** Detail is aligned with the bottom edge of the carousel view (not yet implemented) **/
        public static final int VIEW_BOTTOM = 1 << 2;
        /** Detail is positioned above the card (not yet implemented) **/
        public static final int ABOVE = 1 << 3;
        /** Detail is positioned below the card **/
        public static final int BELOW = 1 << 4;
        /** Mask that selects those bits that control vertical alignment **/
        public static final int VERTICAL_ALIGNMENT_MASK = 0xff;

        /**
         * Detail is centered horizontally with respect to either the top or bottom
         * extent of the card, depending on whether the detail is above or below the card.
         */
        public static final int CENTER_HORIZONTAL = 1 << 8;
        /**
         * Detail is aligned with the left edge of either the top or the bottom of
         * the card, depending on whether the detail is above or below the card.
         */
        public static final int LEFT = 1 << 9;
        /**
         * Detail is aligned with the right edge of either the top or the bottom of
         * the card, depending on whether the detail is above or below the card.
         * (not yet implemented)
         */
        public static final int RIGHT = 1 << 10;
        /** Mask that selects those bits that control horizontal alignment **/
        public static final int HORIZONTAL_ALIGNMENT_MASK = 0xff00;
    }

    public static class Info {
        public Info(int _resId) { resId = _resId; }
        public int resId; // resource for renderscript resource (e.g. R.raw.carousel)
    }

    public abstract Info getRenderScriptInfo();

    public CarouselView(Context context) {
        this(context, new CarouselController());
    }

    public CarouselView(Context context, CarouselController controller) {
        this(context, null, controller);
    }

    /**
     * Constructor used when this widget is created from a layout file.
     */
    public CarouselView(Context context, AttributeSet attrs) {
        this(context, attrs, new CarouselController());
    }

    public CarouselView(Context context, AttributeSet attrs, CarouselController controller) {
        super(context, attrs);
        mContext = context;
        mController = controller;
        boolean useDepthBuffer = true;
        ensureRenderScript();
        // TODO: add parameters to layout

        setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                if (interpretLongPressEvents()) {
                    mController.onLongPress();
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void ensureRenderScript() {
        if (mRS == null) {
            RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
            if (USE_DEPTH_BUFFER) {
                sc.setDepth(16, 24);
            }
            mRS = createRenderScriptGL(sc);
        }
        if (mRenderScript == null) {
            mRenderScript = new CarouselRS(mRS, mContext.getResources(),
                    getRenderScriptInfo().resId);
            mRenderScript.resumeRendering();
        }
        mController.setRS(mRS, mRenderScript);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
        // setZOrderOnTop(true);
        mController.onSurfaceChanged();
    }

    public CarouselController getController() {
        return mController;
    }

    public void setController(CarouselController controller) {
        mController = controller;
        mController.setRS(mRS, mRenderScript);
    }

    /**
     * Do I want to interpret the long-press gesture? If so, long-presses will cancel the
     * current selection and call the appropriate callbacks. Otherwise, a long press will
     * not be handled any way other than as a continued drag.
     *
     * @return True if we interpret long-presses
     */
    public boolean interpretLongPressEvents() {
        return false;
    }

    /**
     * Loads geometry from a resource id.
     *
     * @param resId
     * @return the loaded mesh or null if it cannot be loaded
     */
    public Mesh loadGeometry(int resId) {
        return mController.loadGeometry(resId);
    }

    /**
     * Set the geometry for a given item.
     * @param n
     * @param mesh
     */
    public void setGeometryForItem(int n, Mesh mesh) {
        mController.setGeometryForItem(n, mesh);
    }

    /**
     * Set the matrix for a given item.
     * @param n
     * @param matrix the requested matrix; null to just use the default
     */
    public void setMatrixForItem(int n, float[] matrix) {
        mController.setMatrixForItem(n, matrix);
    }

    /**
     * Set the number of slots around the Carousel. Basically equivalent to the poles horses
     * might attach to on a real Carousel.
     *
     * @param n the number of slots
     */
    public void setSlotCount(int n) {
        mController.setSlotCount(n);
    }

    /**
     * Sets the number of visible slots around the Carousel.  This is primarily used as a cheap
     * form of clipping. The Carousel will never show more than this many cards.
     * @param n the number of visible slots
     */
    public void setVisibleSlots(int n) {
        mController.setVisibleSlots(n);
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
        mController.setPrefetchCardCount(n);
    }

    /**
     * Sets the number of rows of cards to show in each slot.
     */
    public void setRowCount(int n) {
        mController.setRowCount(n);
    }

    /**
     * Sets the spacing between each row of cards when rowCount > 1.
     */
    public void setRowSpacing(float s) {
        mController.setRowSpacing(s);
    }

    /**
     * Sets the position of the first card when rowCount > 1.
     */
    public void setFirstCardTop(boolean f) {
        mController.setFirstCardTop(f);
    }

    /**
     * Sets the amount of allowed overscroll (in slots)
     */
    public void setOverscrollSlots(float slots) {
        mController.setOverscrollSlots(slots);
    }

    /**
     * Set the number of detail textures that can be visible at one time.
     *
     * @param n the number of slots
     */
    public void setVisibleDetails(int n) {
        mController.setVisibleDetails(n);
    }

    /**
     * Sets how detail textures are aligned with respect to the card.
     *
     * @param alignment a bitmask of DetailAlignment flags.
     */
    public void setDetailTextureAlignment(int alignment) {
        mController.setDetailTextureAlignment(alignment);
    }

    /**
     * Set whether depth is enabled while blending. Generally, this is discouraged because
     * it causes bad artifacts. Careful attention to geometry and alpha transparency of
     * textures can mitigate much of this. For example, geometry for an item must be drawn
     * back-to-front if any edges overlap.
     *
     * @param enabled True to enable depth while blending, and false to disable it.
     */
    public void setForceBlendCardsWithZ(boolean enabled) {
        mController.setForceBlendCardsWithZ(enabled);
    }

    /**
     * Set whether to draw a ruler from the card to the detail texture
     *
     * @param drawRuler True to draw a ruler, false to draw nothing where the ruler would go.
     */
    public void setDrawRuler(boolean drawRuler) {
        mController.setDrawRuler(drawRuler);
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
        mController.createCards(n);
    }

    public int getCardCount() {
        return mController.getCardCount();
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
        mController.setTextureForItem(n, bitmap);
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
        mController.setDetailTextureForItem(n, offx, offy, loffx, loffy, bitmap);
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
        mController.setDefaultBitmap(bitmap);
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
        mController.setLoadingBitmap(bitmap);
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
        mController.setBackgroundColor(red, green, blue, alpha);
    }

    /**
     * Can be used to optionally set the background to a bitmap. When set to something other than
     * null, this overrides {@link CarouselView#setBackgroundColor(Float4)}.
     *
     * @param bitmap
     */
    public void setBackgroundBitmap(Bitmap bitmap) {
        mController.setBackgroundBitmap(bitmap);
    }

    /**
     * Can be used to optionally set a "loading" detail bitmap. Typically, this is just a black
     * texture with alpha = 0 to allow details to slowly fade in.
     *
     * @param bitmap
     */
    public void setDetailLoadingBitmap(Bitmap bitmap) {
        mController.setDetailLoadingBitmap(bitmap);
    }

    /**
     * This texture is used to draw a line from the card alongside the texture detail. The line
     * will be as wide as the texture. It can be used to give the line glow effects as well as
     * allowing other blending effects. It is typically one dimensional, e.g. 3x1.
     *
     * @param bitmap
     */
    public void setDetailLineBitmap(Bitmap bitmap) {
        mController.setDetailLineBitmap(bitmap);
    }

    /**
     * This geometry will be shown when no geometry has been loaded for a given slot. If not set,
     * a quad will be drawn in its place. It is shared for all cards. If something other than
     * simple planar geometry is used, consider enabling depth test with
     * {@link CarouselView#setForceBlendCardsWithZ(boolean)}
     *
     * @param resId
     */
    public void setDefaultGeometry(int resId) {
        mController.setDefaultGeometry(resId);
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
        mController.setDefaultCardMatrix(matrix);
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
        mController.setLoadingGeometry(resId);
    }

    /**
     * Sets the callback for receiving events from RenderScript.
     *
     * @param callback
     */
    public void setCallback(CarouselCallback callback)
    {
        mController.setCallback(callback);
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
        mController.setStartAngle(angle);
    }

    public void setRadius(float radius) {
        mController.setRadius(radius);
    }

    public void setCardRotation(float cardRotation) {
        mController.setCardRotation(cardRotation);
    }

    public void setCardsFaceTangent(boolean faceTangent) {
        mController.setCardsFaceTangent(faceTangent);
    }

    public void setSwaySensitivity(float swaySensitivity) {
        mController.setSwaySensitivity(swaySensitivity);
    }

    public void setFrictionCoefficient(float frictionCoefficient) {
        mController.setFrictionCoefficient(frictionCoefficient);
    }

    public void setDragFactor(float dragFactor) {
        mController.setDragFactor(dragFactor);
    }

    public void setDragModel(int model) {
        mController.setDragModel(model);
    }

    public void setLookAt(float[] eye, float[] at, float[] up) {
        mController.setLookAt(eye, at, up);
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
        mController.setRezInCardCount(n);
    }

    /**
     * This sets the duration (in ms) that a card takes to fade in when loaded via a call
     * to {@link CarouselView#setTextureForItem(int, Bitmap)}. The timer starts the
     * moment {@link CarouselView#setTextureForItem(int, Bitmap)} is called and continues
     * until all of the cards have faded in.  Note: using large values will extend the
     * animation until all cards have faded in.
     *
     * @param t
     */
    public void setFadeInDuration(long t) {
        mController.setFadeInDuration(t);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mRenderScript = null;
        if (mRS != null) {
            mRS = null;
            destroyRenderScriptGL();
        }
        mController.setRS(mRS, mRenderScript);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureRenderScript();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        final int action = event.getAction();

        if (mRenderScript == null) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mTracking = true;
                mController.onTouchStarted(event.getX(), event.getY(), event.getEventTime());
                break;

            case MotionEvent.ACTION_MOVE:
                if (mTracking) {
                    for (int i = 0; i < event.getHistorySize(); i++) {
                        mController.onTouchMoved(event.getHistoricalX(i), event.getHistoricalY(i),
                                event.getHistoricalEventTime(i));
                    }
                    mController.onTouchMoved(event.getX(), event.getY(), event.getEventTime());
                }
                break;

            case MotionEvent.ACTION_UP:
                mController.onTouchStopped(event.getX(), event.getY(), event.getEventTime());
                mTracking = false;
                break;
        }

        return true;
    }
}
