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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Typeface;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.R.dimen;
import com.android.mail.R.id;
import com.android.mail.ui.ViewMode;
import com.android.mail.utils.Utils;
import com.google.common.base.Objects;

/**
 * Represents the coordinates of elements inside a CanvasConversationHeaderView
 * (eg, checkmark, star, subject, sender, folders, etc.) It will inflate a view,
 * and record the coordinates of each element after layout. This will allows us
 * to easily improve performance by creating custom view while still defining
 * layout in XML files.
 *
 * @author phamm
 */
public class ConversationItemViewCoordinates {
    // Modes
    static final int MODE_COUNT = 2;
    static final int WIDE_MODE = 0;
    static final int NORMAL_MODE = 1;

    // Left-side gadget modes
    static final int GADGET_NONE = 0;
    static final int GADGET_CONTACT_PHOTO = 1;
    static final int GADGET_CHECKBOX = 2;

    // Attachment previews modes
    static final int ATTACHMENT_PREVIEW_NONE = 0;
    static final int ATTACHMENT_PREVIEW_UNREAD = 1;
    static final int ATTACHMENT_PREVIEW_READ = 2;

    // For combined views
    private static int COLOR_BLOCK_WIDTH = -1;
    private static int COLOR_BLOCK_HEIGHT = -1;

    /**
     * Simple holder class for an item's abstract configuration state. ListView binding creates an
     * instance per item, and {@link #forConfig(Context, Config, SparseArray)} uses it to hide/show
     * optional views and determine the correct coordinates for that item configuration.
     */
    public static final class Config {
        private int mWidth;
        private int mViewMode = ViewMode.UNKNOWN;
        private int mGadgetMode = GADGET_NONE;
        private int mAttachmentPreviewMode = ATTACHMENT_PREVIEW_NONE;
        private boolean mShowFolders = false;
        private boolean mShowReplyState = false;
        private boolean mShowColorBlock = false;
        private boolean mShowPersonalIndicator = false;

        public Config setViewMode(int viewMode) {
            mViewMode = viewMode;
            return this;
        }

        public Config withGadget(int gadget) {
            mGadgetMode = gadget;
            return this;
        }

        public Config withAttachmentPreviews(int attachmentPreviewMode) {
            mAttachmentPreviewMode = attachmentPreviewMode;
            return this;
        }

        public Config showFolders() {
            mShowFolders = true;
            return this;
        }

        public Config showReplyState() {
            mShowReplyState = true;
            return this;
        }

        public Config showColorBlock() {
            mShowColorBlock = true;
            return this;
        }

        public Config showPersonalIndicator() {
            mShowPersonalIndicator  = true;
            return this;
        }

        public Config updateWidth(int width) {
            mWidth = width;
            return this;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getViewMode() {
            return mViewMode;
        }

        public int getGadgetMode() {
            return mGadgetMode;
        }

        public int getAttachmentPreviewMode() {
            return mAttachmentPreviewMode;
        }

        public boolean areFoldersVisible() {
            return mShowFolders;
        }

        public boolean isReplyStateVisible() {
            return mShowReplyState;
        }

        public boolean isColorBlockVisible() {
            return mShowColorBlock;
        }

        public boolean isPersonalIndicatorVisible() {
            return mShowPersonalIndicator;
        }

        private int getCacheKey() {
            // hash the attributes that contribute to item height and child view geometry
            return Objects.hashCode(mWidth, mViewMode, mGadgetMode, mAttachmentPreviewMode,
                    mShowFolders, mShowReplyState, mShowPersonalIndicator);
        }

    }

    public static class CoordinatesCache {
        private final SparseArray<ConversationItemViewCoordinates> mCoordinatesCache
                = new SparseArray<ConversationItemViewCoordinates>();
        private final SparseArray<View> mViewsCache = new SparseArray<View>();

        public ConversationItemViewCoordinates getCoordinates(final int key) {
            return mCoordinatesCache.get(key);
        }

        public View getView(final int layoutId) {
            return mViewsCache.get(layoutId);
        }

        public void put(final int key, final ConversationItemViewCoordinates coords) {
            mCoordinatesCache.put(key, coords);
        }

        public void put(final int layoutId, final View view) {
            mViewsCache.put(layoutId, view);
        }
    }

    /**
     * One of either NORMAL_MODE or WIDE_MODE.
     */
    private final int mMode;

