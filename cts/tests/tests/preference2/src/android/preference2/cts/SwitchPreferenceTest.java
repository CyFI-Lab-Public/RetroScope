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

package android.preference2.cts;

import 	android.content.res.Resources;
import android.preference.SwitchPreference;
import android.test.ActivityInstrumentationTestCase2;

import com.android.cts.preference2.R;

public class SwitchPreferenceTest
        extends ActivityInstrumentationTestCase2<PreferenceFromCodeActivity> {

    private PreferenceFromCodeActivity mActivity;
    private SwitchPreference mSwitchPref;
    private Resources mResources;

    public SwitchPreferenceTest() {
        super(PreferenceFromCodeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mSwitchPref = (SwitchPreference) mActivity.findPreference(
                "switch_preference");
        mResources = mActivity.getResources();
    }

    public void testGetTitle() {
        String title = (String) mSwitchPref.getTitle();
        String titleExp = mResources.getString(R.string.title_switch_preference);
        assertEquals(titleExp, title);
    }

    public void testGetSummary() {
        String summary = (String) mSwitchPref.getSummary();
        String summaryExp = mResources.getString(R.string.summary_switch_preference);  
        assertEquals(summaryExp, summary);
    }

    public void testGetSummaryOn() {
        String summaryOn = (String) mSwitchPref.getSummaryOn();
        String summaryOnExp = mResources.getString(
                R.string.summary_on_switch_preference);
        assertEquals(summaryOnExp, summaryOn);
    }

    public void testSummaryOff() {
        String summaryOff = (String) mSwitchPref.getSummaryOff();
        String summaryOffExp = mResources.getString(
                R.string.summary_off_switch_preference);
        assertEquals(summaryOffExp, summaryOff);
    }

    public void testSetSwitchTextOff_One() throws Throwable {
        final CharSequence switchOff = "SwitchedOff";
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mSwitchPref.setSwitchTextOff(switchOff);
            }
        });
        CharSequence switchOffExp = mSwitchPref.getSwitchTextOff();
        assertEquals(switchOffExp, switchOff);
    }

    public void testSetSwitchTextOff_Two() throws Throwable {
        final CharSequence switchOffExp = mResources.getString(
                R.string.switchtext_off);
        CharSequence switchOff = mSwitchPref.getSwitchTextOff();
        assertEquals(switchOffExp, switchOff);
    }

    public void testSetSwitchTextOn_One() throws Throwable {
        final CharSequence switchOn = "SwitchedOff";
        this.runTestOnUiThread(new Runnable() {
            public void run() {
                mSwitchPref.setSwitchTextOn(switchOn);
            }
        }); 
        CharSequence switchOnExp = mSwitchPref.getSwitchTextOn();
        assertEquals(switchOnExp, switchOn);
    }

    public void testSetSwitchTextOn_Two() throws Throwable {
        final CharSequence switchOnExp = mResources.getString(
                R.string.switchtext_on);

        CharSequence switchOn = mSwitchPref.getSwitchTextOn();
        assertEquals(switchOnExp, switchOn);
    }
}
