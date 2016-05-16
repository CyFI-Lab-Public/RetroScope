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

package com.android.cts.verifier.nfc;

import com.android.cts.verifier.PassFailButtons;
import com.android.cts.verifier.R;
import com.android.cts.verifier.nfc.tech.MifareUltralightTagTester;
import com.android.cts.verifier.nfc.tech.NdefTagTester;
import com.android.cts.verifier.nfc.tech.TagTester;
import com.android.cts.verifier.nfc.tech.TagVerifier;
import com.android.cts.verifier.nfc.tech.TagVerifier.Result;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Test activity for reading and writing NFC tags using different technologies.
 * First, it asks the user to write some random data to the tag. Then it asks
 * the user to scan that tag again to verify that the data was properly written
 * and read back.
 */
public class TagVerifierActivity<T> extends PassFailButtons.ListActivity {

    static final String TAG = TagVerifierActivity.class.getSimpleName();

    /** Non-optional argument specifying the tag technology to be used to read and write tags. */
    static final String EXTRA_TECH = "tech";

    private static final int NFC_NOT_ENABLED_DIALOG_ID = 1;
    private static final int TESTABLE_TAG_DISCOVERED_DIALOG_ID = 2;
    private static final int TESTABLE_TAG_REMINDER_DIALOG_ID = 3;
    private static final int WRITE_PROGRESS_DIALOG_ID = 4;
    private static final int READ_PROGRESS_DIALOG_ID = 5;
    private static final int VERIFY_RESULT_DIALOG_ID = 6;

    // Arguments used for the dialog showing what was written to the tag and read from the tag.
    private static final String EXPECTED_CONTENT_ID = "expectedContent";
    private static final String ACTUAL_CONTENT_ID = "actualContent";
    private static final String IS_MATCH_ID = "isMatch";

    // The test activity has two states - writing data to a tag and then verifying it.
    private static final int WRITE_STEP = 0;
    private static final int VERIFY_STEP = 1;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private Class<?> mTechClass;

