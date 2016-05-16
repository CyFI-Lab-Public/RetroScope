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

package android.holo.cts;

import com.android.cts.holo.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;

/**
 * {@link Activity} that iterates over all the test layouts for a single theme
 * and either compares or generates bitmaps.
 */
public class BitmapDeletionActivity extends Activity {

    static final String EXTRA_BITMAP_TYPE = "bitmapType";

    private static final int DIALOG_DELETING_ID = 1;
    private static final int DIALOG_FINISHED_DELETING_ID = 2;
    private static final int DIALOG_ERROR_DELETING_ID = 3;

    private int mBitmapType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBitmapType = getIntent().getIntExtra(EXTRA_BITMAP_TYPE, -1);
        new DeleteBitmapsTask().execute();
    }

    class DeleteBitmapsTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(DIALOG_DELETING_ID);
        }

        @Override
        protected Boolean doInBackground(Void... darthVoider) {
            return BitmapAssets.clearDirectory(mBitmapType);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            dismissDialog(DIALOG_DELETING_ID);
            showDialog(success ? DIALOG_FINISHED_DELETING_ID : DIALOG_ERROR_DELETING_ID);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_DELETING_ID:
                ProgressDialog dialog = new ProgressDialog(BitmapDeletionActivity.this);
                dialog.setMessage(getString(R.string.deleting_bitmaps));
                return dialog;

            case DIALOG_FINISHED_DELETING_ID:
                return createFinishingDialog(R.string.deleting_bitmaps_finished);

            case DIALOG_ERROR_DELETING_ID:
                return createFinishingDialog(R.string.deleting_bitmaps_error);

            default:
                return super.onCreateDialog(id, args);
        }
    }

    private AlertDialog createFinishingDialog(int message) {
        DialogInterface.OnClickListener finishListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                };
        return new AlertDialog.Builder(BitmapDeletionActivity.this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, finishListener)
                .create();
    }
}
