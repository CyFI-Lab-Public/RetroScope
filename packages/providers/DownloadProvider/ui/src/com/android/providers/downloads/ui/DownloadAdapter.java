/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * List adapter for Cursors returned by {@link DownloadManager}.
 */
public class DownloadAdapter extends CursorAdapter {
    private final DownloadList mDownloadList;
    private Cursor mCursor;
    private Resources mResources;
    private DateFormat mDateFormat;
    private DateFormat mTimeFormat;

    private final int mTitleColumnId;
    private final int mDescriptionColumnId;
    private final int mStatusColumnId;
    private final int mReasonColumnId;
    private final int mTotalBytesColumnId;
    private final int mMediaTypeColumnId;
    private final int mDateColumnId;
    private final int mIdColumnId;
    private final int mFileNameColumnId;

    public DownloadAdapter(DownloadList downloadList, Cursor cursor) {
        super(downloadList, cursor);
        mDownloadList = downloadList;
        mCursor = cursor;
        mResources = mDownloadList.getResources();
        mDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        mTimeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        mIdColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
        mTitleColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE);
        mDescriptionColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION);
        mStatusColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
        mReasonColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON);
        mTotalBytesColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
        mMediaTypeColumnId = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE);
        mDateColumnId =
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
        mFileNameColumnId =
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME);
    }

    public View newView() {
        final DownloadItem view = (DownloadItem) LayoutInflater.from(mDownloadList)
                .inflate(R.layout.download_list_item, null);
        view.setDownloadListObj(mDownloadList);
        return view;
    }

    public void bindView(View convertView, int position) {
        if (!(convertView instanceof DownloadItem)) {
            return;
        }

        long downloadId = mCursor.getLong(mIdColumnId);
        ((DownloadItem) convertView).setData(downloadId, position,
                mCursor.getString(mFileNameColumnId),
                mCursor.getString(mMediaTypeColumnId));

        // Retrieve the icon for this download
        retrieveAndSetIcon(convertView);

        String title = mCursor.getString(mTitleColumnId);
        if (title.isEmpty()) {
            title = mResources.getString(R.string.missing_title);
        }
        setTextForView(convertView, R.id.download_title, title);
        setTextForView(convertView, R.id.domain, mCursor.getString(mDescriptionColumnId));
        setTextForView(convertView, R.id.size_text, getSizeText());

        final int status = mCursor.getInt(mStatusColumnId);
        final CharSequence statusText;
        if (status == DownloadManager.STATUS_SUCCESSFUL) {
            statusText = getDateString();
        } else {
            statusText = mResources.getString(getStatusStringId(status));
        }
        setTextForView(convertView, R.id.status_text, statusText);

        ((DownloadItem) convertView).getCheckBox()
                .setChecked(mDownloadList.isDownloadSelected(downloadId));
    }

    private String getDateString() {
        Date date = new Date(mCursor.getLong(mDateColumnId));
        if (date.before(getStartOfToday())) {
            return mDateFormat.format(date);
        } else {
            return mTimeFormat.format(date);
        }
    }

    private Date getStartOfToday() {
        Calendar today = new GregorianCalendar();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today.getTime();
    }

    private String getSizeText() {
        long totalBytes = mCursor.getLong(mTotalBytesColumnId);
        String sizeText = "";
        if (totalBytes >= 0) {
            sizeText = Formatter.formatFileSize(mContext, totalBytes);
        }
        return sizeText;
    }

    private int getStatusStringId(int status) {
        switch (status) {
            case DownloadManager.STATUS_FAILED:
                return R.string.download_error;

            case DownloadManager.STATUS_SUCCESSFUL:
                return R.string.download_success;

            case DownloadManager.STATUS_PENDING:
            case DownloadManager.STATUS_RUNNING:
                return R.string.download_running;

            case DownloadManager.STATUS_PAUSED:
                final int reason = mCursor.getInt(mReasonColumnId);
                switch (reason) {
                    case DownloadManager.PAUSED_QUEUED_FOR_WIFI:
                        return R.string.download_queued;
                    default:
                        return R.string.download_running;
                }
        }
        throw new IllegalStateException("Unknown status: " + mCursor.getInt(mStatusColumnId));
    }

    private void retrieveAndSetIcon(View convertView) {
        String mediaType = mCursor.getString(mMediaTypeColumnId);
        ImageView iconView = (ImageView) convertView.findViewById(R.id.download_icon);
        iconView.setVisibility(View.INVISIBLE);

        if (mediaType == null) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromParts("file", "", null), mediaType);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> list = pm.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        if (list.size() == 0) {
            // no icon found for this mediatype. use "unknown" icon
            iconView.setImageResource(R.drawable.ic_download_misc_file_type);
        } else {
            Drawable icon = list.get(0).activityInfo.loadIcon(pm);
            iconView.setImageDrawable(icon);
        }
        iconView.setVisibility(View.VISIBLE);
    }

    private void setTextForView(View parent, int textViewId, CharSequence text) {
        TextView view = (TextView) parent.findViewById(textViewId);
        view.setText(text);
    }

    // CursorAdapter overrides

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return newView();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        bindView(view, cursor.getPosition());
    }
}
