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

package com.android.mail.ui;

import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.browse.ConversationItemView;
import com.android.mail.providers.Account;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.utils.Utils;
import com.google.common.collect.ImmutableList;

public class LeaveBehindItem extends FrameLayout implements OnClickListener, SwipeableItemView {

    private ToastBarOperation mUndoOp;
    private Account mAccount;
    private AnimatedAdapter mAdapter;
    private TextView mText;
    private View mSwipeableContent;
    public int position;
    private Conversation mData;
    private int mWidth;
    /**
     * The height of this view. Typically, this matches the height of the originating
     * {@link ConversationItemView}.
     */
    private int mHeight;
    private int mAnimatedHeight = -1;
    private boolean mAnimating;
    private boolean mFadingInText;
    private boolean mInert = false;
    private ObjectAnimator mFadeIn;

    private static int sShrinkAnimationDuration = -1;
    private static int sFadeInAnimationDuration = -1;
    private static float sScrollSlop;
    private static final float OPAQUE = 1.0f;
    private static final float TRANSPARENT = 0.0f;

    public LeaveBehindItem(Context context) {
        this(context, null);
    }

    public LeaveBehindItem(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public LeaveBehindItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadStatics(context);
    }

    private static void loadStatics(final Context context) {
        if (sShrinkAnimationDuration == -1) {
            Resources res = context.getResources();
            sShrinkAnimationDuration = res.getInteger(R.integer.shrink_animation_duration);
            sFadeInAnimationDuration = res.getInteger(R.integer.fade_in_animation_duration);
            sScrollSlop = res.getInteger(R.integer.leaveBehindSwipeScrollSlop);
        }
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        if (id == R.id.swipeable_content) {
            if (mAccount.undoUri != null && !mInert) {
                // NOTE: We might want undo to return the messages affected,
                // in which case the resulting cursor might be interesting...
                // TODO: Use UIProvider.SEQUENCE_QUERY_PARAMETER to indicate
                // the set of commands to undo
                mAdapter.setSwipeUndo(true);
                mAdapter.clearLeaveBehind(getConversationId());
                ConversationCursor cursor = mAdapter.getConversationCursor();
                if (cursor != null) {
                    cursor.undo(getContext(), mAccount.undoUri);
                }
            }
        } else if (id == R.id.undo_descriptionview) {
            // Essentially, makes sure that tapping description view doesn't highlight
            // either the undo button icon or text.
        }
    }

    public void bind(int pos, Account account, AnimatedAdapter adapter,
            ToastBarOperation undoOp, Conversation target, Folder folder, int height) {
        position = pos;
        mUndoOp = undoOp;
        mAccount = account;
        mAdapter = adapter;
        mHeight = height;
        setData(target);
        mSwipeableContent = findViewById(R.id.swipeable_content);
        // Listen on swipeable content so that we can show both the undo icon
        // and button text as selected since they set duplicateParentState to true
        mSwipeableContent.setOnClickListener(this);
        mSwipeableContent.setAlpha(TRANSPARENT);
        mText = ((TextView) findViewById(R.id.undo_descriptionview));
        mText.setText(Utils.convertHtmlToPlainText(mUndoOp
                .getSingularDescription(getContext(), folder)));
        mText.setOnClickListener(this);
    }

    public void commit() {
        ConversationCursor cursor = mAdapter.getConversationCursor();
        if (cursor != null) {
            cursor.delete(ImmutableList.of(getData()));
        }
    }

    @Override
    public void dismiss() {
        if (mAdapter != null) {
            Analytics.getInstance().sendEvent("list_swipe", "leave_behind", null, 0);
            mAdapter.fadeOutSpecificLeaveBehindItem(mData.id);
            mAdapter.notifyDataSetChanged();
        }
    }

    public long getConversationId() {
        return getData().id;
    }

    @Override
    public SwipeableView getSwipeableView() {
        return SwipeableView.from(mSwipeableContent);
    }

    @Override
    public boolean canChildBeDismissed() {
        return !mInert;
    }

    public LeaveBehindData getLeaveBehindData() {
        return new LeaveBehindData(getData(), mUndoOp, mHeight);
    }

