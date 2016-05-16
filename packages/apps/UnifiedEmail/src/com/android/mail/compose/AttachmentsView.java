/**
 * Copyright (c) 2011, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.mail.compose;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.android.mail.R;
import com.android.mail.providers.Account;
import com.android.mail.providers.Attachment;
import com.android.mail.ui.AttachmentTile;
import com.android.mail.ui.AttachmentTileGrid;
import com.android.mail.ui.AttachmentTile.AttachmentPreview;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/*
 * View for displaying attachments in the compose screen.
 */
class AttachmentsView extends LinearLayout {
    private static final String LOG_TAG = LogTag.getLogTag();

    private final ArrayList<Attachment> mAttachments;
    private AttachmentAddedOrDeletedListener mChangeListener;
    private AttachmentTileGrid mTileGrid;
    private LinearLayout mAttachmentLayout;

    public AttachmentsView(Context context) {
        this(context, null);
    }

    public AttachmentsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAttachments = Lists.newArrayList();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTileGrid = (AttachmentTileGrid) findViewById(R.id.attachment_tile_grid);
        mAttachmentLayout = (LinearLayout) findViewById(R.id.attachment_bar_list);
    }

    public void expandView() {
        mTileGrid.setVisibility(VISIBLE);
        mAttachmentLayout.setVisibility(VISIBLE);

        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }

    /**
     * Set a listener for changes to the attachments.
     * @param listener
     */
    public void setAttachmentChangesListener(AttachmentAddedOrDeletedListener listener) {
        mChangeListener = listener;
    }

    /**
     * Add an attachment and update the ui accordingly.
     * @param attachment
     */
    public void addAttachment(final Attachment attachment) {
        if (!isShown()) {
            setVisibility(View.VISIBLE);
        }

        mAttachments.add(attachment);
        expandView();

        // If we have an attachment that should be shown in a tiled look,
        // set up the tile and add it to the tile grid.
        if (AttachmentTile.isTiledAttachment(attachment)) {
            final ComposeAttachmentTile attachmentTile =
                    mTileGrid.addComposeTileFromAttachment(attachment);
            attachmentTile.addDeleteListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAttachment(attachmentTile, attachment);
                }
            });
        // Otherwise, use the old bar look and add it to the new
        // inner LinearLayout.
        } else {
            final AttachmentComposeView attachmentView =
                new AttachmentComposeView(getContext(), attachment);

            attachmentView.addDeleteListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteAttachment(attachmentView, attachment);
                }
            });


            mAttachmentLayout.addView(attachmentView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
        }
        if (mChangeListener != null) {
            mChangeListener.onAttachmentAdded();
        }
    }

    @VisibleForTesting
    protected void deleteAttachment(final View attachmentView,
            final Attachment attachment) {
        mAttachments.remove(attachment);
        ((ViewGroup) attachmentView.getParent()).removeView(attachmentView);
        if (mChangeListener != null) {
            mChangeListener.onAttachmentDeleted();
        }
    }

    /**
     * Get all attachments being managed by this view.
     * @return attachments.
     */
    public ArrayList<Attachment> getAttachments() {
        return mAttachments;
    }

    /**
     * Get all attachments previews that have been loaded
     * @return attachments previews.
     */
    public ArrayList<AttachmentPreview> getAttachmentPreviews() {
        return mTileGrid.getAttachmentPreviews();
    }

    /**
     * Call this on restore instance state so previews persist across configuration changes
     */
    public void setAttachmentPreviews(ArrayList<AttachmentPreview> previews) {
        mTileGrid.setAttachmentPreviews(previews);
    }

    /**
     * Delete all attachments being managed by this view.
     */
    public void deleteAllAttachments() {
        mAttachments.clear();
        mTileGrid.removeAllViews();
        mAttachmentLayout.removeAllViews();
        setVisibility(GONE);
    }

    /**
     * Get the total size of all attachments currently in this view.
     */
    public long getTotalAttachmentsSize() {
        long totalSize = 0;
        for (Attachment attachment : mAttachments) {
            totalSize += attachment.size;
        }
        return totalSize;
    }

    /**
     * Interface to implement to be notified about changes to the attachments
     * explicitly made by the user.
     */
    public interface AttachmentAddedOrDeletedListener {
        public void onAttachmentDeleted();

        public void onAttachmentAdded();
    }

    /**
     * Generate an {@link Attachment} object for a given local content URI. Attempts to populate
     * the {@link Attachment#name}, {@link Attachment#size}, and {@link Attachment#contentType}
     * fields using a {@link ContentResolver}.
     *
     * @param contentUri
     * @return an Attachment object
     * @throws AttachmentFailureException
     */
    public Attachment generateLocalAttachment(Uri contentUri) throws AttachmentFailureException {
        // FIXME: do not query resolver for type on the UI thread
        final ContentResolver contentResolver = getContext().getContentResolver();
        String contentType = contentResolver.getType(contentUri);
        if (contentUri == null || TextUtils.isEmpty(contentUri.getPath())) {
            throw new AttachmentFailureException("Failed to create local attachment");
        }

        if (contentType == null) contentType = "";

        final Attachment attachment = new Attachment();
        attachment.uri = null; // URI will be assigned by the provider upon send/save
        attachment.setName(null);
        attachment.size = 0;
        attachment.contentUri = contentUri;
        attachment.thumbnailUri = contentUri;

        Cursor metadataCursor = null;
        try {
            metadataCursor = contentResolver.query(
                    contentUri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                    null, null, null);
            if (metadataCursor != null) {
                try {
                    if (metadataCursor.moveToNext()) {
                        attachment.setName(metadataCursor.getString(0));
                        attachment.size = metadataCursor.getInt(1);
                    }
                } finally {
                    metadataCursor.close();
                }
            }
        } catch (SQLiteException ex) {
            // One of the two columns is probably missing, let's make one more attempt to get at
            // least one.
            // Note that the documentations in Intent#ACTION_OPENABLE and
            // OpenableColumns seem to contradict each other about whether these columns are
            // required, but it doesn't hurt to fail properly.

            // Let's try to get DISPLAY_NAME
            try {
                metadataCursor = getOptionalColumn(contentResolver, contentUri,
                        OpenableColumns.DISPLAY_NAME);
                if (metadataCursor != null && metadataCursor.moveToNext()) {
                    attachment.setName(metadataCursor.getString(0));
                }
            } finally {
                if (metadataCursor != null) metadataCursor.close();
            }

            // Let's try to get SIZE
            try {
                metadataCursor =
                        getOptionalColumn(contentResolver, contentUri, OpenableColumns.SIZE);
                if (metadataCursor != null && metadataCursor.moveToNext()) {
                    attachment.size = metadataCursor.getInt(0);
                } else {
                    // Unable to get the size from the metadata cursor. Open the file and seek.
                    attachment.size = getSizeFromFile(contentUri, contentResolver);
                }
            } finally {
                if (metadataCursor != null) metadataCursor.close();
            }
        } catch (SecurityException e) {
            throw new AttachmentFailureException("Security Exception from attachment uri", e);
        }

        if (attachment.getName() == null) {
            attachment.setName(contentUri.getLastPathSegment());
        }
        if (attachment.size == 0) {
            // if the attachment is not a content:// for example, a file:// URI
            attachment.size = getSizeFromFile(contentUri, contentResolver);
        }

        attachment.setContentType(contentType);
        return attachment;
    }

    /**
     * Adds a local attachment by file path.
     * @param account
     * @param contentUri the uri of the local file path
     *
     * @return size of the attachment added.
     * @throws AttachmentFailureException if an error occurs adding the attachment.
     */
    public long addAttachment(Account account, Uri contentUri)
            throws AttachmentFailureException {
        return addAttachment(account, generateLocalAttachment(contentUri));
    }

    /**
     * Adds an attachment of either local or remote origin, checking to see if the attachment
     * exceeds file size limits.
     * @param account
     * @param attachment the attachment to be added.
     *
     * @return size of the attachment added.
     * @throws AttachmentFailureException if an error occurs adding the attachment.
     */
    public long addAttachment(Account account, Attachment attachment)
            throws AttachmentFailureException {
        final int maxSize = account.settings.getMaxAttachmentSize();

        // Error getting the size or the size was too big.
        if (attachment.size == -1 || attachment.size > maxSize) {
            throw new AttachmentFailureException(
                    "Attachment too large to attach", R.string.too_large_to_attach_single);
        } else if ((getTotalAttachmentsSize()
                + attachment.size) > maxSize) {
            throw new AttachmentFailureException(
                    "Attachment too large to attach", R.string.too_large_to_attach_additional);
        } else {
            addAttachment(attachment);
        }

        return attachment.size;
    }

    private static int getSizeFromFile(Uri uri, ContentResolver contentResolver) {
        int size = -1;
        ParcelFileDescriptor file = null;
        try {
            file = contentResolver.openFileDescriptor(uri, "r");
            size = (int) file.getStatSize();
        } catch (FileNotFoundException e) {
            LogUtils.w(LOG_TAG, e, "Error opening file to obtain size.");
        } finally {
            try {
                if (file != null) {
                    file.close();
                }
            } catch (IOException e) {
                LogUtils.w(LOG_TAG, "Error closing file opened to obtain size.");
            }
        }
        // We only want to return a non-negative value. (ParcelFileDescriptor#getStatSize() will
        // return -1 if the fd is not a file
        return Math.max(size, 0);
    }

    /**
     * @return a cursor to the requested column or null if an exception occurs while trying
     * to query it.
     */
    private static Cursor getOptionalColumn(ContentResolver contentResolver, Uri uri,
            String columnName) {
        Cursor result = null;
        try {
            result = contentResolver.query(uri, new String[]{columnName}, null, null, null);
        } catch (SQLiteException ex) {
            // ignore, leave result null
        }
        return result;
    }

    public void focusLastAttachment() {
        Attachment lastAttachment = mAttachments.get(mAttachments.size() - 1);
        View lastView = null;
        int last = 0;
        if (AttachmentTile.isTiledAttachment(lastAttachment)) {
            last = mTileGrid.getChildCount() - 1;
            if (last > 0) {
                lastView = mTileGrid.getChildAt(last);
            }
        } else {
            last = mAttachmentLayout.getChildCount() - 1;
            if (last > 0) {
                lastView = mAttachmentLayout.getChildAt(last);
            }
        }
        if (lastView != null) {
            lastView.requestFocus();
        }
    }

    /**
     * Class containing information about failures when adding attachments.
     */
    static class AttachmentFailureException extends Exception {
        private static final long serialVersionUID = 1L;
        private final int errorRes;

        public AttachmentFailureException(String detailMessage) {
            super(detailMessage);
            this.errorRes = R.string.generic_attachment_problem;
        }

        public AttachmentFailureException(String error, int errorRes) {
            super(error);
            this.errorRes = errorRes;
        }

        public AttachmentFailureException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
            this.errorRes = R.string.generic_attachment_problem;
        }

        /**
         * Get the error string resource that corresponds to this attachment failure. Always a valid
         * string resource.
         */
        public int getErrorRes() {
            return errorRes;
        }
    }
}