    final int height;

    // Star.
    final int starX;
    final int starY;
    final int starWidth;

    // Senders.
    final int sendersX;
    final int sendersY;
    final int sendersWidth;
    final int sendersHeight;
    final int sendersLineCount;
    final int sendersLineHeight;
    final float sendersFontSize;

    // Subject.
    final int subjectX;
    final int subjectY;
    final int subjectWidth;
    final int subjectHeight;
    final int subjectLineCount;
    final float subjectFontSize;

    // Folders.
    final int foldersX;
    final int foldersXEnd;
    final int foldersY;
    final int foldersHeight;
    final Typeface foldersTypeface;
    final float foldersFontSize;
    final int foldersTextBottomPadding;

    // Info icon
    final int infoIconXEnd;
    final int infoIconY;

    // Date.
    final int dateXEnd;
    final int dateY;
    final int datePaddingLeft;
    final float dateFontSize;
    final int dateYBaseline;

    // Paperclip.
    final int paperclipY;
    final int paperclipPaddingLeft;

    // Color block.
    final int colorBlockX;
    final int colorBlockY;
    final int colorBlockWidth;
    final int colorBlockHeight;

    // Reply state of a conversation.
    final int replyStateX;
    final int replyStateY;

    final int personalIndicatorX;
    final int personalIndicatorY;

    final int contactImagesHeight;
    final int contactImagesWidth;
    final int contactImagesX;
    final int contactImagesY;

    // Attachment previews
    public final int attachmentPreviewsX;
    public final int attachmentPreviewsY;
    final int attachmentPreviewsWidth;
    final int attachmentPreviewsHeight;
    public final int attachmentPreviewsDecodeHeight;

    // Attachment previews overflow badge and count
    public final int overflowXEnd;
    public final int overflowYEnd;
    public final int overflowDiameter;
    public final float overflowFontSize;
    public final Typeface overflowTypeface;

    // Attachment previews placeholder
    final int placeholderY;
    public final int placeholderWidth;
    public final int placeholderHeight;
    // Attachment previews progress bar
    final int progressBarY;
    public final int progressBarWidth;
    public final int progressBarHeight;

    /**
     * The smallest item width for which we use the "wide" layout.
     */
    private final int mMinListWidthForWide;
    /**
     * The smallest item width for which we use the "spacious" variant of the normal layout,
     * if the normal version is used at all. Larger than {@link #mMinListWidthForWide}, we use
     * wide mode anyway, and this value is unused.
     */
    private final int mMinListWidthIsSpacious;
    private final int mFolderCellWidth;
    private final int mFolderMinimumWidth;

