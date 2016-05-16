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

package com.android.camera;

import android.content.Context;
import android.view.LayoutInflater;

import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.ListPrefSettingPopup;
import com.android.camera.ui.MoreSettingPopup;
import com.android.camera.ui.PieItem;
import com.android.camera.ui.PieItem.OnClickListener;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.TimeIntervalPopup;
import com.android.camera2.R;

public class VideoMenu extends PieController
        implements MoreSettingPopup.Listener,
        ListPrefSettingPopup.Listener,
        TimeIntervalPopup.Listener {

    private static String TAG = "CAM_VideoMenu";

    private VideoUI mUI;
    private String[] mOtherKeys;
    private AbstractSettingPopup mPopup;

    private static final int POPUP_NONE = 0;
    private static final int POPUP_FIRST_LEVEL = 1;
    private static final int POPUP_SECOND_LEVEL = 2;
    private int mPopupStatus;
    private CameraActivity mActivity;

    public VideoMenu(CameraActivity activity, VideoUI ui, PieRenderer pie) {
        super(activity, pie);
        mUI = ui;
        mActivity = activity;
    }


    public void initialize(PreferenceGroup group) {
        super.initialize(group);
        mPopup = null;
        mPopupStatus = POPUP_NONE;
        PieItem item = null;
        // white balance
        if (group.findPreference(CameraSettings.KEY_WHITE_BALANCE) != null) {
            item = makeItem(CameraSettings.KEY_WHITE_BALANCE);
            mRenderer.addItem(item);
        }
        // settings popup
        mOtherKeys = new String[] {
                CameraSettings.KEY_VIDEO_EFFECT,
                CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL,
                CameraSettings.KEY_VIDEO_QUALITY,
                CameraSettings.KEY_RECORD_LOCATION
        };
        item = makeItem(R.drawable.ic_settings_holo_light);
        item.setLabel(mActivity.getResources().getString(R.string.camera_menu_settings_label));
        item.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(PieItem item) {
                if (mPopup == null || mPopupStatus != POPUP_FIRST_LEVEL) {
                    initializePopup();
                    mPopupStatus = POPUP_FIRST_LEVEL;
                }
                mUI.showPopup(mPopup);
            }
        });
        mRenderer.addItem(item);
        // camera switcher
        if (group.findPreference(CameraSettings.KEY_CAMERA_ID) != null) {
            item = makeItem(R.drawable.ic_switch_back);
            IconListPreference lpref = (IconListPreference) group.findPreference(
                    CameraSettings.KEY_CAMERA_ID);
            item.setLabel(lpref.getLabel());
            item.setImageResource(mActivity,
                    ((IconListPreference) lpref).getIconIds()
                    [lpref.findIndexOfValue(lpref.getValue())]);

            final PieItem fitem = item;
            item.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(PieItem item) {
                    // Find the index of next camera.
                    ListPreference pref =
                            mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_ID);
                    if (pref != null) {
                        int index = pref.findIndexOfValue(pref.getValue());
                        CharSequence[] values = pref.getEntryValues();
                        index = (index + 1) % values.length;
                        int newCameraId = Integer.parseInt((String) values[index]);
                        fitem.setImageResource(mActivity,
                                ((IconListPreference) pref).getIconIds()[index]);
                        fitem.setLabel(pref.getLabel());
                        mListener.onCameraPickerClicked(newCameraId);
                    }
                }
            });
            mRenderer.addItem(item);
        }
        // flash
        if (group.findPreference(CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE) != null) {
            item = makeItem(CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE);
            mRenderer.addItem(item);
        }
    }

    @Override
    public void reloadPreferences() {
        super.reloadPreferences();
        if (mPopup != null) {
            mPopup.reloadPreference();
        }
    }

    @Override
    public void overrideSettings(final String ... keyvalues) {
        super.overrideSettings(keyvalues);
        if (mPopup == null || mPopupStatus != POPUP_FIRST_LEVEL) {
            mPopupStatus = POPUP_FIRST_LEVEL;
            initializePopup();
        }
        ((MoreSettingPopup) mPopup).overrideSettings(keyvalues);
    }

    @Override
    // Hit when an item in the second-level popup gets selected
    public void onListPrefChanged(ListPreference pref) {
        if (mPopup != null) {
            if (mPopupStatus == POPUP_SECOND_LEVEL) {
                mUI.dismissPopup(true);
            }
        }
        super.onSettingChanged(pref);
    }

    protected void initializePopup() {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        MoreSettingPopup popup = (MoreSettingPopup) inflater.inflate(
                R.layout.more_setting_popup, null, false);
        popup.setSettingChangedListener(this);
        popup.initialize(mPreferenceGroup, mOtherKeys);
        if (mActivity.isSecureCamera()) {
            // Prevent location preference from getting changed in secure camera mode
            popup.setPreferenceEnabled(CameraSettings.KEY_RECORD_LOCATION, false);
        }
        mPopup = popup;
    }

    public void popupDismissed(boolean topPopupOnly) {
        // if the 2nd level popup gets dismissed
        if (mPopupStatus == POPUP_SECOND_LEVEL) {
            initializePopup();
            mPopupStatus = POPUP_FIRST_LEVEL;
            if (topPopupOnly) mUI.showPopup(mPopup);
        }
    }

    @Override
    // Hit when an item in the first-level popup gets selected, then bring up
    // the second-level popup
    public void onPreferenceClicked(ListPreference pref) {
        if (mPopupStatus != POPUP_FIRST_LEVEL) return;

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        if (CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL.equals(pref.getKey())) {
            TimeIntervalPopup timeInterval = (TimeIntervalPopup) inflater.inflate(
                    R.layout.time_interval_popup, null, false);
            timeInterval.initialize((IconListPreference) pref);
            timeInterval.setSettingChangedListener(this);
            mUI.dismissPopup(true);
            mPopup = timeInterval;
        } else {
            ListPrefSettingPopup basic = (ListPrefSettingPopup) inflater.inflate(
                    R.layout.list_pref_setting_popup, null, false);
            basic.initialize(pref);
            basic.setSettingChangedListener(this);
            mUI.dismissPopup(true);
            mPopup = basic;
        }
        mUI.showPopup(mPopup);
        mPopupStatus = POPUP_SECOND_LEVEL;
    }
}
