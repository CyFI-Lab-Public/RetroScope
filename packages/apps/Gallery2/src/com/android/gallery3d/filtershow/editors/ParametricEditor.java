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
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.ActionSlider;
import com.android.gallery3d.filtershow.controller.BasicSlider;
import com.android.gallery3d.filtershow.controller.ColorChooser;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.controller.Parameter;
import com.android.gallery3d.filtershow.controller.ParameterActionAndInt;
import com.android.gallery3d.filtershow.controller.ParameterBrightness;
import com.android.gallery3d.filtershow.controller.ParameterColor;
import com.android.gallery3d.filtershow.controller.ParameterHue;
import com.android.gallery3d.filtershow.controller.ParameterInteger;
import com.android.gallery3d.filtershow.controller.ParameterOpacity;
import com.android.gallery3d.filtershow.controller.ParameterSaturation;
import com.android.gallery3d.filtershow.controller.ParameterStyles;
import com.android.gallery3d.filtershow.controller.SliderBrightness;
import com.android.gallery3d.filtershow.controller.SliderHue;
import com.android.gallery3d.filtershow.controller.SliderOpacity;
import com.android.gallery3d.filtershow.controller.SliderSaturation;
import com.android.gallery3d.filtershow.controller.StyleChooser;
import com.android.gallery3d.filtershow.controller.TitledSlider;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class ParametricEditor extends Editor {
    private int mLayoutID;
    private int mViewID;
    public static int ID = R.id.editorParametric;
    private final String LOGTAG = "ParametricEditor";
    protected Control mControl;
    public static final int MINIMUM_WIDTH = 600;
    public static final int MINIMUM_HEIGHT = 800;
    View mActionButton;
    View mEditControl;
    static HashMap<String, Class> portraitMap = new HashMap<String, Class>();
    static HashMap<String, Class> landscapeMap = new HashMap<String, Class>();
    static {
        portraitMap.put(ParameterSaturation.sParameterType, SliderSaturation.class);
        landscapeMap.put(ParameterSaturation.sParameterType, SliderSaturation.class);
        portraitMap.put(ParameterHue.sParameterType, SliderHue.class);
        landscapeMap.put(ParameterHue.sParameterType, SliderHue.class);
        portraitMap.put(ParameterOpacity.sParameterType, SliderOpacity.class);
        landscapeMap.put(ParameterOpacity.sParameterType, SliderOpacity.class);
        portraitMap.put(ParameterBrightness.sParameterType, SliderBrightness.class);
        landscapeMap.put(ParameterBrightness.sParameterType, SliderBrightness.class);
        portraitMap.put(ParameterColor.sParameterType, ColorChooser.class);
        landscapeMap.put(ParameterColor.sParameterType, ColorChooser.class);

        portraitMap.put(ParameterInteger.sParameterType, BasicSlider.class);
        landscapeMap.put(ParameterInteger.sParameterType, TitledSlider.class);
        portraitMap.put(ParameterActionAndInt.sParameterType, ActionSlider.class);
        landscapeMap.put(ParameterActionAndInt.sParameterType, ActionSlider.class);
        portraitMap.put(ParameterStyles.sParameterType, StyleChooser.class);
        landscapeMap.put(ParameterStyles.sParameterType, StyleChooser.class);
    }

    static Constructor getConstructor(Class cl) {
        try {
            return cl.getConstructor(Context.class, ViewGroup.class);
        } catch (Exception e) {
            return null;
        }
    }

    public ParametricEditor() {
        super(ID);
    }

    protected ParametricEditor(int id) {
        super(id);
    }

    protected ParametricEditor(int id, int layoutID, int viewID) {
        super(id);
        mLayoutID = layoutID;
        mViewID = viewID;
    }

    @Override
    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        String apply = "";

        if (mShowParameter == SHOW_VALUE_INT & useCompact(context)) {
           if (getLocalRepresentation() instanceof FilterBasicRepresentation) {
            FilterBasicRepresentation interval = (FilterBasicRepresentation) getLocalRepresentation();
                apply += " " + effectName.toUpperCase() + " " + interval.getStateRepresentation();
           } else {
                apply += " " + effectName.toUpperCase() + " " + parameterValue;
           }
        } else {
            apply += " " + effectName.toUpperCase();
        }
        return apply;
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        unpack(mViewID, mLayoutID);
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        if (getLocalRepresentation() != null
                && getLocalRepresentation() instanceof FilterBasicRepresentation) {
            FilterBasicRepresentation interval = (FilterBasicRepresentation) getLocalRepresentation();
            mControl.setPrameter(interval);
        }
    }

    @Override
    public Control[] getControls() {
        BasicSlider slider = new BasicSlider();
        return new Control[] {
                slider
        };
    }

    protected static boolean useCompact(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
    }

    protected Parameter getParameterToEdit(FilterRepresentation rep) {
        if (this instanceof Parameter) {
            return (Parameter) this;
        } else if (rep instanceof Parameter) {
            return ((Parameter) rep);
        }
        return null;
    }

    @Override
    public void setUtilityPanelUI(View actionButton, View editControl) {
        mActionButton = actionButton;
        mEditControl = editControl;
        FilterRepresentation rep = getLocalRepresentation();
        Parameter param = getParameterToEdit(rep);
        if (param != null) {
            control(param, editControl);
        } else {
            mSeekBar = new SeekBar(editControl.getContext());
            LayoutParams lp = new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            mSeekBar.setLayoutParams(lp);
            ((LinearLayout) editControl).addView(mSeekBar);
            mSeekBar.setOnSeekBarChangeListener(this);
        }
    }

    protected void control(Parameter p, View editControl) {
        String pType = p.getParameterType();
        Context context = editControl.getContext();
        Class c = ((useCompact(context)) ? portraitMap : landscapeMap).get(pType);

        if (c != null) {
            try {
                mControl = (Control) c.newInstance();
                p.setController(mControl);
                mControl.setUp((ViewGroup) editControl, p, this);
            } catch (Exception e) {
                Log.e(LOGTAG, "Error in loading Control ", e);
            }
        } else {
            Log.e(LOGTAG, "Unable to find class for " + pType);
            for (String string : portraitMap.keySet()) {
                Log.e(LOGTAG, "for " + string + " use " + portraitMap.get(string));
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar sbar, int progress, boolean arg2) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
    }
}
