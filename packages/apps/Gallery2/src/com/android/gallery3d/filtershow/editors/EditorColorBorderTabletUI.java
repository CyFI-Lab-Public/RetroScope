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

package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.colorpicker.ColorCompareView;
import com.android.gallery3d.filtershow.colorpicker.ColorHueView;
import com.android.gallery3d.filtershow.colorpicker.ColorListener;
import com.android.gallery3d.filtershow.colorpicker.ColorOpacityView;
import com.android.gallery3d.filtershow.colorpicker.ColorSVRectView;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import com.android.gallery3d.filtershow.filters.FilterColorBorderRepresentation;

import java.util.Arrays;

public class EditorColorBorderTabletUI {
    private EditorColorBorder mEditorDraw;
    private static int sIconDim = 120;
    private int mSelectedColorButton;
    private FilterColorBorderRepresentation mRep;
    private Button[] mColorButton;
    private ColorHueView mHueView;
    private ColorSVRectView mSatValView;
    private ColorOpacityView mOpacityView;
    private ColorCompareView mColorCompareView;

    private int[] mBasColors;
    private int mSelected;
    private int mTransparent;
    private SeekBar mCBSizeSeekBar;
    private SeekBar mCBCornerSizeSeekBar;
    TextView mCBCornerSizeValue;
    TextView mCBSizeValue;

    private int[] ids = {
            R.id.draw_color_button01,
            R.id.draw_color_button02,
            R.id.draw_color_button03,
            R.id.draw_color_button04,
            R.id.draw_color_button05,
    };

    public void setColorBorderRepresentation(FilterColorBorderRepresentation rep) {
        mRep = rep;
        BasicParameterInt size;
        size = (BasicParameterInt) mRep.getParam(FilterColorBorderRepresentation.PARAM_SIZE);
        mCBSizeSeekBar.setMax(size.getMaximum() - size.getMinimum());
        mCBSizeSeekBar.setProgress(size.getValue());

        BasicParameterInt radius;
        radius = (BasicParameterInt) mRep.getParam(FilterColorBorderRepresentation.PARAM_RADIUS);
        mCBCornerSizeSeekBar.setMax(radius.getMaximum() - radius.getMinimum());
        mCBCornerSizeSeekBar.setProgress(radius.getValue());

        ParameterColor color;
        color = (ParameterColor) mRep.getParam(FilterColorBorderRepresentation.PARAM_COLOR);
        mBasColors = color.getColorPalette();
        color.setValue(mBasColors[mSelectedColorButton]);
    }

    public EditorColorBorderTabletUI(EditorColorBorder editorDraw, Context context, View base) {
        mEditorDraw = editorDraw;
        mBasColors = editorDraw.mBasColors;
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout lp = (LinearLayout) inflater.inflate(
                R.layout.filtershow_color_border_ui, (ViewGroup) base, true);

        Resources res = context.getResources();
        sIconDim = res.getDimensionPixelSize(R.dimen.draw_style_icon_dim);
        LinearLayout buttonContainer = (LinearLayout) lp.findViewById(R.id.listStyles);

        mCBCornerSizeSeekBar = (SeekBar) lp.findViewById(R.id.colorBorderCornerSizeSeekBar);
        mCBCornerSizeValue = (TextView) lp.findViewById(R.id.colorBorderCornerValue);
        mCBSizeSeekBar = (SeekBar) lp.findViewById(R.id.colorBorderSizeSeekBar);

        mCBSizeValue = (TextView) lp.findViewById(R.id.colorBorderSizeValue);
        setupCBSizeSeekBar(lp);
        setupCBCornerSizeSeekBar(lp);
        setupColor(lp, res);
    }

