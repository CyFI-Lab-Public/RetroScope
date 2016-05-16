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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;

public class Test5DetailFragment extends Fragment {

    public static final String ARG_ITEM_ID = "item_id";

    class PointerEvent {
        int startX;
        int startY;
        int endX;
        int endY;
    }

    private final PointerEvent mPointerEvent = new PointerEvent();

    public Test5DetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View rootView = inflater.inflate(R.layout.test5_detail_fragment, container, false);

        // Set the content description for the following
        Spinner spinner = (Spinner) rootView.findViewById(R.id.test_5_spinner);
        spinner.setContentDescription("Spinner");
        ImageButton imageButton = (ImageButton) rootView.findViewById(R.id.test_5_imageButton);
        imageButton.setContentDescription("Image button");

        // Each time this view is displayed, reset the following states
        SeekBar seekBar = (SeekBar) rootView.findViewById(R.id.test_5_seekBar);
        seekBar.setProgress(50);
        seekBar.setContentDescription("Progress is 50 %");
        CheckBox checkbox = (CheckBox) rootView.findViewById(R.id.test_5_checkBox);
        checkbox.setChecked(false);

        // Register click event handlers for the following
        Button button = (Button) rootView.findViewById(R.id.test_5_button1);
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // we want an artificial crash
                throw new RuntimeException("Artificial crash to test UiWatcher");
            }
        });

        imageButton.setOnTouchListener(new ImageButton.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    resetTouchResults();
                    collectStartAction(event, v);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    collectEndAction(event, v);
                    displayTouchResults();
                }
                return false;
            }
        });

        return rootView;
    }

    private void displayTouchResults() {
        StringBuilder output = new StringBuilder();

        output.append(String.format("%d,%d:%d,%d\n",
                mPointerEvent.startX, mPointerEvent.startY, mPointerEvent.endX,
                mPointerEvent.endY));

        // display the submitted text
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.drag_item_touch_dialog_title);
        builder.setPositiveButton(R.string.OK, null);
        builder.setMessage(output.toString());
        AlertDialog diag = builder.create();
        diag.show();
    }

    /**
     * Clears all collected pointer results
     */
    private void resetTouchResults() {
         mPointerEvent.startX = mPointerEvent.startY =
                    mPointerEvent.endX = mPointerEvent.endY = -1;
    }

    /**
     * Collects pointer touch information converting from relative to absolute before
     * storing it as starting touch coordinates.
     *
     * @param event
     * @param view
     * @param pointerIndex
     */
    private void collectStartAction(MotionEvent event, View view) {
        int offsetInScreen[] = new int[2];
        view.getLocationOnScreen(offsetInScreen);
        mPointerEvent.startX = (int)(event.getX() + offsetInScreen[0]);
        mPointerEvent.startY = (int)(event.getY() + offsetInScreen[1]);
    }

    /**
     * Collects pointer touch information converting from relative to absolute before
     * storing it as ending touch coordinates.
     *
     * @param event
     * @param view
     * @param pointerIndex
     */
    private void collectEndAction(MotionEvent event, View view) {
        int offsetInScreen[] = new int[2];
        view.getLocationOnScreen(offsetInScreen);
        mPointerEvent.endX = (int)(event.getX() + offsetInScreen[0]);
        mPointerEvent.endY = (int)(event.getY() + offsetInScreen[1]);
    }
}
