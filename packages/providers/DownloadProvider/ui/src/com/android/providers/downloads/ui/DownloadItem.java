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

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.GridLayout;

/**
 * This class customizes RelativeLayout to directly handle clicks on the left part of the view and
 * treat them at clicks on the checkbox. This makes rapid selection of many items easier. This class
 * also keeps an ID associated with the currently displayed download and notifies a listener upon
 * selection changes with that ID.
 */
public class DownloadItem extends GridLayout implements Checkable {
    private static float CHECKMARK_AREA = -1;

    private boolean mIsInDownEvent = false;
    private CheckBox mCheckBox;
    private long mDownloadId;
    private String mFileName;
    private String mMimeType;
    private DownloadList mDownloadList;
    private int mPosition;

    public DownloadItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    public DownloadItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public DownloadItem(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        if (CHECKMARK_AREA == -1) {
            CHECKMARK_AREA = getResources().getDimensionPixelSize(R.dimen.checkmark_area);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCheckBox = (CheckBox) findViewById(R.id.download_checkbox);
    }

    public void setData(long downloadId, int position, String fileName, String mimeType) {
        mDownloadId = downloadId;
        mPosition = position;
        mFileName = fileName;
        mMimeType = mimeType;
        if (mDownloadList.isDownloadSelected(downloadId)) {
            setChecked(true);
        }
    }

    public void setDownloadListObj(DownloadList downloadList) {
        mDownloadList = downloadList;
    }

    private boolean inCheckArea(MotionEvent event) {
        if (isLayoutRtl()) {
            return event.getX() > getWidth() - CHECKMARK_AREA;
        } else {
            return event.getX() < CHECKMARK_AREA;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (inCheckArea(event)) {
                    mIsInDownEvent = true;
                    handled = true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                mIsInDownEvent = false;
                break;

            case MotionEvent.ACTION_UP:
                if (mIsInDownEvent && inCheckArea(event)) {
                    toggle();
                    sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
                    handled = true;
                }
                mIsInDownEvent = false;
                break;
        }

        if (handled) {
            postInvalidate();
        } else {
            handled = super.onTouchEvent(event);
        }

        return handled;
    }

    @Override
    public boolean isChecked() {
        return mCheckBox.isChecked();
    }

    @Override
    public void setChecked(boolean checked) {
        mCheckBox.setChecked(checked);
        mDownloadList.onDownloadSelectionChanged(mDownloadId, mCheckBox.isChecked(),
                mFileName, mMimeType);
        mDownloadList.getCurrentView().setItemChecked(mPosition, mCheckBox.isChecked());
    }

    @Override
    public void toggle() {
        setChecked(!isChecked());
    }

    public CheckBox getCheckBox() {
        return this.mCheckBox;
    }

    public String getFileName() {
        return mFileName;
    }

    public String getMimeType() {
        return mMimeType;
    }
}