    private ConversationItemViewCoordinates(final Context context, final Config config,
            final CoordinatesCache cache) {
        Utils.traceBeginSection("CIV coordinates constructor");
        final Resources res = context.getResources();
        mFolderCellWidth = res.getDimensionPixelSize(R.dimen.folder_cell_width);
        mMinListWidthForWide = res.getDimensionPixelSize(R.dimen.list_min_width_is_wide);
        mMinListWidthIsSpacious = res.getDimensionPixelSize(
                R.dimen.list_normal_mode_min_width_is_spacious);
        mFolderMinimumWidth = res.getDimensionPixelSize(R.dimen.folder_minimum_width);

        mMode = calculateMode(res, config);

        final int layoutId;
        if (mMode == WIDE_MODE) {
            layoutId = R.layout.conversation_item_view_wide;
        } else {
            if (config.getWidth() >= mMinListWidthIsSpacious) {
                layoutId = R.layout.conversation_item_view_normal_spacious;
            } else {
                layoutId = R.layout.conversation_item_view_normal;
            }
        }

        ViewGroup view = (ViewGroup) cache.getView(layoutId);
        if (view == null) {
            view = (ViewGroup) LayoutInflater.from(context).inflate(layoutId, null);
            cache.put(layoutId, view);
        }

        // Show/hide optional views before measure/layout call

        final View attachmentPreviews = view.findViewById(R.id.attachment_previews);;
        if (config.getAttachmentPreviewMode() != ATTACHMENT_PREVIEW_NONE) {
            final LayoutParams params = attachmentPreviews.getLayoutParams();
            attachmentPreviews.setVisibility(View.VISIBLE);
            params.height = getAttachmentPreviewsHeight(context, config.getAttachmentPreviewMode());
            attachmentPreviews.setLayoutParams(params);
        } else {
            attachmentPreviews.setVisibility(View.GONE);
        }
        attachmentPreviewsDecodeHeight = getAttachmentPreviewsHeight(context,
                ATTACHMENT_PREVIEW_UNREAD);

        final TextView folders = (TextView) view.findViewById(R.id.folders);
        folders.setVisibility(config.areFoldersVisible() ? View.VISIBLE : View.GONE);

        // Add margin between attachment previews and folders
        final View attachmentPreviewsBottomMargin = view
                .findViewById(R.id.attachment_previews_bottom_margin);
        final boolean marginVisible = config.getAttachmentPreviewMode() != ATTACHMENT_PREVIEW_NONE
                && config.areFoldersVisible();
        attachmentPreviewsBottomMargin.setVisibility(marginVisible ? View.VISIBLE : View.GONE);

        View contactImagesView = view.findViewById(R.id.contact_image);

        switch (config.getGadgetMode()) {
            case GADGET_CONTACT_PHOTO:
                contactImagesView.setVisibility(View.VISIBLE);
                break;
            case GADGET_CHECKBOX:
                contactImagesView.setVisibility(View.GONE);
                contactImagesView = null;
                break;
            default:
                contactImagesView.setVisibility(View.GONE);
                contactImagesView = null;
                break;
        }

        final View replyState = view.findViewById(R.id.reply_state);
        replyState.setVisibility(config.isReplyStateVisible() ? View.VISIBLE : View.GONE);

        final View personalIndicator = view.findViewById(R.id.personal_indicator);
        personalIndicator.setVisibility(
                config.isPersonalIndicatorVisible() ? View.VISIBLE : View.GONE);

        // Layout the appropriate view.
        final int widthSpec = MeasureSpec.makeMeasureSpec(config.getWidth(), MeasureSpec.EXACTLY);
        final int heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

//        Utils.dumpViewTree((ViewGroup) view);

        // Records coordinates.

        // Contact images view
        if (contactImagesView != null) {
            contactImagesWidth = contactImagesView.getWidth();
            contactImagesHeight = contactImagesView.getHeight();
            contactImagesX = getX(contactImagesView);
            contactImagesY = getY(contactImagesView);
        } else {
            contactImagesX = contactImagesY = contactImagesWidth = contactImagesHeight = 0;
        }

        final View star = view.findViewById(R.id.star);
        starX = getX(star);
        starY = getY(star);
        starWidth = star.getWidth();

        final TextView senders = (TextView) view.findViewById(R.id.senders);
        final int sendersTopAdjust = getLatinTopAdjustment(senders);
        sendersX = getX(senders);
        sendersY = getY(senders) + sendersTopAdjust;
        sendersWidth = senders.getWidth();
        sendersHeight = senders.getHeight();
        sendersLineCount = getLineCount(senders);
        sendersLineHeight = senders.getLineHeight();
        sendersFontSize = senders.getTextSize();

        final TextView subject = (TextView) view.findViewById(R.id.subject);
        final int subjectTopAdjust = getLatinTopAdjustment(subject);
        subjectX = getX(subject);
        if (isWide()) {
            subjectY = getY(subject) + subjectTopAdjust;
        } else {
            subjectY = getY(subject) + sendersTopAdjust;
        }
        subjectWidth = subject.getWidth();
        subjectHeight = subject.getHeight();
        subjectLineCount = getLineCount(subject);
        subjectFontSize = subject.getTextSize();

        if (config.areFoldersVisible()) {
            // vertically align folders min left edge with subject
            foldersX = subjectX;
            foldersXEnd = getX(folders) + folders.getWidth();
            if (isWide()) {
                foldersY = getY(folders);
            } else {
                foldersY = getY(folders) + sendersTopAdjust;
            }
            foldersHeight = folders.getHeight();
            foldersTypeface = folders.getTypeface();
            foldersTextBottomPadding = res
                    .getDimensionPixelSize(R.dimen.folders_text_bottom_padding);
            foldersFontSize = folders.getTextSize();
        } else {
            foldersX = 0;
            foldersXEnd = 0;
            foldersY = 0;
            foldersHeight = 0;
            foldersTypeface = null;
            foldersTextBottomPadding = 0;
            foldersFontSize = 0;
        }

        final View colorBlock = view.findViewById(R.id.color_block);
        if (config.isColorBlockVisible() && colorBlock != null) {
            colorBlockX = getX(colorBlock);
            colorBlockY = getY(colorBlock);
            colorBlockWidth = colorBlock.getWidth();
            colorBlockHeight = colorBlock.getHeight();
        } else {
            colorBlockX = colorBlockY = colorBlockWidth = colorBlockHeight = 0;
        }

        if (config.isReplyStateVisible()) {
            replyStateX = getX(replyState);
            replyStateY = getY(replyState);
        } else {
            replyStateX = replyStateY = 0;
        }

        if (config.isPersonalIndicatorVisible()) {
            personalIndicatorX = getX(personalIndicator);
            personalIndicatorY = getY(personalIndicator);
        } else {
            personalIndicatorX = personalIndicatorY = 0;
        }

        final View infoIcon = view.findViewById(R.id.info_icon);
        infoIconXEnd = getX(infoIcon) + infoIcon.getWidth();
        infoIconY = getY(infoIcon);

        final TextView date = (TextView) view.findViewById(R.id.date);
        dateXEnd = getX(date) + date.getWidth();
        dateY = getY(date);
        datePaddingLeft = date.getPaddingLeft();
        dateFontSize = date.getTextSize();
        dateYBaseline = dateY + getLatinTopAdjustment(date) + date.getBaseline();

        final View paperclip = view.findViewById(R.id.paperclip);
        paperclipY = getY(paperclip);
        paperclipPaddingLeft = paperclip.getPaddingLeft();

        if (attachmentPreviews != null) {
            attachmentPreviewsX = subjectX;
            attachmentPreviewsY = getY(attachmentPreviews) + sendersTopAdjust;
            attachmentPreviewsWidth = subjectWidth;
            attachmentPreviewsHeight = attachmentPreviews.getHeight();

            // We only care about the right and bottom of the overflow count
            final TextView overflow = (TextView) view.findViewById(id.ap_overflow);
            final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overflow
                    .getLayoutParams();
            overflowXEnd = attachmentPreviewsX + attachmentPreviewsWidth - params.rightMargin;
            overflowYEnd = attachmentPreviewsY + attachmentPreviewsHeight - params.bottomMargin;
            overflowDiameter = overflow.getWidth();
            overflowFontSize = overflow.getTextSize();
            overflowTypeface = overflow.getTypeface();

            final View placeholder = view.findViewById(id.ap_placeholder);
            placeholderWidth = placeholder.getWidth();
            placeholderHeight = placeholder.getHeight();
            placeholderY = attachmentPreviewsY + attachmentPreviewsHeight / 2
                    - placeholderHeight / 2;

            final View progressBar = view.findViewById(id.ap_progress_bar);
            progressBarWidth = progressBar.getWidth();
            progressBarHeight = progressBar.getHeight();
            progressBarY = attachmentPreviewsY + attachmentPreviewsHeight / 2
                    - progressBarHeight / 2;
        } else {
            attachmentPreviewsX = 0;
            attachmentPreviewsY = 0;
            attachmentPreviewsWidth = 0;
            attachmentPreviewsHeight = 0;
            overflowXEnd = 0;
            overflowYEnd = 0;
            overflowDiameter = 0;
            overflowFontSize = 0;
            overflowTypeface = null;
            placeholderY = 0;
            placeholderWidth = 0;
            placeholderHeight = 0;
            progressBarY = 0;
            progressBarWidth = 0;
            progressBarHeight = 0;
        }

        height = view.getHeight() + (isWide() ? 0 : sendersTopAdjust);
        Utils.traceEndSection();
    }

