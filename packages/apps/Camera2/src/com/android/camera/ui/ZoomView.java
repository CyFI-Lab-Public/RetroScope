/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.camera.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ZoomView extends ImageView {

    private static final String TAG = "ZoomView";

    private int mViewportWidth = 0;
    private int mViewportHeight = 0;

    private int mFullResImageWidth;
    private int mFullResImageHeight;

    private BitmapRegionDecoder mRegionDecoder;
    private DecodePartialBitmap mPartialDecodingTask;

    private Uri mUri;
    private int mOrientation;

    private class DecodePartialBitmap extends AsyncTask<RectF, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(RectF... params) {
            RectF endRect = params[0];

            // Calculate the rotation matrix to apply orientation on the original image
            // rect.
            RectF fullResRect = new RectF(0, 0, mFullResImageWidth - 1, mFullResImageHeight - 1);
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.setRotate(mOrientation, 0, 0);
            rotationMatrix.mapRect(fullResRect);
            // Set the translation of the matrix so that after rotation, the top left
            // of the image rect is at (0, 0)
            rotationMatrix.postTranslate(-fullResRect.left, -fullResRect.top);
            rotationMatrix.mapRect(fullResRect, new RectF(0, 0, mFullResImageWidth - 1,
                    mFullResImageHeight - 1));

            // Find intersection with the screen
            RectF visibleRect = new RectF(endRect);
            visibleRect.intersect(0, 0, mViewportWidth - 1, mViewportHeight - 1);
            // Calculate the mapping (i.e. transform) between current low res rect
            // and full res image rect, and apply the mapping on current visible rect
            // to find out the partial region in the full res image that we need
            // to decode.
            Matrix mapping = new Matrix();
            mapping.setRectToRect(endRect, fullResRect, Matrix.ScaleToFit.CENTER);
            RectF visibleAfterRotation = new RectF();
            mapping.mapRect(visibleAfterRotation, visibleRect);

            // Now the visible region we have is rotated, we need to reverse the
            // rotation to find out the region in the original image
            RectF visibleInImage = new RectF();
            Matrix invertRotation = new Matrix();
            rotationMatrix.invert(invertRotation);
            invertRotation.mapRect(visibleInImage, visibleAfterRotation);

            // Decode region
            Rect region = new Rect();
            visibleInImage.round(region);

            // Make sure region to decode is inside the image.
            region.intersect(0, 0, mFullResImageWidth - 1, mFullResImageHeight - 1);

            if (region.width() == 0 || region.height() == 0) {
                Log.e(TAG, "Invalid size for partial region. Region: " + region.toString());
                return null;
            }

            if (isCancelled()) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();

            if ((mOrientation + 360) % 180 == 0) {
                options.inSampleSize = getSampleFactor(region.width(), region.height());
            } else {
                // The decoded region will be rotated 90/270 degrees before showing
                // on screen. In other words, the width and height will be swapped.
                // Therefore, sample factor should be calculated using swapped width
                // and height.
                options.inSampleSize = getSampleFactor(region.height(), region.width());
            }

            if (mRegionDecoder == null) {
                InputStream is = getInputStream();
                try {
                    mRegionDecoder = BitmapRegionDecoder.newInstance(is, false);
                    is.close();
                } catch (IOException e) {
                    Log.e(TAG, "Failed to instantiate region decoder");
                }
            }
            if (mRegionDecoder == null) {
                return null;
            }
            Bitmap b = mRegionDecoder.decodeRegion(region, options);
            if (isCancelled()) {
                return null;
            }
            Matrix rotation = new Matrix();
            rotation.setRotate(mOrientation);
            return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), rotation, false);
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b == null) {
                return;
            }
            setImageBitmap(b);
            showPartiallyDecodedImage(true);
            mPartialDecodingTask = null;
        }
    }

    public ZoomView(Context context) {
        super(context);
        setScaleType(ScaleType.FIT_CENTER);
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int w = right - left;
                int h = bottom - top;
                if (mViewportHeight != h || mViewportWidth != w) {
                    mViewportWidth = w;
                    mViewportHeight = h;
                }
            }
        });
    }

    public void loadBitmap(Uri uri, int orientation, RectF imageRect) {
        if (!uri.equals(mUri)) {
            mUri = uri;
            mOrientation = orientation;
            mFullResImageHeight = 0;
            mFullResImageWidth = 0;
            decodeImageSize();
            mRegionDecoder = null;
        }
        startPartialDecodingTask(imageRect);
    }

    private void showPartiallyDecodedImage(boolean show) {
        if (show) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
        mPartialDecodingTask = null;
    }

    public void cancelPartialDecodingTask() {
        if (mPartialDecodingTask != null && !mPartialDecodingTask.isCancelled()) {
            mPartialDecodingTask.cancel(true);
            setVisibility(GONE);
        }
        mPartialDecodingTask = null;
    }

    /**
     * If the given rect is smaller than viewport on x or y axis, center rect within
     * viewport on the corresponding axis. Otherwise, make sure viewport is within
     * the bounds of the rect.
     */
    public static RectF adjustToFitInBounds(RectF rect, int viewportWidth, int viewportHeight) {
        float dx = 0, dy = 0;
        RectF newRect = new RectF(rect);
        if (newRect.width() < viewportWidth) {
            dx = viewportWidth / 2 - (newRect.left + newRect.right) / 2;
        } else {
            if (newRect.left > 0) {
                dx = -newRect.left;
            } else if (newRect.right < viewportWidth) {
                dx = viewportWidth - newRect.right;
            }
        }

        if (newRect.height() < viewportHeight) {
            dy = viewportHeight / 2 - (newRect.top + newRect.bottom) / 2;
        } else {
            if (newRect.top > 0) {
                dy = -newRect.top;
            } else if (newRect.bottom < viewportHeight) {
                dy = viewportHeight - newRect.bottom;
            }
        }

        if (dx != 0 || dy != 0) {
            newRect.offset(dx, dy);
        }
        return newRect;
    }

    private void startPartialDecodingTask(RectF endRect) {
        // Cancel on-going partial decoding tasks
        cancelPartialDecodingTask();
        mPartialDecodingTask = new DecodePartialBitmap();
        mPartialDecodingTask.execute(endRect);
    }

    private void decodeImageSize() {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        InputStream is = getInputStream();
        BitmapFactory.decodeStream(is, null, option);
        try {
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close input stream");
        }
        mFullResImageWidth = option.outWidth;
        mFullResImageHeight = option.outHeight;
    }

    // TODO: Cache the inputstream
    private InputStream getInputStream() {
        InputStream is = null;
        try {
            is = getContext().getContentResolver().openInputStream(mUri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found at: " + mUri);
        }
        return is;
    }

    /**
     * Find closest sample factor that is power of 2, based on the given width and height
     *
     * @param width width of the partial region to decode
     * @param height height of the partial region to decode
     * @return sample factor
     */
    private int getSampleFactor(int width, int height) {

        float fitWidthScale = ((float) mViewportWidth) / ((float) width);
        float fitHeightScale = ((float) mViewportHeight) / ((float) height);

        float scale = Math.min(fitHeightScale, fitWidthScale);

        // Find the closest sample factor that is power of 2
        int sampleFactor = (int) (1f / scale);
        if (sampleFactor <=1) {
            return 1;
        }
        for (int i = 0; i < 32; i++) {
            if ((1 << (i + 1)) > sampleFactor) {
                sampleFactor = (1 << i);
                break;
            }
        }
        return sampleFactor;
    }
}
