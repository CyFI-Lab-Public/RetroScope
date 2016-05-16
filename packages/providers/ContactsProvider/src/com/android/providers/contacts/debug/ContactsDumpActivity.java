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

package com.android.providers.contacts.debug;

import com.android.providers.contacts.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;

import java.io.IOException;

/**
 * Activity to export all app data files as a zip file on sdcard, and send it via email.
 *
 * Usage:
 * adb shell am start -a com.android.providers.contacts.DUMP_DATABASE
 */
public class ContactsDumpActivity extends Activity implements OnClickListener {
    private static String TAG = "ContactsDumpActivity";
    private Button mConfirmButton;
    private Button mCancelButton;
    private Button mDeleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.contact_dump_activity);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                android.R.drawable.ic_dialog_alert);

        mConfirmButton = (Button) findViewById(R.id.confirm);
        mCancelButton = (Button) findViewById(R.id.cancel);
        mDeleteButton = (Button) findViewById(R.id.delete);
        updateDeleteButton();
    }

    private void updateDeleteButton() {
        mDeleteButton.setEnabled(DataExporter.dumpFileExists(this));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.confirm:
                mConfirmButton.setEnabled(false);
                mCancelButton.setEnabled(false);
                new DumpDbTask().execute();
                break;
            case R.id.delete:
                cleanup();
                updateDeleteButton();
                break;
            case R.id.cancel:
                finish();
                break;
        }
    }

    private void cleanup() {
        DataExporter.removeDumpFiles(this);
    }

    private class DumpDbTask extends AsyncTask<Void, Void, Uri> {
        /**
         * Starts spinner while task is running.
         */
        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Uri doInBackground(Void... params) {
            try {
                return DataExporter.exportData(getApplicationContext());
            } catch (IOException e) {
                Log.e(TAG, "Failed to export", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (uri != null) {
                emailFile(uri);
            }
        }
    }

    private void emailFile(Uri uri) {
        Log.i(TAG, "Drafting email");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.debug_dump_email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.debug_dump_email_body));
        intent.setType(DataExporter.ZIP_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.debug_dump_email_sender_picker)), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateDeleteButton();
        mConfirmButton.setEnabled(true);
        mCancelButton.setEnabled(true);
    }
}
