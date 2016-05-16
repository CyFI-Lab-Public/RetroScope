/*
 * Copyright (C) 2013 Google Inc.
 * Licensed to The Android Open Source Project.
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
package com.android.mail.ui.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.Toast;

import com.android.mail.R;
import com.android.mail.preferences.MailPrefs;

public class ClearPictureApprovalsDialogFragment extends DialogFragment implements OnClickListener {

    public static final String FRAGMENT_TAG = "ClearPictureApprovalsDialogFragment";

    // Public no-args constructor needed for fragment re-instantiation
    public ClearPictureApprovalsDialogFragment() {}

    /**
     * Creates a new instance of {@link ClearPictureApprovalsDialogFragment}.
     * @return The newly created {@link ClearPictureApprovalsDialogFragment}.
     */
    public static ClearPictureApprovalsDialogFragment newInstance() {
        final ClearPictureApprovalsDialogFragment fragment =
                new ClearPictureApprovalsDialogFragment();
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.clear_display_images_whitelist_dialog_title)
                .setMessage(R.string.clear_display_images_whitelist_dialog_message)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(R.string.clear, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            final MailPrefs mailPrefs = MailPrefs.get(getActivity());
            mailPrefs.clearSenderWhiteList();
            Toast.makeText(getActivity(), R.string.sender_whitelist_cleared, Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
