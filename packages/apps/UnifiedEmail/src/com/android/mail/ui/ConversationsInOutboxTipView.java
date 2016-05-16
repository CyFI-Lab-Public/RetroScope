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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.content.ObjectCursor;
import com.android.mail.content.ObjectCursorLoader;
import com.android.mail.preferences.AccountPreferences;
import com.android.mail.providers.Account;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.utils.Utils;

/**
 * Tip that is displayed in conversation list of 'Sent' folder whenever there are
 * one or more messages in the Outbox.
 */
public class ConversationsInOutboxTipView extends FrameLayout
        implements ConversationSpecialItemView, SwipeableItemView {

    private static int sScrollSlop = 0;
    private static int sShrinkAnimationDuration;

    private Account mAccount = null;
    private AccountPreferences mAccountPreferences;
    private AnimatedAdapter mAdapter;
    private LoaderManager mLoaderManager;
    private FolderSelector mFolderSelector;
    private Folder mOutbox;
    private int mOutboxCount = -1;

    private View mSwipeableContent;
    private TextView mText;

    private int mAnimatedHeight = -1;

    private View mTeaserRightEdge;
    /** Whether we are on a tablet device or not */
    private final boolean mTabletDevice;
    /** When in conversation mode, true if the list is hidden */
    private final boolean mListCollapsible;

    private static final int LOADER_FOLDER_LIST =
            AbstractActivityController.LAST_FRAGMENT_LOADER_ID + 100;

    public ConversationsInOutboxTipView(final Context context) {
        this(context, null);
    }

    public ConversationsInOutboxTipView(final Context context, final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ConversationsInOutboxTipView(
            final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = context.getResources();

        if (sScrollSlop == 0) {
            sScrollSlop = resources.getInteger(R.integer.swipeScrollSlop);
            sShrinkAnimationDuration = resources.getInteger(
                    R.integer.shrink_animation_duration);
        }

        mTabletDevice = Utils.useTabletUI(resources);
        mListCollapsible = resources.getBoolean(R.bool.list_collapsible);
    }

    public void bind(final Account account, final FolderSelector folderSelector) {
        mAccount = account;
        mAccountPreferences = AccountPreferences.get(getContext(), account.getEmailAddress());
        mFolderSelector = folderSelector;
    }

    @Override
    public void onGetView() {
        // Do nothing
    }

    @Override
    protected void onFinishInflate() {
        mSwipeableContent = findViewById(R.id.swipeable_content);

        mText = (TextView) findViewById(R.id.outbox);

        findViewById(R.id.outbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToOutbox();
            }
        });

        findViewById(R.id.dismiss_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mTeaserRightEdge = findViewById(R.id.teaser_right_edge);
    }

    private void goToOutbox() {
        if (mOutbox != null) {
            mFolderSelector.onFolderSelected(mOutbox);
        }
    }

    @Override
    public void onUpdate(Folder folder, ConversationCursor cursor) {
        if (mLoaderManager != null && folder != null) {
            if ((folder.type & UIProvider.FolderType.SENT) > 0) {
                // Only display this tip if user is viewing the Sent folder
                mLoaderManager.initLoader(LOADER_FOLDER_LIST, null, mFolderListLoaderCallbacks);
            }
        }
    }

    private final LoaderCallbacks<ObjectCursor<Folder>> mFolderListLoaderCallbacks =
            new LoaderManager.LoaderCallbacks<ObjectCursor<Folder>>() {
        @Override
        public void onLoaderReset(final Loader<ObjectCursor<Folder>> loader) {
            // Do nothing
        }

        @Override
        public void onLoadFinished(final Loader<ObjectCursor<Folder>> loader,
                final ObjectCursor<Folder> data) {
            if (data != null && data.moveToFirst()) {
                do {
                    final Folder folder = data.getModel();
                    if ((folder.type & UIProvider.FolderType.OUTBOX) > 0) {
                        mOutbox = folder;
                        onOutboxTotalCount(folder.totalCount);
                    }
                } while (data.moveToNext());
            }
        }

        @Override
        public Loader<ObjectCursor<Folder>> onCreateLoader(final int id, final Bundle args) {
            // This loads all folders in order to find 'Outbox'.  We could consider adding a new
            // query to load folders of a given type to make this more efficient, but should be
            // okay for now since this is triggered infrequently (only when user visits the
            // 'Sent' folder).
            final ObjectCursorLoader<Folder> loader = new ObjectCursorLoader<Folder>(getContext(),
                    mAccount.folderListUri, UIProvider.FOLDERS_PROJECTION, Folder.FACTORY);
            return loader;
        }
    };

    private void onOutboxTotalCount(int outboxCount) {
        if (mOutboxCount != outboxCount) {
            mOutboxCount = outboxCount;
            if (outboxCount > 0) {
                if (mText != null) {
                    updateText();
                }
            }
        }
        if (outboxCount == 0) {
            // Clear the last seen count, so that new messages in Outbox will always cause this
            // tip to appear again.
            mAccountPreferences.setLastSeenOutboxCount(0);
        }
    }

    private void updateText() {
        // Update the display text to reflect current mOutboxCount
        final Resources resources = getContext().getResources();
        final String subString = mOutbox.name;
        final String entireString = resources.getString(
                R.string.unsent_messages_in_outbox,
                String.valueOf(mOutboxCount), subString);
        final SpannableString text = new SpannableString(entireString);
        final int index = entireString.indexOf(subString);
        text.setSpan(
                new TextAppearanceSpan(getContext(), R.style.LinksInTipTextAppearance),
                index,
                index + subString.length(),
                0);
        mText.setText(text);
    }

    @Override
    public boolean getShouldDisplayInList() {
        return (mOutboxCount > 0 && mOutboxCount != mAccountPreferences.getLastSeenOutboxCount());
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
        mLoaderManager = loaderManager;
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
    }

    @Override
    public void onCabModeExited() {
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
        return true;
    }

    @Override
    public void dismiss() {
        // Do not show this tip again until we have a new count.  Note this is not quite
        // ideal behavior since after a user dismisses an "1 unsent in outbox" tip,
        // the message stuck in Outbox could get sent, and a new one gets stuck.
        // If the user checks back on on Sent folder then, we don't reshow the message since count
        // itself hasn't changed, but ideally we should since it's a different message than before.
        // However if user checks the Sent folder in between (when there were 0 messages
        // in Outbox), the preference is cleared (see {@link onOutboxTotalCount}).
        mAccountPreferences.setLastSeenOutboxCount(mOutboxCount);

        startDestroyAnimation();
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
        heightAnimator.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(final Animator animation) {
                // Do nothing
            }

            @Override
            public void onAnimationRepeat(final Animator animation) {
                // Do nothing
            }

            @Override
            public void onAnimationEnd(final Animator animation) {
                // We should no longer exist, so notify the adapter
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onAnimationCancel(final Animator animation) {
                // Do nothing
            }
        });
        heightAnimator.start();
    }

    /**
     * This method is used by the animator.  It is explicitly kept in proguard.flags to prevent it
     * from being removed, inlined, or obfuscated.
     * Edit ./vendor/unbundled/packages/apps/UnifiedGmail/proguard.flags
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
            setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), mAnimatedHeight);
        }
    }
}
