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

package com.android.mail.photo;

import android.app.ActionBar;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.ex.photo.Intents;
import com.android.ex.photo.PhotoViewActivity;
import com.android.ex.photo.fragments.PhotoViewFragment;
import com.android.ex.photo.views.ProgressBarWrapper;
import com.android.mail.R;
import com.android.mail.browse.AttachmentActionHandler;
import com.android.mail.providers.Attachment;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AttachmentDestination;
import com.android.mail.providers.UIProvider.AttachmentState;
import com.android.mail.utils.AttachmentUtils;
import com.android.mail.utils.Utils;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Derives from {@link PhotoViewActivity} to allow customization
 * to the {@link ActionBar} from the default implementation.
 */
public class MailPhotoViewActivity extends PhotoViewActivity {
    private MenuItem mSaveItem;
    private MenuItem mSaveAllItem;
    private MenuItem mShareItem;
    private MenuItem mShareAllItem;
    /**
     * Only for attachments that are currently downloading. Attachments that failed show the
     * retry button.
     */
    private MenuItem mDownloadAgainItem;
    private AttachmentActionHandler mActionHandler;
    private Menu mMenu;

    /**
     * Start a new MailPhotoViewActivity to view the given images.
     *
     * @param imageListUri The uri to query for the images that you want to view. The resulting
     *                     cursor must have the columns as defined in
     *                     {@link com.android.ex.photo.provider.PhotoContract.PhotoViewColumns}.
     * @param photoIndex The index of the photo to show first.
     */
    public static void startMailPhotoViewActivity(final Context context, final Uri imageListUri,
            final int photoIndex) {
        final Intents.PhotoViewIntentBuilder builder =
                Intents.newPhotoViewIntentBuilder(context, MailPhotoViewActivity.class);
        builder
                .setPhotosUri(imageListUri.toString())
                .setProjection(UIProvider.ATTACHMENT_PROJECTION)
                .setPhotoIndex(photoIndex);

        context.startActivity(builder.build());
    }

    /**
     * Start a new MailPhotoViewActivity to view the given images.
     *
     * @param imageListUri The uri to query for the images that you want to view. The resulting
     *                     cursor must have the columns as defined in
     *                     {@link com.android.ex.photo.provider.PhotoContract.PhotoViewColumns}.
     * @param initialPhotoUri The uri of the photo to show first.
     */
    public static void startMailPhotoViewActivity(final Context context, final Uri imageListUri,
            final String initialPhotoUri) {
        final Intents.PhotoViewIntentBuilder builder =
                Intents.newPhotoViewIntentBuilder(context, MailPhotoViewActivity.class);
        builder
                .setPhotosUri(imageListUri.toString())
                .setProjection(UIProvider.ATTACHMENT_PROJECTION)
                .setInitialPhotoUri(initialPhotoUri);

        context.startActivity(builder.build());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);

