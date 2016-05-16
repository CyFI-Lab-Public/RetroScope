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
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.controller.BasicParameterStyle;
import com.android.gallery3d.filtershow.controller.BitmapCaller;
import com.android.gallery3d.filtershow.controller.FilterView;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;
import com.android.gallery3d.filtershow.filters.FilterChanSatRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

public class EditorChanSat extends ParametricEditor implements OnSeekBarChangeListener, FilterView {
    public static final int ID = R.id.editorChanSat;
    private final String LOGTAG = "EditorGrunge";
    private SwapButton mButton;
    private final Handler mHandler = new Handler();

    private SeekBar mMainBar;
    private SeekBar mRedBar;
    private SeekBar mYellowBar;
    private SeekBar mGreenBar;
    private SeekBar mCyanBar;
    private SeekBar mBlueBar;
    private SeekBar mMagentaBar;
    private TextView mMainValue;
    private TextView mRedValue;
    private TextView mYellowValue;
    private TextView mGreenValue;
    private TextView mCyanValue;
    private TextView mBlueValue;
    private TextView mMagentaValue;

    int[] mMenuStrings = {
            R.string.editor_chan_sat_main,
            R.string.editor_chan_sat_red,
            R.string.editor_chan_sat_yellow,
            R.string.editor_chan_sat_green,
            R.string.editor_chan_sat_cyan,
            R.string.editor_chan_sat_blue,
            R.string.editor_chan_sat_magenta
    };

    String mCurrentlyEditing = null;

    public EditorChanSat() {
        super(ID, R.layout.filtershow_default_editor, R.id.basicEditor);
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep == null || !(rep instanceof FilterChanSatRepresentation)) {
            return "";
        }
        FilterChanSatRepresentation csrep = (FilterChanSatRepresentation) rep;
        int mode = csrep.getParameterMode();
        String paramString;

        paramString = mContext.getString(mMenuStrings[mode]);

        int val = csrep.getCurrentParameter();
        return paramString + ((val > 0) ? " +" : " ") + val;
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        mButton = (SwapButton) accessoryViewList.findViewById(R.id.applyEffect);
        mButton.setText(mContext.getString(R.string.editor_chan_sat_main));

