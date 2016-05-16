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

import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.android.ex.photo.util.ImageUtils;
import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.photo.MailPhotoViewActivity;
import com.android.mail.providers.Attachment;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.AttachmentDestination;
import com.android.mail.providers.UIProvider.AttachmentRendition;
import com.android.mail.ui.AttachmentTile;
import com.android.mail.ui.AttachmentTileGrid;
import com.android.mail.utils.AttachmentUtils;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * View for a single attachment in conversation view. Shows download status and allows launching
 * intents to act on an attachment.
 *
 */
public class MessageAttachmentTile extends AttachmentTile implements OnClickListener,
        AttachmentViewInterface {

    private int mPhotoIndex;
    private Uri mAttachmentsListUri;
    private View mTextContainer;

    private final AttachmentActionHandler mActionHandler;

    private static final String LOG_TAG = LogTag.getLogTag();

    public MessageAttachmentTile(Context context) {
        this(context, null);
    }

    public MessageAttachmentTile(Context context, AttributeSet attrs) {
        super(context, attrs);

        mActionHandler = new AttachmentActionHandler(context, this);
    }

    public void initialize(FragmentManager fragmentManager) {
        mActionHandler.initialize(fragmentManager);
    }

    /**
     * Render or update an attachment's view. This happens immediately upon instantiation, and
     * repeatedly as status updates stream in, so only properties with new or changed values will
     * cause sub-views to update.
     */
    @Override
    public void render(Attachment attachment, Uri attachmentsListUri, int index,
            AttachmentPreviewCache attachmentPreviewCache, boolean loaderResult) {
        super.render(attachment, attachmentsListUri, index, attachmentPreviewCache, loaderResult);

        mAttachmentsListUri = attachmentsListUri;
        mPhotoIndex = index;

        mActionHandler.setAttachment(mAttachment);
        mActionHandler.updateStatus(loaderResult);
    }

    public static MessageAttachmentTile inflate(LayoutInflater inflater, ViewGroup parent) {
        MessageAttachmentTile view = (MessageAttachmentTile) inflater.inflate(
                R.layout.conversation_message_attachment_tile, parent, false);
        return view;
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTextContainer = findViewById(R.id.attachment_tile_text_container);

        setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        onClick();
    }

    private boolean onClick() {
        showAndDownloadAttachments();
        return true;
    }

    private void showAndDownloadAttachments() {
        AttachmentTileGrid tileGrid = ((AttachmentTileGrid) getParent());
        int childCount = tileGrid.getChildCount();

        PriorityQueue<MessageAttachmentTile> queue = new PriorityQueue<MessageAttachmentTile>(
                childCount, new ViewIndexDistanceComparator(mPhotoIndex));
        for (int i = 0; i < childCount; i++) {
            MessageAttachmentTile tile = (MessageAttachmentTile) tileGrid.getChildAt(i);
            queue.add(tile);
        }

        // we want our downloads to have higher priority than the highest background downloads
        int maxAdditionalPriority = childCount;
        for (int i = 0; i < childCount; i++) {
            // higher priority tiles are returned first
            MessageAttachmentTile tile = queue.remove();
            tile.downloadAttachment(maxAdditionalPriority - i, i != 0);
        }

        viewAttachment();
    }

    public void downloadAttachment(int additionalPriority, boolean delayDownload) {
        if (!mAttachment.isPresentLocally()) {
            mActionHandler.startDownloadingAttachment(AttachmentDestination.CACHE,
                    UIProvider.AttachmentRendition.BEST, additionalPriority, delayDownload);
        }
    }

    @Override
    public void viewAttachment() {
        final String mime = Utils.normalizeMimeType(mAttachment.getContentType());

        Analytics.getInstance()
                .sendEvent("view_attachment", mime, "attachment_tile", mAttachment.size);

        if (ImageUtils.isImageMimeType(mime)) {
            MailPhotoViewActivity
                    .startMailPhotoViewActivity(getContext(), mAttachmentsListUri, mPhotoIndex);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        Utils.setIntentDataAndTypeAndNormalize(
                intent, mAttachment.contentUri, mime);
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // couldn't find activity for View intent
            LogUtils.e(LOG_TAG, "Couldn't find Activity for intent", e);
        }
    }

    @Override
    public void updateProgress(boolean showDeterminateProgress) {
        // do not show progress for image tiles
    }

    @Override
    public void onUpdateStatus() {
    }

    @Override
    public void setThumbnailToDefault() {
        super.setThumbnailToDefault();
        mTextContainer.setVisibility(VISIBLE);
    }

    @Override
    public void setThumbnail(Bitmap result) {
        super.setThumbnail(result);
        mTextContainer.setVisibility(GONE);
    }

    @Override
    public void thumbnailLoadFailed() {
        super.thumbnailLoadFailed();

        if (AttachmentUtils.canDownloadAttachment(getContext(), null)) {
            // Download if there is network. This check prevents the attachment
            // download from failing and making the error toast show
            mActionHandler.startDownloadingAttachment(
                    AttachmentDestination.CACHE, AttachmentRendition.SIMPLE, 0, false);
        }
    }

    /**
     * Given two child views, figure out whose index is closest to the specified
     * index.
     */
    public static class ViewIndexDistanceComparator implements Comparator<View>{
        final private int mIndex;
        /**
         * @param index Compare based on each view's distance to this index
         */
        public ViewIndexDistanceComparator(int index) {
            mIndex = index;
        }

        @Override
        public int compare(View lhs, View rhs) {
            ViewParent parent = lhs.getParent();
            if (parent == rhs.getParent()) {
                if (parent instanceof ViewGroup) {
                    ViewGroup p = (ViewGroup) parent;
                    int lhsIndex = p.indexOfChild(lhs);
                    int rhsIndex = p.indexOfChild(rhs);
                    int lhsDistance = Math.abs(mIndex - lhsIndex);
                    int rhsDistance = Math.abs(mIndex - rhsIndex);
                    // prefer shorter distance since they are the next ones to be swiped to
                    int result = lhsDistance - rhsDistance;
                    if (result == 0) {
                        // prefer higher index since they are to the right in the photoviewer
                        return rhsIndex - lhsIndex;
                    }
                    return result;
                }
            }
            return 0;
        }
    }
}
