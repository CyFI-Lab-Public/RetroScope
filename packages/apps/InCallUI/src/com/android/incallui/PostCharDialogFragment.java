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

package com.android.incallui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.WindowManager;

/**
 * Pop up an alert dialog with OK and Cancel buttons to allow user to Accept or Reject the WAIT
 * inserted as part of the Dial string.
 */
public class PostCharDialogFragment extends DialogFragment {

    private int mCallId;
    private String mPostDialStr;

    public PostCharDialogFragment(int callId, String postDialStr) {
        mCallId = callId;
        mPostDialStr = postDialStr;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        final StringBuilder buf = new StringBuilder();
        buf.append(getResources().getText(R.string.wait_prompt_str));
        buf.append(mPostDialStr);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(buf.toString());

        builder.setPositiveButton(R.string.pause_prompt_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                CallCommandClient.getInstance().postDialWaitContinue(mCallId);
            }
        });
        builder.setNegativeButton(R.string.pause_prompt_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        CallCommandClient.getInstance().postDialCancel(mCallId);
    }
}