    private void setupCBSizeSeekBar(LinearLayout lp) {
        mCBSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int type = FilterColorBorderRepresentation.PARAM_SIZE;
                BasicParameterInt size = (BasicParameterInt) mRep.getParam(type);
                size.setValue(progress + size.getMinimum());
                mCBSizeValue.setText(Integer.toString(size.getValue()));
                mEditorDraw.commitLocalRepresentation();
            }
        });

    }

    private void setupCBCornerSizeSeekBar(LinearLayout lp) {
        mCBCornerSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int type = FilterColorBorderRepresentation.PARAM_RADIUS;
                BasicParameterInt size = (BasicParameterInt) mRep.getParam(type);
                size.setValue(progress + size.getMinimum());
                mCBCornerSizeValue.setText(size.getValue() + "");
                mEditorDraw.commitLocalRepresentation();
            }
        });

    }

    private void setupColor(LinearLayout lp, Resources res) {
        final LinearLayout ctls = (LinearLayout) lp.findViewById(R.id.controls);
        final LinearLayout pick = (LinearLayout) lp.findViewById(R.id.colorPicker);
        Button b = (Button) lp.findViewById(R.id.draw_color_popupbutton);
        b.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                boolean b = ctls.getVisibility() == View.VISIBLE;
                ctls.setVisibility((b) ? View.GONE : View.VISIBLE);
                pick.setVisibility((!b) ? View.GONE : View.VISIBLE);
            }
        }
        );

        mTransparent = res.getColor(R.color.color_chooser_unslected_border);
        mSelected = res.getColor(R.color.color_chooser_slected_border);

        mColorButton = new Button[ids.length];
        for (int i = 0; i < ids.length; i++) {
            mColorButton[i] = (Button) lp.findViewById(ids[i]);
            float[] hsvo = new float[4];
            Color.colorToHSV(mBasColors[i], hsvo);
            hsvo[3] = (0xFF & (mBasColors[i] >> 24)) / (float) 255;
            mColorButton[i].setTag(hsvo);
            GradientDrawable sd = ((GradientDrawable) mColorButton[i].getBackground());
            sd.setColor(mBasColors[i]);
            sd.setStroke(3, (0 == i) ? mSelected : mTransparent);
            final int buttonNo = i;
            mColorButton[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    mSelectedColorButton = buttonNo;
                    float[] hsvo = Arrays.copyOf((float[]) mColorButton[buttonNo].getTag(), 4);
                    resetBorders();
                    if (mRep == null) {
                        return;
                    }
                    int type = FilterColorBorderRepresentation.PARAM_COLOR;
                    ParameterColor pram = (ParameterColor) mRep.getParam(type);
                    pram.setValue(mBasColors[mSelectedColorButton]);
                    mEditorDraw.commitLocalRepresentation();
                    mHueView.setColor(hsvo);
                    mSatValView.setColor(hsvo);
                    mOpacityView.setColor(hsvo);
                    mColorCompareView.setOrigColor(hsvo);

                }
            });
        }
        mHueView = (ColorHueView) lp.findViewById(R.id.ColorHueView);
        mSatValView = (ColorSVRectView) lp.findViewById(R.id.colorRectView);
        mOpacityView = (ColorOpacityView) lp.findViewById(R.id.colorOpacityView);
        mColorCompareView = (ColorCompareView) lp.findViewById(R.id.btnSelect);

        float[] hsvo = new float[4];
        Color.colorToHSV(mBasColors[0], hsvo);
        hsvo[3] = (0xFF & (mBasColors[0] >> 24)) / (float) 255;
        mColorCompareView.setOrigColor(hsvo);

        ColorListener[] colorViews = {mHueView, mSatValView, mOpacityView, mColorCompareView};
        for (int i = 0; i < colorViews.length; i++) {
            colorViews[i].setColor(hsvo);
            for (int j = 0; j < colorViews.length; j++) {
                if (i == j) {
                    continue;
                }
                colorViews[i].addColorListener(colorViews[j]);
            }
        }

        ColorListener colorListener = new ColorListener() {
            @Override
            public void setColor(float[] hsvo) {
                int color = Color.HSVToColor((int) (hsvo[3] * 255), hsvo);
                Button b = mColorButton[mSelectedColorButton];
                float[] f = (float[]) b.getTag();
                System.arraycopy(hsvo, 0, f, 0, 4);
                mBasColors[mSelectedColorButton] = color;
                GradientDrawable sd = ((GradientDrawable) b.getBackground());
                sd.setColor(color);
                resetBorders();
                ParameterColor pram;
                pram = (ParameterColor) mRep.getParam(FilterColorBorderRepresentation.PARAM_COLOR);
                pram.setValue(color);
                mEditorDraw.commitLocalRepresentation();
            }
            @Override
            public void addColorListener(ColorListener l) {
            }
        };

        for (int i = 0; i < colorViews.length; i++) {
            colorViews[i].addColorListener(colorListener);
        }
    }

    private void resetBorders() {
        for (int i = 0; i < ids.length; i++) {
            final Button button = mColorButton[i];
            GradientDrawable sd = ((GradientDrawable) button.getBackground());
            sd.setColor(mBasColors[i]);
            sd.setStroke(3, (mSelectedColorButton == i) ? mSelected : mTransparent);
        }
    }
}
