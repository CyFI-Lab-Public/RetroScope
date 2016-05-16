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

package com.android.camera;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.camera2.R;

/**
 * The on-screen indicators of the pie menu button. They show the camera
 * settings in the viewfinder.
 */
public class OnScreenIndicators {
    public static final String SCENE_MODE_HDR_PLUS = "hdr_plus";

    private final int[] mWBArray;
    private final View mOnScreenIndicators;
    private final ImageView mExposureIndicator;
    private final ImageView mFlashIndicator;
    private final ImageView mSceneIndicator;
    private final ImageView mLocationIndicator;
    private final ImageView mTimerIndicator;
    private final ImageView mWBIndicator;

    public OnScreenIndicators(Context ctx, View onScreenIndicatorsView) {
        TypedArray iconIds = ctx.getResources().obtainTypedArray(
                R.array.camera_wb_indicators);
        final int n = iconIds.length();
        mWBArray = new int[n];
        for (int i = 0; i < n; i++) {
            mWBArray[i] = iconIds.getResourceId(i, R.drawable.ic_indicator_wb_off);
        }
        mOnScreenIndicators = onScreenIndicatorsView;
        mExposureIndicator = (ImageView) onScreenIndicatorsView.findViewById(
                R.id.menu_exposure_indicator);
        mFlashIndicator = (ImageView) onScreenIndicatorsView.findViewById(
                R.id.menu_flash_indicator);
        mSceneIndicator = (ImageView) onScreenIndicatorsView.findViewById(
                R.id.menu_scenemode_indicator);
        mLocationIndicator = (ImageView) onScreenIndicatorsView.findViewById(
                R.id.menu_location_indicator);
        mTimerIndicator = (ImageView) onScreenIndicatorsView.findViewById(
                R.id.menu_timer_indicator);
        mWBIndicator = (ImageView) onScreenIndicatorsView.findViewById(
                R.id.menu_wb_indicator);
    }

    /**
     * Resets all indicators to show the default values.
     */
    public void resetToDefault() {
        updateExposureOnScreenIndicator(0);
        updateFlashOnScreenIndicator(Parameters.FLASH_MODE_OFF);
        updateSceneOnScreenIndicator(Parameters.SCENE_MODE_AUTO);
        updateWBIndicator(2);
        updateTimerIndicator(false);
        updateLocationIndicator(false);
    }

    /**
     * Sets the exposure indicator using exposure compensations step rounding.
     */
    public void updateExposureOnScreenIndicator(Camera.Parameters params, int value) {
        if (mExposureIndicator == null) {
            return;
        }
        float step = params.getExposureCompensationStep();
        value = Math.round(value * step);
        updateExposureOnScreenIndicator(value);
    }

    /**
     * Set the exposure indicator to the given value.
     *
     * @param value Value between -3 and 3. If outside this range, 0 is used by
     *            default.
     */
    public void updateExposureOnScreenIndicator(int value) {
        int id = 0;
        switch(value) {
        case -3:
            id = R.drawable.ic_indicator_ev_n3;
            break;
        case -2:
            id = R.drawable.ic_indicator_ev_n2;
            break;
        case -1:
            id = R.drawable.ic_indicator_ev_n1;
            break;
        case 0:
            id = R.drawable.ic_indicator_ev_0;
            break;
        case 1:
            id = R.drawable.ic_indicator_ev_p1;
            break;
        case 2:
            id = R.drawable.ic_indicator_ev_p2;
            break;
        case 3:
            id = R.drawable.ic_indicator_ev_p3;
            break;
        }
        mExposureIndicator.setImageResource(id);
    }

    public void updateWBIndicator(int wbIndex) {
        if (mWBIndicator == null) return;
        mWBIndicator.setImageResource(mWBArray[wbIndex]);
    }

    public void updateTimerIndicator(boolean on) {
        if (mTimerIndicator == null) return;
        mTimerIndicator.setImageResource(on ? R.drawable.ic_indicator_timer_on
                : R.drawable.ic_indicator_timer_off);
    }

    public void updateLocationIndicator(boolean on) {
        if (mLocationIndicator == null) return;
        mLocationIndicator.setImageResource(on ? R.drawable.ic_indicator_loc_on
                : R.drawable.ic_indicator_loc_off);
    }

    /**
     * Set the flash indicator to the given value.
     *
     * @param value One of Parameters.FLASH_MODE_OFF,
     *            Parameters.FLASH_MODE_AUTO, Parameters.FLASH_MODE_ON.
     */
    public void updateFlashOnScreenIndicator(String value) {
        if (mFlashIndicator == null) {
            return;
        }
        if (value == null || Parameters.FLASH_MODE_OFF.equals(value)) {
            mFlashIndicator.setImageResource(R.drawable.ic_indicator_flash_off);
        } else {
            if (Parameters.FLASH_MODE_AUTO.equals(value)) {
                mFlashIndicator.setImageResource(R.drawable.ic_indicator_flash_auto);
            } else if (Parameters.FLASH_MODE_ON.equals(value)
                    || Parameters.FLASH_MODE_TORCH.equals(value)) {
                mFlashIndicator.setImageResource(R.drawable.ic_indicator_flash_on);
            } else {
                mFlashIndicator.setImageResource(R.drawable.ic_indicator_flash_off);
            }
        }
    }

    /**
     * Set the scene indicator depending on the given scene mode.
     *
     * @param value the current Parameters.SCENE_MODE_* value or
     *            {@link #SCENE_MODE_HDR_PLUS}.
     */
    public void updateSceneOnScreenIndicator(String value) {
        if (mSceneIndicator == null) {
            return;
        }

        if (SCENE_MODE_HDR_PLUS.equals(value)) {
            mSceneIndicator.setImageResource(R.drawable.ic_indicator_hdr_plus_on);
        } else if ((value == null) || Parameters.SCENE_MODE_AUTO.equals(value)) {
            mSceneIndicator.setImageResource(R.drawable.ic_indicator_sce_off);
        } else if (Parameters.SCENE_MODE_HDR.equals(value)) {
            mSceneIndicator.setImageResource(R.drawable.ic_indicator_sce_hdr);
        } else {
            mSceneIndicator.setImageResource(R.drawable.ic_indicator_sce_on);
        }
    }

    /**
     * Sets the visibility of all indicators.
     *
     * @param visibility View.VISIBLE, View.GONE etc.
     */
    public void setVisibility(int visibility) {
        mOnScreenIndicators.setVisibility(visibility);
    }
}
