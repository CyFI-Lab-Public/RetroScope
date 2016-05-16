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
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A FrameLayout that holds two photos, back to back.
 */
public class PhotoCarousel extends FrameLayout {
    private static final String TAG = "PhotoCarousel";
    private static final boolean DEBUG = false;

    private static final int LANDSCAPE = 1;
    private static final int PORTRAIT = 2;

    private final Flipper mFlipper;
    private final PhotoSourcePlexor mPhotoSource;
    private final GestureDetector mGestureDetector;
    private final View[] mPanel;
    private final int mFlipDuration;
    private final int mDropPeriod;
    private final int mBitmapQueueLimit;
    private final HashMap<View, Bitmap> mBitmapStore;
    private final LinkedList<Bitmap> mBitmapQueue;
    private final LinkedList<PhotoLoadTask> mBitmapLoaders;
    private View mSpinner;
    private int mOrientation;
    private int mWidth;
    private int mHeight;
    private int mLongSide;
    private int mShortSide;
    private long mLastFlipTime;

    class Flipper implements Runnable {
        @Override
        public void run() {
            maybeLoadMore();

            if (mBitmapQueue.isEmpty()) {
                mSpinner.setVisibility(View.VISIBLE);
            } else {
                mSpinner.setVisibility(View.GONE);
            }

            long now = System.currentTimeMillis();
            long elapsed = now - mLastFlipTime;

            if (elapsed < mDropPeriod) {
                scheduleNext((int) mDropPeriod - elapsed);
            } else {
                scheduleNext(mDropPeriod);
                if (changePhoto() ||
                        (elapsed > (5 * mDropPeriod) && canFlip())) {
                    flip(1f);
                    mLastFlipTime = now;
                }
            }
        }

        private void scheduleNext(long delay) {
            removeCallbacks(mFlipper);
            postDelayed(mFlipper, delay);
        }
    }