        mActionHandler = new AttachmentActionHandler(this, null);
        mActionHandler.initialize(getFragmentManager());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.photo_view_menu, menu);
        mMenu = menu;

        mSaveItem = mMenu.findItem(R.id.menu_save);
        mSaveAllItem = mMenu.findItem(R.id.menu_save_all);
        mShareItem = mMenu.findItem(R.id.menu_share);
        mShareAllItem = mMenu.findItem(R.id.menu_share_all);
        mDownloadAgainItem = mMenu.findItem(R.id.menu_download_again);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateActionItems();
        return true;
    }

    /**
     * Updates the action items to tweak their visibility in case
     * there is functionality that is not relevant (eg, the Save
     * button should not appear if the photo has already been saved).
     */
    @Override
    protected void updateActionItems() {
        final boolean runningJellyBeanOrLater = Utils.isRunningJellybeanOrLater();
        final Attachment attachment = getCurrentAttachment();

        if (attachment != null && mSaveItem != null && mShareItem != null) {
            mSaveItem.setEnabled(!attachment.isDownloading()
                    && attachment.canSave() && !attachment.isSavedToExternal());
            mShareItem.setEnabled(attachment.canShare());
            mDownloadAgainItem.setEnabled(attachment.canSave() && attachment.isDownloading());
        } else {
            if (mMenu != null) {
                mMenu.setGroupEnabled(R.id.photo_view_menu_group, false);
            }
            return;
        }

        List<Attachment> attachments = getAllAttachments();
        if (attachments != null) {
            boolean enabled = false;
            for (final Attachment a : attachments) {
                // If one attachment can be saved, enable save all
                if (!a.isDownloading() && a.canSave() && !a.isSavedToExternal()) {
                    enabled = true;
                    break;
                }
            }
            mSaveAllItem.setEnabled(enabled);

            // all attachments must be present to be able to share all
            enabled = true;
            for (final Attachment a : attachments) {
                if (!a.canShare()) {
                    enabled = false;
                    break;
                }
            }
            mShareAllItem.setEnabled(enabled);
        }

        // Turn off the functionality that only works on JellyBean.
        if (!runningJellyBeanOrLater) {
            mShareItem.setVisible(false);
            mShareAllItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // app icon in action bar clicked; go back to conversation
            finish();
            return true;
        } else if (itemId == R.id.menu_save) { // save the current photo
            saveAttachment();
            return true;
        } else if (itemId == R.id.menu_save_all) { // save all of the photos
            saveAllAttachments();
            return true;
        } else if (itemId == R.id.menu_share) { // share the current photo
            shareAttachment();
            return true;
        } else if (itemId == R.id.menu_share_all) { // share all of the photos
            shareAllAttachments();
            return true;
        } else if (itemId == R.id.menu_download_again) { // redownload the current photo
            redownloadAttachment();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adjusts the activity title and subtitle to reflect the image name and size.
     */
    @Override
    protected void updateActionBar() {
        super.updateActionBar();

        final Attachment attachment = getCurrentAttachment();
        final ActionBar actionBar = getActionBar();
        final String size = AttachmentUtils.convertToHumanReadableSize(this, attachment.size);

        // update the status
        // There are 3 states
        //      1. Saved, Attachment Size
        //      2. Saving...
        //      3. Default, Attachment Size
        if (attachment.isSavedToExternal()) {
            actionBar.setSubtitle(getResources().getString(R.string.saved, size));
        } else if (attachment.isDownloading() &&
                attachment.destination == AttachmentDestination.EXTERNAL) {
                actionBar.setSubtitle(R.string.saving);
        } else {
            actionBar.setSubtitle(size);
        }
        updateActionItems();
    }

    @Override
    public void onFragmentVisible(PhotoViewFragment fragment) {
        super.onFragmentVisible(fragment);
        final Attachment attachment = getCurrentAttachment();
        if (attachment.state == AttachmentState.PAUSED) {
            mActionHandler.setAttachment(attachment);
            mActionHandler.startDownloadingAttachment(attachment.destination);
        }
    }

    @Override
    public void onCursorChanged(PhotoViewFragment fragment, Cursor cursor) {
        super.onCursorChanged(fragment, cursor);
        updateProgressAndEmptyViews(fragment, new Attachment(cursor));
    }

    /**
     * Updates the empty views of the fragment based upon the current
     * state of the attachment.
     * @param fragment the current fragment
     */
    private void updateProgressAndEmptyViews(
            final PhotoViewFragment fragment, final Attachment attachment) {
        final ProgressBarWrapper progressBar = fragment.getPhotoProgressBar();
        final TextView emptyText = fragment.getEmptyText();
        final ImageView retryButton = fragment.getRetryButton();

        // update the progress
        if (attachment.shouldShowProgress()) {
            progressBar.setMax(attachment.size);
            progressBar.setProgress(attachment.downloadedSize);
            progressBar.setIndeterminate(false);
        } else if (fragment.isProgressBarNeeded()) {
            progressBar.setIndeterminate(true);
        }

        // If the download failed, show the empty text and retry button
        if (attachment.isDownloadFailed()) {
            emptyText.setText(R.string.photo_load_failed);
            emptyText.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.VISIBLE);
            retryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    redownloadAttachment();
                    emptyText.setVisibility(View.GONE);
                    retryButton.setVisibility(View.GONE);
                }
            });
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Save the current attachment.
     */
    private void saveAttachment() {
        saveAttachment(getCurrentAttachment());
    }

    /**
     * Redownloads the attachment.
     */
    private void redownloadAttachment() {
        final Attachment attachment = getCurrentAttachment();
        if (attachment != null && attachment.canSave()) {
            // REDOWNLOADING command is only for attachments that are finished or failed.
            // For an attachment that is downloading (or paused in the DownloadManager), we need to
            // cancel it first.
            mActionHandler.setAttachment(attachment);
            mActionHandler.cancelAttachment();
            mActionHandler.startDownloadingAttachment(attachment.destination);
        }
    }

    /**
     * Saves the attachment.
     * @param attachment the attachment to save.
     */
    private void saveAttachment(final Attachment attachment) {
        if (attachment != null && attachment.canSave()) {
            mActionHandler.setAttachment(attachment);
            mActionHandler.startDownloadingAttachment(AttachmentDestination.EXTERNAL);
        }
    }

    /**
     * Save all of the attachments in the cursor.
     */
    private void saveAllAttachments() {
        Cursor cursor = getCursorAtProperPosition();

        if (cursor == null) {
            return;
        }

        int i = -1;
        while (cursor.moveToPosition(++i)) {
            saveAttachment(new Attachment(cursor));
        }
    }

    /**
     * Share the current attachment.
     */
    private void shareAttachment() {
        shareAttachment(getCurrentAttachment());
    }

    /**
     * Shares the attachment
     * @param attachment the attachment to share
     */
    private void shareAttachment(final Attachment attachment) {
        if (attachment != null) {
            mActionHandler.setAttachment(attachment);
            mActionHandler.shareAttachment();
        }
    }

    /**
     * Share all of the attachments in the cursor.
     */
    private void shareAllAttachments() {
        Cursor cursor = getCursorAtProperPosition();

        if (cursor == null) {
            return;
        }

        ArrayList<Parcelable> uris = new ArrayList<Parcelable>();
        int i = -1;
        while (cursor.moveToPosition(++i)) {
            uris.add(Utils.normalizeUri(new Attachment(cursor).contentUri));
        }

        mActionHandler.shareAttachments(uris);
    }

    /**
     * Helper method to get the currently visible attachment.
     */
    protected Attachment getCurrentAttachment() {
        final Cursor cursor = getCursorAtProperPosition();

        if (cursor == null) {
            return null;
        }

        return new Attachment(cursor);
    }

    private List<Attachment> getAllAttachments() {
        final Cursor cursor = getCursor();

        if (cursor == null || cursor.isClosed() || !cursor.moveToFirst()) {
            return null;
        }

        List<Attachment> list = Lists.newArrayList();
        do {
            list.add(new Attachment(cursor));
        } while (cursor.moveToNext());

        return list;
    }
}
