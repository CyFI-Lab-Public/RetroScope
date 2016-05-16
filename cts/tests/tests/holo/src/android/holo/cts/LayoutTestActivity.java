/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.holo.cts;

import com.android.cts.holo.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.holo.cts.LayoutAdapter.LayoutInfo;
import android.holo.cts.ThemeAdapter.ThemeInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * {@link Activity} that applies a theme, inflates a layout, and then either
 * compares or generates a bitmap of the layout.
 */
public class LayoutTestActivity extends Activity {

    private static final String TAG = LayoutTestActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    // Input extras
    static final String EXTRA_THEME_INDEX = "themeIndex";
    static final String EXTRA_LAYOUT_INDEX = "layoutIndex";
    static final String EXTRA_TASK = "task";
    static final String EXTRA_LAYOUT_ADAPTER_MODE = "layoutAdapterMode";

    // Output extras
    static final String EXTRA_BITMAP_NAME = "bitmapName";
    static final String EXTRA_MESSAGE = "message";
    static final String EXTRA_SUCCESS = "success";

    private View mTestView;
    private String mBitmapName;

    private ReferenceViewGroup mViewGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int themeIndex = getIntent().getIntExtra(EXTRA_THEME_INDEX, -1);
        int layoutIndex = getIntent().getIntExtra(EXTRA_LAYOUT_INDEX, -1);
        int layoutMode = getIntent().getIntExtra(EXTRA_LAYOUT_ADAPTER_MODE, -1);
        int task = getIntent().getIntExtra(EXTRA_TASK, -1);

        if (DEBUG) {
            Log.i(TAG, "onCreate theme:" + themeIndex + " layout:" + layoutIndex +
                    " mode:" + layoutMode + " task:" + task);
        }
        ThemeAdapter themeAdapter = new ThemeAdapter(getLayoutInflater());
        LayoutAdapter layoutAdapter = new LayoutAdapter(getLayoutInflater(), layoutMode);

        ThemeInfo themeInfo = themeAdapter.getItem(themeIndex);
        LayoutInfo layoutInfo = layoutAdapter.getItem(layoutIndex);
        mBitmapName = BitmapAssets.getBitmapName(themeInfo, layoutInfo);

        setTheme(themeInfo.getTheme());
        setContentView(R.layout.holo_test);

        if (layoutInfo.hasModifier()) {
            layoutInfo.getModifier().prepare();
        }

        // Inflate the view in our special view group that is fixed at a certain size
        // and layout the inflated view with the fixed measurements...
        mViewGroup = (ReferenceViewGroup) findViewById(R.id.reference_view_group);
        mTestView = getLayoutInflater().inflate(layoutInfo.getLayout(), mViewGroup, false);
        mViewGroup.addView(mTestView);
        if (layoutInfo.hasModifier()) {
            mTestView = layoutInfo.getModifier().modifyView(mTestView);
        }
        mViewGroup.measure(0,  0);
        mViewGroup.layout(0, 0, mViewGroup.getMeasuredWidth(), mViewGroup.getMeasuredHeight());
        mTestView.setFocusable(false);