    public PhotoCarousel(Context context, AttributeSet as) {
        super(context, as);
        final Resources resources = getResources();
        mDropPeriod = resources.getInteger(R.integer.carousel_drop_period);
        mBitmapQueueLimit = resources.getInteger(R.integer.num_images_to_preload);
        mFlipDuration = resources.getInteger(R.integer.flip_duration);
        mPhotoSource = new PhotoSourcePlexor(getContext(),
                getContext().getSharedPreferences(FlipperDreamSettings.PREFS_NAME, 0));
        mBitmapStore = new HashMap<View, Bitmap>();
        mBitmapQueue = new LinkedList<Bitmap>();
        mBitmapLoaders = new LinkedList<PhotoLoadTask>();

        mPanel = new View[2];
        mFlipper = new Flipper();
        // this is dead code if the dream calls setInteractive(false)
        mGestureDetector = new GestureDetector(context,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                        log("fling with " + vX);
                        flip(Math.signum(vX));
                        return true;
                    }
                });
    }

    private float lockTo180(float a) {
        return 180f * (float) Math.floor(a / 180f);
    }

    private float wrap360(float a) {
        return a - 360f * (float) Math.floor(a / 360f);
    }

    private class PhotoLoadTask extends AsyncTask<Void, Void, Bitmap> {
        private final BitmapFactory.Options mOptions;

        public PhotoLoadTask () {
            mOptions = new BitmapFactory.Options();
            mOptions.inTempStorage = new byte[32768];
        }

        @Override
        public Bitmap doInBackground(Void... unused) {
            Bitmap decodedPhoto;
            if (mLongSide == 0 || mShortSide == 0) {
                return null;
            }
            decodedPhoto = mPhotoSource.next(mOptions, mLongSide, mShortSide);
            return decodedPhoto;
        }

        @Override
        public void onPostExecute(Bitmap photo) {
            if (photo != null) {
                mBitmapQueue.offer(photo);
            }
            mFlipper.run();
        }
    };

    private void maybeLoadMore() {
        if (!mBitmapLoaders.isEmpty()) {
            for(ListIterator<PhotoLoadTask> i = mBitmapLoaders.listIterator(0);
                i.hasNext();) {
                PhotoLoadTask loader = i.next();
                if (loader.getStatus() == AsyncTask.Status.FINISHED) {
                    i.remove();
                }
            }
        }

        if ((mBitmapLoaders.size() + mBitmapQueue.size()) < mBitmapQueueLimit) {
            PhotoLoadTask task = new PhotoLoadTask();
            mBitmapLoaders.offer(task);
            task.execute();
        }
    }

    private ImageView getBackface() {
        return (ImageView) ((mPanel[0].getAlpha() < 0.5f) ? mPanel[0] : mPanel[1]);
    }

    private boolean canFlip() {
        return mBitmapStore.containsKey(getBackface());
    }

    private boolean changePhoto() {
        Bitmap photo = mBitmapQueue.poll();
        if (photo != null) {
            ImageView destination = getBackface();
            int width = photo.getWidth();
            int height = photo.getHeight();
            int orientation = (width > height ? LANDSCAPE : PORTRAIT);

            destination.setImageBitmap(photo);
            destination.setTag(R.id.photo_orientation, Integer.valueOf(orientation));
            destination.setTag(R.id.photo_width, Integer.valueOf(width));
            destination.setTag(R.id.photo_height, Integer.valueOf(height));
            setScaleType(destination);

            Bitmap old = mBitmapStore.put(destination, photo);
            mPhotoSource.recycle(old);

            return true;
        } else {
            return false;
        }
    }

    private void setScaleType(View photo) {
        if (photo.getTag(R.id.photo_orientation) != null) {
            int orientation = ((Integer) photo.getTag(R.id.photo_orientation)).intValue();
            int width = ((Integer) photo.getTag(R.id.photo_width)).intValue();
            int height = ((Integer) photo.getTag(R.id.photo_height)).intValue();

            if (width < mWidth && height < mHeight) {
                log("too small: FIT_CENTER");
                ((ImageView) photo).setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else if (orientation == mOrientation) {
                log("orientations match: CENTER_CROP");
                ((ImageView) photo).setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                log("orientations do not match: CENTER_INSIDE");
                ((ImageView) photo).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }
        } else {
            log("no tag!");
        }
    }

    public void flip(float sgn) {
        mPanel[0].animate().cancel();
        mPanel[1].animate().cancel();

        float frontY = mPanel[0].getRotationY();
        float backY = mPanel[1].getRotationY();
        float frontA = mPanel[0].getAlpha();
        float backA = mPanel[1].getAlpha();

        frontY = wrap360(frontY);
        backY = wrap360(backY);

        mPanel[0].setRotationY(frontY);
        mPanel[1].setRotationY(backY);

        frontY = lockTo180(frontY + sgn * 180f);
        backY = lockTo180(backY + sgn * 180f);
        frontA = 1f - frontA;
        backA = 1f - backA;

        // Don't rotate
        frontY = backY = 0f;

        ViewPropertyAnimator frontAnim = mPanel[0].animate()
                .rotationY(frontY)
                .alpha(frontA)
                .setDuration(mFlipDuration);
        ViewPropertyAnimator backAnim = mPanel[1].animate()
                .rotationY(backY)
                .alpha(backA)
                .setDuration(mFlipDuration)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        maybeLoadMore();
                    }
                });

        frontAnim.start();
        backAnim.start();
    }

    @Override
    public void onAttachedToWindow() {
        mPanel[0]= findViewById(R.id.front);
        mPanel[1] = findViewById(R.id.back);
        mSpinner = findViewById(R.id.spinner);
        mFlipper.run();
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mHeight = bottom - top;
        mWidth = right - left;

        mOrientation = (mWidth > mHeight ? LANDSCAPE : PORTRAIT);

        mLongSide = (int) Math.max(mWidth, mHeight);
        mShortSide = (int) Math.min(mWidth, mHeight);

        // reset scale types for new aspect ratio
        setScaleType(mPanel[0]);
        setScaleType(mPanel[1]);

        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    private void log(String message) {
        if (DEBUG) {
            Log.i(TAG, message);
        }
    }
}
