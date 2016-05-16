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
package com.android.dialer.list;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;

import com.android.contacts.common.util.ViewUtil;
import com.android.dialer.R;

public class PhoneFavoriteRegularRowView extends PhoneFavoriteTileView {
    private static final String TAG = PhoneFavoriteRegularRowView.class.getSimpleName();
    private static final boolean DEBUG = false;

    public PhoneFavoriteRegularRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final View favoriteContactCard = findViewById(R.id.contact_favorite_card);

        final int rowPaddingStart;
        final int rowPaddingEnd;
        final int rowPaddingTop;
        final int rowPaddingBottom;

        final Resources resources = getResources();
        rowPaddingStart = resources.getDimensionPixelSize(
                R.dimen.favorites_row_start_padding);
        rowPaddingEnd = resources.getDimensionPixelSize(
                R.dimen.favorites_row_end_padding);
        rowPaddingTop = resources.getDimensionPixelSize(
                R.dimen.favorites_row_top_padding);
        rowPaddingBottom = resources.getDimensionPixelSize(
                R.dimen.favorites_row_bottom_padding);

        favoriteContactCard.setBackgroundResource(R.drawable.contact_list_item_background);

        favoriteContactCard.setPaddingRelative(rowPaddingStart, rowPaddingTop, rowPaddingEnd,
                rowPaddingBottom);

        final View quickContactBadge = findViewById(R.id.contact_tile_quick);
        quickContactBadge.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return PhoneFavoriteRegularRowView.this.performLongClick();
            }
        });
    }

    @Override
    protected int getApproximateImageSize() {
        return ViewUtil.getConstantPreLayoutWidth(getQuickContact());
    }
}