    public int getMode() {
        return mMode;
    }

    public boolean isWide() {
        return mMode == WIDE_MODE;
    }

    /**
     * Returns a negative corrective value that you can apply to a TextView's vertical dimensions
     * that will nudge the first line of text upwards such that uppercase Latin characters are
     * truly top-aligned.
     * <p>
     * N.B. this will cause other characters to draw above the top! only use this if you have
     * adequate top margin.
     *
     */
    private static int getLatinTopAdjustment(TextView t) {
        final FontMetricsInt fmi = t.getPaint().getFontMetricsInt();
        return (fmi.top - fmi.ascent);
    }

    /**
     * Returns the mode of the header view (Wide/Normal).
     */
    private int calculateMode(Resources res, Config config) {
        switch (config.getViewMode()) {
            case ViewMode.CONVERSATION_LIST:
                return config.getWidth() >= mMinListWidthForWide ? WIDE_MODE : NORMAL_MODE;

            case ViewMode.SEARCH_RESULTS_LIST:
                return res.getInteger(R.integer.conversation_list_search_header_mode);

            default:
                return res.getInteger(R.integer.conversation_header_mode);
        }
    }

    private int getAttachmentPreviewsHeight(final Context context,
            final int attachmentPreviewMode) {
        final Resources res = context.getResources();
        switch (attachmentPreviewMode) {
            case ATTACHMENT_PREVIEW_UNREAD:
                return (int) (isWide() ? res.getDimension(dimen.attachment_preview_height_tall_wide)
                        : res.getDimension(dimen.attachment_preview_height_tall));
            case ATTACHMENT_PREVIEW_READ:
                return (int) res.getDimension(dimen.attachment_preview_height_short);
            default:
                return 0;
        }
    }

