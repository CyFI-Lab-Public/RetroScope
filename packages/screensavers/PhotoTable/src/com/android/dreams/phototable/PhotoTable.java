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
package com.android.dreams.phototable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.service.dreams.DreamService;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

/**
 * A surface where photos sit.
 */
public class PhotoTable extends FrameLayout {
    private static final String TAG = "PhotoTable";
    private static final boolean DEBUG = false;

    class Launcher implements Runnable {
        @Override
        public void run() {
            PhotoTable.this.scheduleNext(mDropPeriod);
            PhotoTable.this.launch();
        }
    }

    class FocusReaper implements Runnable {
        @Override
        public void run() {
            PhotoTable.this.clearFocus();
        }
    }

    class SelectionReaper implements Runnable {
        @Override
        public void run() {
            PhotoTable.this.clearSelection();
        }
    }

    private static final int NEXT = 1;
    private static final int PREV = 0;
    private static Random sRNG = new Random();

    private final Launcher mLauncher;
    private final FocusReaper mFocusReaper;
    private final SelectionReaper mSelectionReaper;
    private final LinkedList<View> mOnTable;
    private final int mDropPeriod;
    private final int mFastDropPeriod;
    private final int mNowDropDelay;
    private final float mImageRatio;
    private final float mTableRatio;
    private final float mImageRotationLimit;
    private final float mThrowRotation;
    private final float mThrowSpeed;
    private final boolean mTapToExit;
    private final int mTableCapacity;
    private final int mRedealCount;
    private final int mInset;
    private final PhotoSource mPhotoSource;
    private final Resources mResources;
    private final Interpolator mThrowInterpolator;
    private final Interpolator mDropInterpolator;
    private final DragGestureDetector mDragGestureDetector;
    private final EdgeSwipeDetector mEdgeSwipeDetector;
    private final KeyboardInterpreter mKeyboardInterpreter;
    private final boolean mStoryModeEnabled;
    private final boolean mBackgroudOptimization;
    private final long mPickUpDuration;
    private final int mMaxSelectionTime;
    private final int mMaxFocusTime;
    private DreamService mDream;
    private PhotoLaunchTask mPhotoLaunchTask;
    private LoadNaturalSiblingTask mLoadOnDeckTasks[];
    private boolean mStarted;
    private boolean mIsLandscape;
    private int mLongSide;
    private int mShortSide;
    private int mWidth;
    private int mHeight;
    private View mSelection;
    private View mOnDeck[];
    private View mFocus;
    private int mHighlightColor;
    private ViewGroup mBackground;
    private ViewGroup mStageLeft;
    private View mScrim;
    private final Set<View> mWaitingToJoinBackground;