    private int mStep;
    private TagTester mTagTester;
    private TagVerifier mTagVerifier;
    private Tag mTag;
    private ArrayAdapter<String> mTechListAdapter;
    private TextView mEmptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_tag);
        setInfoResources(R.string.nfc_tag_verifier, R.string.nfc_tag_verifier_info, 0);
        setPassFailButtonClickListeners();
        getPassButton().setEnabled(false);

        parseIntentExtras();
        if (mTechClass != null) {
            mTagTester = getTagTester(mTechClass);

            mEmptyText = (TextView) findViewById(android.R.id.empty);

            mTechListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            setListAdapter(mTechListAdapter);

            NfcManager nfcManager = (NfcManager) getSystemService(NFC_SERVICE);
            mNfcAdapter = nfcManager.getDefaultAdapter();
            mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            goToWriteStep();
        } else {
            finish();
        }
    }

    private void parseIntentExtras() {
        try {
            String tech = getIntent().getStringExtra(EXTRA_TECH);
            if (tech != null) {
                mTechClass = Class.forName(tech);
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Couldn't find tech for class", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mNfcAdapter.isEnabled()) {
            showDialog(NFC_NOT_ENABLED_DIALOG_ID);
        }

        mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNfcAdapter.disableForegroundDispatch(this);
    }

    private TagTester getTagTester(Class<?> techClass) {
        if (Ndef.class.equals(techClass)) {
            return new NdefTagTester(this);
        } else if (MifareUltralight.class.equals(techClass)) {
            return new MifareUltralightTagTester();
        } else {
            throw new IllegalArgumentException("Unsupported technology: " + techClass);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            mTag = tag;
            updateTechListAdapter(tag);
            switch (mStep) {
                case WRITE_STEP:
                    handleWriteStep(tag);
                    break;

                case VERIFY_STEP:
                    handleVerifyStep();
                    break;
            }
        }
    }

    private void handleWriteStep(Tag tag) {
        if (mTagTester.isTestableTag(tag)) {
            brutallyDismissDialog(TESTABLE_TAG_REMINDER_DIALOG_ID);
            showDialog(TESTABLE_TAG_DISCOVERED_DIALOG_ID);
        } else {
            brutallyDismissDialog(TESTABLE_TAG_DISCOVERED_DIALOG_ID);
            showDialog(TESTABLE_TAG_REMINDER_DIALOG_ID);
        }
    }

    private void brutallyDismissDialog(int id) {
        try {
            dismissDialog(id);
        } catch (IllegalArgumentException e) {
            // Don't care if it hasn't been shown before...
        }
    }

    private void handleVerifyStep() {
        new VerifyTagTask().execute(mTag);
    }

    private void updateTechListAdapter(Tag tag) {
        mEmptyText.setText(R.string.nfc_no_tech);
        String[] techList = tag.getTechList();
        mTechListAdapter.clear();
        for (String tech : techList) {
            mTechListAdapter.add(tech);
        }
    }

    class WriteTagTask extends AsyncTask<Tag, Void, TagVerifier> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(WRITE_PROGRESS_DIALOG_ID);
        }

        @Override
        protected TagVerifier doInBackground(Tag... tags) {
            try {
                return mTagTester.writeTag(tags[0]);
            } catch (Exception e) {
                Log.e(TAG, "Error writing NFC tag...", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(TagVerifier tagVerifier) {
            dismissDialog(WRITE_PROGRESS_DIALOG_ID);
            mTagVerifier = tagVerifier;
            if (tagVerifier != null) {
                goToVerifyStep();
            } else {
                Toast.makeText(TagVerifierActivity.this, R.string.nfc_writing_tag_error,
                        Toast.LENGTH_SHORT).show();
                goToWriteStep();
            }
        }
    }

    private void goToWriteStep() {
        mStep = WRITE_STEP;
        mEmptyText.setText(getString(R.string.nfc_scan_tag, mTechClass.getSimpleName()));
        mTechListAdapter.clear();
    }

    private void goToVerifyStep() {
        mStep = VERIFY_STEP;
        mEmptyText.setText(getString(R.string.nfc_scan_tag_again, mTechClass.getSimpleName()));
        mTechListAdapter.clear();
    }

    class VerifyTagTask extends AsyncTask<Tag, Void, Result> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(READ_PROGRESS_DIALOG_ID);
        }

        @Override
        protected Result doInBackground(Tag... tags) {
            try {
                return mTagVerifier.verifyTag(tags[0]);
            } catch (Exception e) {
                Log.e(TAG, "Error verifying NFC tag...", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            dismissDialog(READ_PROGRESS_DIALOG_ID);
            mTagVerifier = null;
            if (result != null) {
                getPassButton().setEnabled(result.isMatch());

                Bundle args = new Bundle();
                args.putCharSequence(EXPECTED_CONTENT_ID, result.getExpectedContent());
                args.putCharSequence(ACTUAL_CONTENT_ID, result.getActualContent());
                args.putBoolean(IS_MATCH_ID, result.isMatch());
                showDialog(VERIFY_RESULT_DIALOG_ID, args);

                goToWriteStep();
            } else {
                Toast.makeText(TagVerifierActivity.this, R.string.nfc_reading_tag_error,
                        Toast.LENGTH_SHORT).show();
                goToWriteStep();
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case NFC_NOT_ENABLED_DIALOG_ID:
                return NfcDialogs.createNotEnabledDialog(this);

            case TESTABLE_TAG_DISCOVERED_DIALOG_ID:
                return createTestableTagDiscoveredDialog();

            case TESTABLE_TAG_REMINDER_DIALOG_ID:
                return createTestableTagReminderDialog();

            case WRITE_PROGRESS_DIALOG_ID:
                return createWriteProgressDialog();

            case READ_PROGRESS_DIALOG_ID:
                return createReadProgressDialog();

            case VERIFY_RESULT_DIALOG_ID:
                return createVerifyResultDialog();

            default:
                return super.onCreateDialog(id, args);
        }
    }

    private AlertDialog createTestableTagDiscoveredDialog() {
        return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle(R.string.nfc_write_tag_title)
                .setMessage(R.string.nfc_write_tag_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new WriteTagTask().execute(mTag);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        goToWriteStep();
                    }
                })
                .show();
    }

    private AlertDialog createTestableTagReminderDialog() {
        return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.nfc_wrong_tag_title)
                .setMessage(getString(R.string.nfc_scan_tag, mTechClass.getSimpleName()))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private ProgressDialog createWriteProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.nfc_writing_tag));
        return dialog;
    }

    private ProgressDialog createReadProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.nfc_reading_tag));
        return dialog;
    }

    private AlertDialog createVerifyResultDialog() {
        // Placeholder title and message that will be set properly in onPrepareDialog
        return new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.nfc_result_failure)
                .setMessage("")
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        switch (id) {
            case VERIFY_RESULT_DIALOG_ID:
                prepareVerifyResultDialog(dialog, args);
                break;

            default:
                super.onPrepareDialog(id, dialog, args);
                break;
        }
    }

    private void prepareVerifyResultDialog(Dialog dialog, Bundle args) {
        CharSequence expectedContent = args.getCharSequence(EXPECTED_CONTENT_ID);
        CharSequence actualContent = args.getCharSequence(ACTUAL_CONTENT_ID);
        boolean isMatch = args.getBoolean(IS_MATCH_ID);

        AlertDialog alert = (AlertDialog) dialog;
        alert.setTitle(isMatch
                ? R.string.nfc_result_success
                : R.string.nfc_result_failure);
        alert.setMessage(getString(R.string.nfc_result_message, expectedContent, actualContent));
    }

    @Override
    public String getTestId() {
        return getTagTestId(mTechClass);
    }

    static String getTagTestId(Class<?> primaryTech) {
        return NfcTestActivity.class.getName() + "_" + primaryTech.getSimpleName();
    }
}