    /**
     * Returns the x coordinates of a view by tracing up its hierarchy.
     */
    private static int getX(View view) {
        int x = 0;
        while (view != null) {
            x += (int) view.getX();
            view = (View) view.getParent();
        }
        return x;
    }

    /**
     * Returns the y coordinates of a view by tracing up its hierarchy.
     */
    private static int getY(View view) {
        int y = 0;
        while (view != null) {
            y += (int) view.getY();
            view = (View) view.getParent();
        }
        return y;
    }

    /**
     * Returns the number of lines of this text view. Delegates to built-in TextView logic on JB+.
     */
    private static int getLineCount(TextView textView) {
        if (Utils.isRunningJellybeanOrLater()) {
            return textView.getMaxLines();
        } else {
            return Math.round(((float) textView.getHeight()) / textView.getLineHeight());
        }
    }

    /**
     * Returns the length (maximum of characters) of subject in this mode.
     */
    public static int getSendersLength(Context context, int mode, boolean hasAttachments) {
        final Resources res = context.getResources();
        if (hasAttachments) {
            return res.getIntArray(R.array.senders_with_attachment_lengths)[mode];
        } else {
            return res.getIntArray(R.array.senders_lengths)[mode];
        }
    }

    @Deprecated
    public static int getColorBlockWidth(Context context) {
        Resources res = context.getResources();
        if (COLOR_BLOCK_WIDTH <= 0) {
            COLOR_BLOCK_WIDTH = res.getDimensionPixelSize(R.dimen.color_block_width);
        }
        return COLOR_BLOCK_WIDTH;
    }

    @Deprecated
    public static int getColorBlockHeight(Context context) {
        Resources res = context.getResources();
        if (COLOR_BLOCK_HEIGHT <= 0) {
            COLOR_BLOCK_HEIGHT = res.getDimensionPixelSize(R.dimen.color_block_height);
        }
        return COLOR_BLOCK_HEIGHT;
    }

    public static boolean displaySendersInline(int mode) {
        switch (mode) {
            case WIDE_MODE:
                return false;
            case NORMAL_MODE:
                return true;
            default:
                throw new IllegalArgumentException("Unknown conversation header view mode " + mode);
        }
    }

    /**
     * Returns coordinates for elements inside a conversation header view given
     * the view width.
     */
    public static ConversationItemViewCoordinates forConfig(final Context context,
            final Config config, final CoordinatesCache cache) {
        final int cacheKey = config.getCacheKey();
        ConversationItemViewCoordinates coordinates = cache.getCoordinates(cacheKey);
        if (coordinates != null) {
            return coordinates;
        }

        coordinates = new ConversationItemViewCoordinates(context, config, cache);
        cache.put(cacheKey, coordinates);
        return coordinates;
    }

    /**
     * Return the minimum width of a folder cell with no text. Essentially this is the left+right
     * intra-cell margin within cells.
     *
     */
    public int getFolderCellWidth() {
        return mFolderCellWidth;
    }

    /**
     * Return the minimum width of a folder cell, period. This will affect the
     * maximum number of folders we can display.
     */
    public int getFolderMinimumWidth() {
        return mFolderMinimumWidth;
    }

    public static boolean isWideMode(int mode) {
        return mode == WIDE_MODE;
    }

}
