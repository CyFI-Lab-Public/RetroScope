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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.RILConstants;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


/**
 * List of Phone-specific settings screens.
 */
public class CellBroadcastSms extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener{
    // debug data
    private static final String LOG_TAG = "CellBroadcastSms";
    private static final boolean DBG = false;

    //String keys for preference lookup
    private static final String BUTTON_ENABLE_DISABLE_BC_SMS_KEY =
        "button_enable_disable_cell_bc_sms";
    private static final String LIST_LANGUAGE_KEY =
        "list_language";
    private static final String BUTTON_EMERGENCY_BROADCAST_KEY =
        "button_emergency_broadcast";
    private static final String BUTTON_ADMINISTRATIVE_KEY =
        "button_administrative";
    private static final String BUTTON_MAINTENANCE_KEY =
        "button_maintenance";
    private static final String BUTTON_LOCAL_WEATHER_KEY =
        "button_local_weather";
    private static final String BUTTON_ATR_KEY =
        "button_atr";
    private static final String BUTTON_LAFS_KEY =
        "button_lafs";
    private static final String BUTTON_RESTAURANTS_KEY =
        "button_restaurants";
    private static final String BUTTON_LODGINGS_KEY =
        "button_lodgings";
    private static final String BUTTON_RETAIL_DIRECTORY_KEY =
        "button_retail_directory";
    private static final String BUTTON_ADVERTISEMENTS_KEY =
        "button_advertisements";
    private static final String BUTTON_STOCK_QUOTES_KEY =
        "button_stock_quotes";
    private static final String BUTTON_EO_KEY =
        "button_eo";
    private static final String BUTTON_MHH_KEY =
        "button_mhh";
    private static final String BUTTON_TECHNOLOGY_NEWS_KEY =
        "button_technology_news";
    private static final String BUTTON_MULTI_CATEGORY_KEY =
        "button_multi_category";

    private static final String BUTTON_LOCAL_GENERAL_NEWS_KEY =
        "button_local_general_news";
    private static final String BUTTON_REGIONAL_GENERAL_NEWS_KEY =
        "button_regional_general_news";
    private static final String BUTTON_NATIONAL_GENERAL_NEWS_KEY =
        "button_national_general_news";
    private static final String BUTTON_INTERNATIONAL_GENERAL_NEWS_KEY =
        "button_international_general_news";

    private static final String BUTTON_LOCAL_BF_NEWS_KEY =
        "button_local_bf_news";
    private static final String BUTTON_REGIONAL_BF_NEWS_KEY =
        "button_regional_bf_news";
    private static final String BUTTON_NATIONAL_BF_NEWS_KEY =
        "button_national_bf_news";
    private static final String BUTTON_INTERNATIONAL_BF_NEWS_KEY =
        "button_international_bf_news";

    private static final String BUTTON_LOCAL_SPORTS_NEWS_KEY =
        "button_local_sports_news";
    private static final String BUTTON_REGIONAL_SPORTS_NEWS_KEY =
        "button_regional_sports_news";
    private static final String BUTTON_NATIONAL_SPORTS_NEWS_KEY =
        "button_national_sports_news";
    private static final String BUTTON_INTERNATIONAL_SPORTS_NEWS_KEY =
        "button_international_sports_news";

    private static final String BUTTON_LOCAL_ENTERTAINMENT_NEWS_KEY =
        "button_local_entertainment_news";
    private static final String BUTTON_REGIONAL_ENTERTAINMENT_NEWS_KEY =
        "button_regional_entertainment_news";
    private static final String BUTTON_NATIONAL_ENTERTAINMENT_NEWS_KEY =
        "button_national_entertainment_news";
    private static final String BUTTON_INTERNATIONAL_ENTERTAINMENT_NEWS_KEY =
        "button_international_entertainment_news";

    //Class constants
    //These values are related to the C structs. See the comments in  method
    //setCbSmsConfig for more information.
    private static final int NO_OF_SERVICE_CATEGORIES = 31;
    private static final int NO_OF_INTS_STRUCT_1 = 3;
    private static final int MAX_LENGTH_RESULT = NO_OF_SERVICE_CATEGORIES * NO_OF_INTS_STRUCT_1 + 1;
    //Handler keys
    private static final int MESSAGE_ACTIVATE_CB_SMS = 1;
    private static final int MESSAGE_GET_CB_SMS_CONFIG = 2;
    private static final int MESSAGE_SET_CB_SMS_CONFIG = 3;

