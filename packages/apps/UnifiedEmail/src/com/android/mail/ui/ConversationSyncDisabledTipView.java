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
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.preferences.AccountPreferences;
import com.android.mail.preferences.MailPrefs;
import com.android.mail.providers.Account;
import com.android.mail.providers.Folder;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

/**
 * A tip displayed on top of conversation view to indicate that Gmail sync is
 * currently disabled on this account.
 */
public class ConversationSyncDisabledTipView extends FrameLayout
        implements ConversationSpecialItemView, SwipeableItemView {

    private static final String LOG_TAG = LogTag.getLogTag();

    private static int sScrollSlop = 0;
    private static int sShrinkAnimationDuration;

    private Account mAccount = null;
    private Folder mFolder = null;
    private final MailPrefs mMailPrefs;
    private AccountPreferences mAccountPreferences;
    private AnimatedAdapter mAdapter;
    private Activity mActivity;

    private View mSwipeableContent;
    private TextView mText1;
    private TextView mText2;
    private View mTextArea;
    private SpannableString mEnableSyncInAccountSettingsText;
    private final OnClickListener mAutoSyncOffTextClickedListener;
    private final OnClickListener mAccountSyncOffTextClickedListener;

    private int mAnimatedHeight = -1;

    private int mReasonSyncOff = ReasonSyncOff.NONE;

    private View mTeaserRightEdge;
    /** Whether we are on a tablet device or not */
    private final boolean mTabletDevice;
    /** When in conversation mode, true if the list is hidden */
    private final boolean mListCollapsible;

    public interface ReasonSyncOff {
        // Background sync is enabled for current account, do not display this tip
        public static final int NONE = 0;
        // Global auto-sync (affects all apps and all accounts) is turned off
        public static final int AUTO_SYNC_OFF = 1;
        // Global auto-sync is on, but Gmail app level sync is disabled for this particular account
        public static final int ACCOUNT_SYNC_OFF = 2;
    }

    public ConversationSyncDisabledTipView(final Context context) {
        this(context, null);
    }

    public ConversationSyncDisabledTipView(final Context context, final AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ConversationSyncDisabledTipView(
            final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        final Resources resources = context.getResources();

        if (sScrollSlop == 0) {
            sScrollSlop = resources.getInteger(R.integer.swipeScrollSlop);
            sShrinkAnimationDuration = resources.getInteger(
                    R.integer.shrink_animation_duration);
        }

        mMailPrefs = MailPrefs.get(context);

        mAutoSyncOffTextClickedListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                final TurnAutoSyncOnDialog dialog = TurnAutoSyncOnDialog.newInstance(
                        mAccount.getAccountManagerAccount(), mAccount.syncAuthority);
                dialog.show(mActivity.getFragmentManager(), TurnAutoSyncOnDialog.DIALOG_TAG);
            }
        };

        mAccountSyncOffTextClickedListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.showAccountSettings(getContext(), mAccount);
            }
        };

        // Create the "Turn on in Account settings." text where "Account settings" appear as
        // a blue link.
        final String subString = resources.getString(R.string.account_settings_param);
        final String entireString = resources.getString(
                R.string.enable_sync_in_account_settings, subString);
        mEnableSyncInAccountSettingsText = new SpannableString(entireString);
        final int index = entireString.indexOf(subString);
        mEnableSyncInAccountSettingsText.setSpan(
                new TextAppearanceSpan(context, R.style.LinksInTipTextAppearance),
                index,
                index + subString.length(),
                0);

        mTabletDevice = Utils.useTabletUI(resources);
        mListCollapsible = resources.getBoolean(R.bool.list_collapsible);
    }

    public void bindAccount(Account account, ControllableActivity activity) {
        mAccount = account;
        mAccountPreferences = AccountPreferences.get(getContext(), account.getEmailAddress());
        mActivity = (Activity) activity;
    }

    @Override
    public void onGetView() {
        // Do nothing
    }

    @Override
    protected void onFinishInflate() {
        mSwipeableContent = findViewById(R.id.swipeable_content);

        mText1 = (TextView) findViewById(R.id.text_line1);
        mText2 = (TextView) findViewById(R.id.text_line2);
        mTextArea = findViewById(R.id.text_area);

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
        mFolder = folder;
    }

    @Override
    public boolean getShouldDisplayInList() {
        if (mAccount == null || mAccount.syncAuthority == null) {
            return false;
        }

        // Do not show this message for folders/labels that are not set to sync.
        if (mFolder == null || mFolder.syncWindow <= 0) {
            return false;
        }

        setReasonSyncOff(calculateReasonSyncOff(mMailPrefs, mAccount, mAccountPreferences));

        if (mReasonSyncOff != ReasonSyncOff.NONE) {
            LogUtils.i(LOG_TAG, "Sync is off with reason %d", mReasonSyncOff);
        }

        switch (mReasonSyncOff) {
            case ReasonSyncOff.AUTO_SYNC_OFF:
                return (mMailPrefs.getNumOfDismissesForAutoSyncOff() == 0);
            case ReasonSyncOff.ACCOUNT_SYNC_OFF:
                return (mAccountPreferences.getNumOfDismissesForAccountSyncOff() == 0);
            default:
                return false;
        }
    }

    public static int calculateReasonSyncOff(MailPrefs mailPrefs,
            Account account, AccountPreferences accountPreferences) {
        if (!ContentResolver.getMasterSyncAutomatically()) {
            // Global sync is turned off
            accountPreferences.resetNumOfDismissesForAccountSyncOff();
            // Logging to track down bug where this tip is being showing when it shouldn't be.
            LogUtils.i(LOG_TAG, "getMasterSyncAutomatically() return false");
            return ReasonSyncOff.AUTO_SYNC_OFF;
        } else {
            // Global sync is on, clear the number of times users has dismissed this
            // warning so that next time global sync is off, warning gets displayed again.
            mailPrefs.resetNumOfDismissesForAutoSyncOff();

            // Now check for whether account level sync is on/off.
            android.accounts.Account acct = account.getAccountManagerAccount();
            if (!TextUtils.isEmpty(account.syncAuthority) &&
                    !ContentResolver.getSyncAutomatically(acct, account.syncAuthority)) {
                // Account level sync is off
                return ReasonSyncOff.ACCOUNT_SYNC_OFF;
            } else {
                // Account sync is on, clear the number of times users has dismissed this
                // warning so that next time sync is off, warning gets displayed again.
                accountPreferences.resetNumOfDismissesForAccountSyncOff();
                return ReasonSyncOff.NONE;
            }
        }
    }

    private void setReasonSyncOff(int reason) {
        if (mReasonSyncOff != reason) {
            mReasonSyncOff = reason;
            switch (mReasonSyncOff) {
                case ReasonSyncOff.AUTO_SYNC_OFF:
                    mText1.setText(R.string.auto_sync_off);
                    mText2.setText(R.string.tap_to_enable_sync);
                    mText2.setVisibility(View.VISIBLE);
                    mTextArea.setClickable(true);
                    mTextArea.setOnClickListener(mAutoSyncOffTextClickedListener);
                    break;
                case ReasonSyncOff.ACCOUNT_SYNC_OFF:
                    mText1.setText(R.string.account_sync_off);
                    mText2.setText(mEnableSyncInAccountSettingsText);
                    mText2.setVisibility(View.VISIBLE);
                    mTextArea.setClickable(true);
                    mTextArea.setOnClickListener(mAccountSyncOffTextClickedListener);
                    break;
                default:
                    // Doesn't matter what mText is since this view is not displayed
            }
        }
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
    public void bindFragment(LoaderManager loaderManager, final Bundle savedInstanceState) {
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
        return true;
    }

    @Override
    public void dismiss() {
        final String reason;
        switch (mReasonSyncOff) {
            case ReasonSyncOff.AUTO_SYNC_OFF:
                mMailPrefs.incNumOfDismissesForAutoSyncOff();
                reason = "auto_sync_off";
                break;
            case ReasonSyncOff.ACCOUNT_SYNC_OFF:
                mAccountPreferences.incNumOfDismissesForAccountSyncOff();
                reason = "account_sync_off";
                break;
            default:
                reason = null;
                break;
        }
        Analytics.getInstance().sendEvent("list_swipe", "sync_disabled_tip", reason, 0);
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
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), mAnimatedHeight);
        }
    }
}
