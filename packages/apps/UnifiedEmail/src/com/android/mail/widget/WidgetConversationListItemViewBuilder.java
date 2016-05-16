/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.mail.widget;

import com.android.mail.R;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.ui.FolderDisplayer;
import com.android.mail.utils.FolderUri;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.RemoteViews;

public class WidgetConversationListItemViewBuilder {
    // Static font sizes
    private static int DATE_FONT_SIZE;
    private static int SUBJECT_FONT_SIZE;

    // Static colors
    private static int SUBJECT_TEXT_COLOR_READ;
    private static int SUBJECT_TEXT_COLOR_UNREAD;
    private static int DATE_TEXT_COLOR;

    // Static bitmap
    private static Bitmap ATTACHMENT;

    private final Context mContext;
    private WidgetFolderDisplayer mFolderDisplayer;

    /**
     * Label Displayer for Widget
     */
    protected static class WidgetFolderDisplayer extends FolderDisplayer {
        public WidgetFolderDisplayer(Context context) {
            super(context);
        }

        // Maximum number of folders we want to display
        private static final int MAX_DISPLAYED_FOLDERS_COUNT = 3;

        /*
         * Load Conversation Labels
         */
        @Override
        public void loadConversationFolders(Conversation conv, final FolderUri ignoreFolderUri,
                final int ignoreFolderType) {
            super.loadConversationFolders(conv, ignoreFolderUri, ignoreFolderType);
        }

        private static int getFolderViewId(int position) {
            switch (position) {
                case 0:
                    return R.id.widget_folder_0;
                case 1:
                    return R.id.widget_folder_1;
                case 2:
                    return R.id.widget_folder_2;
            }
            return 0;
        }

        /**
         * Display folders
         */
        public void displayFolders(RemoteViews remoteViews) {
            int displayedFolder = 0;
            for (Folder folderValues : mFoldersSortedSet) {
                int viewId = getFolderViewId(displayedFolder);
                if (viewId == 0) {
                    continue;
                }
                remoteViews.setViewVisibility(viewId, View.VISIBLE);
                int color[] = new int[] {folderValues.getBackgroundColor(mDefaultBgColor)};
                Bitmap bitmap = Bitmap.createBitmap(color, 1, 1, Bitmap.Config.RGB_565);
                remoteViews.setImageViewBitmap(viewId, bitmap);

                if (++displayedFolder == MAX_DISPLAYED_FOLDERS_COUNT) {
                    break;
                }
            }

            for (int i = displayedFolder; i < MAX_DISPLAYED_FOLDERS_COUNT; i++) {
                remoteViews.setViewVisibility(getFolderViewId(i), View.GONE);
            }
        }
    }

    /*
     * Get font sizes and bitmaps from Resources
     */
    public WidgetConversationListItemViewBuilder(Context context) {
        mContext = context;
        Resources res = context.getResources();

        // Initialize font sizes
        DATE_FONT_SIZE = res.getDimensionPixelSize(R.dimen.widget_date_font_size);
        SUBJECT_FONT_SIZE = res.getDimensionPixelSize(R.dimen.widget_subject_font_size);

        // Initialize colors
        SUBJECT_TEXT_COLOR_READ = res.getColor(R.color.subject_text_color_read);
        SUBJECT_TEXT_COLOR_UNREAD = res.getColor(R.color.subject_text_color_unread);
        DATE_TEXT_COLOR = res.getColor(R.color.date_text_color);

        // Initialize Bitmap
        ATTACHMENT = BitmapFactory.decodeResource(res, R.drawable.ic_attachment_holo_light);
    }

    /*
     * Add size, color and style to a given text
     */
    private static CharSequence addStyle(CharSequence text, int size, int color) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        builder.setSpan(
                new AbsoluteSizeSpan(size), 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (color != 0) {
            builder.setSpan(new ForegroundColorSpan(color), 0, text.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    /*
     * Return the full View
     */
    public RemoteViews getStyledView(final CharSequence date, final Conversation conversation,
            final FolderUri folderUri, final int ignoreFolderType,
            final SpannableStringBuilder senders, final String filteredSubject) {

        final boolean isUnread = !conversation.read;
        final String snippet = conversation.getSnippet();
        final boolean hasAttachments = conversation.hasAttachments;

        // Add style to date
        final CharSequence styledDate = addStyle(date, DATE_FONT_SIZE, DATE_TEXT_COLOR);

        // Add style to subject
        final int subjectColor = isUnread ? SUBJECT_TEXT_COLOR_UNREAD : SUBJECT_TEXT_COLOR_READ;
        final SpannableStringBuilder subjectAndSnippet = new SpannableStringBuilder(
                Conversation.getSubjectAndSnippetForDisplay(mContext, filteredSubject, snippet));
        if (isUnread) {
            subjectAndSnippet.setSpan(new StyleSpan(Typeface.BOLD), 0, filteredSubject.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        subjectAndSnippet.setSpan(new ForegroundColorSpan(subjectColor), 0, subjectAndSnippet
                .length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final CharSequence styledSubject = addStyle(subjectAndSnippet, SUBJECT_FONT_SIZE, 0);

        // Paper clip for attachment
        Bitmap paperclipBitmap = null;
        if (hasAttachments) {
            paperclipBitmap = ATTACHMENT;
        }

        // Inflate and fill out the remote view
        final RemoteViews remoteViews = new RemoteViews(
                mContext.getPackageName(), R.layout.widget_conversation_list_item);
        remoteViews.setTextViewText(R.id.widget_senders, senders);
        remoteViews.setTextViewText(R.id.widget_date, styledDate);
        remoteViews.setTextViewText(R.id.widget_subject, styledSubject);
        if (paperclipBitmap != null) {
            remoteViews.setViewVisibility(R.id.widget_attachment, View.VISIBLE);
            remoteViews.setImageViewBitmap(R.id.widget_attachment, paperclipBitmap);
        } else {
            remoteViews.setViewVisibility(R.id.widget_attachment, View.GONE);
        }
        if (isUnread) {
            remoteViews.setViewVisibility(R.id.widget_unread_background, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.widget_read_background, View.GONE);
        } else {
            remoteViews.setViewVisibility(R.id.widget_unread_background, View.GONE);
            remoteViews.setViewVisibility(R.id.widget_read_background, View.VISIBLE);
        }
        if (mContext.getResources().getBoolean(R.bool.display_folder_colors_in_widget)) {
            mFolderDisplayer = new WidgetFolderDisplayer(mContext);
            mFolderDisplayer.loadConversationFolders(conversation, folderUri, ignoreFolderType);
            mFolderDisplayer.displayFolders(remoteViews);
        }

        return remoteViews;
    }
}
