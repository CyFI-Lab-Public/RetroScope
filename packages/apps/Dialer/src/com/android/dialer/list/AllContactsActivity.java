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
package com.android.dialer.list;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.util.Log;

import com.android.contacts.common.CallUtil;
import com.android.contacts.common.activity.TransactionSafeActivity;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;
import com.android.dialer.interactions.PhoneNumberInteraction;

public class AllContactsActivity extends TransactionSafeActivity {
    private static final String TAG = AllContactsActivity.class.getSimpleName();

    private AllContactsFragment mAllContactsFragment;

    // Same behavior as {@link DialtactsActivity}
    private final OnPhoneNumberPickerActionListener mPhoneNumberPickerActionListener =
            new OnPhoneNumberPickerActionListener() {
                @Override
                public void onPickPhoneNumberAction(Uri dataUri) {
                    // Specify call-origin so that users will see the previous tab instead of
                    // CallLog screen (search UI will be automatically exited).
                    PhoneNumberInteraction.startInteractionForPhoneCall(
                        AllContactsActivity.this, dataUri, null);
                }

                @Override
                public void onCallNumberDirectly(String phoneNumber) {
                final Intent intent = CallUtil.getCallIntent(phoneNumber, null);
                    startActivity(intent);
                }

                @Override
                public void onShortcutIntentCreated(Intent intent) {
                    Log.w(TAG, "Unsupported intent has come (" + intent + "). Ignoring.");
                }

                @Override
                public void onHomeInActionBarSelected() {
                    // {@link PhoneNumberPickerFragment handles onClick on the home button
                    // and performs the callback here. This means we don't have to handle it
                    // ourself in the activity.
                    final Intent intent = new Intent(AllContactsActivity.this,
                            DialtactsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

        setContentView(R.layout.all_contacts_activity);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof AllContactsFragment) {
            mAllContactsFragment = (AllContactsFragment) fragment;
            mAllContactsFragment.setOnPhoneNumberPickerActionListener(
                    mPhoneNumberPickerActionListener);
        }
    }
}