    public PhotoTable(Context context, AttributeSet as) {
        super(context, as);
        mResources = getResources();
        mInset = mResources.getDimensionPixelSize(R.dimen.photo_inset);
        mDropPeriod = mResources.getInteger(R.integer.table_drop_period);
        mFastDropPeriod = mResources.getInteger(R.integer.fast_drop);
        mNowDropDelay = mResources.getInteger(R.integer.now_drop);
        mImageRatio = mResources.getInteger(R.integer.image_ratio) / 1000000f;
        mTableRatio = mResources.getInteger(R.integer.table_ratio) / 1000000f;
        mImageRotationLimit = (float) mResources.getInteger(R.integer.max_image_rotation);
        mThrowSpeed = mResources.getDimension(R.dimen.image_throw_speed);
        mPickUpDuration = mResources.getInteger(R.integer.photo_pickup_duration);
        mThrowRotation = (float) mResources.getInteger(R.integer.image_throw_rotatioan);
        mTableCapacity = mResources.getInteger(R.integer.table_capacity);
        mRedealCount = mResources.getInteger(R.integer.redeal_count);
        mTapToExit = mResources.getBoolean(R.bool.enable_tap_to_exit);
        mStoryModeEnabled = mResources.getBoolean(R.bool.enable_story_mode);
        mBackgroudOptimization = mResources.getBoolean(R.bool.enable_background_optimization);
        mHighlightColor = mResources.getColor(R.color.highlight_color);
        mMaxSelectionTime = mResources.getInteger(R.integer.max_selection_time);
        mMaxFocusTime = mResources.getInteger(R.integer.max_focus_time);
        mThrowInterpolator = new SoftLandingInterpolator(
                mResources.getInteger(R.integer.soft_landing_time) / 1000000f,
                mResources.getInteger(R.integer.soft_landing_distance) / 1000000f);
        mDropInterpolator = new DecelerateInterpolator(
                (float) mResources.getInteger(R.integer.drop_deceleration_exponent));
        mOnTable = new LinkedList<View>();
        mPhotoSource = new PhotoSourcePlexor(getContext(),
                getContext().getSharedPreferences(PhotoTableDreamSettings.PREFS_NAME, 0));
        mWaitingToJoinBackground = new HashSet<View>();
        mLauncher = new Launcher();
        mFocusReaper = new FocusReaper();
        mSelectionReaper = new SelectionReaper();
        mDragGestureDetector = new DragGestureDetector(context, this);
        mEdgeSwipeDetector = new EdgeSwipeDetector(context, this);
        mKeyboardInterpreter = new KeyboardInterpreter(this);
        mLoadOnDeckTasks = new LoadNaturalSiblingTask[2];
        mOnDeck = new View[2];
        mStarted = false;
    }

    @Override
    public void onFinishInflate() {
        mBackground = (ViewGroup) findViewById(R.id.background);
        mStageLeft = (ViewGroup) findViewById(R.id.stageleft);
        mScrim = findViewById(R.id.scrim);
    }

    public void setDream(DreamService dream) {
        mDream = dream;
    }

    public boolean hasSelection() {
        return mSelection != null;
    }

    public View getSelection() {
        return mSelection;
    }

    public void clearSelection() {
        if (hasSelection()) {
            dropOnTable(mSelection);
            mPhotoSource.donePaging(getBitmap(mSelection));
            if (mStoryModeEnabled) {
                fadeInBackground(mSelection);
            }
            mSelection = null;
        }
        for (int slot = 0; slot < mOnDeck.length; slot++) {
            if (mOnDeck[slot] != null) {
                fadeAway(mOnDeck[slot], false);
                mOnDeck[slot] = null;
            }
            if (mLoadOnDeckTasks[slot] != null &&
                    mLoadOnDeckTasks[slot].getStatus() != AsyncTask.Status.FINISHED) {
                mLoadOnDeckTasks[slot].cancel(true);
                mLoadOnDeckTasks[slot] = null;
            }
        }
    }

    public void setSelection(View selected) {
        if (selected != null) {
            clearSelection();
            mSelection = selected;
            promoteSelection();
            if (mStoryModeEnabled) {
                fadeOutBackground(mSelection);
            }
        }
    }

    public void selectNext() {
        if (mStoryModeEnabled) {
            log("selectNext");
            if (hasSelection() && mOnDeck[NEXT] != null) {
                placeOnDeck(mSelection, PREV);
                mSelection = mOnDeck[NEXT];
                mOnDeck[NEXT] = null;
                promoteSelection();
            }
        } else {
            clearSelection();
        }
    }

    public void selectPrevious() {
        if (mStoryModeEnabled) {
            log("selectPrevious");
            if (hasSelection() && mOnDeck[PREV] != null) {
                placeOnDeck(mSelection, NEXT);
                mSelection = mOnDeck[PREV];
                mOnDeck[PREV] = null;
                promoteSelection();
            }
        } else {
            clearSelection();
        }
    }