    //UI objects
    private CheckBoxPreference mButtonBcSms;

    private ListPreference mListLanguage;

    private CheckBoxPreference mButtonEmergencyBroadcast;
    private CheckBoxPreference mButtonAdministrative;
    private CheckBoxPreference mButtonMaintenance;
    private CheckBoxPreference mButtonLocalWeather;
    private CheckBoxPreference mButtonAtr;
    private CheckBoxPreference mButtonLafs;
    private CheckBoxPreference mButtonRestaurants;
    private CheckBoxPreference mButtonLodgings;
    private CheckBoxPreference mButtonRetailDirectory;
    private CheckBoxPreference mButtonAdvertisements;
    private CheckBoxPreference mButtonStockQuotes;
    private CheckBoxPreference mButtonEo;
    private CheckBoxPreference mButtonMhh;
    private CheckBoxPreference mButtonTechnologyNews;
    private CheckBoxPreference mButtonMultiCategory;

    private CheckBoxPreference mButtonLocal1;
    private CheckBoxPreference mButtonRegional1;
    private CheckBoxPreference mButtonNational1;
    private CheckBoxPreference mButtonInternational1;

    private CheckBoxPreference mButtonLocal2;
    private CheckBoxPreference mButtonRegional2;
    private CheckBoxPreference mButtonNational2;
    private CheckBoxPreference mButtonInternational2;

    private CheckBoxPreference mButtonLocal3;
    private CheckBoxPreference mButtonRegional3;
    private CheckBoxPreference mButtonNational3;
    private CheckBoxPreference mButtonInternational3;

    private CheckBoxPreference mButtonLocal4;
    private CheckBoxPreference mButtonRegional4;
    private CheckBoxPreference mButtonNational4;
    private CheckBoxPreference mButtonInternational4;


