/**
 * Copyright (c) 2013, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.android.mail.R;
import com.android.mail.browse.ConversationViewAdapter.BorderItem;
import com.android.mail.utils.LogUtils;

/**
 * View displaying the border between messages.
 * Contains two nine-patches and a {@link android.widget.Space}.
 * The nine patches are the bottom of the preceding message
 * and the top of the following message.
 */
public class BorderView extends LinearLayout {

    private static final String LOG_TAG = "BorderView";

    private static int sMessageBorderSpaceHeight = -1;
    private static int sMessageBorderHeightCollapsed = -1;
    private static int sExpandedHeight = -1;

    private View mCardBottom;
    private View mBorderSpace;
    private View mCardTop;

    public BorderView(Context context) {
        this(context, null);
    }

    public BorderView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public BorderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // In order to update the height appropriately based on
        // whether the border is expanded or collapsed,
        // we want to stash the height values for for the
        // space in both its expanded and collapsed values.
        // Additionally, we stash the total height of the view
        // when both nine patches are visible.
        if (sMessageBorderSpaceHeight == -1) {
            final Resources res = context.getResources();
            sMessageBorderSpaceHeight =
                    res.getDimensionPixelSize(R.dimen.message_border_height);
            sMessageBorderHeightCollapsed = res.getDimensionPixelSize(
                    R.dimen.message_border_height_collapsed);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mCardBottom = findViewById(R.id.card_bottom);
        mBorderSpace = findViewById(R.id.border_space);
        mCardTop = findViewById(R.id.card_top);
    }

    public void bind(BorderItem borderItem, boolean measureOnly) {
        final boolean isExpanded = borderItem.isExpanded();
        if (sExpandedHeight == -1 && isExpanded &&
                !borderItem.isFirstBorder() && !borderItem.isLastBorder() &&
                borderItem.getHeight() > 0) {
            sExpandedHeight = borderItem.getHeight();
            LogUtils.d(LOG_TAG, "Full Border Height: %s", sExpandedHeight);
        }



        // Selectively show/hide the card nine-patches if the border is expanded or collapsed.
        // Additionally this will occur if this is the first or last border.
        mCardBottom.setVisibility(!isExpanded || borderItem.isFirstBorder() ? GONE : VISIBLE);
        mCardTop.setVisibility(!isExpanded || borderItem.isLastBorder() ? GONE : VISIBLE);

        // Adjust space height based on expanded state.
        final ViewGroup.LayoutParams params = mBorderSpace.getLayoutParams();
        params.height = isExpanded ? sMessageBorderSpaceHeight : sMessageBorderHeightCollapsed;
        mBorderSpace.setLayoutParams(params);
    }

    public void disableCardBottomBorder() {
        mCardBottom.setVisibility(GONE);
    }

    public void disableCardTopBorder() {
        mCardTop.setVisibility(GONE);
    }

    /**
     * Returns the full expanded height value of the border view.
     * This height should never change.
     */
    public static int getExpandedHeight() {
        if (sExpandedHeight == -1) {
            LogUtils.wtf(LOG_TAG, "full height not initialized");
        }

        return sExpandedHeight;
    }

    /**
     * Returns the collapsed height value of the border view.
     * This height should never change.
     */
    public static int getCollapsedHeight() {
        return sMessageBorderHeightCollapsed;
    }
}