    private void promoteSelection() {
        if (hasSelection()) {
            scheduleSelectionReaper(mMaxSelectionTime);
            mSelection.animate().cancel();
            mSelection.setAlpha(1f);
            moveToTopOfPile(mSelection);
            pickUp(mSelection);
            if (mStoryModeEnabled) {
                for (int slot = 0; slot < mOnDeck.length; slot++) {
                    if (mLoadOnDeckTasks[slot] != null &&
                            mLoadOnDeckTasks[slot].getStatus() != AsyncTask.Status.FINISHED) {
                        mLoadOnDeckTasks[slot].cancel(true);
                    }
                    if (mOnDeck[slot] == null) {
                        mLoadOnDeckTasks[slot] = new LoadNaturalSiblingTask(slot);
                        mLoadOnDeckTasks[slot].execute(mSelection);
                    }
                }
            }
        }
    }

    public boolean hasFocus() {
        return mFocus != null;
    }

    public View getFocus() {
        return mFocus;
    }

    public void clearFocus() {
        if (hasFocus()) {
            setHighlight(getFocus(), false);
        }
        mFocus = null;
    }

    public void setDefaultFocus() {
        if (mOnTable.size() > 0) {
            setFocus(mOnTable.getLast());
        }
    }

    public void setFocus(View focus) {
        assert(focus != null);
        clearFocus();
        mFocus = focus;
        moveToTopOfPile(focus);
        setHighlight(focus, true);
        scheduleFocusReaper(mMaxFocusTime);
    }

    static float lerp(float a, float b, float f) {
        return (b-a)*f + a;
    }

    static float randfrange(float a, float b) {
        return lerp(a, b, sRNG.nextFloat());
    }

    static PointF randFromCurve(float t, PointF[] v) {
        PointF p = new PointF();
        if (v.length == 4 && t >= 0f && t <= 1f) {
            float a = (float) Math.pow(1f-t, 3f);
            float b = (float) Math.pow(1f-t, 2f) * t;
            float c = (1f-t) * (float) Math.pow(t, 2f);
            float d = (float) Math.pow(t, 3f);

            p.x = a * v[0].x + 3 * b * v[1].x + 3 * c * v[2].x + d * v[3].x;
            p.y = a * v[0].y + 3 * b * v[1].y + 3 * c * v[2].y + d * v[3].y;
        }
        return p;
    }

    private static PointF randMultiDrop(int n, float i, float j, int width, int height) {
        log("randMultiDrop (%d, %f, %f, %d, %d)", n, i, j, width, height);
        final float[] cx = {0.3f, 0.3f, 0.5f, 0.7f, 0.7f};
        final float[] cy = {0.3f, 0.7f, 0.5f, 0.3f, 0.7f};
        n = Math.abs(n);
        float x = cx[n % cx.length];
        float y = cy[n % cx.length];
        PointF p = new PointF();
        p.x = x * width + 0.05f * width * i;
        p.y = y * height + 0.05f * height * j;
        log("randInCenter returning %f, %f", p.x, p.y);
        return p;
    }

    private double cross(double[] a, double[] b) {
        return a[0] * b[1] - a[1] * b[0];
    }

    private double norm(double[] a) {
        return Math.hypot(a[0], a[1]);
    }

    private double[] getCenter(View photo) {
        float width = (float) ((Integer) photo.getTag(R.id.photo_width)).intValue();
        float height = (float) ((Integer) photo.getTag(R.id.photo_height)).intValue();
        double[] center = { photo.getX() + width / 2f,
                            - (photo.getY() + height / 2f) };
        return center;
    }

    public View moveFocus(View focus, float direction) {
        return moveFocus(focus, direction, 90f);
    }

