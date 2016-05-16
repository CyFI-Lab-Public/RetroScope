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

package com.android.mail.ui;

import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.android.mail.R;
import com.android.mail.browse.MessageAttachmentTile;
import com.android.mail.compose.ComposeAttachmentTile;
import com.android.mail.providers.Attachment;
import com.android.mail.ui.AttachmentTile.AttachmentPreview;
import com.android.mail.ui.AttachmentTile.AttachmentPreviewCache;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Acts as a grid composed of {@link AttachmentTile}s.
 */
public class AttachmentTileGrid extends FrameLayout implements AttachmentPreviewCache {
    private final LayoutInflater mInflater;
    private Uri mAttachmentsListUri;
    private final int mTileMinSize;
    private int mColumnCount;
    private List<Attachment> mAttachments;
    private final HashMap<String, AttachmentPreview> mAttachmentPreviews;
    private FragmentManager mFragmentManager;

    public AttachmentTileGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInflater = LayoutInflater.from(context);
        mTileMinSize = context.getResources()
                .getDimensionPixelSize(R.dimen.attachment_tile_min_size);
        mAttachmentPreviews = Maps.newHashMap();
    }

    /**
     * Configures the grid to add {@link Attachment}s information to the views.
     */
    public void configureGrid(FragmentManager fragmentManager, Uri attachmentsListUri,
            List<Attachment> list, boolean loaderResult) {
        mFragmentManager = fragmentManager;
        mAttachmentsListUri = attachmentsListUri;
        mAttachments = list;
        // Adding tiles to grid and filling in attachment information
        int index = 0;
        for (Attachment attachment : list) {
            addMessageTileFromAttachment(attachment, index++, loaderResult);
        }
    }

    private void addMessageTileFromAttachment(Attachment attachment, int index,
            boolean loaderResult) {
        final MessageAttachmentTile attachmentTile;

        if (getChildCount() <= index) {
            attachmentTile = MessageAttachmentTile.inflate(mInflater, this);
            attachmentTile.initialize(mFragmentManager);
            addView(attachmentTile);
        } else {
            attachmentTile = (MessageAttachmentTile) getChildAt(index);
        }

        attachmentTile.render(attachment, mAttachmentsListUri, index, this, loaderResult);
    }

    public ComposeAttachmentTile addComposeTileFromAttachment(Attachment attachment) {
        final ComposeAttachmentTile attachmentTile =
                ComposeAttachmentTile.inflate(mInflater, this);

        addView(attachmentTile);
        attachmentTile.render(attachment, null, -1, this, false);

        return attachmentTile;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        onMeasureForTiles(widthMeasureSpec);
    }

    private void onMeasureForTiles(int widthMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);

        final int childCount = getChildCount();
        if (childCount == 0) {
            // Just in case...
            setMeasuredDimension(width, 0);
            return;
        }

        // Divide width by minimum tile size to get the number of columns.
        // Truncation will ensure that the minimum will always be the minimum
        // but that the tiles can (and likely will) grow larger.
        mColumnCount = width / mTileMinSize;

        // Just in case...
        if (mColumnCount == 0) {
            mColumnCount = 1;
        }

        // 1. Calculate image size.
        //      = [total width] / [child count]
        //
        // 2. Set it to width/height of each children.
        //    If we have a remainder, some tiles will have
        //    1 pixel larger width than its height.
        //
        // 3. Set the dimensions of itself.
        //    Let width = given width.
        //    Let height = image size + bottom padding.

        final int imageSize = (width) / mColumnCount;
        final int remainder = width - (imageSize * mColumnCount);

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            // Compensate for the remainder
            final int childWidth = imageSize + (i < remainder ? 1 : 0);
            child.measure(
                    MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(imageSize, MeasureSpec.EXACTLY)
                    );
        }

        // Calculate the number of rows so we can get the proper height.
        // Then multiply by the height of one tile to get the grid height.
        final int numRows = ((childCount - 1) / mColumnCount) + 1;
        setMeasuredDimension(width,
                numRows*(imageSize + getChildAt(0).getPaddingBottom()));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        onLayoutForTiles();
    }

    private void onLayoutForTiles() {
        final int count = getChildCount();
        int childLeft = 0;
        int childTop = 0;
        boolean skipBeginningOfRowFirstTime = true;

        // Layout the grid.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);

            // Note MeasuredWidth and MeasuredHeight include the padding.
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();

            // If we're at the beginning of a row and it is not the first row
            // in the grid, reset childLeft to 0 and update childTop
            // to reflect the top of the new row.
            if (!skipBeginningOfRowFirstTime && i % mColumnCount == 0) {
                childLeft = 0;
                childTop += childHeight;
            } else {
                skipBeginningOfRowFirstTime = false;
            }

            child.layout(childLeft, childTop,
                    childLeft + childWidth, childTop + childHeight);
            childLeft += childWidth;
        }
    }

    @Override
    public void sendAccessibilityEvent(int eventType) {
        // This method is called when the child tile is INVISIBLE (meaning "empty"), and the
        // Accessibility Manager needs to find alternative content description to speak.
        // Here, we ignore the default behavior, since we don't want to let the manager speak
        // a contact name for the tile next to the INVISIBLE tile.
    }

    public List<Attachment> getAttachments() {
        return mAttachments;
    }

    public ArrayList<AttachmentPreview> getAttachmentPreviews() {
        return Lists.newArrayList(mAttachmentPreviews.values());
    }

    public void setAttachmentPreviews(ArrayList<AttachmentPreview> previews) {
        if (previews != null) {
            for (AttachmentPreview preview : previews) {
                mAttachmentPreviews.put(preview.attachmentIdentifier, preview);
            }
        }
    }

    /*
     * Save the preview for an attachment
     */
    @Override
    public void set(Attachment attachment, Bitmap preview) {
        final String attachmentIdentifier = attachment.getIdentifierUri().toString();
        if (attachmentIdentifier != null) {
            mAttachmentPreviews.put(
                    attachmentIdentifier, new AttachmentPreview(attachment, preview));
        }
    }

    /*
     * Returns a saved preview that was previously set
     */
    @Override
    public Bitmap get(Attachment attachment) {
        final String attachmentIdentifier = attachment.getIdentifierUri().toString();
        if (attachmentIdentifier != null) {
            final AttachmentPreview attachmentPreview = mAttachmentPreviews.get(
                    attachmentIdentifier);
            if (attachmentPreview != null) {
                return attachmentPreview.preview;
            }
        }
        return null;
    }
}
