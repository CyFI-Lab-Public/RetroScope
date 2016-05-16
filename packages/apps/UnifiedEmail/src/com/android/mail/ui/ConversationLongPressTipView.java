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

package com.android.mail.ui;

import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.preferences.MailPrefs;
import com.android.mail.providers.Folder;
import com.android.mail.utils.Utils;

import android.animation.ObjectAnimator;
import android.app.LoaderManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * A tip to educate users about long press to enter CAB mode.  Appears on top of conversation list.
 */
// TODO: this class was shamelessly copied from ConversationPhotoTeaserView.  Look into
// extracting a common base class.
public class ConversationLongPressTipView extends FrameLayout
        implements ConversationSpecialItemView, SwipeableItemView {

    private static int sScrollSlop = 0;
    private static int sShrinkAnimationDuration;

    private final MailPrefs mMailPrefs;
    private AnimatedAdapter mAdapter;

    private View mSwipeableContent;

    private boolean mShow;
    private int mAnimatedHeight = -1;

    private View mTeaserRightEdge;
    /** Whether we are on a tablet device or not */
    private final boolean mTabletDevice;
    /** When in conversation mode, true if the list is hidden */
    private final boolean mListCollapsible;

    public ConversationLongPressTipView(final Context context) {
        this(context, null);
    }

    public ConversationLongPressTipView(final Context context, final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ConversationLongPressTipView(
            final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = context.getResources();

        if (sScrollSlop == 0) {
            sScrollSlop = resources.getInteger(R.integer.swipeScrollSlop);
            sShrinkAnimationDuration = resources.getInteger(
                    R.integer.shrink_animation_duration);
        }

        mMailPrefs = MailPrefs.get(context);

        mTabletDevice = Utils.useTabletUI(resources);
        mListCollapsible = resources.getBoolean(R.bool.list_collapsible);
    }

    @Override
    protected void onFinishInflate() {
        mSwipeableContent = findViewById(R.id.swipeable_content);

        findViewById(R.id.dismiss_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mTeaserRightEdge = findViewById(R.id.teaser_right_edge);
    }

    @Override
    public void onUpdate(Folder folder, ConversationCursor cursor) {
        // It's possible user has enabled/disabled sender images in settings, which affects
        // whether we want to show this tip or not.
        mShow = checkWhetherToShow();
    }

    @Override
    public void onGetView() {
        // Do nothing
    }

    @Override
    public boolean getShouldDisplayInList() {
        mShow = checkWhetherToShow();
        return mShow;
    }

    private boolean checkWhetherToShow() {
        // show if 1) sender images are disabled 2) there are items
        return !shouldShowSenderImage() && !mAdapter.isEmpty()
                && !mMailPrefs.isLongPressToSelectTipAlreadyShown();
    }

    @Override
    public int getPosition() {
        // We want this teaser to go before the first real conversation
        return 0;
    }

    @Override
    public void setAdapter(AnimatedAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public void bindFragment(final LoaderManager loaderManager, final Bundle savedInstanceState) {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void onConversationSelected() {
        // DO NOTHING
    }

    @Override
    public void onCabModeEntered() {
        if (mShow) {
            dismiss();
        }
    }

    @Override
    public void onCabModeExited() {
        // Do nothing
    }

    @Override
    public void onConversationListVisibilityChanged(final boolean visible) {
        // Do nothing
    }

    @Override
    public void saveInstanceState(final Bundle outState) {
        // Do nothing
    }

    @Override
    public boolean acceptsUserTaps() {
        // No, we don't allow user taps.
        return false;
    }

    @Override
    public void dismiss() {
        setDismissed();
        startDestroyAnimation();
    }

    private void setDismissed() {
        if (mShow) {
            mMailPrefs.setLongPressToSelectTipAlreadyShown();
            mShow = false;
            Analytics.getInstance().sendEvent("list_swipe", "long_press_tip", null, 0);
        }
    }

    protected boolean shouldShowSenderImage() {
        return mMailPrefs.getShowSenderImages();
    }

    @Override
    public SwipeableView getSwipeableView() {
        return SwipeableView.from(mSwipeableContent);
    }

    @Override
    public boolean canChildBeDismissed() {
        return true;
    }

    @Override
    public float getMinAllowScrollDistance() {
        return sScrollSlop;
    }

    private void startDestroyAnimation() {
        final int start = getHeight();
        final int end = 0;
        mAnimatedHeight = start;
        final ObjectAnimator heightAnimator =
                ObjectAnimator.ofInt(this, "animatedHeight", start, end);
        heightAnimator.setInterpolator(new DecelerateInterpolator(2.0f));
        heightAnimator.setDuration(sShrinkAnimationDuration);
        heightAnimator.start();

        /*
         * Ideally, we would like to call mAdapter.notifyDataSetChanged() in a listener's
         * onAnimationEnd(), but we are in the middle of a touch event, and this will cause all the
         * views to get recycled, which will cause problems.
         *
         * Instead, we'll just leave the item in the list with a height of 0, and the next
         * notifyDatasetChanged() will remove it from the adapter.
         */
    }

    /**
     * This method is used by the animator.  It is explicitly kept in proguard.flags to prevent it
     * from being removed, inlined, or obfuscated.
     * Edit ./packages/apps/UnifiedEmail/proguard.flags
     * In the future, we want to use @Keep
     */
    public void setAnimatedHeight(final int height) {
        mAnimatedHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        if (Utils.getDisplayListRightEdgeEffect(mTabletDevice, mListCollapsible,
                mAdapter.getViewMode())) {
            mTeaserRightEdge.setVisibility(VISIBLE);
        } else {
            mTeaserRightEdge.setVisibility(GONE);
        }

        if (mAnimatedHeight == -1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mAnimatedHeight);
        }
    }
}