    //Member variables
    private Phone mPhone;
    private MyHandler mHandler;

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mButtonBcSms) {
            if (DBG) Log.d(LOG_TAG, "onPreferenceTreeClick: preference == mButtonBcSms.");
            if(mButtonBcSms.isChecked()) {
                mPhone.activateCellBroadcastSms(RILConstants.CDMA_CELL_BROADCAST_SMS_ENABLED,
                        Message.obtain(mHandler, MESSAGE_ACTIVATE_CB_SMS));
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.CDMA_CELL_BROADCAST_SMS,
                        RILConstants.CDMA_CELL_BROADCAST_SMS_ENABLED);
                enableDisableAllCbConfigButtons(true);
            } else {
                mPhone.activateCellBroadcastSms(RILConstants.CDMA_CELL_BROADCAST_SMS_DISABLED,
                        Message.obtain(mHandler, MESSAGE_ACTIVATE_CB_SMS));
                android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                        android.provider.Settings.Global.CDMA_CELL_BROADCAST_SMS,
                        RILConstants.CDMA_CELL_BROADCAST_SMS_DISABLED);
                enableDisableAllCbConfigButtons(false);
            }
        } else if (preference == mListLanguage) {
            //Do nothing here, because this click will be handled in onPreferenceChange
        } else if (preference == mButtonEmergencyBroadcast) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonEmergencyBroadcast.isChecked(), 1);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(
                    mButtonEmergencyBroadcast.isChecked(), 1);
        } else if (preference == mButtonAdministrative) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonAdministrative.isChecked(), 2);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonAdministrative.isChecked(), 2);
        } else if (preference == mButtonMaintenance) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonMaintenance.isChecked(), 3);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonMaintenance.isChecked(), 3);
        } else if (preference == mButtonLocalWeather) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonLocalWeather.isChecked(), 20);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonLocalWeather.isChecked(), 20);
        } else if (preference == mButtonAtr) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonAtr.isChecked(), 21);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonAtr.isChecked(), 21);
        } else if (preference == mButtonLafs) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonLafs.isChecked(), 22);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonLafs.isChecked(), 22);
        } else if (preference == mButtonRestaurants) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonRestaurants.isChecked(), 23);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonRestaurants.isChecked(), 23);
        } else if (preference == mButtonLodgings) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonLodgings.isChecked(), 24);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonLodgings.isChecked(), 24);
        } else if (preference == mButtonRetailDirectory) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonRetailDirectory.isChecked(), 25);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonRetailDirectory.isChecked(), 25);
        } else if (preference == mButtonAdvertisements) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonAdvertisements.isChecked(), 26);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonAdvertisements.isChecked(), 26);
        } else if (preference == mButtonStockQuotes) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonStockQuotes.isChecked(), 27);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonStockQuotes.isChecked(), 27);
        } else if (preference == mButtonEo) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonEo.isChecked(), 28);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonEo.isChecked(), 28);
        } else if (preference == mButtonMhh) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonMhh.isChecked(), 29);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonMhh.isChecked(), 29);
        } else if (preference == mButtonTechnologyNews) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonTechnologyNews.isChecked(), 30);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonTechnologyNews.isChecked(), 30);
        } else if (preference == mButtonMultiCategory) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonMultiCategory.isChecked(), 31);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonMultiCategory.isChecked(), 31);
        } else if (preference == mButtonLocal1) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonLocal1.isChecked(), 4);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonLocal1.isChecked(), 4);
        } else if (preference == mButtonRegional1) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonRegional1.isChecked(), 5);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonRegional1.isChecked(), 5);
        } else if (preference == mButtonNational1) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonNational1.isChecked(), 6);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonNational1.isChecked(), 6);
        } else if (preference == mButtonInternational1) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonInternational1.isChecked(), 7);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonInternational1.isChecked(), 7);
        } else if (preference == mButtonLocal2) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonLocal2.isChecked(), 8);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonLocal2.isChecked(), 8);
        } else if (preference == mButtonRegional2) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonRegional2.isChecked(), 9);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonRegional2.isChecked(), 9);
        } else if (preference == mButtonNational2) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonNational2.isChecked(), 10);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonNational2.isChecked(), 10);
        } else if (preference == mButtonInternational2) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonInternational2.isChecked(), 11);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonInternational2.isChecked(), 11);
        } else if (preference == mButtonLocal3) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonLocal3.isChecked(), 12);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonLocal3.isChecked(), 12);
        } else if (preference == mButtonRegional3) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonRegional3.isChecked(), 13);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonRegional3.isChecked(), 13);
        } else if (preference == mButtonNational3) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonNational3.isChecked(), 14);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonNational3.isChecked(), 14);
        } else if (preference == mButtonInternational3) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonInternational3.isChecked(), 15);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonInternational3.isChecked(), 15);
        } else if (preference == mButtonLocal4) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(mButtonLocal4.isChecked(), 16);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonLocal4.isChecked(), 16);
        } else if (preference == mButtonRegional4) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonRegional4.isChecked(), 17);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonRegional4.isChecked(), 17);
        } else if (preference == mButtonNational4) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonNational4.isChecked(), 18);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonNational4.isChecked(), 18);
        } else if (preference == mButtonInternational4) {
            CellBroadcastSmsConfig.setConfigDataCompleteBSelected(
                    mButtonInternational4.isChecked(), 19);
            CellBroadcastSmsConfig.setCbSmsBSelectedValue(mButtonInternational4.isChecked(), 19);
        } else {
            preferenceScreen.setEnabled(false);
            return false;
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mListLanguage) {
            // set the new language to the array which will be transmitted later
            CellBroadcastSmsConfig.setConfigDataCompleteLanguage(
                    mListLanguage.findIndexOfValue((String) objValue) + 1);
        }

        // always let the preference setting proceed.
        return true;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.cell_broadcast_sms);

        mPhone = PhoneGlobals.getPhone();
        mHandler = new MyHandler();

        PreferenceScreen prefSet = getPreferenceScreen();

        mButtonBcSms = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_ENABLE_DISABLE_BC_SMS_KEY);
        mListLanguage = (ListPreference) prefSet.findPreference(
                LIST_LANGUAGE_KEY);
        // set the listener for the language list preference
        mListLanguage.setOnPreferenceChangeListener(this);
        mButtonEmergencyBroadcast = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_EMERGENCY_BROADCAST_KEY);
        mButtonAdministrative = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_ADMINISTRATIVE_KEY);
        mButtonMaintenance = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_MAINTENANCE_KEY);
        mButtonLocalWeather = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_LOCAL_WEATHER_KEY);
        mButtonAtr = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_ATR_KEY);
        mButtonLafs = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_LAFS_KEY);
        mButtonRestaurants = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_RESTAURANTS_KEY);
        mButtonLodgings = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_LODGINGS_KEY);
        mButtonRetailDirectory = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_RETAIL_DIRECTORY_KEY);
        mButtonAdvertisements = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_ADVERTISEMENTS_KEY);
        mButtonStockQuotes = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_STOCK_QUOTES_KEY);
        mButtonEo = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_EO_KEY);
        mButtonMhh = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_MHH_KEY);
        mButtonTechnologyNews = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_TECHNOLOGY_NEWS_KEY);
        mButtonMultiCategory = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_MULTI_CATEGORY_KEY);

        mButtonLocal1 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_LOCAL_GENERAL_NEWS_KEY);
        mButtonRegional1 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_REGIONAL_GENERAL_NEWS_KEY);
        mButtonNational1 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_NATIONAL_GENERAL_NEWS_KEY);
        mButtonInternational1 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_INTERNATIONAL_GENERAL_NEWS_KEY);

        mButtonLocal2 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_LOCAL_BF_NEWS_KEY);
        mButtonRegional2 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_REGIONAL_BF_NEWS_KEY);
        mButtonNational2 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_NATIONAL_BF_NEWS_KEY);
        mButtonInternational2 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_INTERNATIONAL_BF_NEWS_KEY);

        mButtonLocal3 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_LOCAL_SPORTS_NEWS_KEY);
        mButtonRegional3 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_REGIONAL_SPORTS_NEWS_KEY);
        mButtonNational3 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_NATIONAL_SPORTS_NEWS_KEY);
        mButtonInternational3 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_INTERNATIONAL_SPORTS_NEWS_KEY);

        mButtonLocal4 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_LOCAL_ENTERTAINMENT_NEWS_KEY);
        mButtonRegional4 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_REGIONAL_ENTERTAINMENT_NEWS_KEY);
        mButtonNational4 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_NATIONAL_ENTERTAINMENT_NEWS_KEY);
        mButtonInternational4 = (CheckBoxPreference) prefSet.findPreference(
                BUTTON_INTERNATIONAL_ENTERTAINMENT_NEWS_KEY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getPreferenceScreen().setEnabled(true);

        int settingCbSms = android.provider.Settings.Global.getInt(
                mPhone.getContext().getContentResolver(),
                android.provider.Settings.Global.CDMA_CELL_BROADCAST_SMS,
                RILConstants.CDMA_CELL_BROADCAST_SMS_DISABLED);
        mButtonBcSms.setChecked(settingCbSms == RILConstants.CDMA_CELL_BROADCAST_SMS_ENABLED);

        if(mButtonBcSms.isChecked()) {
            enableDisableAllCbConfigButtons(true);
        } else {
            enableDisableAllCbConfigButtons(false);
        }

        mPhone.getCellBroadcastSmsConfig(Message.obtain(mHandler, MESSAGE_GET_CB_SMS_CONFIG));
    }

    @Override
    protected void onPause() {
        super.onPause();

            CellBroadcastSmsConfig.setCbSmsNoOfStructs(NO_OF_SERVICE_CATEGORIES);

            mPhone.setCellBroadcastSmsConfig(CellBroadcastSmsConfig.getCbSmsAllValues(),
                    Message.obtain(mHandler, MESSAGE_SET_CB_SMS_CONFIG));
        }

    private void enableDisableAllCbConfigButtons(boolean enable) {
        mButtonEmergencyBroadcast.setEnabled(enable);
        mListLanguage.setEnabled(enable);
        mButtonAdministrative.setEnabled(enable);
        mButtonMaintenance.setEnabled(enable);
        mButtonLocalWeather.setEnabled(enable);
        mButtonAtr.setEnabled(enable);
        mButtonLafs.setEnabled(enable);
        mButtonRestaurants.setEnabled(enable);
        mButtonLodgings.setEnabled(enable);
        mButtonRetailDirectory.setEnabled(enable);
        mButtonAdvertisements.setEnabled(enable);
        mButtonStockQuotes.setEnabled(enable);
        mButtonEo.setEnabled(enable);
        mButtonMhh.setEnabled(enable);
        mButtonTechnologyNews.setEnabled(enable);
        mButtonMultiCategory.setEnabled(enable);

        mButtonLocal1.setEnabled(enable);
        mButtonRegional1.setEnabled(enable);
        mButtonNational1.setEnabled(enable);
        mButtonInternational1.setEnabled(enable);

        mButtonLocal2.setEnabled(enable);
        mButtonRegional2.setEnabled(enable);
        mButtonNational2.setEnabled(enable);
        mButtonInternational2.setEnabled(enable);

        mButtonLocal3.setEnabled(enable);
        mButtonRegional3.setEnabled(enable);
        mButtonNational3.setEnabled(enable);
        mButtonInternational3.setEnabled(enable);

        mButtonLocal4.setEnabled(enable);
        mButtonRegional4.setEnabled(enable);
        mButtonNational4.setEnabled(enable);
        mButtonInternational4.setEnabled(enable);
    }

    private void setAllCbConfigButtons(int[] configArray) {
        //These buttons are in a well defined sequence. If you want to change it,
        //be sure to map the buttons to their corresponding slot in the configArray !
        mButtonEmergencyBroadcast.setChecked(configArray[1] != 0);
        //subtract 1, because the values are handled in an array which starts with 0 and not with 1
        mListLanguage.setValueIndex(CellBroadcastSmsConfig.getConfigDataLanguage() - 1);
        mButtonAdministrative.setChecked(configArray[2] != 0);
        mButtonMaintenance.setChecked(configArray[3] != 0);
        mButtonLocalWeather.setChecked(configArray[20] != 0);
        mButtonAtr.setChecked(configArray[21] != 0);
        mButtonLafs.setChecked(configArray[22] != 0);
        mButtonRestaurants.setChecked(configArray[23] != 0);
        mButtonLodgings.setChecked(configArray[24] != 0);
        mButtonRetailDirectory.setChecked(configArray[25] != 0);
        mButtonAdvertisements.setChecked(configArray[26] != 0);
        mButtonStockQuotes.setChecked(configArray[27] != 0);
        mButtonEo.setChecked(configArray[28] != 0);
        mButtonMhh.setChecked(configArray[29] != 0);
        mButtonTechnologyNews.setChecked(configArray[30] != 0);
        mButtonMultiCategory.setChecked(configArray[31] != 0);

        mButtonLocal1.setChecked(configArray[4] != 0);
        mButtonRegional1.setChecked(configArray[5] != 0);
        mButtonNational1.setChecked(configArray[6] != 0);
        mButtonInternational1.setChecked(configArray[7] != 0);

        mButtonLocal2.setChecked(configArray[8] != 0);
        mButtonRegional2.setChecked(configArray[9] != 0);
        mButtonNational2.setChecked(configArray[10] != 0);
        mButtonInternational2.setChecked(configArray[11] != 0);

        mButtonLocal3.setChecked(configArray[12] != 0);
        mButtonRegional3.setChecked(configArray[13] != 0);
        mButtonNational3.setChecked(configArray[14] != 0);
        mButtonInternational3.setChecked(configArray[15] != 0);

        mButtonLocal4.setChecked(configArray[16] != 0);
        mButtonRegional4.setChecked(configArray[17] != 0);
        mButtonNational4.setChecked(configArray[18] != 0);
        mButtonInternational4.setChecked(configArray[19] != 0);
    }

    private class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_ACTIVATE_CB_SMS:
                //Only a log message here, because the received response is always null
                if (DBG) Log.d(LOG_TAG, "Cell Broadcast SMS enabled/disabled.");
                break;
            case MESSAGE_GET_CB_SMS_CONFIG:
                int result[] = (int[])((AsyncResult)msg.obj).result;

                // check if the actual service categoties table size on the NV is '0'
                if (result[0] == 0) {
                    result[0] = NO_OF_SERVICE_CATEGORIES;

                    mButtonBcSms.setChecked(false);
                    mPhone.activateCellBroadcastSms(RILConstants.CDMA_CELL_BROADCAST_SMS_DISABLED,
                            Message.obtain(mHandler, MESSAGE_ACTIVATE_CB_SMS));
                    android.provider.Settings.Global.putInt(mPhone.getContext().getContentResolver(),
                            android.provider.Settings.Global.CDMA_CELL_BROADCAST_SMS,
                            RILConstants.CDMA_CELL_BROADCAST_SMS_DISABLED);
                    enableDisableAllCbConfigButtons(false);
                }

                CellBroadcastSmsConfig.setCbSmsConfig(result);
                setAllCbConfigButtons(CellBroadcastSmsConfig.getCbSmsBselectedValues());

                break;
            case MESSAGE_SET_CB_SMS_CONFIG:
                //Only a log message here, because the received response is always null
                if (DBG) Log.d(LOG_TAG, "Set Cell Broadcast SMS values.");
                break;
            default:
                Log.e(LOG_TAG, "Error! Unhandled message in CellBroadcastSms.java. Message: "
                        + msg.what);
            break;
            }
        }
    }

    private static final class CellBroadcastSmsConfig {

        //The values in this array are stored in a particular order. This order
        //is calculated in the setCbSmsConfig method of this class.
        //For more information see comments below...
        //NO_OF_SERVICE_CATEGORIES +1 is used, because we will leave the first array entry 0
        private static int mBSelected[] = new int[NO_OF_SERVICE_CATEGORIES + 1];
        private static int mConfigDataComplete[] = new int[MAX_LENGTH_RESULT];

        private static void setCbSmsConfig(int[] configData) {
            if(configData == null) {
                Log.e(LOG_TAG, "Error! No cell broadcast service categories returned.");
                return;
            }

            if(configData[0] > MAX_LENGTH_RESULT) {
                Log.e(LOG_TAG, "Error! Wrong number of service categories returned from RIL");
                return;
            }

            //The required config values for broadcast SMS are stored in a C struct:
            //
            //  typedef struct {
            //      int size;
            //      RIL_CDMA_BcServiceInfo *entries;
            //  } RIL_CDMA_BcSMSConfig;
            //
            //  typedef struct {
            //      int uServiceCategory;
            //      int uLanguage;
            //      unsigned char bSelected;
            //  } RIL_CDMA_BcServiceInfo;
            //
            // This means, that we have to ignore the first value and check every
            // 3rd value starting with the 2nd of all. This value indicates, where we
            // will store the appropriate bSelected value, which is 2 values behind it.
            for(int i = 1; i < configData.length; i += NO_OF_INTS_STRUCT_1) {
                mBSelected[configData[i]] = configData[i +2];
            }

            //Store all values in an extra array
            mConfigDataComplete = configData;
        }

        private static void setCbSmsBSelectedValue(boolean value, int pos) {
            if(pos < mBSelected.length) {
                mBSelected[pos] = (value == true ? 1 : 0);
            } else {
                Log.e(LOG_TAG,"Error! Invalid value position.");
            }
        }

        private static int[] getCbSmsBselectedValues() {
            return(mBSelected);
        }

        // TODO: Change the return value to a RIL_BroadcastSMSConfig
        private static int[] getCbSmsAllValues() {
            return(mConfigDataComplete);
        }

        private static void setCbSmsNoOfStructs(int value) {
            //Sets the size parameter, which contains the number of structs
            //that will be transmitted
            mConfigDataComplete[0] = value;
        }

        private static void setConfigDataCompleteBSelected(boolean value, int serviceCategory) {
            //Sets the bSelected value for a specific serviceCategory
            for(int i = 1; i < mConfigDataComplete.length; i += NO_OF_INTS_STRUCT_1) {
                if(mConfigDataComplete[i] == serviceCategory) {
                    mConfigDataComplete[i + 2] = value == true ? 1 : 0;
                    break;
                }
            }
        }

        private static void setConfigDataCompleteLanguage(int language) {
            //It is only possible to set the same language for all entries
            for(int i = 2; i < mConfigDataComplete.length; i += NO_OF_INTS_STRUCT_1) {
                mConfigDataComplete[i] = language;
            }
        }

        private static int getConfigDataLanguage() {
            int language = mConfigDataComplete[2];
            //2 is the language value of the first entry
            //It is only possible to set the same language for all entries
            if (language < 1 || language > 7) {
                Log.e(LOG_TAG, "Error! Wrong language returned from RIL...defaulting to 1, english");
                return 1;
            }
            else {
                return language;
            }
        }
    }
}