    public View moveFocus(View focus, float direction, float angle) {
        if (focus == null) {
            if (mOnTable.size() > 0) {
                setFocus(mOnTable.getLast());
            }
        } else {
            final double alpha = Math.toRadians(direction);
            final double beta = Math.toRadians(Math.min(angle, 180f) / 2f);
            final double[] left = { Math.sin(alpha - beta),
                                    Math.cos(alpha - beta) };
            final double[] right = { Math.sin(alpha + beta),
                                     Math.cos(alpha + beta) };
            final double[] a = getCenter(focus);
            View bestFocus = null;
            double bestDistance = Double.MAX_VALUE;
            for (View candidate: mOnTable) {
                if (candidate != focus) {
                    final double[] b = getCenter(candidate);
                    final double[] delta = { b[0] - a[0],
                                             b[1] - a[1] };
                    if (cross(delta, left) > 0.0 && cross(delta, right) < 0.0) {
                        final double distance = norm(delta);
                        if (bestDistance > distance) {
                            bestDistance = distance;
                            bestFocus = candidate;
                        }
                    }
                }
            }
            if (bestFocus == null) {
                if (angle < 180f) {
                    return moveFocus(focus, direction, 180f);
                }
            } else {
                setFocus(bestFocus);
            }
        }
        return getFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mKeyboardInterpreter.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return mEdgeSwipeDetector.onTouchEvent(event) || mDragGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (hasSelection()) {
                clearSelection();
            } else  {
                if (mTapToExit && mDream != null) {
                    mDream.finish();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        log("onLayout (%d, %d, %d, %d)", left, top, right, bottom);

        mHeight = bottom - top;
        mWidth = right - left;

        mLongSide = (int) (mImageRatio * Math.max(mWidth, mHeight));
        mShortSide = (int) (mImageRatio * Math.min(mWidth, mHeight));

        boolean isLandscape = mWidth > mHeight;
        if (mIsLandscape != isLandscape) {
            for (View photo: mOnTable) {
                if (photo != getSelection()) {
                    dropOnTable(photo);
                }
            }
            if (hasSelection()) {
                pickUp(getSelection());
                for (int slot = 0; slot < mOnDeck.length; slot++) {
                    if (mOnDeck[slot] != null) {
                        placeOnDeck(mOnDeck[slot], slot);
                    }
                }
            }
            mIsLandscape = isLandscape;
        }
        start();
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    /** Put a nice border on the bitmap. */
    private static View applyFrame(final PhotoTable table, final BitmapFactory.Options options,
            Bitmap decodedPhoto) {
        LayoutInflater inflater = (LayoutInflater) table.getContext()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View photo = inflater.inflate(R.layout.photo, null);
        ImageView image = (ImageView) photo;
        Drawable[] layers = new Drawable[2];
        int photoWidth = options.outWidth;
        int photoHeight = options.outHeight;
        if (decodedPhoto == null || options.outWidth <= 0 || options.outHeight <= 0) {
            photo = null;
        } else {
            decodedPhoto.setHasMipMap(true);
            layers[0] = new BitmapDrawable(table.mResources, decodedPhoto);
            layers[1] = table.mResources.getDrawable(R.drawable.frame);
            LayerDrawable layerList = new LayerDrawable(layers);
            layerList.setLayerInset(0, table.mInset, table.mInset,
                                    table.mInset, table.mInset);
            image.setImageDrawable(layerList);

            photo.setTag(R.id.photo_width, Integer.valueOf(photoWidth));
            photo.setTag(R.id.photo_height, Integer.valueOf(photoHeight));

            photo.setOnTouchListener(new PhotoTouchListener(table.getContext(),
                                                            table));
        }
        return photo;
    }

    private class LoadNaturalSiblingTask extends AsyncTask<View, Void, View> {
        private final BitmapFactory.Options mOptions;
        private final int mSlot;
        private View mParent;

        public LoadNaturalSiblingTask (int slot) {
            mOptions = new BitmapFactory.Options();
            mOptions.inTempStorage = new byte[32768];
            mSlot = slot;
        }

        @Override
        public View doInBackground(View... views) {
            log("load natural %s", (mSlot == NEXT ? "next" : "previous"));
            final PhotoTable table = PhotoTable.this;
            mParent = views[0];
            final Bitmap current = getBitmap(mParent);
            Bitmap decodedPhoto;
            if (mSlot == NEXT) {
                decodedPhoto = table.mPhotoSource.naturalNext(current,
                    mOptions, table.mLongSide, table.mShortSide);
            } else {
                decodedPhoto = table.mPhotoSource.naturalPrevious(current,
                    mOptions, table.mLongSide, table.mShortSide);
            }
            return applyFrame(PhotoTable.this, mOptions, decodedPhoto);
        }

        @Override
        public void onPostExecute(View photo) {
            if (photo != null) {
                if (hasSelection() && getSelection() == mParent) {
                    log("natural %s being rendered", (mSlot == NEXT ? "next" : "previous"));
                    PhotoTable.this.addView(photo, new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT));
                    PhotoTable.this.mOnDeck[mSlot] = photo;
                    float width = (float) ((Integer) photo.getTag(R.id.photo_width)).intValue();
                    float height = (float) ((Integer) photo.getTag(R.id.photo_height)).intValue();
                    photo.setX(mSlot == PREV ? -2 * width : mWidth + 2 * width);
                    photo.setY((mHeight - height) / 2);
                    photo.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                        @Override
                        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                int oldLeft, int oldTop, int oldRight, int oldBottom) {
                            PhotoTable.this.placeOnDeck(v, mSlot);
                            v.removeOnLayoutChangeListener(this);
                        }
                    });
                } else {
                   recycle(photo);
                }
            } else {
                log("natural, %s was null!", (mSlot == NEXT ? "next" : "previous"));
            }
        }
    };

    private class PhotoLaunchTask extends AsyncTask<Void, Void, View> {
        private final BitmapFactory.Options mOptions;

        public PhotoLaunchTask () {
            mOptions = new BitmapFactory.Options();
            mOptions.inTempStorage = new byte[32768];
        }

        @Override
        public View doInBackground(Void... unused) {
            log("load a new photo");
            final PhotoTable table = PhotoTable.this;
            return applyFrame(PhotoTable.this, mOptions,
                 table.mPhotoSource.next(mOptions,
                      table.mLongSide, table.mShortSide));
        }

        @Override
        public void onPostExecute(View photo) {
            if (photo != null) {
                final PhotoTable table = PhotoTable.this;

                table.addView(photo, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
                if (table.hasSelection()) {
                    for (int slot = 0; slot < mOnDeck.length; slot++) {
                        if (mOnDeck[slot] != null) {
                            table.moveToTopOfPile(mOnDeck[slot]);
                        }
                    }
                    table.moveToTopOfPile(table.getSelection());
                }

                log("drop it");
                table.throwOnTable(photo);

                if (mOnTable.size() > mTableCapacity) {
                    int targetSize = Math.max(0, mOnTable.size() - mRedealCount);
                    while (mOnTable.size() > targetSize) {
                        fadeAway(mOnTable.poll(), false);
                    }
                }

                if(table.mOnTable.size() < table.mTableCapacity) {
                    table.scheduleNext(table.mFastDropPeriod);
                }
            }
        }
    };

    /** Bring a new photo onto the table. */
    public void launch() {
        log("launching");
        setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        if (!hasSelection()) {
            log("inflate it");
            if (mPhotoLaunchTask == null ||
                mPhotoLaunchTask.getStatus() == AsyncTask.Status.FINISHED) {
                mPhotoLaunchTask = new PhotoLaunchTask();
                mPhotoLaunchTask.execute();
            }
        }
    }

    /** De-emphasize the other photos on the table. */
    public void fadeOutBackground(final View photo) {
        resolveBackgroundQueue();
        if (mBackgroudOptimization) {
            mBackground.animate()
                    .withLayer()
                    .setDuration(mPickUpDuration)
                    .alpha(0f);
        } else {
            mScrim.setAlpha(0f);
            mScrim.setVisibility(View.VISIBLE);
            bringChildToFront(mScrim);
            bringChildToFront(photo);
            mScrim.animate()
                    .withLayer()
                    .setDuration(mPickUpDuration)
                    .alpha(1f);
        }
    }


    /** Return the other photos to foreground status. */
    public void fadeInBackground(final View photo) {
        if (mBackgroudOptimization) {
            mWaitingToJoinBackground.add(photo);
            mBackground.animate()
                    .withLayer()
                    .setDuration(mPickUpDuration)
                    .alpha(1f)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            resolveBackgroundQueue();
                        }
                    });
        } else {
            bringChildToFront(mScrim);
            bringChildToFront(photo);
            mScrim.animate()
                    .withLayer()
                    .setDuration(mPickUpDuration)
                    .alpha(0f)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mScrim.setVisibility(View.GONE);
                        }
                    });
        }
    }

    private void resolveBackgroundQueue() {
        for(View photo: mWaitingToJoinBackground) {
              moveToBackground(photo);
        }
        mWaitingToJoinBackground.clear();
    }

    /** Dispose of the photo gracefully, in case we can see some of it. */
    public void fadeAway(final View photo, final boolean replace) {
        // fade out of view
        mOnTable.remove(photo);
        exitStageLeft(photo);
        photo.setOnTouchListener(null);
        photo.animate().cancel();
        photo.animate()
                .withLayer()
                .alpha(0f)
                .setDuration(mPickUpDuration)
                .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (photo == getFocus()) {
                                clearFocus();
                            }
                            mStageLeft.removeView(photo);
                            recycle(photo);
                            if (replace) {
                                scheduleNext(mNowDropDelay);
                            }
                        }
                    });
    }

    /** Visually on top, and also freshest, for the purposes of timeouts. */
    public void moveToTopOfPile(View photo) {
        // make this photo the last to be removed.
        if (isInBackground(photo)) {
           mBackground.bringChildToFront(photo);
        } else {
            bringChildToFront(photo);
        }
        invalidate();
        mOnTable.remove(photo);
        mOnTable.offer(photo);
    }

    /** On deck is to the left or right of the selected photo. */
    private void placeOnDeck(final View photo, final int slot ) {
        if (slot < mOnDeck.length) {
            if (mOnDeck[slot] != null && mOnDeck[slot] != photo) {
                fadeAway(mOnDeck[slot], false);
            }
            mOnDeck[slot] = photo;
            float photoWidth = photo.getWidth();
            float photoHeight = photo.getHeight();
            float scale = Math.min(getHeight() / photoHeight, getWidth() / photoWidth);

            float x = (getWidth() - photoWidth) / 2f;
            float y = (getHeight() - photoHeight) / 2f;

            float offset = (((float) mWidth + scale * (photoWidth - 2f * mInset)) / 2f);
            x += (slot == NEXT? 1f : -1f) * offset;

            photo.animate()
                .withLayer()
                .rotation(0f)
                .rotationY(0f)
                .scaleX(scale)
                .scaleY(scale)
                .x(x)
                .y(y)
                .setDuration(mPickUpDuration)
                .setInterpolator(new DecelerateInterpolator(2f));
        }
    }

    /** Move in response to touch. */
    public void move(final View photo, float x, float y, float a) {
        photo.animate().cancel();
        photo.setAlpha(1f);
        photo.setX((int) x);
        photo.setY((int) y);
        photo.setRotation((int) a);
    }

    /** Wind up off screen, so we can animate in. */
    private void throwOnTable(final View photo) {
        mOnTable.offer(photo);
        log("start offscreen");
        photo.setRotation(mThrowRotation);
        photo.setX(-mLongSide);
        photo.setY(-mLongSide);

        dropOnTable(photo, mThrowInterpolator);
    }

    public void move(final View photo, float dx, float dy, boolean drop) {
        if (photo != null) {
            final float x = photo.getX() + dx;
            final float y = photo.getY() + dy;
            photo.setX(x);
            photo.setY(y);
            Log.d(TAG, "[" + photo.getX() + ", " + photo.getY() + "] + (" + dx + "," + dy + ")");
            if (drop && photoOffTable(photo)) {
                fadeAway(photo, true);
            }
        }
    }

    /** Fling with no touch hints, then land off screen. */
    public void fling(final View photo) {
        final float[] o = { mWidth + mLongSide / 2f,
                            mHeight + mLongSide / 2f };
        final float[] a = { photo.getX(), photo.getY() };
        final float[] b = { o[0], a[1] + o[0] - a[0] };
        final float[] c = { a[0] + o[1] - a[1], o[1] };
        float[] delta = { 0f, 0f };
        if (Math.hypot(b[0] - a[0], b[1] - a[1]) < Math.hypot(c[0] - a[0], c[1] - a[1])) {
            delta[0] = b[0] - a[0];
            delta[1] = b[1] - a[1];
        } else {
            delta[0] = c[0] - a[0];
            delta[1] = c[1] - a[1];
        }

        final float dist = (float) Math.hypot(delta[0], delta[1]);
        final int duration = (int) (1000f * dist / mThrowSpeed);
        fling(photo, delta[0], delta[1], duration, true);
    }

    /** Continue dynamically after a fling gesture, possibly off the screen. */
    public void fling(final View photo, float dx, float dy, int duration, boolean spin) {
        if (photo == getFocus()) {
            if (moveFocus(photo, 0f) == null) {
                moveFocus(photo, 180f);
            }
        }
        moveToForeground(photo);
        ViewPropertyAnimator animator = photo.animate()
                .withLayer()
                .xBy(dx)
                .yBy(dy)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator(2f));

        if (spin) {
            animator.rotation(mThrowRotation);
        }

        if (photoOffTable(photo, (int) dx, (int) dy)) {
            log("fling away");
            animator.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        fadeAway(photo, true);
                    }
                });
        }
    }
    public boolean photoOffTable(final View photo) {
        return photoOffTable(photo, 0, 0);
    }

    public boolean photoOffTable(final View photo, final int dx, final int dy) {
        Rect hit = new Rect();
        photo.getHitRect(hit);
        hit.offset(dx, dy);
        return (hit.bottom < 0f || hit.top > getHeight() ||
                hit.right < 0f || hit.left > getWidth());
    }

    /** Animate to a random place and orientation, down on the table (visually small). */
    public void dropOnTable(final View photo) {
        dropOnTable(photo, mDropInterpolator);
    }

    /** Animate to a random place and orientation, down on the table (visually small). */
    public void dropOnTable(final View photo, final Interpolator interpolator) {
        float angle = randfrange(-mImageRotationLimit, mImageRotationLimit);
        PointF p = randMultiDrop(sRNG.nextInt(),
                                 (float) sRNG.nextGaussian(), (float) sRNG.nextGaussian(),
                                 mWidth, mHeight);
        float x = p.x;
        float y = p.y;

        log("drop it at %f, %f", x, y);

        float x0 = photo.getX();
        float y0 = photo.getY();

        x -= mLongSide / 2f;
        y -= mShortSide / 2f;
        log("fixed offset is %f, %f ", x, y);

        float dx = x - x0;
        float dy = y - y0;

        float dist = (float) Math.hypot(dx, dy);
        int duration = (int) (1000f * dist / mThrowSpeed);
        duration = Math.max(duration, 1000);

        log("animate it");
        // toss onto table
        resolveBackgroundQueue();
        photo.animate()
            .withLayer()
            .scaleX(mTableRatio / mImageRatio)
            .scaleY(mTableRatio / mImageRatio)
            .rotation(angle)
            .x(x)
            .y(y)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    mWaitingToJoinBackground.add(photo);
                }
            });
    }

    private void moveToBackground(View photo) {
        if (mBackgroudOptimization && !isInBackground(photo)) {
            removeViewFromParent(photo);
            mBackground.addView(photo, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
        }
    }

    private void exitStageLeft(View photo) {
        removeViewFromParent(photo);
        mStageLeft.addView(photo, new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
    }

    private void removeViewFromParent(View photo) {
        ViewParent parent = photo.getParent();
        if (parent != null) {  // should never be null, just being paranoid
            ((ViewGroup) parent).removeView(photo);
        }
    }

    private void moveToForeground(View photo) {
        if (mBackgroudOptimization && isInBackground(photo)) {
            mBackground.removeView(photo);
            addView(photo, new LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
        }
    }

    private boolean isInBackground(View photo) {
        return mBackgroudOptimization && mBackground.indexOfChild(photo) != -1;
    }

    /** wrap all orientations to the interval [-180, 180). */
    private float wrapAngle(float angle) {
        float result = angle + 180;
        result = ((result % 360) + 360) % 360; // catch negative numbers
        result -= 180;
        return result;
    }

    /** Animate the selected photo to the foreground: zooming in to bring it forward. */
    private void pickUp(final View photo) {
        float photoWidth = photo.getWidth();
        float photoHeight = photo.getHeight();

        float scale = Math.min(getHeight() / photoHeight, getWidth() / photoWidth);

        log("scale is %f", scale);
        log("target it");
        float x = (getWidth() - photoWidth) / 2f;
        float y = (getHeight() - photoHeight) / 2f;

        photo.setRotation(wrapAngle(photo.getRotation()));

        log("animate it");
        // lift up to the glass for a good look
        mWaitingToJoinBackground.remove(photo);
        moveToForeground(photo);
        photo.animate()
            .withLayer()
            .rotation(0f)
            .rotationY(0f)
            .alpha(1f)
            .scaleX(scale)
            .scaleY(scale)
            .x(x)
            .y(y)
            .setDuration(mPickUpDuration)
            .setInterpolator(new DecelerateInterpolator(2f))
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    log("endtimes: %f", photo.getX());
                }
            });
    }

    private Bitmap getBitmap(View photo) {
        if (photo == null) {
            return null;
        }
        ImageView image = (ImageView) photo;
        LayerDrawable layers = (LayerDrawable) image.getDrawable();
        if (layers == null) {
            return null;
        }
        BitmapDrawable bitmap = (BitmapDrawable) layers.getDrawable(0);
        if (bitmap == null) {
            return null;
        }
        return bitmap.getBitmap();
    }

    private void recycle(View photo) {
        if (photo != null) {
            removeViewFromParent(photo);
            mPhotoSource.recycle(getBitmap(photo));
        }
    }

    public void setHighlight(View photo, boolean highlighted) {
        ImageView image = (ImageView) photo;
        LayerDrawable layers = (LayerDrawable) image.getDrawable();
        if (highlighted) {
            layers.getDrawable(1).setColorFilter(mHighlightColor, PorterDuff.Mode.SRC_IN);
        } else {
            layers.getDrawable(1).clearColorFilter();
        }
    }

    /** Schedule the first launch.  Idempotent. */
    public void start() {
        if (!mStarted) {
            log("kick it");
            mStarted = true;
            scheduleNext(0);
        }
    }

    public void refreshSelection() {
        scheduleSelectionReaper(mMaxFocusTime);
    }

    public void scheduleSelectionReaper(int delay) {
        removeCallbacks(mSelectionReaper);
        postDelayed(mSelectionReaper, delay);
    }

    public void refreshFocus() {
        scheduleFocusReaper(mMaxFocusTime);
    }

    public void scheduleFocusReaper(int delay) {
        removeCallbacks(mFocusReaper);
        postDelayed(mFocusReaper, delay);
    }

    public void scheduleNext(int delay) {
        removeCallbacks(mLauncher);
        postDelayed(mLauncher, delay);
    }

    private static void log(String message, Object... args) {
        if (DEBUG) {
            Formatter formatter = new Formatter();
            formatter.format(message, args);
            Log.i(TAG, formatter.toString());
        }
    }
}
