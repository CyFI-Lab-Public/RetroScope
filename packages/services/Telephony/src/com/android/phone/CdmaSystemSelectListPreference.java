/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.TelephonyProperties;

public class CdmaSystemSelectListPreference extends ListPreference {

    private static final String LOG_TAG = "CdmaRoamingListPreference";
    private static final boolean DBG = false;

    private Phone mPhone;
    private MyHandler mHandler = new MyHandler();

    public CdmaSystemSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPhone = PhoneGlobals.getPhone();
        mHandler = new MyHandler();
        mPhone.queryCdmaRoamingPreference(
                mHandler.obtainMessage(MyHandler.MESSAGE_GET_ROAMING_PREFERENCE));
    }

    public CdmaSystemSelectListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void showDialog(Bundle state) {
        if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode do not show selection options
        } else {
            super.showDialog(state);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && (getValue() != null)) {
            int buttonCdmaRoamingMode = Integer.valueOf(getValue()).intValue();
            int settingsCdmaRoamingMode =
                    Settings.Global.getInt(mPhone.getContext().getContentResolver(),
                    Settings.Global.CDMA_ROAMING_MODE, Phone.CDMA_RM_HOME);
            if (buttonCdmaRoamingMode != settingsCdmaRoamingMode) {
                int statusCdmaRoamingMode;
                switch(buttonCdmaRoamingMode) {
                    case Phone.CDMA_RM_ANY:
                        statusCdmaRoamingMode = Phone.CDMA_RM_ANY;
                        break;
                    case Phone.CDMA_RM_HOME:
                    default:
                        statusCdmaRoamingMode = Phone.CDMA_RM_HOME;
                }
                //Set the Settings.Secure network mode
                Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        Settings.Global.CDMA_ROAMING_MODE,
                        buttonCdmaRoamingMode );
                //Set the roaming preference mode
                mPhone.setCdmaRoamingPreference(statusCdmaRoamingMode, mHandler
                        .obtainMessage(MyHandler.MESSAGE_SET_ROAMING_PREFERENCE));
            }
        } else {
            Log.d(LOG_TAG, String.format("onDialogClosed: positiveResult=%b value=%s -- do nothing",
                    positiveResult, getValue()));
        }
    }

    private class MyHandler extends Handler {

        static final int MESSAGE_GET_ROAMING_PREFERENCE = 0;
        static final int MESSAGE_SET_ROAMING_PREFERENCE = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GET_ROAMING_PREFERENCE:
                    handleQueryCdmaRoamingPreference(msg);
                    break;

                case MESSAGE_SET_ROAMING_PREFERENCE:
                    handleSetCdmaRoamingPreference(msg);
                    break;
            }
        }

        private void handleQueryCdmaRoamingPreference(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if (ar.exception == null) {
                int statusCdmaRoamingMode = ((int[])ar.result)[0];
                int settingsRoamingMode = Settings.Global.getInt(
                        mPhone.getContext().getContentResolver(),
                        Settings.Global.CDMA_ROAMING_MODE, Phone.CDMA_RM_HOME);
                //check that statusCdmaRoamingMode is from an accepted value
                if (statusCdmaRoamingMode == Phone.CDMA_RM_HOME ||
                        statusCdmaRoamingMode == Phone.CDMA_RM_ANY ) {
                    //check changes in statusCdmaRoamingMode and updates settingsRoamingMode
                    if (statusCdmaRoamingMode != settingsRoamingMode) {
                        settingsRoamingMode = statusCdmaRoamingMode;
                        //changes the Settings.Secure accordingly to statusCdmaRoamingMode
                        Settings.Global.putInt(
                                mPhone.getContext().getContentResolver(),
                                Settings.Global.CDMA_ROAMING_MODE,
                                settingsRoamingMode );
                    }
                    //changes the mButtonPreferredNetworkMode accordingly to modemNetworkMode
                    setValue(Integer.toString(statusCdmaRoamingMode));
                }
                else {
                    if(DBG) Log.i(LOG_TAG, "reset cdma roaming mode to default" );
                    resetCdmaRoamingModeToDefault();
                }
            }
        }

        private void handleSetCdmaRoamingPreference(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;

            if ((ar.exception == null) && (getValue() != null)) {
                int cdmaRoamingMode = Integer.valueOf(getValue()).intValue();
                Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        Settings.Global.CDMA_ROAMING_MODE,
                        cdmaRoamingMode );
            } else {
                mPhone.queryCdmaRoamingPreference(obtainMessage(MESSAGE_GET_ROAMING_PREFERENCE));
            }
        }

        private void resetCdmaRoamingModeToDefault() {
            //set the mButtonCdmaRoam
            setValue(Integer.toString(Phone.CDMA_RM_HOME));
            //set the Settings.System
            Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        Settings.Global.CDMA_ROAMING_MODE,
                        Phone.CDMA_RM_HOME );
            //Set the Status
            mPhone.setCdmaRoamingPreference(Phone.CDMA_RM_HOME,
                    obtainMessage(MyHandler.MESSAGE_SET_ROAMING_PREFERENCE));
        }
    }

}
