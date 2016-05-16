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

package com.android.dreams.bummer;

import android.graphics.Color;
import android.service.dreams.Dream;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class Bummer extends Dream {
    public static final int MOVE = 1;

    public static final int DELAY = 1000; // ms

    public String message = "Bummer!";
    public int color = Color.WHITE;
    public float size = 20.0f;

    private FrameLayout mFrame;
    private BummerView mApology;

    @Override
    public void onStart() {
        mFrame = new FrameLayout(this);
        mFrame.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
                ));

        mApology = new BummerView(this);
        mApology.setText(message);
        mApology.setTextColor(color);
        mApology.setTextSize(size);
        mApology.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.stat_sys_warning, 0, 0,
                0);
        mApology.setCompoundDrawablePadding(8);
        mApology.setAlpha(0.5f);

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
                );
        mApology.setVisibility(View.INVISIBLE);
        mFrame.addView(mApology, lp);

        setContentView(mFrame);
    }

}
