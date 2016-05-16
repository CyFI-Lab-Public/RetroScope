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

package com.android.gallery3d.filtershow.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.editors.Editor;

public class BasicSlider implements Control {
    private SeekBar mSeekBar;
    private ParameterInteger mParameter;
    Editor mEditor;

    @Override
    public void setUp(ViewGroup container, Parameter parameter, Editor editor) {
        container.removeAllViews();
        mEditor = editor;
        Context context = container.getContext();
        mParameter = (ParameterInteger) parameter;
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout lp = (LinearLayout) inflater.inflate(
                R.layout.filtershow_seekbar, container, true);
        mSeekBar = (SeekBar) lp.findViewById(R.id.primarySeekBar);
        mSeekBar.setVisibility(View.VISIBLE);
        updateUI();
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mParameter != null) {
                    mParameter.setValue(progress + mParameter.getMinimum());
                    mEditor.commitLocalRepresentation();

                }
            }
        });
    }

    @Override
    public View getTopView() {
        return mSeekBar;
    }

    @Override
    public void setPrameter(Parameter parameter) {
        mParameter = (ParameterInteger) parameter;
        if (mSeekBar != null) {
            updateUI();
        }
    }

    @Override
    public void updateUI() {
        mSeekBar.setMax(mParameter.getMaximum() - mParameter.getMinimum());
        mSeekBar.setProgress(mParameter.getValue() - mParameter.getMinimum());
    }
}
