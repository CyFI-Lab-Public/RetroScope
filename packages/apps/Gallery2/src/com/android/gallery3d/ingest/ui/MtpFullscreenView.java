/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.gallery3d.ingest.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;

import com.android.gallery3d.R;
import com.android.gallery3d.ingest.adapter.CheckBroker;

public class MtpFullscreenView extends RelativeLayout implements Checkable,
    CompoundButton.OnCheckedChangeListener, CheckBroker.OnCheckedChangedListener {

    private MtpImageView mImageView;
    private CheckBox mCheckbox;
    private int mPosition = -1;
    private CheckBroker mBroker;

    public MtpFullscreenView(Context context) {
        super(context);
    }

    public MtpFullscreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MtpFullscreenView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mImageView = (MtpImageView) findViewById(R.id.ingest_fullsize_image);
        mCheckbox = (CheckBox) findViewById(R.id.ingest_fullsize_image_checkbox);
        mCheckbox.setOnCheckedChangeListener(this);
    }

    @Override
    public boolean isChecked() {
        return mCheckbox.isChecked();
    }

    @Override
    public void setChecked(boolean checked) {
        mCheckbox.setChecked(checked);
    }

    @Override
    public void toggle() {
        mCheckbox.toggle();
    }

    @Override
    public void onDetachedFromWindow() {
        setPositionAndBroker(-1, null);
        super.onDetachedFromWindow();
    }

    public MtpImageView getImageView() {
        return mImageView;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPositionAndBroker(int position, CheckBroker b) {
        if (mBroker != null) {
            mBroker.unregisterOnCheckedChangeListener(this);
        }
        mPosition = position;
        mBroker = b;
        if (mBroker != null) {
            setChecked(mBroker.isItemChecked(position));
            mBroker.registerOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
        if (mBroker != null) mBroker.setItemChecked(mPosition, isChecked);
    }

    @Override
    public void onCheckedChanged(int position, boolean isChecked) {
        if (position == mPosition) {
            setChecked(isChecked);
        }
    }

    @Override
    public void onBulkCheckedChanged() {
        if(mBroker != null) setChecked(mBroker.isItemChecked(mPosition));
    }
}