    /**
     * Animate shrinking the height of this view.
     * @param item the conversation to animate
     * @param listener the method to call when the animation is done
     * @param undo true if an operation is being undone. We animate the item
     *            away during delete. Undoing populates the item.
     */
    public void startShrinkAnimation(AnimatorListener listener) {
        if (!mAnimating) {
            mAnimating = true;
            final ObjectAnimator height = ObjectAnimator.ofInt(this, "animatedHeight", mHeight, 0);
            setMinimumHeight(mHeight);
            mWidth = getWidth();
            height.setInterpolator(new DecelerateInterpolator(1.75f));
            height.setDuration(sShrinkAnimationDuration);
            height.addListener(listener);
            height.start();
        }
    }

    /**
     * Set the alpha value for the text displayed by this item.
     */
    public void setTextAlpha(float alpha) {
        if (mSwipeableContent.getAlpha() > TRANSPARENT) {
            mSwipeableContent.setAlpha(alpha);
        }
    }

    /**
     * Kick off the animation to fade in the leave behind text.
     * @param delay Whether to delay the start of the animation or not.
     */
    public void startFadeInTextAnimation(int delay) {
        // If this thing isn't already fully visible AND its not already animating...
        if (!mFadingInText && mSwipeableContent.getAlpha() != OPAQUE) {
            mFadingInText = true;
            mFadeIn = startFadeInTextAnimation(mSwipeableContent, delay);
        }
    }

    /**
     * Creates and starts the animator for the fade-in text
     * @param delay The delay, in milliseconds, before starting the animation
     * @return The {@link ObjectAnimator}
     */
    public static ObjectAnimator startFadeInTextAnimation(final View view, final int delay) {
        loadStatics(view.getContext());

        final float start = TRANSPARENT;
        final float end = OPAQUE;
        final ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", start, end);
        view.setAlpha(TRANSPARENT);
        if (delay != 0) {
            fadeIn.setStartDelay(delay);
        }
        fadeIn.setInterpolator(new DecelerateInterpolator(OPAQUE));
        fadeIn.setDuration(sFadeInAnimationDuration / 2);
        fadeIn.start();

        return fadeIn;
    }

    /**
     * Increase the overall time before fading in a the text description this view.
     * @param newDelay Amount of total delay the user should see
     */
    public void increaseFadeInDelay(int newDelay) {
        // If this thing isn't already fully visible AND its not already animating...
        if (!mFadingInText && mSwipeableContent.getAlpha() != OPAQUE) {
            mFadingInText = true;
            long delay = mFadeIn.getStartDelay();
            if (newDelay == delay || mFadeIn.isRunning()) {
                return;
            }
            mFadeIn.cancel();
            mFadeIn.setStartDelay(newDelay - delay);
            mFadeIn.start();
        }
    }

    /**
     * Cancel fading in the text description for this view.
     */
    public void cancelFadeInTextAnimation() {
        if (mFadeIn != null) {
            mFadingInText = false;
            mFadeIn.cancel();
        }
    }

    /**
     * Cancel fading in the text description for this view only if it the
     * animation hasn't already started.
     * @return whether the animation was cancelled
     */
    public boolean cancelFadeInTextAnimationIfNotStarted() {
        // The animation was started, so don't cancel and restart it.
        if (mFadeIn != null && !mFadeIn.isRunning()) {
            cancelFadeInTextAnimation();
            return true;
        }
        return false;
    }

    public void setData(Conversation conversation) {
        mData = conversation;
    }

    public Conversation getData() {
        return mData;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAnimatedHeight != -1) {
            setMeasuredDimension(mWidth, mAnimatedHeight);
        } else {
            // override the height MeasureSpec to ensure this is sized up at the desired height
            super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
        }
    }

    // Used by animator
    @SuppressWarnings("unused")
    public void setAnimatedHeight(int height) {
        mAnimatedHeight = height;
        requestLayout();
    }

    @Override
    public float getMinAllowScrollDistance() {
        return sScrollSlop;
    }

    public void makeInert() {
        if (mFadeIn != null) {
            mFadeIn.cancel();
        }
        mSwipeableContent.setVisibility(View.GONE);
        mInert = true;
    }

    public void cancelFadeOutText() {
        mSwipeableContent.setAlpha(OPAQUE);
    }

    public boolean isAnimating() {
        return this.mFadingInText;
    }
}