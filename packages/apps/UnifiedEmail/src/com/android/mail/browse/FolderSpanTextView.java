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
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.TextView;

import com.android.mail.R;

/**
 * A TextView that knows the widest that any of its containing
 * {@link FolderSpan}s can be. They cannot exceed the TextView line width, or
 * else {@link Layout} will split up the spans in strange places.
 */
public class FolderSpanTextView extends TextView implements FolderSpan.FolderSpanDimensions {

    private final int mFolderPadding;
    private final int mFolderPaddingExtraWidth;
    private final int mFolderPaddingBefore;
    private final int mFolderPaddingAbove;

    private int mMaxSpanWidth;

    public FolderSpanTextView(Context context) {
        this(context, null);
    }

    public FolderSpanTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Resources r = getResources();
        mFolderPadding = r.getDimensionPixelOffset(R.dimen.conversation_folder_padding);
        mFolderPaddingExtraWidth = r.getDimensionPixelOffset(
                R.dimen.conversation_folder_padding_extra_width);
        mFolderPaddingBefore = r.getDimensionPixelOffset(
                R.dimen.conversation_folder_padding_before);
        mFolderPaddingAbove = r.getDimensionPixelOffset(R.dimen.conversation_folder_padding_above);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMaxSpanWidth = MeasureSpec.getSize(widthMeasureSpec) - getTotalPaddingLeft()
                - getTotalPaddingRight();

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public int getPadding() {
        return mFolderPadding;
    }

    @Override
    public int getPaddingExtraWidth() {
        return mFolderPaddingExtraWidth;
    }

    @Override
    public int getPaddingBefore() {
        return mFolderPaddingBefore;
    }

    @Override
    public int getPaddingAbove() {
        return mFolderPaddingAbove;
    }

    @Override
    public int getMaxWidth() {
        return mMaxSpanWidth;
    }
}
