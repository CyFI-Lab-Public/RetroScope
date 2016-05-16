/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.mail.browse;


import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Parcelable;

import com.android.mail.providers.Attachment;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AttachmentColumns;
import com.android.mail.providers.UIProvider.AttachmentContentValueKeys;
import com.android.mail.providers.UIProvider.AttachmentDestination;
import com.android.mail.providers.UIProvider.AttachmentState;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

import java.util.ArrayList;

public class AttachmentActionHandler {
    private static final String PROGRESS_FRAGMENT_TAG = "attachment-progress";

    private Attachment mAttachment;

    private final AttachmentCommandHandler mCommandHandler;
    private final AttachmentViewInterface mView;
    private final Context mContext;
    private final Handler mHandler;
    private FragmentManager mFragmentManager;
    private boolean mViewOnFinish;

    private static final String LOG_TAG = LogTag.getLogTag();

    public AttachmentActionHandler(Context context, AttachmentViewInterface view) {
        mCommandHandler = new AttachmentCommandHandler(context);
        mView = view;
        mContext = context;
        mHandler = new Handler();
        mViewOnFinish = true;
    }

    public void initialize(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    public void setAttachment(Attachment attachment) {
        mAttachment = attachment;
    }

    public void setViewOnFinish(boolean viewOnFinish) {
        mViewOnFinish = viewOnFinish;
    }

    public void showAttachment(int destination) {
        if (mView == null) {
            return;
        }

        // If the caller requested that this attachments be saved to the external storage, we should
        // verify that the it was saved there.
        if (mAttachment.isPresentLocally() &&
                (destination == AttachmentDestination.CACHE ||
                        mAttachment.destination == destination)) {
            mView.viewAttachment();
        } else {
            showDownloadingDialog();
            startDownloadingAttachment(destination);
        }
    }

    /**
     * Start downloading the full size attachment set with
     * {@link #setAttachment(Attachment)} immediately.
     */
    public void startDownloadingAttachment(int destination) {
        startDownloadingAttachment(destination, UIProvider.AttachmentRendition.BEST, 0, false);
    }

    public void startDownloadingAttachment(
            int destination, int rendition, int additionalPriority, boolean delayDownload) {
        startDownloadingAttachment(
                mAttachment, destination, rendition, additionalPriority, delayDownload);
    }

    private void startDownloadingAttachment(
            Attachment attachment, int destination, int rendition, int additionalPriority,
            boolean delayDownload) {
        final ContentValues params = new ContentValues(5);
        params.put(AttachmentColumns.STATE, AttachmentState.DOWNLOADING);
        params.put(AttachmentColumns.DESTINATION, destination);
        params.put(AttachmentContentValueKeys.RENDITION, rendition);
        params.put(AttachmentContentValueKeys.ADDITIONAL_PRIORITY, additionalPriority);
        params.put(AttachmentContentValueKeys.DELAY_DOWNLOAD, delayDownload);

        mCommandHandler.sendCommand(attachment.uri, params);
    }

    public void cancelAttachment() {
        final ContentValues params = new ContentValues(1);
        params.put(AttachmentColumns.STATE, AttachmentState.NOT_SAVED);

        mCommandHandler.sendCommand(mAttachment.uri, params);
    }

    public void startRedownloadingAttachment(Attachment attachment) {
        final ContentValues params = new ContentValues(2);
        params.put(AttachmentColumns.STATE, AttachmentState.REDOWNLOADING);
        params.put(AttachmentColumns.DESTINATION, attachment.destination);

        mCommandHandler.sendCommand(attachment.uri, params);
    }

    /**
     * Displays a loading dialog to be used for downloading attachments.
     * Must be called on the UI thread.
     */
    public void showDownloadingDialog() {
        final FragmentTransaction ft = mFragmentManager.beginTransaction();
        final Fragment prev = mFragmentManager.findFragmentByTag(PROGRESS_FRAGMENT_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

         // Create and show the dialog.
        final DialogFragment newFragment = AttachmentProgressDialogFragment.newInstance(
                mAttachment);
        newFragment.show(ft, PROGRESS_FRAGMENT_TAG);
    }

    /**
     * Update progress-related views. Will also trigger a view intent if a progress dialog was
     * previously brought up (by tapping 'View') and the download has now finished.
     */
    public void updateStatus(boolean loaderResult) {
        if (mView == null) {
            return;
        }

        final boolean showProgress = mAttachment.shouldShowProgress();

        final AttachmentProgressDialogFragment dialog = (AttachmentProgressDialogFragment)
                mFragmentManager.findFragmentByTag(PROGRESS_FRAGMENT_TAG);
        if (dialog != null && dialog.isShowingDialogForAttachment(mAttachment)) {
            dialog.setProgress(mAttachment.downloadedSize);

            // We don't want the progress bar to switch back to indeterminate mode after
            // have been in determinate progress mode.
            final boolean indeterminate = !showProgress && dialog.isIndeterminate();
            dialog.setIndeterminate(indeterminate);

            if (loaderResult && mAttachment.isDownloadFinishedOrFailed()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });
            }

            if (mAttachment.state == AttachmentState.SAVED && mViewOnFinish) {
                mView.viewAttachment();
            }
        } else {
            mView.updateProgress(showProgress);
        }

        // Call on update status for the view so that it can do some specific things.
        mView.onUpdateStatus();
    }

    public boolean isProgressDialogVisible() {
        final Fragment dialog = mFragmentManager.findFragmentByTag(PROGRESS_FRAGMENT_TAG);
        return dialog != null && dialog.isVisible();
    }

    public void shareAttachment() {
        if (mAttachment.contentUri == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        final Uri uri = Utils.normalizeUri(mAttachment.contentUri);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setType(Utils.normalizeMimeType(mAttachment.getContentType()));

        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // couldn't find activity for SEND intent
            LogUtils.e(LOG_TAG, "Couldn't find Activity for intent", e);
        }
    }

    public void shareAttachments(ArrayList<Parcelable> uris) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

        intent.setType("image/*");
        intent.putParcelableArrayListExtra(
                Intent.EXTRA_STREAM, uris);

        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // couldn't find activity for SEND_MULTIPLE intent
            LogUtils.e(LOG_TAG, "Couldn't find Activity for intent", e);
        }
    }
}
