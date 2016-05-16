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

package com.android.providers.calendar;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Prompts the user before copying their calendar database to the SD card.
 *
 */
public class CalendarDebugActivity extends Activity implements OnClickListener {
    private static String TAG = "CalendarDebugActivity";
    private Button mConfirmButton;
    private Button mCancelButton;
    private Button mDeleteButton;
    private TextView mTextView;

    private static final String OUT_FILE = "calendar.db.zip";
    private static final String MIME_TYPE = "application/zip";

    protected void onCreate(Bundle savedInstanceState) {
        // Be sure to call the super class.
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.dialog_activity);

        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                android.R.drawable.ic_dialog_alert);

        mConfirmButton = (Button) findViewById(R.id.confirm);
        mCancelButton = (Button) findViewById(R.id.cancel);
        mDeleteButton = (Button) findViewById(R.id.delete);
        updateDeleteButton();
    }

    private void updateDeleteButton() {
        final boolean fileExist =
            new File(Environment.getExternalStorageDirectory(), OUT_FILE).exists();
        mDeleteButton.setEnabled(fileExist);
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
        Log.i(TAG, "Deleting " + OUT_FILE);
        File outFile = new File(Environment.getExternalStorageDirectory(), OUT_FILE);
        outFile.delete();
    }

    private class DumpDbTask extends AsyncTask<Void, Void, File> {

        /**
         * Starts spinner while task is running.
         */
        @Override
        protected void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }

        protected File doInBackground(Void... params) {
            InputStream is = null;
            ZipOutputStream os = null;

            try {
                File path = Environment.getExternalStorageDirectory();
                File outFile = new File(path, OUT_FILE);
                outFile.delete();
                Log.i(TAG, "Outfile=" + outFile.getAbsolutePath());

                final File inFile = getDatabasePath("calendar.db");
                is = new FileInputStream(inFile);

                os = new ZipOutputStream(new FileOutputStream(outFile));
                os.putNextEntry(new ZipEntry(inFile.getName()));

                byte[] buf = new byte[4096];
                int totalLen = 0;
                while (true) {
                    int len = is.read(buf);
                    if (len <= 0) {
                        break;
                    }
                    os.write(buf, 0, len);
                    totalLen += len;
                }
                os.closeEntry();

                Log.i(TAG, "bytes read " + totalLen);
                os.flush();
                os.close();
                os = null;

                // Tell the media scanner about the new file so that it is
                // immediately available to the user.
                MediaScannerConnection.scanFile(CalendarDebugActivity.this, new String[] {
                    outFile.toString()
                }, new String[] {
                    MIME_TYPE
                }, null);
                return outFile;

            } catch (IOException e) {
                Log.i(TAG, "Error " + e.toString());
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    Log.i(TAG, "Error " + e.toString());
                }
            }
            return null;
        }

        /**
         * Runs on the UI thread
         */
        @Override
        protected void onPostExecute(File outFile) {
            if (outFile != null) {
                emailFile(outFile);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateDeleteButton();
        mConfirmButton.setEnabled(true);
        mCancelButton.setEnabled(true);
    }

    private void emailFile(File file) {
        Log.i(TAG, "Drafting email to send " + file.getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.debug_tool_email_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.debug_tool_email_body));
        intent.setType(MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.debug_tool_email_sender_picker)), 0);
    }
}
