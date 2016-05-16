/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.gallery3d.filtershow.imageshow;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterMirrorRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.ImageFilter;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;
import com.android.gallery3d.filtershow.tools.SaveImage;

import java.io.File;
import java.util.ArrayList;

public class ImageShow extends View implements OnGestureListener,
        ScaleGestureDetector.OnScaleGestureListener,
        OnDoubleTapListener {

    private static final String LOGTAG = "ImageShow";
    private static final boolean ENABLE_ZOOMED_COMPARISON = false;

    protected Paint mPaint = new Paint();
    protected int mTextSize;
    protected int mTextPadding;

    protected int mBackgroundColor;

    private GestureDetector mGestureDetector = null;
    private ScaleGestureDetector mScaleGestureDetector = null;

    protected Rect mImageBounds = new Rect();
    private boolean mOriginalDisabled = false;
    private boolean mTouchShowOriginal = false;
    private long mTouchShowOriginalDate = 0;
    private final long mTouchShowOriginalDelayMin = 200; // 200ms
    private int mShowOriginalDirection = 0;
    private static int UNVEIL_HORIZONTAL = 1;
    private static int UNVEIL_VERTICAL = 2;

    private NinePatchDrawable mShadow = null;
    private Rect mShadowBounds = new Rect();
    private int mShadowMargin = 15; // not scaled, fixed in the asset
    private boolean mShadowDrawn = false;

    private Point mTouchDown = new Point();
    private Point mTouch = new Point();
    private boolean mFinishedScalingOperation = false;

    private int mOriginalTextMargin;
    private int mOriginalTextSize;
    private String mOriginalText;
    private boolean mZoomIn = false;
    Point mOriginalTranslation = new Point();
    float mOriginalScale;
    float mStartFocusX, mStartFocusY;

    private EdgeEffectCompat mEdgeEffect = null;
    private static final int EDGE_LEFT = 1;
    private static final int EDGE_TOP = 2;
    private static final int EDGE_RIGHT = 3;
    private static final int EDGE_BOTTOM = 4;
    private int mCurrentEdgeEffect = 0;
    private int mEdgeSize = 100;

    private static final int mAnimationSnapDelay = 200;
    private static final int mAnimationZoomDelay = 400;
    private ValueAnimator mAnimatorScale = null;
    private ValueAnimator mAnimatorTranslateX = null;
    private ValueAnimator mAnimatorTranslateY = null;

    private enum InteractionMode {
        NONE,
        SCALE,
        MOVE
    }
    InteractionMode mInteractionMode = InteractionMode.NONE;

    private static Bitmap sMask;
    private Paint mMaskPaint = new Paint();
    private Matrix mShaderMatrix = new Matrix();
    private boolean mDidStartAnimation = false;

    private static Bitmap convertToAlphaMask(Bitmap b) {
        Bitmap a = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(a);
        c.drawBitmap(b, 0.0f, 0.0f, null);
        return a;
    }

    private static Shader createShader(Bitmap b) {
        return new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    private FilterShowActivity mActivity = null;

    public FilterShowActivity getActivity() {
        return mActivity;
    }

    public boolean hasModifications() {
        return MasterImage.getImage().hasModifications();
    }

    public void resetParameter() {
        // TODO: implement reset
    }

    public void onNewValue(int parameter) {
        invalidate();
    }

    public ImageShow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupImageShow(context);
    }

    public ImageShow(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupImageShow(context);

    }

    public ImageShow(Context context) {
        super(context);
        setupImageShow(context);
    }

    private void setupImageShow(Context context) {
        Resources res = context.getResources();
        mTextSize = res.getDimensionPixelSize(R.dimen.photoeditor_text_size);
        mTextPadding = res.getDimensionPixelSize(R.dimen.photoeditor_text_padding);
        mOriginalTextMargin = res.getDimensionPixelSize(R.dimen.photoeditor_original_text_margin);
        mOriginalTextSize = res.getDimensionPixelSize(R.dimen.photoeditor_original_text_size);
        mBackgroundColor = res.getColor(R.color.background_screen);
        mOriginalText = res.getString(R.string.original_picture_text);
        mShadow = (NinePatchDrawable) res.getDrawable(R.drawable.geometry_shadow);
        setupGestureDetector(context);
        mActivity = (FilterShowActivity) context;
        if (sMask == null) {
            Bitmap mask = BitmapFactory.decodeResource(res, R.drawable.spot_mask);
            sMask = convertToAlphaMask(mask);
        }
        mEdgeEffect = new EdgeEffectCompat(context);
        mEdgeSize = res.getDimensionPixelSize(R.dimen.edge_glow_size);
    }

    public void attach() {
        MasterImage.getImage().addObserver(this);
        bindAsImageLoadListener();
        MasterImage.getImage().resetGeometryImages(false);
    }

    public void detach() {
        MasterImage.getImage().removeObserver(this);
        mMaskPaint.reset();
    }

    public void setupGestureDetector(Context context) {
        mGestureDetector = new GestureDetector(context, this);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(parentWidth, parentHeight);
    }

    public ImageFilter getCurrentFilter() {
        return MasterImage.getImage().getCurrentFilter();
    }

    /* consider moving the following 2 methods into a subclass */
    /**
     * This function calculates a Image to Screen Transformation matrix
     *
     * @param reflectRotation set true if you want the rotation encoded
     * @return Image to Screen transformation matrix
     */
    protected Matrix getImageToScreenMatrix(boolean reflectRotation) {
        MasterImage master = MasterImage.getImage();
        if (master.getOriginalBounds() == null) {
            return new Matrix();
        }
        Matrix m = GeometryMathUtils.getImageToScreenMatrix(master.getPreset().getGeometryFilters(),
                reflectRotation, master.getOriginalBounds(), getWidth(), getHeight());
        Point translate = master.getTranslation();
        float scaleFactor = master.getScaleFactor();
        m.postTranslate(translate.x, translate.y);
        m.postScale(scaleFactor, scaleFactor, getWidth() / 2.0f, getHeight() / 2.0f);
        return m;
    }

    /**
     * This function calculates a to Screen Image Transformation matrix
     *
     * @param reflectRotation set true if you want the rotation encoded
     * @return Screen to Image transformation matrix
     */
    protected Matrix getScreenToImageMatrix(boolean reflectRotation) {
        Matrix m = getImageToScreenMatrix(reflectRotation);
        Matrix invert = new Matrix();
        m.invert(invert);
        return invert;
    }

    public ImagePreset getImagePreset() {
        return MasterImage.getImage().getPreset();
    }

    @Override
    public void onDraw(Canvas canvas) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setFilterBitmap(true);
        MasterImage.getImage().setImageShowSize(
                getWidth() - 2*mShadowMargin,
                getHeight() - 2*mShadowMargin);

        MasterImage img = MasterImage.getImage();
        // Hide the loading indicator as needed
        if (mActivity.isLoadingVisible() && getFilteredImage() != null) {
            if ((img.getLoadedPreset() == null)
                    || (img.getLoadedPreset() != null
                    && img.getLoadedPreset().equals(img.getCurrentPreset()))) {
                mActivity.stopLoadingIndicator();
            } else if (img.getLoadedPreset() != null) {
                return;
            }
            mActivity.stopLoadingIndicator();
        }

        canvas.save();

        mShadowDrawn = false;

        Bitmap highresPreview = MasterImage.getImage().getHighresImage();
        Bitmap fullHighres = MasterImage.getImage().getPartialImage();

        boolean isDoingNewLookAnimation = MasterImage.getImage().onGoingNewLookAnimation();

        if (highresPreview == null || isDoingNewLookAnimation) {
            drawImageAndAnimate(canvas, getFilteredImage());
        } else {
            drawImageAndAnimate(canvas, highresPreview);
        }

        drawHighresImage(canvas, fullHighres);
        drawCompareImage(canvas, getGeometryOnlyImage());

        canvas.restore();

        if (!mEdgeEffect.isFinished()) {
            canvas.save();
            float dx = (getHeight() - getWidth()) / 2f;
            if (getWidth() > getHeight()) {
                dx = - (getWidth() - getHeight()) / 2f;
            }
            if (mCurrentEdgeEffect == EDGE_BOTTOM) {
                canvas.rotate(180, getWidth()/2, getHeight()/2);
            } else if (mCurrentEdgeEffect == EDGE_RIGHT) {
                canvas.rotate(90, getWidth()/2, getHeight()/2);
                canvas.translate(0, dx);
            } else if (mCurrentEdgeEffect == EDGE_LEFT) {
                canvas.rotate(270, getWidth()/2, getHeight()/2);
                canvas.translate(0, dx);
            }
            if (mCurrentEdgeEffect != 0) {
                mEdgeEffect.draw(canvas);
            }
            canvas.restore();
            invalidate();
        } else {
            mCurrentEdgeEffect = 0;
        }
    }

    private void drawHighresImage(Canvas canvas, Bitmap fullHighres) {
        Matrix originalToScreen = MasterImage.getImage().originalImageToScreen();
        if (fullHighres != null && originalToScreen != null) {
            Matrix screenToOriginal = new Matrix();
            originalToScreen.invert(screenToOriginal);
            Rect rBounds = new Rect();
            rBounds.set(MasterImage.getImage().getPartialBounds());
            if (fullHighres != null) {
                originalToScreen.preTranslate(rBounds.left, rBounds.top);
                canvas.clipRect(mImageBounds);
                canvas.drawBitmap(fullHighres, originalToScreen, mPaint);
            }
        }
    }

    public void resetImageCaches(ImageShow caller) {
        MasterImage.getImage().invalidatePreview();
    }

    public Bitmap getFiltersOnlyImage() {
        return MasterImage.getImage().getFiltersOnlyImage();
    }

    public Bitmap getGeometryOnlyImage() {
        return MasterImage.getImage().getGeometryOnlyImage();
    }

    public Bitmap getFilteredImage() {
        return MasterImage.getImage().getFilteredImage();
    }

    public void drawImageAndAnimate(Canvas canvas,
                                    Bitmap image) {
        if (image == null) {
            return;
        }
        MasterImage master = MasterImage.getImage();
        Matrix m = master.computeImageToScreen(image, 0, false);
        if (m == null) {
            return;
        }

        canvas.save();

        RectF d = new RectF(0, 0, image.getWidth(), image.getHeight());
        m.mapRect(d);
        d.roundOut(mImageBounds);

        boolean showAnimatedImage = master.onGoingNewLookAnimation();
        if (!showAnimatedImage && mDidStartAnimation) {
            // animation ended, but do we have the correct image to show?
            if (master.getPreset().equals(master.getCurrentPreset())) {
                // we do, let's stop showing the animated image
                mDidStartAnimation = false;
                MasterImage.getImage().resetAnimBitmap();
            } else {
                showAnimatedImage = true;
            }
        } else if (showAnimatedImage) {
            mDidStartAnimation = true;
        }

        if (showAnimatedImage) {
            canvas.save();

            // Animation uses the image before the change
            Bitmap previousImage = master.getPreviousImage();
            Matrix mp = master.computeImageToScreen(previousImage, 0, false);
            RectF dp = new RectF(0, 0, previousImage.getWidth(), previousImage.getHeight());
            mp.mapRect(dp);
            Rect previousBounds = new Rect();
            dp.roundOut(previousBounds);
            float centerX = dp.centerX();
            float centerY = dp.centerY();
            boolean needsToDrawImage = true;

            if (master.getCurrentLookAnimation()
                    == MasterImage.CIRCLE_ANIMATION) {
                float maskScale = MasterImage.getImage().getMaskScale();
                if (maskScale >= 0.0f) {
                    float maskW = sMask.getWidth() / 2.0f;
                    float maskH = sMask.getHeight() / 2.0f;
                    Point point = mActivity.hintTouchPoint(this);
                    float maxMaskScale = 2 * Math.max(getWidth(), getHeight())
                            / Math.min(maskW, maskH);
                    maskScale = maskScale * maxMaskScale;
                    float x = point.x - maskW * maskScale;
                    float y = point.y - maskH * maskScale;

                    // Prepare the shader
                    mShaderMatrix.reset();
                    mShaderMatrix.setScale(1.0f / maskScale, 1.0f / maskScale);
                    mShaderMatrix.preTranslate(-x + mImageBounds.left, -y + mImageBounds.top);
                    float scaleImageX = mImageBounds.width() / (float) image.getWidth();
                    float scaleImageY = mImageBounds.height() / (float) image.getHeight();
                    mShaderMatrix.preScale(scaleImageX, scaleImageY);
                    mMaskPaint.reset();
                    mMaskPaint.setShader(createShader(image));
                    mMaskPaint.getShader().setLocalMatrix(mShaderMatrix);

                    drawShadow(canvas, mImageBounds); // as needed
                    canvas.drawBitmap(previousImage, m, mPaint);
                    canvas.clipRect(mImageBounds);
                    canvas.translate(x, y);
                    canvas.scale(maskScale, maskScale);
                    canvas.drawBitmap(sMask, 0, 0, mMaskPaint);
                    needsToDrawImage = false;
                }
            } else if (master.getCurrentLookAnimation()
                    == MasterImage.ROTATE_ANIMATION) {
                Rect d1 = computeImageBounds(master.getPreviousImage().getHeight(),
                        master.getPreviousImage().getWidth());
                Rect d2 = computeImageBounds(master.getPreviousImage().getWidth(),
                        master.getPreviousImage().getHeight());
                float finalScale = d1.width() / (float) d2.height();
                finalScale = (1.0f * (1.0f - master.getAnimFraction()))
                        + (finalScale * master.getAnimFraction());
                canvas.rotate(master.getAnimRotationValue(), centerX, centerY);
                canvas.scale(finalScale, finalScale, centerX, centerY);
            } else if (master.getCurrentLookAnimation()
                    == MasterImage.MIRROR_ANIMATION) {
                if (master.getCurrentFilterRepresentation()
                        instanceof FilterMirrorRepresentation) {
                    FilterMirrorRepresentation rep =
                            (FilterMirrorRepresentation) master.getCurrentFilterRepresentation();

                    ImagePreset preset = master.getPreset();
                    ArrayList<FilterRepresentation> geometry =
                            (ArrayList<FilterRepresentation>) preset.getGeometryFilters();
                    GeometryMathUtils.GeometryHolder holder = null;
                    holder = GeometryMathUtils.unpackGeometry(geometry);

                    if (holder.rotation.value() == 90 || holder.rotation.value() == 270) {
                        if (rep.isHorizontal() && !rep.isVertical()) {
                            canvas.scale(1, master.getAnimRotationValue(), centerX, centerY);
                        } else if (rep.isVertical() && !rep.isHorizontal()) {
                            canvas.scale(1, master.getAnimRotationValue(), centerX, centerY);
                        } else if (rep.isHorizontal() && rep.isVertical()) {
                            canvas.scale(master.getAnimRotationValue(), 1, centerX, centerY);
                        } else {
                            canvas.scale(master.getAnimRotationValue(), 1, centerX, centerY);
                        }
                    } else {
                        if (rep.isHorizontal() && !rep.isVertical()) {
                            canvas.scale(master.getAnimRotationValue(), 1, centerX, centerY);
                        } else if (rep.isVertical() && !rep.isHorizontal()) {
                            canvas.scale(master.getAnimRotationValue(), 1, centerX, centerY);
                        } else  if (rep.isHorizontal() && rep.isVertical()) {
                            canvas.scale(1, master.getAnimRotationValue(), centerX, centerY);
                        } else {
                            canvas.scale(1, master.getAnimRotationValue(), centerX, centerY);
                        }
                    }
                }
            }

            if (needsToDrawImage) {
                drawShadow(canvas, previousBounds); // as needed
                canvas.drawBitmap(previousImage, mp, mPaint);
            }

            canvas.restore();
        } else {
            drawShadow(canvas, mImageBounds); // as needed
            canvas.drawBitmap(image, m, mPaint);
        }

        canvas.restore();
    }

    private Rect computeImageBounds(int imageWidth, int imageHeight) {
        float scale = GeometryMathUtils.scale(imageWidth, imageHeight,
                getWidth(), getHeight());

        float w = imageWidth * scale;
        float h = imageHeight * scale;
        float ty = (getHeight() - h) / 2.0f;
        float tx = (getWidth() - w) / 2.0f;
        return new Rect((int) tx + mShadowMargin,
                (int) ty + mShadowMargin,
                (int) (w + tx) - mShadowMargin,
                (int) (h + ty) - mShadowMargin);
    }

    private void drawShadow(Canvas canvas, Rect d) {
        if (!mShadowDrawn) {
            mShadowBounds.set(d.left - mShadowMargin, d.top - mShadowMargin,
                    d.right + mShadowMargin, d.bottom + mShadowMargin);
            mShadow.setBounds(mShadowBounds);
            mShadow.draw(canvas);
            mShadowDrawn = true;
        }
    }

    public void drawCompareImage(Canvas canvas, Bitmap image) {
        MasterImage master = MasterImage.getImage();
        boolean showsOriginal = master.showsOriginal();
        if (!showsOriginal && !mTouchShowOriginal)
            return;
        canvas.save();
        if (image != null) {
            if (mShowOriginalDirection == 0) {
                if (Math.abs(mTouch.y - mTouchDown.y) > Math.abs(mTouch.x - mTouchDown.x)) {
                    mShowOriginalDirection = UNVEIL_VERTICAL;
                } else {
                    mShowOriginalDirection = UNVEIL_HORIZONTAL;
                }
            }

            int px = 0;
            int py = 0;
            if (mShowOriginalDirection == UNVEIL_VERTICAL) {
                px = mImageBounds.width();
                py = mTouch.y - mImageBounds.top;
            } else {
                px = mTouch.x - mImageBounds.left;
                py = mImageBounds.height();
                if (showsOriginal) {
                    px = mImageBounds.width();
                }
            }

            Rect d = new Rect(mImageBounds.left, mImageBounds.top,
                    mImageBounds.left + px, mImageBounds.top + py);
            if (mShowOriginalDirection == UNVEIL_HORIZONTAL) {
                if (mTouchDown.x - mTouch.x > 0) {
                    d.set(mImageBounds.left + px, mImageBounds.top,
                            mImageBounds.right, mImageBounds.top + py);
                }
            } else {
                if (mTouchDown.y - mTouch.y > 0) {
                    d.set(mImageBounds.left, mImageBounds.top + py,
                            mImageBounds.left + px, mImageBounds.bottom);
                }
            }
            canvas.clipRect(d);
            Matrix m = master.computeImageToScreen(image, 0, false);
            canvas.drawBitmap(image, m, mPaint);
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(3);

            if (mShowOriginalDirection == UNVEIL_VERTICAL) {
                canvas.drawLine(mImageBounds.left, mTouch.y,
                        mImageBounds.right, mTouch.y, paint);
            } else {
                canvas.drawLine(mTouch.x, mImageBounds.top,
                        mTouch.x, mImageBounds.bottom, paint);
            }

            Rect bounds = new Rect();
            paint.setAntiAlias(true);
            paint.setTextSize(mOriginalTextSize);
            paint.getTextBounds(mOriginalText, 0, mOriginalText.length(), bounds);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawText(mOriginalText, mImageBounds.left + mOriginalTextMargin,
                    mImageBounds.top + bounds.height() + mOriginalTextMargin, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(1);
            paint.setColor(Color.WHITE);
            canvas.drawText(mOriginalText, mImageBounds.left + mOriginalTextMargin,
                    mImageBounds.top + bounds.height() + mOriginalTextMargin, paint);
        }
        canvas.restore();
    }

    public void bindAsImageLoadListener() {
        MasterImage.getImage().addListener(this);
    }

    public void updateImage() {
        invalidate();
    }

    public void imageLoaded() {
        updateImage();
    }

    public void saveImage(FilterShowActivity filterShowActivity, File file) {
        SaveImage.saveImage(getImagePreset(), filterShowActivity, file);
    }


    public boolean scaleInProgress() {
        return mScaleGestureDetector.isInProgress();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getAction();
        action = action & MotionEvent.ACTION_MASK;

        mGestureDetector.onTouchEvent(event);
        boolean scaleInProgress = scaleInProgress();
        mScaleGestureDetector.onTouchEvent(event);
        if (mInteractionMode == InteractionMode.SCALE) {
            return true;
        }
        if (!scaleInProgress() && scaleInProgress) {
            // If we were scaling, the scale will stop but we will
            // still issue an ACTION_UP. Let the subclasses know.
            mFinishedScalingOperation = true;
        }

        int ex = (int) event.getX();
        int ey = (int) event.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            mInteractionMode = InteractionMode.MOVE;
            mTouchDown.x = ex;
            mTouchDown.y = ey;
            mTouchShowOriginalDate = System.currentTimeMillis();
            mShowOriginalDirection = 0;
            MasterImage.getImage().setOriginalTranslation(MasterImage.getImage().getTranslation());
        }

        if (action == MotionEvent.ACTION_MOVE && mInteractionMode == InteractionMode.MOVE) {
            mTouch.x = ex;
            mTouch.y = ey;

            float scaleFactor = MasterImage.getImage().getScaleFactor();
            if (scaleFactor > 1 && (!ENABLE_ZOOMED_COMPARISON || event.getPointerCount() == 2)) {
                float translateX = (mTouch.x - mTouchDown.x) / scaleFactor;
                float translateY = (mTouch.y - mTouchDown.y) / scaleFactor;
                Point originalTranslation = MasterImage.getImage().getOriginalTranslation();
                Point translation = MasterImage.getImage().getTranslation();
                translation.x = (int) (originalTranslation.x + translateX);
                translation.y = (int) (originalTranslation.y + translateY);
                MasterImage.getImage().setTranslation(translation);
                mTouchShowOriginal = false;
            } else if (enableComparison() && !mOriginalDisabled
                    && (System.currentTimeMillis() - mTouchShowOriginalDate
                            > mTouchShowOriginalDelayMin)
                    && event.getPointerCount() == 1) {
                mTouchShowOriginal = true;
            }
        }

        if (action == MotionEvent.ACTION_UP
                || action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_OUTSIDE) {
            mInteractionMode = InteractionMode.NONE;
            mTouchShowOriginal = false;
            mTouchDown.x = 0;
            mTouchDown.y = 0;
            mTouch.x = 0;
            mTouch.y = 0;
            if (MasterImage.getImage().getScaleFactor() <= 1) {
                MasterImage.getImage().setScaleFactor(1);
                MasterImage.getImage().resetTranslation();
            }
        }

        float scaleFactor = MasterImage.getImage().getScaleFactor();
        Point translation = MasterImage.getImage().getTranslation();
        constrainTranslation(translation, scaleFactor);
        MasterImage.getImage().setTranslation(translation);

        invalidate();
        return true;
    }

    private void startAnimTranslation(int fromX, int toX,
                                      int fromY, int toY, int delay) {
        if (fromX == toX && fromY == toY) {
            return;
        }
        if (mAnimatorTranslateX != null) {
            mAnimatorTranslateX.cancel();
        }
        if (mAnimatorTranslateY != null) {
            mAnimatorTranslateY.cancel();
        }
        mAnimatorTranslateX = ValueAnimator.ofInt(fromX, toX);
        mAnimatorTranslateY = ValueAnimator.ofInt(fromY, toY);
        mAnimatorTranslateX.setDuration(delay);
        mAnimatorTranslateY.setDuration(delay);
        mAnimatorTranslateX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point translation = MasterImage.getImage().getTranslation();
                translation.x = (Integer) animation.getAnimatedValue();
                MasterImage.getImage().setTranslation(translation);
                invalidate();
            }
        });
        mAnimatorTranslateY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point translation = MasterImage.getImage().getTranslation();
                translation.y = (Integer) animation.getAnimatedValue();
                MasterImage.getImage().setTranslation(translation);
                invalidate();
            }
        });
        mAnimatorTranslateX.start();
        mAnimatorTranslateY.start();
    }

    private void applyTranslationConstraints() {
        float scaleFactor = MasterImage.getImage().getScaleFactor();
        Point translation = MasterImage.getImage().getTranslation();
        int x = translation.x;
        int y = translation.y;
        constrainTranslation(translation, scaleFactor);

        if (x != translation.x || y != translation.y) {
            startAnimTranslation(x, translation.x,
                                 y, translation.y,
                                 mAnimationSnapDelay);
        }
    }

    protected boolean enableComparison() {
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent arg0) {
        mZoomIn = !mZoomIn;
        float scale = 1.0f;
        final float x = arg0.getX();
        final float y = arg0.getY();
        if (mZoomIn) {
            scale = MasterImage.getImage().getMaxScaleFactor();
        }
        if (scale != MasterImage.getImage().getScaleFactor()) {
            if (mAnimatorScale != null) {
                mAnimatorScale.cancel();
            }
            mAnimatorScale = ValueAnimator.ofFloat(
                    MasterImage.getImage().getScaleFactor(),
                    scale
            );
            float translateX = (getWidth() / 2 - x);
            float translateY = (getHeight() / 2 - y);
            Point translation = MasterImage.getImage().getTranslation();
            int startTranslateX = translation.x;
            int startTranslateY = translation.y;
            if (scale != 1.0f) {
                translation.x = (int) (mOriginalTranslation.x + translateX);
                translation.y = (int) (mOriginalTranslation.y + translateY);
            } else {
                translation.x = 0;
                translation.y = 0;
            }
            constrainTranslation(translation, scale);

            startAnimTranslation(startTranslateX, translation.x,
                                 startTranslateY, translation.y,
                                 mAnimationZoomDelay);
            mAnimatorScale.setDuration(mAnimationZoomDelay);
            mAnimatorScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    MasterImage.getImage().setScaleFactor((Float) animation.getAnimatedValue());
                    invalidate();
                }
            });
            mAnimatorScale.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    applyTranslationConstraints();
                    MasterImage.getImage().needsUpdatePartialPreview();
                    invalidate();
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            mAnimatorScale.start();
        }
        return true;
    }

    private void constrainTranslation(Point translation, float scale) {
        int currentEdgeEffect = 0;
        if (scale <= 1) {
            mCurrentEdgeEffect = 0;
            mEdgeEffect.finish();
            return;
        }

        Matrix originalToScreen = MasterImage.getImage().originalImageToScreen();
        Rect originalBounds = MasterImage.getImage().getOriginalBounds();
        RectF screenPos = new RectF(originalBounds);
        originalToScreen.mapRect(screenPos);

        boolean rightConstraint = screenPos.right < getWidth() - mShadowMargin;
        boolean leftConstraint = screenPos.left > mShadowMargin;
        boolean topConstraint = screenPos.top > mShadowMargin;
        boolean bottomConstraint = screenPos.bottom < getHeight() - mShadowMargin;

        if (screenPos.width() > getWidth()) {
            if (rightConstraint && !leftConstraint) {
                float tx = screenPos.right - translation.x * scale;
                translation.x = (int) ((getWidth() - mShadowMargin - tx) / scale);
                currentEdgeEffect = EDGE_RIGHT;
            } else if (leftConstraint && !rightConstraint) {
                float tx = screenPos.left - translation.x * scale;
                translation.x = (int) ((mShadowMargin - tx) / scale);
                currentEdgeEffect = EDGE_LEFT;
            }
        } else {
            float tx = screenPos.right - translation.x * scale;
            float dx = (getWidth() - 2 * mShadowMargin - screenPos.width()) / 2f;
            translation.x = (int) ((getWidth() - mShadowMargin - tx - dx) / scale);
        }

        if (screenPos.height() > getHeight()) {
            if (bottomConstraint && !topConstraint) {
                float ty = screenPos.bottom - translation.y * scale;
                translation.y = (int) ((getHeight() - mShadowMargin - ty) / scale);
                currentEdgeEffect = EDGE_BOTTOM;
            } else if (topConstraint && !bottomConstraint) {
                float ty = screenPos.top - translation.y * scale;
                translation.y = (int) ((mShadowMargin - ty) / scale);
                currentEdgeEffect = EDGE_TOP;
            }
        } else {
            float ty = screenPos.bottom - translation.y * scale;
            float dy = (getHeight()- 2 * mShadowMargin - screenPos.height()) / 2f;
            translation.y = (int) ((getHeight() - mShadowMargin - ty - dy) / scale);
        }

        if (mCurrentEdgeEffect != currentEdgeEffect) {
            if (mCurrentEdgeEffect == 0 || currentEdgeEffect != 0) {
                mCurrentEdgeEffect = currentEdgeEffect;
                mEdgeEffect.finish();
            }
            mEdgeEffect.setSize(getWidth(), mEdgeSize);
        }
        if (currentEdgeEffect != 0) {
            mEdgeEffect.onPull(mEdgeSize);
        }
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent startEvent, MotionEvent endEvent, float arg2, float arg3) {
        if (mActivity == null) {
            return false;
        }
        if (endEvent.getPointerCount() == 2) {
            return false;
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }

    public boolean useUtilityPanel() {
        return false;
    }

    public void openUtilityPanel(final LinearLayout accessoryViewList) {
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        MasterImage img = MasterImage.getImage();
        float scaleFactor = img.getScaleFactor();

        scaleFactor = scaleFactor * detector.getScaleFactor();
        if (scaleFactor > MasterImage.getImage().getMaxScaleFactor()) {
            scaleFactor = MasterImage.getImage().getMaxScaleFactor();
        }
        if (scaleFactor < 1.0f) {
            scaleFactor = 1.0f;
        }
        MasterImage.getImage().setScaleFactor(scaleFactor);
        scaleFactor = img.getScaleFactor();
        float focusx = detector.getFocusX();
        float focusy = detector.getFocusY();
        float translateX = (focusx - mStartFocusX) / scaleFactor;
        float translateY = (focusy - mStartFocusY) / scaleFactor;
        Point translation = MasterImage.getImage().getTranslation();
        translation.x = (int) (mOriginalTranslation.x + translateX);
        translation.y = (int) (mOriginalTranslation.y + translateY);
        MasterImage.getImage().setTranslation(translation);
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Point pos = MasterImage.getImage().getTranslation();
        mOriginalTranslation.x = pos.x;
        mOriginalTranslation.y = pos.y;
        mOriginalScale = MasterImage.getImage().getScaleFactor();
        mStartFocusX = detector.getFocusX();
        mStartFocusY = detector.getFocusY();
        mInteractionMode = InteractionMode.SCALE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        mInteractionMode = InteractionMode.NONE;
        if (MasterImage.getImage().getScaleFactor() < 1) {
            MasterImage.getImage().setScaleFactor(1);
            invalidate();
        }
    }

    public boolean didFinishScalingOperation() {
        if (mFinishedScalingOperation) {
            mFinishedScalingOperation = false;
            return true;
        }
        return false;
    }

}
