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

package com.android.cts.verifier.backup;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.BackupManager;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Test for checking whether the BackupManager is working properly. It lists the values of
 * several preferences and contents of files that should get backed up and restored after
 * running the backup manager and reinstalling the CTS verifier.
 */
public class BackupTestActivity extends PassFailButtons.ListActivity {

    private static final String TAG = BackupTestActivity.class.getSimpleName();

    private static final int INSTRUCTIONS_DIALOG_ID = 1;

    private static final String TEST_PREFS_1 = "test-prefs-1";
    private static final String INT_PREF = "int-pref";
    private static final String BOOL_PREF = "bool-pref";

    private static final String TEST_PREFS_2 = "test-prefs-2";
    private static final String FLOAT_PREF = "float-pref";
    private static final String LONG_PREF = "long-pref";
    private static final String STRING_PREF = "string-pref";

    private static final String TEST_FILE_1 = "test-file-1";
    private static final String TEST_FILE_2 = "test-file-2";

    private BackupAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.bu_main);
        setPassFailButtonClickListeners();
        setInfoResources(R.string.backup_test, R.string.backup_info, 0);

        mAdapter = new BackupAdapter(this);
        setListAdapter(mAdapter);

        new LoadBackupItemsTask().execute();

        findViewById(R.id.generate_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new GenerateValuesTask().execute();
            }
        });
    }

    public static SharedPreferencesBackupHelper getSharedPreferencesBackupHelper(Context context) {
        return new SharedPreferencesBackupHelper(context, TEST_PREFS_1, TEST_PREFS_2);
    }

    public static FileBackupHelper getFileBackupHelper(Context context) {
        return new FileBackupHelper(context, TEST_FILE_1, TEST_FILE_2);
    }

    class LoadBackupItemsTask extends AsyncTask<Void, Void, List<BackupItem>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected List<BackupItem> doInBackground(Void... params) {
            List<BackupItem> items = new ArrayList<BackupItem>();

            items.add(new CategoryBackupItem(R.string.bu_preferences));
            loadPreferenceGroup1(items);
            loadPreferenceGroup2(items);

            items.add(new CategoryBackupItem(R.string.bu_files));
            loadFile(TEST_FILE_1, items);
            loadFile(TEST_FILE_2, items);

            return items;
        }

        private void loadPreferenceGroup1(List<BackupItem> items) {
            SharedPreferences prefs = getSharedPreferences(TEST_PREFS_1, MODE_PRIVATE);

            int intValue = prefs.getInt(INT_PREF, 0);
            items.add(new PreferenceBackupItem(TEST_PREFS_1, INT_PREF, "" + intValue));

            boolean boolValue = prefs.getBoolean(BOOL_PREF, false);
            items.add(new PreferenceBackupItem(TEST_PREFS_1, BOOL_PREF, "" + boolValue));
        }

        private void loadPreferenceGroup2(List<BackupItem> items) {
            SharedPreferences prefs = getSharedPreferences(TEST_PREFS_2, MODE_PRIVATE);

            float floatValue = prefs.getFloat(FLOAT_PREF, 0.0f);
            items.add(new PreferenceBackupItem(TEST_PREFS_2, FLOAT_PREF, "" + floatValue));

            long longValue = prefs.getLong(LONG_PREF, 0L);
            items.add(new PreferenceBackupItem(TEST_PREFS_2, LONG_PREF, "" + longValue));

            String stringValue = prefs.getString(STRING_PREF, null);
            items.add(new PreferenceBackupItem(TEST_PREFS_2, STRING_PREF, stringValue));
        }

        private void loadFile(String fileName, List<BackupItem> items) {
            StringBuilder contents = new StringBuilder();
            Scanner scanner = null;
            try {
                scanner = new Scanner(new File(getFilesDir(), fileName));
                while (scanner.hasNext()) {
                    contents.append(scanner.nextLine());
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Couldn't find test file but this may be fine...", e);
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
            items.add(new FileBackupItem(fileName, contents.toString()));
        }

        @Override
        protected void onPostExecute(List<BackupItem> result) {
            super.onPostExecute(result);
            setProgressBarIndeterminateVisibility(false);
            mAdapter.clear();
            mAdapter.addAll(result);
        }
    }

    class GenerateValuesTask extends AsyncTask<Void, Void, Exception> {

        @Override
        protected Exception doInBackground(Void... params) {
            Random random = new Random();
            generatePreferenceGroup1(random);
            generatePreferenceGroup2(random);
            try {
                generateTestFile(TEST_FILE_1, random);
                generateTestFile(TEST_FILE_2, random);
            } catch (FileNotFoundException e) {
                return e;
            }
            return null;
        }

        private void generatePreferenceGroup1(Random random) {
            SharedPreferences prefs = getSharedPreferences(TEST_PREFS_1, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(INT_PREF, (random.nextInt(100) + 1));
            editor.putBoolean(BOOL_PREF, random.nextBoolean());
            editor.commit();
        }

        private void generatePreferenceGroup2(Random random) {
            SharedPreferences prefs = getSharedPreferences(TEST_PREFS_2, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putFloat(FLOAT_PREF, random.nextFloat());
            editor.putLong(LONG_PREF, random.nextLong());
            editor.putString(STRING_PREF, "Random number: " + (random.nextInt(100) + 1));
            editor.commit();
        }

        private void generateTestFile(String fileName, Random random)
                throws FileNotFoundException {
            File file = new File(getFilesDir(), fileName);
            PrintWriter writer = new PrintWriter(file);
            writer.write("Random number: " + (random.nextInt(100) + 1));
            writer.close();
        }

        @Override
        protected void onPostExecute(Exception exception) {
            super.onPostExecute(exception);
            if (exception != null) {
                Log.e(TAG, "Couldn't generate test data...", exception);
                Toast.makeText(BackupTestActivity.this, R.string.bu_generate_error,
                        Toast.LENGTH_LONG).show();
            } else {
                showDialog(INSTRUCTIONS_DIALOG_ID);

                BackupManager backupManager = new BackupManager(BackupTestActivity.this);
                backupManager.dataChanged();

                new LoadBackupItemsTask().execute();
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case INSTRUCTIONS_DIALOG_ID:
                return new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle(R.string.backup_test)
                    .setMessage(R.string.bu_instructions)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(R.string.bu_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_PRIVACY_SETTINGS));
                        }
                    })
                    .create();

            default:
                return super.onCreateDialog(id, args);
        }
    }

    interface BackupItem {
        int getViewType();
        View getView(LayoutInflater inflater, int position, View convertView, ViewGroup parent);
    }

    static class CategoryBackupItem implements BackupItem {

        private final int mTitleResId;

        CategoryBackupItem(int titleResId) {
            mTitleResId = titleResId;
        }

        @Override
        public int getViewType() {
            return 0;
        }

        @Override
        public View getView(LayoutInflater inflater, int position, View convertView,
                ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (convertView == null) {
                view = (TextView) inflater.inflate(R.layout.test_category_row, parent, false);
            }
            view.setText(mTitleResId);
            return view;
        }
    }

    static class PreferenceBackupItem implements BackupItem {

        private final String mGroup;

        private final String mName;

        private final String mValue;

        PreferenceBackupItem(String group, String name, String value) {
            mGroup = group;
            mName = name;
            mValue = value;
        }

        @Override
        public int getViewType() {
            return 1;
        }

        @Override
        public View getView(LayoutInflater inflater, int position, View convertView,
                ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (convertView == null) {
                view = (TextView) inflater.inflate(R.layout.bu_preference_row, parent, false);
            }
            view.setText(mGroup + "/" + mName + " : " + mValue);
            return view;
        }
    }

    static class FileBackupItem implements BackupItem {

        private final String mName;

        private final String mContents;

        FileBackupItem(String name, String contents) {
            mName = name;
            mContents = contents;
        }

        @Override
        public int getViewType() {
            return 2;
        }

        @Override
        public View getView(LayoutInflater inflater, int position, View convertView,
                ViewGroup parent) {
            TextView view = (TextView) convertView;
            if (convertView == null) {
                view = (TextView) inflater.inflate(R.layout.bu_preference_row, parent, false);
            }
            view.setText(mName + " : " + mContents);
            return view;
        }
    }

    class BackupAdapter extends BaseAdapter {

        private final LayoutInflater mLayoutInflater;

        private final List<BackupItem> mItems = new ArrayList<BackupItem>();

        public BackupAdapter(Context context) {
            mLayoutInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        public void clear() {
            mItems.clear();
        }

        public void addAll(List<BackupItem> items) {
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public BackupItem getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position).getViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return getItem(position).getView(mLayoutInflater, position, convertView, parent);
        }
    }
}