        if (useCompact(mContext)) {
            final PopupMenu popupMenu = new PopupMenu(mImageShow.getActivity(), mButton);

            popupMenu.getMenuInflater().inflate(R.menu.filtershow_menu_chan_sat,
                    popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    selectMenuItem(item);
                    return true;
                }
            });
            mButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    popupMenu.show();
                    ((FilterShowActivity)mContext).onShowMenu(popupMenu);
                }
            });
            mButton.setListener(this);

            FilterChanSatRepresentation csrep = getChanSatRep();
            String menuString = mContext.getString(mMenuStrings[0]);
            switchToMode(csrep, FilterChanSatRepresentation.MODE_MASTER, menuString);
        } else {
            mButton.setText(mContext.getString(R.string.saturation));
        }
    }

    @Override
    public void reflectCurrentFilter() {
        if (useCompact(mContext)) {
            super.reflectCurrentFilter();
            updateText();
            return;
        }
        mLocalRepresentation = null;
        if (getLocalRepresentation() != null
                && getLocalRepresentation() instanceof FilterChanSatRepresentation) {
            FilterChanSatRepresentation rep =
                    (FilterChanSatRepresentation) getLocalRepresentation();
            int value = rep.getValue(FilterChanSatRepresentation.MODE_MASTER);
            mMainBar.setProgress(value + 100);
            mMainValue.setText("" + value);
            value = rep.getValue(FilterChanSatRepresentation.MODE_RED);
            mRedBar.setProgress(value + 100);
            mRedValue.setText("" + value);
            value = rep.getValue(FilterChanSatRepresentation.MODE_YELLOW);
            mYellowBar.setProgress(value + 100);
            mYellowValue.setText("" + value);
            value = rep.getValue(FilterChanSatRepresentation.MODE_GREEN);
            mGreenBar.setProgress(value + 100);
            mGreenValue.setText("" + value);
            value = rep.getValue(FilterChanSatRepresentation.MODE_CYAN);
            mCyanBar.setProgress(value + 100);
            mCyanValue.setText("" + value);
            value = rep.getValue(FilterChanSatRepresentation.MODE_BLUE);
            mBlueBar.setProgress(value + 100);
            mBlueValue.setText("" + value);
            value = rep.getValue(FilterChanSatRepresentation.MODE_MAGENTA);
            mMagentaBar.setProgress(value + 100);
            mMagentaValue.setText("" + value);
            String text = mContext.getString(rep.getTextId()).toUpperCase();
            mFilterTitle.setText(text);
            updateText();
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
                R.layout.filtershow_saturation_controls, group, false);
        ViewGroup.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controls.setLayoutParams(lp);
        group.removeAllViews();
        group.addView(controls);
        mMainBar = (SeekBar) controls.findViewById(R.id.mainSeekbar);
        mMainBar.setMax(200);
        mMainBar.setOnSeekBarChangeListener(this);
        mMainValue = (TextView) controls.findViewById(R.id.mainValue);
        mRedBar = (SeekBar) controls.findViewById(R.id.redSeekBar);
        mRedBar.setMax(200);
        mRedBar.setOnSeekBarChangeListener(this);
        mRedValue = (TextView) controls.findViewById(R.id.redValue);
        mYellowBar = (SeekBar) controls.findViewById(R.id.yellowSeekBar);
        mYellowBar.setMax(200);
        mYellowBar.setOnSeekBarChangeListener(this);
        mYellowValue = (TextView) controls.findViewById(R.id.yellowValue);
        mGreenBar = (SeekBar) controls.findViewById(R.id.greenSeekBar);
        mGreenBar.setMax(200);
        mGreenBar.setOnSeekBarChangeListener(this);
        mGreenValue = (TextView) controls.findViewById(R.id.greenValue);
        mCyanBar = (SeekBar) controls.findViewById(R.id.cyanSeekBar);
        mCyanBar.setMax(200);
        mCyanBar.setOnSeekBarChangeListener(this);
        mCyanValue = (TextView) controls.findViewById(R.id.cyanValue);
        mBlueBar = (SeekBar) controls.findViewById(R.id.blueSeekBar);
        mBlueBar.setMax(200);
        mBlueBar.setOnSeekBarChangeListener(this);
        mBlueValue = (TextView) controls.findViewById(R.id.blueValue);
        mMagentaBar = (SeekBar) controls.findViewById(R.id.magentaSeekBar);
        mMagentaBar.setMax(200);
        mMagentaBar.setOnSeekBarChangeListener(this);
        mMagentaValue = (TextView) controls.findViewById(R.id.magentaValue);
    }

    public int getParameterIndex(int id) {
        switch (id) {
            case R.id.editor_chan_sat_main:
                return FilterChanSatRepresentation.MODE_MASTER;
            case R.id.editor_chan_sat_red:
                return FilterChanSatRepresentation.MODE_RED;
            case R.id.editor_chan_sat_yellow:
                return FilterChanSatRepresentation.MODE_YELLOW;
            case R.id.editor_chan_sat_green:
                return FilterChanSatRepresentation.MODE_GREEN;
            case R.id.editor_chan_sat_cyan:
                return FilterChanSatRepresentation.MODE_CYAN;
            case R.id.editor_chan_sat_blue:
                return FilterChanSatRepresentation.MODE_BLUE;
            case R.id.editor_chan_sat_magenta:
                return FilterChanSatRepresentation.MODE_MAGENTA;
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

    private void updateSeekBar(FilterChanSatRepresentation rep) {
        mControl.updateUI();
    }

    @Override
    protected Parameter getParameterToEdit(FilterRepresentation rep) {
        if (rep instanceof FilterChanSatRepresentation) {
            FilterChanSatRepresentation csrep = (FilterChanSatRepresentation) rep;
            Parameter param = csrep.getFilterParameter(csrep.getParameterMode());
            if (param instanceof BasicParameterStyle) {
                param.setFilterView(EditorChanSat.this);
            }
            return param;
        }
        return null;
    }

    private FilterChanSatRepresentation getChanSatRep() {
        FilterRepresentation rep = getLocalRepresentation();
        if (rep != null
                && rep instanceof FilterChanSatRepresentation) {
            FilterChanSatRepresentation csrep = (FilterChanSatRepresentation) rep;
            return csrep;
        }
        return null;
    }

    @Override
    public void computeIcon(int n, BitmapCaller caller) {
        FilterChanSatRepresentation rep = getChanSatRep();
        if (rep == null) return;
        rep = (FilterChanSatRepresentation) rep.copy();
        ImagePreset preset = new ImagePreset();
        preset.addFilter(rep);
        Bitmap src = MasterImage.getImage().getThumbnailBitmap();
        caller.available(src);
    }

    protected void selectMenuItem(MenuItem item) {
        if (getLocalRepresentation() != null
                && getLocalRepresentation() instanceof FilterChanSatRepresentation) {
            FilterChanSatRepresentation csrep =
                    (FilterChanSatRepresentation) getLocalRepresentation();

            switchToMode(csrep, getParameterIndex(item.getItemId()), item.getTitle().toString());

        }
    }

    protected void switchToMode(FilterChanSatRepresentation csrep, int mode, String title) {
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
        FilterChanSatRepresentation rep = getChanSatRep();
        int value = progress - 100;
        switch (sbar.getId()) {
            case R.id.mainSeekbar:
                rep.setParameterMode(FilterChanSatRepresentation.MODE_MASTER);
                mMainValue.setText("" + value);
                break;
            case R.id.redSeekBar:
                rep.setParameterMode(FilterChanSatRepresentation.MODE_RED);
                mRedValue.setText("" + value);
                break;
            case R.id.yellowSeekBar:
                rep.setParameterMode(FilterChanSatRepresentation.MODE_YELLOW);
                mYellowValue.setText("" + value);
                break;
            case R.id.greenSeekBar:
                rep.setParameterMode(FilterChanSatRepresentation.MODE_GREEN);
                mGreenValue.setText("" + value);
                break;
            case R.id.cyanSeekBar:
                rep.setParameterMode(FilterChanSatRepresentation.MODE_CYAN);
                mCyanValue.setText("" + value);
                break;
            case R.id.blueSeekBar:
                rep.setParameterMode(FilterChanSatRepresentation.MODE_BLUE);
                mBlueValue.setText("" + value);
                break;
            case R.id.magentaSeekBar:
                rep.setParameterMode(FilterChanSatRepresentation.MODE_MAGENTA);
                mMagentaValue.setText("" + value);
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
