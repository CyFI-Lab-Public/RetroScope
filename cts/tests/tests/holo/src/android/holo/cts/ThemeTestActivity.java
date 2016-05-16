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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link Activity} that iterates over all the test layouts for a single theme
 * and either compares or generates bitmaps.
 */
public class ThemeTestActivity extends Activity {

    private static final String TAG = ThemeTestActivity.class.getSimpleName();
    private static final boolean DEBUG = false;

    static final String EXTRA_TASK = "task";
    static final String EXTRA_THEME_INDEX = "themeIndex";
    static final String EXTRA_LAYOUT_INDEX = "layoutIndex";
    static final String EXTRA_LAYOUT_ADAPTER_MODE = "layoutAdapterMode";

    static final int TASK_VIEW_LAYOUTS = 1;
    static final int TASK_GENERATE_BITMAPS = 2;
    static final int TASK_COMPARE_BITMAPS = 3;

    private static final int VIEW_TESTS_REQUEST_CODE = 1;
    private static final int GENERATE_BITMAP_REQUEST_CODE = 2;
    private static final int COMPARE_BITMAPS_REQUEST_CODE = 3;

    private int mRequestCode;
    private Iterator<Intent> mIterator;
    private Result mPendingResult;
    private ResultFuture<Result> mResultFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResultFuture = new ResultFuture<Result>();
        mPendingResult = new Result();

        int task = getIntent().getIntExtra(EXTRA_TASK, -1);
        switch (task) {
            case TASK_VIEW_LAYOUTS:
                mRequestCode = VIEW_TESTS_REQUEST_CODE;
                break;

            case TASK_GENERATE_BITMAPS:
                mRequestCode = GENERATE_BITMAP_REQUEST_CODE;
                break;

            case TASK_COMPARE_BITMAPS:
                // Don't delete any failure bitmap images that may be useful.
                mRequestCode = COMPARE_BITMAPS_REQUEST_CODE;
                break;

            default:
                throw new IllegalArgumentException("Bad task: " + task);
        }

        int themeIndex = getIntent().getIntExtra(EXTRA_THEME_INDEX, -1);
        int layoutIndex = getIntent().getIntExtra(EXTRA_LAYOUT_INDEX, -1);
        int adapterMode = getIntent().getIntExtra(EXTRA_LAYOUT_ADAPTER_MODE, -1);

        Log.i(TAG, "Theme index: " + themeIndex + " Layout index: " + layoutIndex +
                " adapter mode: " + adapterMode);

        if (themeIndex < 0 && layoutIndex < 0) {
            mIterator = new AllThemesIterator(task, adapterMode);
        } else if (themeIndex >= 0 && layoutIndex >= 0) {
            mIterator = new SingleThemeLayoutIterator(themeIndex, layoutIndex, task, adapterMode);
        } else if (layoutIndex >= 0) {
            mIterator = new SingleLayoutIterator(layoutIndex, task, adapterMode);
        } else if (themeIndex >= 0) {
            mIterator = new SingleThemeIterator(themeIndex, task, adapterMode);
        } else {
            throw new IllegalStateException();
        }

        generateNextBitmap();
    }

    private void generateNextBitmap() {
        if (mIterator.hasNext()) {
            Intent intent = mIterator.next();
            intent.setClass(this, LayoutTestActivity.class);
            startActivityForResult(intent, mRequestCode);
        } else {
            mResultFuture.set(mPendingResult);
            if (mRequestCode == GENERATE_BITMAP_REQUEST_CODE) {
                // finish with result so that generated bitmaps can be captured automatically
                if (DEBUG) {
                    Log.i(TAG, "generateNextBitmap finishing");
                }
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) {
            Log.i(TAG,  "onActivityResult req:" + requestCode + " res:" + resultCode);
        }
        switch (requestCode) {
            case VIEW_TESTS_REQUEST_CODE:
                return;

            case GENERATE_BITMAP_REQUEST_CODE:
            case COMPARE_BITMAPS_REQUEST_CODE:
                handleResult(resultCode, data);
                break;

            default:
                throw new IllegalArgumentException("Bad request code: " + requestCode);
        }
    }

    private void handleResult(int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            throw new IllegalStateException("Did you interrupt the activity?");
        }

        boolean success = data.getBooleanExtra(LayoutTestActivity.EXTRA_SUCCESS, false);
        if (!success) {
            String bitmapName = data.getStringExtra(LayoutTestActivity.EXTRA_BITMAP_NAME);
            mPendingResult.addFailedBitmapName(bitmapName);
        }
        generateNextBitmap();
    }

    public Future<Result> getResultFuture() {
        return mResultFuture;
    }

    static class Result {

        private List<String> mFailedBitmapNames = new ArrayList<String>();

        public boolean passed() {
            return mFailedBitmapNames.isEmpty();
        }

        public List<String> getFailedBitmapNames() {
            return mFailedBitmapNames;
        }

        private void addFailedBitmapName(String bitmapName) {
            mFailedBitmapNames.add(bitmapName);
        }
    }

    class ResultFuture<T> implements Future<T> {

        private final CountDownLatch mLatch = new CountDownLatch(1);

        private T mResult;

        public void set(T result) {
            mResult = result;
            mLatch.countDown();
        }

        @Override
        public T get() throws InterruptedException {
            mLatch.await();
            return mResult;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException,
                TimeoutException {
            if (!mLatch.await(timeout, unit)) {
                throw new TimeoutException();
            }
            return mResult;
        }

        @Override
        public boolean isDone() {
            return mLatch.getCount() > 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
