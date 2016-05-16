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
package com.android.dreams.phototable;

import android.content.Context;
import android.content.res.Resources;
import android.view.MotionEvent;

/**
 * Detect and dispatch edge events.
 */
public class EdgeSwipeDetector {
    @SuppressWarnings("unused")
    private static final String TAG = "EdgeSwipeDetector";
    private float mEdgeSwipeGutter;
    private float mEdgeSwipeThreshold;
    private boolean mEdgeSwipe;

    private final PhotoTable mTable;

    public EdgeSwipeDetector(Context context, PhotoTable table) {
        mTable = table;
        final Resources resources = context.getResources();
        mEdgeSwipeGutter = resources.getInteger(R.integer.table_edge_swipe_gutter) / 1000000f;
        mEdgeSwipeThreshold = resources.getInteger(R.integer.table_edge_swipe_threshold) / 1000000f;
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float edgeGutter = event.getDevice().getMotionRange(MotionEvent.AXIS_X).getMax()
                        * mEdgeSwipeGutter;
                if (event.getX() < edgeGutter) {
                    mEdgeSwipe = true;
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mEdgeSwipe) {
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mEdgeSwipe) {
                    mEdgeSwipe = false;
                    float enough = event.getDevice().getMotionRange(MotionEvent.AXIS_X).getMax()
                            * mEdgeSwipeThreshold;
                    if (event.getX() > enough) {
                        if (mTable.hasFocus()) {
                            mTable.fling(mTable.getFocus());
                        } else if (mTable.hasSelection()) {
                            mTable.clearSelection();
                        }
                    }
                    return true;
                }
                break;
        }
        return false;
    }
}
