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

package com.android.gallery3d.filtershow.editors;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.BasicParameterInt;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.filters.FilterVignetteRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageVignette;

public class EditorVignette extends ParametricEditor {
    public static final int ID = R.id.vignetteEditor;
    private static final String LOGTAG = "EditorVignettePlanet";
    ImageVignette mImageVignette;

    private SeekBar mVignetteBar;
    private SeekBar mExposureBar;
    private SeekBar mSaturationBar;
    private SeekBar mContrastBar;
    private SeekBar mFalloffBar;


    private TextView mVignetteValue;
    private TextView mExposureValue;
    private TextView mSaturationValue;
    private TextView mContrastValue;
    private TextView mFalloffValue;

    private SwapButton mButton;
    private final Handler mHandler = new Handler();

    int[] mMenuStrings = {
            R.string.vignette_main,
            R.string.vignette_exposure,
            R.string.vignette_saturation,
            R.string.vignette_contrast,
            R.string.vignette_falloff,
    };

    String mCurrentlyEditing = null;


    public EditorVignette() {
        super(ID, R.layout.filtershow_vignette_editor, R.id.imageVignette);
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        mImageVignette = (ImageVignette) mImageShow;
        mImageVignette.setEditor(this);
    }

    @Override
    public void reflectCurrentFilter() {
        if (useCompact(mContext)) {
            super.reflectCurrentFilter();

            FilterRepresentation rep = getLocalRepresentation();
            if (rep != null && getLocalRepresentation() instanceof FilterVignetteRepresentation) {
                FilterVignetteRepresentation drawRep = (FilterVignetteRepresentation) rep;
                mImageVignette.setRepresentation(drawRep);
            }
            updateText();
            return;
        }
        mLocalRepresentation = null;
        if (getLocalRepresentation() != null
                && getLocalRepresentation() instanceof FilterVignetteRepresentation) {
            FilterVignetteRepresentation rep =
                    (FilterVignetteRepresentation) getLocalRepresentation();
            int min;
            int []mode = {
                    FilterVignetteRepresentation.MODE_VIGNETTE,
                    FilterVignetteRepresentation.MODE_EXPOSURE,
                    FilterVignetteRepresentation.MODE_SATURATION,
                    FilterVignetteRepresentation.MODE_CONTRAST,
                    FilterVignetteRepresentation.MODE_FALLOFF
            };
            SeekBar []sliders = {
                    mVignetteBar,
                    mExposureBar,
                    mSaturationBar,
                    mContrastBar,
                    mFalloffBar
            };
            TextView []label = {
                    mVignetteValue,
                    mExposureValue,
                    mSaturationValue,
                    mContrastValue,
                    mFalloffValue
            };
            for (int i = 0; i < mode.length; i++) {
                BasicParameterInt p = (BasicParameterInt) rep.getFilterParameter(mode[i]);
                int value = p.getValue();
                sliders[i].setMax(p.getMaximum() - p.getMinimum());
                sliders[i].setProgress(value - p.getMinimum());
                label[i].setText("" + value);
            }

            mImageVignette.setRepresentation(rep);
            String text = mContext.getString(rep.getTextId()).toUpperCase();
            mFilterTitle.setText(text);
            updateText();
        }
    }



    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || !(rep instanceof FilterVignetteRepresentation)) {
            return "";
        }
        FilterVignetteRepresentation csrep = (FilterVignetteRepresentation) rep;
        int mode = csrep.getParameterMode();
        String paramString;

        paramString = mContext.getString(mMenuStrings[mode]);

        int val = csrep.getCurrentParameter();
        return paramString + ((val > 0) ? " +" : " ") + val;
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        mButton = (SwapButton) accessoryViewList.findViewById(R.id.applyEffect);
        mButton.setText(mContext.getString(R.string.vignette_main));

        if (useCompact(mContext)) {
            final PopupMenu popupMenu = new PopupMenu(mImageShow.getActivity(), mButton);

            popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_vignette,
                    popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    selectMenuItem(item);
                    return true;
                }
            });
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    popupMenu.show();
                    ((FilterShowActivity)mContext).onShowMenu(popupMenu);
                }
            });
            mButton.setListener(this);

            FilterVignetteRepresentation csrep = getVignetteRep();
            String menuString = mContext.getString(mMenuStrings[0]);
            switchToMode(csrep, FilterVignetteRepresentation.MODE_VIGNETTE, menuString);
        } else {
            mButton.setText(mContext.getString(R.string.vignette_main));
        }
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        if (useCompact(mContext)) {
            super.setUtilityPanelUI(actionButton, editControl);
            return;
        }
        mActionButton = actionButton;
        mEditControl = editControl;
        mEditTitle.setCompoundDrawables(null, null, null, null);
        LinearLayout group = (LinearLayout) editControl;
        LayoutInflater inflater =
                (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout controls = (LinearLayout) inflater.inflate(
                R.layout.filtershow_vignette_controls, group, false);
        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controls.setLayoutParams(lp);
        group.removeAllViews();
        group.addView(controls);

        mVignetteBar = (SeekBar) controls.findViewById(R.id.mainVignetteSeekbar);
        mVignetteBar.setMax(200);
        mVignetteBar.setOnSeekBarChangeListener(this);
        mVignetteValue = (TextView) controls.findViewById(R.id.mainVignetteValue);
        mExposureBar = (SeekBar) controls.findViewById(R.id.exposureSeekBar);
        mExposureBar.setMax(200);
        mExposureBar.setOnSeekBarChangeListener(this);
        mExposureValue = (TextView) controls.findViewById(R.id.exposureValue);
        mSaturationBar = (SeekBar) controls.findViewById(R.id.saturationSeekBar);
        mSaturationBar.setMax(200);
        mSaturationBar.setOnSeekBarChangeListener(this);
        mSaturationValue = (TextView) controls.findViewById(R.id.saturationValue);
        mContrastBar = (SeekBar) controls.findViewById(R.id.contrastSeekBar);
        mContrastBar.setMax(200);
        mContrastBar.setOnSeekBarChangeListener(this);
        mContrastValue = (TextView) controls.findViewById(R.id.contrastValue);
        mFalloffBar = (SeekBar) controls.findViewById(R.id.falloffSeekBar);
        mFalloffBar.setMax(200);
        mFalloffBar.setOnSeekBarChangeListener(this);
        mFalloffValue = (TextView) controls.findViewById(R.id.falloffValue);
    }

    public int getParameterIndex(int id) {
        switch (id) {
            case R.id.editor_vignette_main:
                return FilterVignetteRepresentation.MODE_VIGNETTE;
            case R.id.editor_vignette_saturation:
                return FilterVignetteRepresentation.MODE_SATURATION;
            case R.id.editor_vignette_contrast:
                return FilterVignetteRepresentation.MODE_CONTRAST;
            case R.id.editor_vignette_exposure:
                return FilterVignetteRepresentation.MODE_EXPOSURE;
            case R.id.editor_vignette_falloff:
                return FilterVignetteRepresentation.MODE_FALLOFF;
        }
        return -1;
    }

    @Override
    public void detach() {
        if (mButton == null) {
            return;
        }
        mButton.setListener(null);
        mButton.setOnClickListener(null);
    }

    private void updateSeekBar(FilterVignetteRepresentation rep) {
        mControl.updateUI();
    }

    @Override
    protected Parameter getParameterToEdit(FilterRepresentation rep) {
        if (rep instanceof FilterVignetteRepresentation) {
            FilterVignetteRepresentation csrep = (FilterVignetteRepresentation) rep;
            Parameter param = csrep.getFilterParameter(csrep.getParameterMode());

            return param;
        }
        return null;
    }

    private FilterVignetteRepresentation getVignetteRep() {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep != null
                && rep instanceof FilterVignetteRepresentation) {
            FilterVignetteRepresentation csrep = (FilterVignetteRepresentation) rep;
            return csrep;
        }
        return null;
    }

    protected void selectMenuItem(MenuItem item) {
        if (getLocalRepresentation() != null
                && getLocalRepresentation() instanceof FilterVignetteRepresentation) {
            FilterVignetteRepresentation csrep =
                    (FilterVignetteRepresentation) getLocalRepresentation();

            switchToMode(csrep, getParameterIndex(item.getItemId()), item.getTitle().toString());
        }
    }

    protected void switchToMode(FilterVignetteRepresentation csrep, int mode, String title) {
        if (csrep == null) {
            return;
        }
        csrep.setParameterMode(mode);
        mCurrentlyEditing = title;
        mButton.setText(mCurrentlyEditing);
        {
            Parameter param = getParameterToEdit(csrep);

            control(param, mEditControl);
        }
        updateSeekBar(csrep);
        mView.invalidate();
    }

    @Override
    public void onProgressChanged(SeekBar sbar, int progress, boolean arg2) {
        FilterVignetteRepresentation rep = getVignetteRep();
        int value = progress;
        BasicParameterInt  p;
        switch (sbar.getId()) {
            case R.id.mainVignetteSeekbar:
                rep.setParameterMode(FilterVignetteRepresentation.MODE_VIGNETTE);
                p = rep.getFilterParameter(rep.getParameterMode());
                value += p.getMinimum();
                mVignetteValue.setText("" + value);
                break;
            case R.id.exposureSeekBar:
                rep.setParameterMode(FilterVignetteRepresentation.MODE_EXPOSURE);
                p = rep.getFilterParameter(rep.getParameterMode());
                value += p.getMinimum();
                mExposureValue.setText("" + value);
                break;
            case R.id.saturationSeekBar:
                rep.setParameterMode(FilterVignetteRepresentation.MODE_SATURATION);
                p = rep.getFilterParameter(rep.getParameterMode());
                value += p.getMinimum();
                mSaturationValue.setText("" + value);
                break;
            case R.id.contrastSeekBar:
                rep.setParameterMode(FilterVignetteRepresentation.MODE_CONTRAST);
                p = rep.getFilterParameter(rep.getParameterMode());
                value += p.getMinimum();
                mContrastValue.setText("" + value);
                break;
            case R.id.falloffSeekBar:
                rep.setParameterMode(FilterVignetteRepresentation.MODE_FALLOFF);
                p = rep.getFilterParameter(rep.getParameterMode());
                value += p.getMinimum();
                mFalloffValue.setText("" + value);
                break;
        }
        rep.setCurrentParameter(value);
        commitLocalRepresentation();
    }

    @Override
    public void swapLeft(MenuItem item) {
        super.swapLeft(item);
        mButton.setTranslationX(0);
        mButton.animate().translationX(mButton.getWidth()).setDuration(SwapButton.ANIM_DURATION);
        Runnable updateButton = new Runnable() {
            @Override
            public void run() {
                mButton.animate().cancel();
                mButton.setTranslationX(0);
            }
        };
        mHandler.postDelayed(updateButton, SwapButton.ANIM_DURATION);
        selectMenuItem(item);
    }

    @Override
    public void swapRight(MenuItem item) {
        super.swapRight(item);
        mButton.setTranslationX(0);
        mButton.animate().translationX(-mButton.getWidth()).setDuration(SwapButton.ANIM_DURATION);
        Runnable updateButton = new Runnable() {
            @Override
            public void run() {
                mButton.animate().cancel();
                mButton.setTranslationX(0);
            }
        };
        mHandler.postDelayed(updateButton, SwapButton.ANIM_DURATION);
        selectMenuItem(item);
    }
}
