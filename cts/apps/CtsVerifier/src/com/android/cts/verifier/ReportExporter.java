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

package com.android.cts.verifier;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Background task to generate a report and save it to external storage.
 */
class ReportExporter extends AsyncTask<Void, Void, String> {
    protected static final Logger LOG = Logger.getLogger(ReportExporter.class.getName());

    private final Context mContext;
    private final TestListAdapter mAdapter;

    ReportExporter(Context context, TestListAdapter adapter) {
        this.mContext = context;
        this.mAdapter = adapter;
    }

    @Override
    protected String doInBackground(Void... params) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            LOG.log(Level.WARNING, "External storage is not writable.");
            return mContext.getString(R.string.no_storage);
        }
        byte[] contents;
        try {
            TestResultsReport report = new TestResultsReport(mContext, mAdapter);
            contents = report.getContents().getBytes();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Couldn't create test results report", e);
            return mContext.getString(R.string.test_results_error);
        }
        File reportPath = new File(Environment.getExternalStorageDirectory(), "ctsVerifierReports");
        reportPath.mkdirs();

        String baseName = getReportBaseName();
        File reportFile = new File(reportPath, baseName + ".zip");
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(reportFile)));
            ZipEntry entry = new ZipEntry(baseName + ".xml");
            out.putNextEntry(entry);
            out.write(contents);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "I/O exception writing report to storage.", e);
            return mContext.getString(R.string.no_storage);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "I/O exception closing report.", e);
            }
        }

        return mContext.getString(R.string.report_saved, reportFile.getPath());
    }

    private String getReportBaseName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss", Locale.ENGLISH);
        String date = dateFormat.format(new Date());
        return "ctsVerifierReport"
                + "-" + date
                + "-" + Build.MANUFACTURER
                + "-" + Build.PRODUCT
                + "-" + Build.DEVICE
                + "-" + Build.ID;
    }

    @Override
    protected void onPostExecute(String result) {
        new AlertDialog.Builder(mContext)
                .setMessage(result)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
