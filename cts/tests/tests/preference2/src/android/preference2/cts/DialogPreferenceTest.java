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

import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.test.ActivityInstrumentationTestCase2;

import com.android.cts.preference2.R;

public class DialogPreferenceTest
        extends ActivityInstrumentationTestCase2<PreferenceFromCodeActivity> {

    private PreferenceFromCodeActivity mActivity;
    private DialogPreference mDialogPreference;

    public DialogPreferenceTest() {
        super(PreferenceFromCodeActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mDialogPreference = (DialogPreference)mActivity.findPreference(
                "dialog_preference");
    }

    public void testGetTitle() {
        String title = (String) mDialogPreference.getTitle();
        String titleExp = mActivity.getResources().getString(R.string.title_dialog_preference);
        assertEquals(titleExp, title);
    }

    public void testGetDialogTitle() {
        String title = (String) mDialogPreference.getDialogTitle();
        String titleExp = mActivity.getResources().getString(
                R.string.dialogtitle_dialog_preference);
        assertEquals(titleExp, title);
    }

    public void testGetDialogIcon() {
        Drawable drawable = mDialogPreference.getDialogIcon();
        Drawable drawableExp = mActivity.getResources().getDrawable(R.drawable.ic_launcher);
        assertEquals(drawableExp.getBounds(), drawable.getBounds());
    }

    public void testGetDialogMessage() {
        CharSequence dialogMessage = mDialogPreference.getDialogMessage();
        CharSequence dialogMessageExp =  mActivity.getResources().getString(
                R.string.dialog_message);
        assertEquals(dialogMessageExp, dialogMessage);
    }

    public void testGetPositiveButtonText() {
        CharSequence positiveButtonText = mDialogPreference.getPositiveButtonText();
        CharSequence postiveButtonTextExp =  mActivity.getResources().getString(
                R.string.positive_button_text);
        assertEquals(positiveButtonText, postiveButtonTextExp);
    }

    public void testGetNegativeButtonText() {
        CharSequence negativeButtonText = mDialogPreference.getNegativeButtonText();
        CharSequence negativeButtonTextExp =  mActivity.getResources().getString(
                R.string.negative_button_text);
        assertEquals(negativeButtonText, negativeButtonTextExp);
    }
}
