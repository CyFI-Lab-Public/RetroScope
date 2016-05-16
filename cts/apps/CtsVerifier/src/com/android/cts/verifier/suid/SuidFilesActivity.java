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

package com.android.cts.verifier.suid;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.TestResult;
import com.android.cts.verifier.os.FileUtils;
import com.android.cts.verifier.os.FileUtils.FileStatus;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** {@link Activity} that tries to find suid files. */
public class SuidFilesActivity extends PassFailButtons.ListActivity {

    private static final String TAG = SuidFilesActivity.class.getSimpleName();

    /** These programs are expected suid binaries. */
    private static final Set<String> WHITELIST = new HashSet<String>(Arrays.asList(
            "run-as"
    ));

    private ProgressDialog mProgressDialog;

    private SuidFilesAdapter mAdapter;

    private SuidFilesTask mFindSuidFilesTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pass_fail_list);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);

        mAdapter = new SuidFilesAdapter();
        setListAdapter(mAdapter);

        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(R.string.suid_files)
            .setMessage(R.string.suid_files_info)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startScan();
                }
            })
            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            })
            .setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            })
            .show();
    }

    private void startScan() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.scanning_directory));
        mProgressDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // If the scanning dialog is cancelled, then stop the task and finish the activity
                // to prevent the user from just seeing a blank listview.
                if (mFindSuidFilesTask != null) {
                    mFindSuidFilesTask.cancel(true);
                }
                finish();
            }
        });

        // Start searching for suid files using a background thread.
        mFindSuidFilesTask = new SuidFilesTask();
        mFindSuidFilesTask.execute(new File("/"));
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        File file = mAdapter.getItem(position);
        String message = getMessage(file);
        new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setMessage(message)
                .show();
    }

    private String getMessage(File file) {
        FileStatus status = new FileStatus();
        if (FileUtils.getFileStatus(file.getAbsolutePath(), status, true)) {
            return getString(R.string.file_status,
                    FileUtils.getUserName(status.getUid()),
                    FileUtils.getGroupName(status.getGid()),
                    FileUtils.getFormattedPermissions(status.getMode()),
                    file.getAbsolutePath());
        } else {
            return getString(R.string.no_file_status);
        }
    }

    @Override
    protected void onDestroy() {
        Log.e("Suid", "onDestroy");
        super.onDestroy();
        if (mFindSuidFilesTask != null) {
            mFindSuidFilesTask.cancel(true);
        }
    }

    /** {@link ListView} items display the basenames of the suid files. */
    class SuidFilesAdapter extends ArrayAdapter<File> {

        SuidFilesAdapter() {
            super(SuidFilesActivity.this, android.R.layout.simple_list_item_1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            File file = getItem(position);
            view.setText(file.getName());
            view.setBackgroundResource(WHITELIST.contains(file.getName())
                    ? R.drawable.test_pass_gradient
                    : R.drawable.test_fail_gradient);
            return view;
        }
    }

    /** {@link AsyncTask} that searches the file system for suid files. */
    class SuidFilesTask extends AsyncTask<File, File, Set<File>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Set<File> doInBackground(File... paths) {
            Set<File> suidFiles = new HashSet<File>();
            DirectoryFileFilter dirFilter = new DirectoryFileFilter();
            SuidFileFilter suidFilter = new SuidFileFilter();
            for (File path : paths) {
                findSuidFiles(path, suidFiles, dirFilter, suidFilter);
            }
            return suidFiles;
        }

        private void findSuidFiles(File dir, Set<File> foundSuidFiles,
                DirectoryFileFilter dirFilter, SuidFileFilter suidFilter) {

            // Recursively traverse sub directories...
            File[] subDirs = dir.listFiles(dirFilter);
            if (subDirs != null && subDirs.length > 0) {
                for (File subDir : subDirs) {
                    findSuidFiles(subDir, foundSuidFiles, dirFilter, suidFilter);
                }
            }

            // / ...then inspect files in directory to find offending binaries.
            publishProgress(dir);
            File[] suidFiles = dir.listFiles(suidFilter);
            if (suidFiles != null && suidFiles.length > 0) {
                Collections.addAll(foundSuidFiles, suidFiles);
            }
        }

        /** {@link FileFilter} that returns only directories that are not symbolic links. */
        private class DirectoryFileFilter implements FileFilter {

            private final FileStatus status = new FileStatus();

            @Override
            public boolean accept(File pathname) {
                // Don't follow symlinks to avoid infinite looping.
                if (FileUtils.getFileStatus(pathname.getPath(), status, true)) {
                    return status.isDirectory() && !status.isSymbolicLink();
                } else {
                    Log.w(TAG, "Could not stat " + pathname);
                    return false;
                }
            }
        }

        /** {@link FileFilter} that returns files that have setuid root or setgid root. */
        private class SuidFileFilter implements FileFilter {

            private final FileStatus status = new FileStatus();

            @Override
            public boolean accept(File pathname) {
                if (FileUtils.getFileStatus(pathname.getPath(), status, true)) {
                    // only files with setUid which can be executable by CTS are reported.
                    return !status.isDirectory()
                            && !status.isSymbolicLink()
                            && status.isSetUid()
                            && status.isExecutableByCTS();
                } else {
                    Log.w(TAG, "Could not stat " + pathname);
                    return false;
                }
            }
        }

        @Override
        protected void onPostExecute(Set<File> results) {
            super.onPostExecute(results);
            mProgressDialog.dismiss();

            // Task could be cancelled and results could be null but don't bother doing anything.
            if (results != null) {
                boolean passed = true;
                for (File result : results) {
                    if (!WHITELIST.contains(result.getName())) {
                        passed = false;
                    }
                    mAdapter.add(result);
                }

                // Alert the user that nothing was found rather than showing an empty list view.
                if (passed) {
                    getPassButton().setEnabled(true);
                    new AlertDialog.Builder(SuidFilesActivity.this)
                            .setTitle(R.string.congratulations)
                            .setMessage(R.string.no_suid_files)
                            .setPositiveButton(android.R.string.ok, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
            }
        }

        @Override
        protected void onProgressUpdate(File... values) {
            super.onProgressUpdate(values);

            // Show the current directory being scanned...
            mProgressDialog.setMessage(values[0].getAbsolutePath());
        }
    }
}
