/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;

/** Top-level {@link ListActivity} for launching tests and managing results. */
public class TestListActivity extends AbstractTestListActivity {

    private static final String TAG = TestListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.title_version, Version.getVersionName(this)));
        setTestListAdapter(new ManifestTestListAdapter(this, null));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.test_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear:
                handleClearItemSelected();
                return true;

            case R.id.view:
                handleViewItemSelected();
                return true;

            case R.id.export:
                handleExportItemSelected();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleClearItemSelected() {
        mAdapter.clearTestResults();
        Toast.makeText(this, R.string.test_results_cleared, Toast.LENGTH_SHORT).show();
    }

    private void handleViewItemSelected() {
        try {
            TestResultsReport report = new TestResultsReport(this, mAdapter);
            Intent intent = new Intent(this, ReportViewerActivity.class);
            intent.putExtra(ReportViewerActivity.EXTRA_REPORT_CONTENTS, report.getContents());
            startActivity(intent);
        } catch (IOException e) {
            Toast.makeText(this, R.string.test_results_error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Couldn't copy test results report", e);
        }
    }

    private void handleExportItemSelected() {
        new ReportExporter(this, mAdapter).execute();
    }
}
