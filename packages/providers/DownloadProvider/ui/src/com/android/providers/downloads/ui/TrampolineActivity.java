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

package com.android.providers.downloads.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.providers.downloads.Constants;
import com.android.providers.downloads.OpenHelper;

import libcore.io.IoUtils;

/**
 * Intercept all download clicks to provide special behavior. For example,
 * PackageInstaller really wants raw file paths.
 */
public class TrampolineActivity extends Activity {
    private static final String TAG_PAUSED = "paused";
    private static final String TAG_FAILED = "failed";

    private static final String KEY_ID = "id";
    private static final String KEY_REASON = "reason";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final long id = ContentUris.parseId(getIntent().getData());

        final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.setAccessAllDownloads(true);

        final int status;
        final int reason;

        final Cursor cursor = dm.query(new Query().setFilterById(id));
        try {
            if (cursor.moveToFirst()) {
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
            } else {
                Toast.makeText(this, R.string.dialog_file_missing_body, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } finally {
            IoUtils.closeQuietly(cursor);
        }

        Log.d(Constants.TAG, "Found " + id + " with status " + status + ", reason " + reason);
        switch (status) {
            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_RUNNING:
                sendRunningDownloadClickedBroadcast(id);
                finish();
                break;

            case DownloadManager.STATUS_PAUSED:
                if (reason == DownloadManager.PAUSED_QUEUED_FOR_WIFI) {
                    PausedDialogFragment.show(getFragmentManager(), id);
                } else {
                    sendRunningDownloadClickedBroadcast(id);
                    finish();
                }
                break;

            case DownloadManager.STATUS_SUCCESSFUL:
                if (!OpenHelper.startViewIntent(this, id, 0)) {
                    Toast.makeText(this, R.string.download_no_application_title, Toast.LENGTH_SHORT)
                            .show();
                }
                finish();
                break;

            case DownloadManager.STATUS_FAILED:
                FailedDialogFragment.show(getFragmentManager(), id, reason);
                break;
        }
    }

    private void sendRunningDownloadClickedBroadcast(long id) {
        final Intent intent = new Intent(Constants.ACTION_LIST);
        intent.setPackage(Constants.PROVIDER_PACKAGE_NAME);
        intent.putExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS, new long[] { id });
        sendBroadcast(intent);
    }

    public static class PausedDialogFragment extends DialogFragment {
        public static void show(FragmentManager fm, long id) {
            final PausedDialogFragment dialog = new PausedDialogFragment();
            final Bundle args = new Bundle();
            args.putLong(KEY_ID, id);
            dialog.setArguments(args);
            dialog.show(fm, TAG_PAUSED);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final DownloadManager dm = (DownloadManager) context.getSystemService(
                    Context.DOWNLOAD_SERVICE);
            dm.setAccessAllDownloads(true);

            final long id = getArguments().getLong(KEY_ID);

            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    context, AlertDialog.THEME_HOLO_LIGHT);
            builder.setTitle(R.string.dialog_title_queued_body);
            builder.setMessage(R.string.dialog_queued_body);

            builder.setPositiveButton(R.string.keep_queued_download, null);

            builder.setNegativeButton(
                    R.string.remove_download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dm.remove(id);
                        }
                    });

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            getActivity().finish();
        }
    }

    public static class FailedDialogFragment extends DialogFragment {
        public static void show(FragmentManager fm, long id, int reason) {
            final FailedDialogFragment dialog = new FailedDialogFragment();
            final Bundle args = new Bundle();
            args.putLong(KEY_ID, id);
            args.putInt(KEY_REASON, reason);
            dialog.setArguments(args);
            dialog.show(fm, TAG_FAILED);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final DownloadManager dm = (DownloadManager) context.getSystemService(
                    Context.DOWNLOAD_SERVICE);
            dm.setAccessAllDownloads(true);

            final long id = getArguments().getLong(KEY_ID);
            final int reason = getArguments().getInt(KEY_REASON);

            final AlertDialog.Builder builder = new AlertDialog.Builder(
                    context, AlertDialog.THEME_HOLO_LIGHT);
            builder.setTitle(R.string.dialog_title_not_available);

            final String message;
            switch (reason) {
                case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                    builder.setMessage(R.string.dialog_file_already_exists);
                    break;
                case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                    builder.setMessage(R.string.dialog_insufficient_space_on_external);
                    break;
                case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                    builder.setMessage(R.string.dialog_media_not_found);
                    break;
                case DownloadManager.ERROR_CANNOT_RESUME:
                    builder.setMessage(R.string.dialog_cannot_resume);
                    break;
                default:
                    builder.setMessage(R.string.dialog_failed_body);
            }

            builder.setNegativeButton(
                    R.string.delete_download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dm.remove(id);
                        }
                    });

            builder.setPositiveButton(
                    R.string.retry_download, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dm.restartDownload(id);
                        }
                    });

            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            getActivity().finish();
        }
    }
}
