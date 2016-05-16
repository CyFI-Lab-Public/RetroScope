/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.holo.cts.modifiers;

import com.android.cts.holo.R;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;

public class ProgressDialogBuilder implements DialogBuilder {

    private int mDialogType;

    public ProgressDialogBuilder(int dialogType) {
        mDialogType = dialogType;
    }

    @Override
    public Dialog buildDialog(View view) {
        Context context = view.getContext();
        ProgressDialog progressDialog = new ProgressDialog(view.getContext());
        progressDialog.setMessage(context.getString(R.string.loading));

        switch (mDialogType) {
            case ProgressDialog.STYLE_SPINNER:
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                break;

            case ProgressDialog.STYLE_HORIZONTAL:
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMax(100);
                break;

            default:
                throw new IllegalArgumentException("Bad dialog type: " + mDialogType);
        }
        progressDialog.show();
        return progressDialog;
    }
}