        switch (task) {
            case ThemeTestActivity.TASK_VIEW_LAYOUTS:
                break;

            case ThemeTestActivity.TASK_GENERATE_BITMAPS:
                mTestView.postDelayed(new GenerateBitmapRunnable(), layoutInfo.getTimeoutMs());
                break;

            case ThemeTestActivity.TASK_COMPARE_BITMAPS:
                mTestView.postDelayed(new CompareBitmapRunnable(), layoutInfo.getTimeoutMs());
                break;
        }
    }

    class GenerateBitmapRunnable implements Runnable {
        @Override
        public void run() {
            new GenerateBitmapTask().execute();
        }
    }

    class CompareBitmapRunnable implements Runnable {
        @Override
        public void run() {
            new CompareBitmapTask().execute();
        }
    }

    class GenerateBitmapTask extends AsyncTask<Void, Void, Boolean> {

        private Bitmap mBitmap;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mBitmap = getBitmap();
        }

        @Override
        protected Boolean doInBackground(Void... avoid) {
            try {
                return saveBitmap(mBitmap, BitmapAssets.TYPE_REFERENCE) != null;
            } finally {
                mBitmap.recycle();
                mBitmap = null;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            String path = BitmapAssets.getBitmapPath(mBitmapName,
                    BitmapAssets.TYPE_REFERENCE).toString();
            String message = path != null
                    ? getString(R.string.generate_bitmap_success, path)
                    : getString(R.string.generate_bitmap_failure, path);
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            finishWithResult(success, message);
        }
    }

    class CompareBitmapTask extends AsyncTask<Void, Void, String[]> {
        private Bitmap mBitmap;
        private Bitmap mReferenceBitmap;
        private boolean mSame;

        @Override
        protected void onPreExecute() {
            mBitmap = getBitmap();
            mReferenceBitmap = BitmapAssets.getBitmap(getApplicationContext(), mBitmapName);
        }

        /* Compares 2 bitmaps' width, height and pixels.
         * 2 Bitmaps are consider the same if color value difference is less than
         * or equal to +/-threshold
         */
        private boolean compareTo(Bitmap bitmap, Bitmap reference, int threshold) {
            if (bitmap.getConfig() != reference.getConfig() ||
                    bitmap.getWidth() != reference.getWidth() ||
                    bitmap.getHeight() != reference.getHeight()) {
                return false;
            }

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();

            ByteBuffer buffer1 = ByteBuffer.allocate(bitmap.getByteCount());
            ByteBuffer buffer2 = ByteBuffer.allocate(reference.getByteCount());

            bitmap.copyPixelsToBuffer(buffer1);
            reference.copyPixelsToBuffer(buffer2);

            final int length = w*h;
            for (int i = 0; i < length; i++) {
                int pel1 = buffer1.getInt(i);
                int pel2 = buffer2.getInt(i);
                int dr = (pel1 & 0x000000FF) - (pel2 & 0x000000FF);
                int dg = ((pel1 & 0x0000FF00) - (pel2 & 0x0000FF00)) >> 8;
                int db = ((pel1 & 0x00FF0000) - (pel2 & 0x00FF0000)) >> 16;

                if (Math.abs(db) > threshold ||
                        Math.abs(dg) > threshold ||
                        Math.abs(dr) > threshold) {
                    return false;
                }
                if (bitmap.hasAlpha()) {
                    int da = ((pel1 & 0xFF000000) - (pel2 & 0xFF000000)) >> 24;
                    if (Math.abs(da) > threshold) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        protected String[] doInBackground(Void... devoid) {
            try {
                final int threshold = 2;
                mSame = compareTo(mBitmap, mReferenceBitmap, threshold);
                if (!mSame) {
                    String[] paths = new String[2];
                    paths[0] = saveDiffBitmap(mBitmap, mReferenceBitmap);
                    paths[1] = saveBitmap(mBitmap, BitmapAssets.TYPE_FAILED);
                    return paths;
                } else {
                    return null;
                }
            } finally {
                mBitmap.recycle();
                mBitmap = null;
            }
        }

        private String saveDiffBitmap(Bitmap bitmap1, Bitmap bitmap2) {
            int width = Math.max(bitmap1.getWidth(), bitmap2.getWidth());
            int height = Math.max(bitmap1.getHeight(), bitmap2.getHeight());
            Bitmap diff = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            try {
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        boolean inBounds1 = i < bitmap1.getWidth() && j < bitmap1.getHeight();
                        boolean inBounds2 = i < bitmap2.getWidth() && j < bitmap2.getHeight();
                        int color;

                        if (inBounds1 && inBounds2) {
                            int color1 = bitmap1.getPixel(i, j);
                            int color2 = bitmap2.getPixel(i, j);
                            color = color1 == color2 ? color1 : Color.RED;
                        } else if (inBounds1 && !inBounds2) {
                            color = Color.BLUE;
                        } else if (!inBounds1 && inBounds2) {
                            color = Color.GREEN;
                        } else {
                            color = Color.MAGENTA;
                        }
                        diff.setPixel(i, j, color);
                    }
                }

                return saveBitmap(diff, BitmapAssets.TYPE_DIFF);
            } finally {
                diff.recycle();
            }
        }

        @Override
        protected void onPostExecute(String[] paths) {
            String message = mSame
                    ? getString(R.string.comparison_success)
                    : getString(R.string.comparison_failure, paths[0], paths[1]);
            finishWithResult(mSame, message);
        }
    }

    private Bitmap getBitmap() {
        Log.i(TAG, "Getting bitmap for " + mBitmapName);
        Bitmap bitmap = Bitmap.createBitmap(mTestView.getWidth(), mTestView.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mTestView.draw(canvas);
        return bitmap;
    }

    private String saveBitmap(Bitmap bitmap, int type) {
        try {
            Log.i(TAG, "Saving bitmap for " + mBitmapName);
            return BitmapAssets.saveBitmap(bitmap, mBitmapName, type);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException while saving " + mBitmapName, e);
            return null;
        } catch (IOException e) {
            Log.e(TAG, "IOException while saving " + mBitmapName, e);
            return null;
        }
    }

    private void finishWithResult(boolean success, String message) {
        if (DEBUG) {
            Log.i(TAG, "finishWithResult finishing:" + message);
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_SUCCESS, success);
        data.putExtra(EXTRA_MESSAGE, message);
        data.putExtra(EXTRA_BITMAP_NAME, mBitmapName);
        setResult(RESULT_OK, data);
        finish();
    }
}
