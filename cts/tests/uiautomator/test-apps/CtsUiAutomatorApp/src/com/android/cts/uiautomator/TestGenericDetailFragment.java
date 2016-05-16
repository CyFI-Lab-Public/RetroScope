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

package com.android.cts.uiautomator;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TestGenericDetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";
    TestItems.TestItem mItem;

    private class PointerEvent {
        int startX;
        int startY;
        int endX;
        int endY;
    }

    private final PointerEvent[] mPointerEvents = new PointerEvent[10];

    public TestGenericDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItem = TestItems.getTest(getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.test_results_detail_fragment, container, false);
        if (mItem != null) {
            ((TextView) rootView.findViewById(R.id.testResultsTextView)).setText(mItem.mName);
        }

        // listen to touch events to verify the multiPointerGesture APIs
        // Since API Level 18
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch(event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        // Reset any collected touch coordinate results on the primary touch down
                        resetTouchResults();
                        // collect this event
                        collectStartAction(event, v, 0);
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        // collect this event
                        collectStartAction(event, v, getPointerIndex(event));
                        break;

                    case MotionEvent.ACTION_POINTER_UP:
                        // collect this event
                        collectEndAction(event, v, getPointerIndex(event));
                        break;

                    case MotionEvent.ACTION_UP:
                        // collect this event
                        collectEndAction(event, v, 0);
                        // on the primary touch up display results collected for all pointers
                        displayTouchResults();
                        break;
                }
                return true;
            }
        });

        return rootView;
    }

    /**
     * Displays collected results from all pointers into a dialog view in the following
     * format: "startX,startY:endX,endY" where each line represent data for a pointer if
     * multiple pointers (fingers) were detected.
     */
    private void displayTouchResults() {
        StringBuilder output = new StringBuilder();
        for (int x = 0; x < mPointerEvents.length; x++) {
            if (mPointerEvents[x].startX == -1)
                break;

            output.append(String.format("%d,%d:%d,%d\n",
                    mPointerEvents[x].startX, mPointerEvents[x].startY, mPointerEvents[x].endX,
                    mPointerEvents[x].endY));
        }

        // display the submitted text
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.generic_item_touch_dialog_title);
        builder.setPositiveButton(R.string.OK, null);
        builder.setMessage(output.toString());
        AlertDialog diag = builder.create();
        diag.show();
    }

    /**
     * Clears all collected pointer results
     */
    private void resetTouchResults() {
        for (int x = 0; x < mPointerEvents.length; x++) {
            if (mPointerEvents[x] == null)
                mPointerEvents[x] = new PointerEvent();
            mPointerEvents[x].startX = mPointerEvents[x].startY =
                    mPointerEvents[x].endX = mPointerEvents[x].endY = -1;
        }
    }

    /**
     * Collects pointer touch information converting from relative to absolute before
     * storing it as starting touch coordinates.
     *
     * @param event
     * @param view
     * @param pointerIndex
     */
    private void collectStartAction(MotionEvent event, View view, int pointerIndex) {
        int offsetInScreen[] = new int[2];
        view.getLocationOnScreen(offsetInScreen);
        mPointerEvents[getPointerId(event)].startX =
                (int)(event.getX(pointerIndex) + offsetInScreen[0]);
        mPointerEvents[getPointerId(event)].startY =
                (int)(event.getY(pointerIndex) + offsetInScreen[1]);
    }

    /**
     * Collects pointer touch information converting from relative to absolute before
     * storing it as ending touch coordinates.
     *
     * @param event
     * @param view
     * @param pointerIndex
     */
    private void collectEndAction(MotionEvent event, View view, int pointerIndex) {
        int offsetInScreen[] = new int[2];
        view.getLocationOnScreen(offsetInScreen);
        mPointerEvents[getPointerId(event)].endX =
                (int)(event.getX(pointerIndex) + offsetInScreen[0]);
        mPointerEvents[getPointerId(event)].endY =
                (int)(event.getY(pointerIndex) + offsetInScreen[1]);
    }

    private int getPointerId(MotionEvent event) {
        return event.getPointerId(getPointerIndex(event));
    }

    private int getPointerIndex(MotionEvent event) {
        return ((event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT);
    }
}
