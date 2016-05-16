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
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.browse.ConversationViewAdapter.ConversationHeaderItem;
import com.android.mail.browse.FolderSpan.FolderSpanDimensions;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.Settings;
import com.android.mail.ui.FolderDisplayer;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

/**
 * A view for the subject and folders in the conversation view. This container
 * makes an attempt to combine subject and folders on the same horizontal line if
 * there is enough room to fit both without wrapping. If they overlap, it
 * adjusts the layout to position the folders below the subject.
 */
public class ConversationViewHeader extends LinearLayout implements OnClickListener {

    public interface ConversationViewHeaderCallbacks {
        /**
         * Called in response to a click on the folders region.
         */
        void onFoldersClicked();

        /**
         * Called when the height of the {@link ConversationViewHeader} changes.
         *
         * @param newHeight the new height in px
         */
        void onConversationViewHeaderHeightChange(int newHeight);
    }

    private static final String LOG_TAG = LogTag.getLogTag();
    private TextView mSubjectView;
    private FolderSpanTextView mFoldersView;
    private ConversationViewHeaderCallbacks mCallbacks;
    private ConversationAccountController mAccountController;
    private ConversationFolderDisplayer mFolderDisplayer;
    private ConversationHeaderItem mHeaderItem;

    private boolean mLargeText;
    private final float mCondensedTextSize;
    private final int mCondensedTopPadding;

    /**
     * Instantiated from this layout: conversation_view_header.xml
     * @param context
     */
    public ConversationViewHeader(Context context) {
        this(context, null);
    }

    public ConversationViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mLargeText = true;
        final Resources resources = getResources();
        mCondensedTextSize =
                resources.getDimensionPixelSize(R.dimen.conversation_header_font_size_condensed);
        mCondensedTopPadding = resources.getDimensionPixelSize(
                R.dimen.conversation_header_vertical_padding_condensed);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mSubjectView = (TextView) findViewById(R.id.subject);
        mFoldersView = (FolderSpanTextView) findViewById(R.id.folders);

        mFoldersView.setOnClickListener(this);
        mFolderDisplayer = new ConversationFolderDisplayer(getContext(), mFoldersView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // If we currently have large text and we have greater than 2 lines,
        // switch to smaller text size with smaller top padding and re-measure
        if (mLargeText && mSubjectView.getLineCount() > 2) {
            mSubjectView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCondensedTextSize);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // start, top, end, bottom
                mSubjectView.setPaddingRelative(mSubjectView.getPaddingStart(),
                        mCondensedTopPadding, mSubjectView.getPaddingEnd(),
                        mSubjectView.getPaddingBottom());
            } else {
                mSubjectView.setPadding(mSubjectView.getPaddingLeft(),
                        mCondensedTopPadding, mSubjectView.getPaddingRight(),
                        mSubjectView.getPaddingBottom());
            }

            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    public void setCallbacks(ConversationViewHeaderCallbacks callbacks,
            ConversationAccountController accountController) {
        mCallbacks = callbacks;
        mAccountController = accountController;
    }

    public void setSubject(final String subject) {
        mSubjectView.setText(subject);
        if (TextUtils.isEmpty(subject)) {
            mSubjectView.setVisibility(GONE);
        }
    }

    public void setFoldersVisible(boolean show) {
        mFoldersView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setFolders(Conversation conv) {
        setFoldersVisible(true);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        final Settings settings = mAccountController.getAccount().settings;
        if (settings.priorityArrowsEnabled && conv.isImportant()) {
            sb.append('.');
            sb.setSpan(new PriorityIndicatorSpan(getContext(),
                    R.drawable.ic_email_caret_none_important_unread, mFoldersView.getPadding(), 0,
                    mFoldersView.getPaddingAbove()),
                    0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        }

        mFolderDisplayer.loadConversationFolders(conv, null /* ignoreFolder */,
                -1 /* ignoreFolderType */);
        mFolderDisplayer.appendFolderSpans(sb);

        mFoldersView.setText(sb);
    }

    public void bind(ConversationHeaderItem headerItem) {
        mHeaderItem = headerItem;
    }

    private int measureHeight() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) {
            LogUtils.e(LOG_TAG, "Unable to measure height of conversation header");
            return getHeight();
        }
        final int h = Utils.measureViewHeight(this, parent);
        return h;
    }

    /**
     * Update the conversation view header to reflect the updated conversation.
     */
    public void onConversationUpdated(Conversation conv) {
        // The only things we have to worry about when the conversation changes
        // in the conversation header are the folders and priority indicators.
        // Updating these will resize the space for the header.
        setFolders(conv);
        if (mHeaderItem != null) {
            final int h = measureHeight();
            if (mHeaderItem.setHeight(h)) {
                mCallbacks.onConversationViewHeaderHeightChange(h);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (R.id.folders == v.getId()) {
            if (mCallbacks != null) {
                mCallbacks.onFoldersClicked();
            }
        }
    }

    private static class ConversationFolderDisplayer extends FolderDisplayer {

        private FolderSpanDimensions mDims;

        public ConversationFolderDisplayer(Context context, FolderSpanDimensions dims) {
            super(context);
            mDims = dims;
        }

        public void appendFolderSpans(SpannableStringBuilder sb) {
            for (final Folder f : mFoldersSortedSet) {
                final int bgColor = f.getBackgroundColor(mDefaultBgColor);
                final int fgColor = f.getForegroundColor(mDefaultFgColor);
                addSpan(sb, f.name, bgColor, fgColor);
            }

            if (mFoldersSortedSet.isEmpty()) {
                final Resources r = mContext.getResources();
                final String name = r.getString(R.string.add_label);
                final int bgColor = r.getColor(R.color.conv_header_add_label_background);
                final int fgColor = r.getColor(R.color.conv_header_add_label_text);
                addSpan(sb, name, bgColor, fgColor);
            }
        }

        private void addSpan(SpannableStringBuilder sb, String name, int bgColor,
                             int fgColor) {
            final int start = sb.length();
            sb.append(name);
            final int end = sb.length();

            sb.setSpan(new BackgroundColorSpan(bgColor), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new ForegroundColorSpan(fgColor), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new FolderSpan(sb, mDims), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

    }
}
