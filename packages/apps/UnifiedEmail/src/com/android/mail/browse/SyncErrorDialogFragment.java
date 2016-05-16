/*
 * Copyright (C) 2012 Google Inc.
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

package com.android.mail.browse;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.mail.R;

/**
 * Implements a {@link DialogFragment} that uses an internal {@link AlertDialog}
 * to show information when there is a {@link UIProvider.LastSyncResult.STORAGE_ERROR}.
 */
public class SyncErrorDialogFragment extends DialogFragment {
    // Public no-args constructor needed for fragment re-instantiation
    public SyncErrorDialogFragment() {}

    public static SyncErrorDialogFragment newInstance() {
        SyncErrorDialogFragment frag = new SyncErrorDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.sync_error)
                .setMessage(R.string.sync_error_message)
                .setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                        }
                    }
                )
                .setNegativeButton(R.string.storage,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = new Intent(
                                    Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                            startActivity(intent);
                            dialog.dismiss();
                        }
                    }
                )
                .create();
    }
}
