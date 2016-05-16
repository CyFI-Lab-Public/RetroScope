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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.SimpleCursorAdapter;

import com.android.bitmap.AltBitmapCache;
import com.android.bitmap.BitmapCache;
import com.android.bitmap.DecodeAggregator;
import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.browse.ConversationItemView;
import com.android.mail.browse.ConversationItemViewCoordinates.CoordinatesCache;
import com.android.mail.browse.SwipeableConversationItemView;
import com.android.mail.preferences.MailPrefs;
import com.android.mail.providers.Account;
import com.android.mail.providers.AccountObserver;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.UIProvider;
import com.android.mail.providers.UIProvider.ConversationListIcon;
import com.android.mail.ui.SwipeableListView.ListItemsRemovedListener;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class AnimatedAdapter extends SimpleCursorAdapter {
    private static int sDismissAllShortDelay = -1;
    private static int sDismissAllLongDelay = -1;
    private static final String LAST_DELETING_ITEMS = "last_deleting_items";
    private static final String LEAVE_BEHIND_ITEM_DATA = "leave_behind_item_data";
    private static final String LEAVE_BEHIND_ITEM_ID = "leave_behind_item_id";
    private final static int TYPE_VIEW_CONVERSATION = 0;
    private final static int TYPE_VIEW_FOOTER = 1;
    private final static int TYPE_VIEW_DONT_RECYCLE = -1;
    private final HashSet<Long> mDeletingItems = new HashSet<Long>();
    private final ArrayList<Long> mLastDeletingItems = new ArrayList<Long>();
    private final HashSet<Long> mUndoingItems = new HashSet<Long>();
    private final HashSet<Long> mSwipeDeletingItems = new HashSet<Long>();
    private final HashSet<Long> mSwipeUndoingItems = new HashSet<Long>();
    private final HashMap<Long, SwipeableConversationItemView> mAnimatingViews =
            new HashMap<Long, SwipeableConversationItemView>();
    private final HashMap<Long, LeaveBehindItem> mFadeLeaveBehindItems =
            new HashMap<Long, LeaveBehindItem>();
    /** The current account */
    private Account mAccount;
    private final Context mContext;
    private final ConversationSelectionSet mBatchConversations;
    private Runnable mCountDown;
    private final Handler mHandler;
    protected long mLastLeaveBehind = -1;

    private final BitmapCache mBitmapCache;
    private final DecodeAggregator mDecodeAggregator;

    public interface ConversationListListener {
        /**
         * @return <code>true</code> if the list is just exiting selection mode (so animations may
         * be required), <code>false</code> otherwise
         */
        boolean isExitingSelectionMode();
    }

    private final AnimatorListener mAnimatorListener = new AnimatorListenerAdapter() {

        @Override
        public void onAnimationStart(Animator animation) {
            if (!mUndoingItems.isEmpty()) {
                mDeletingItems.clear();
                mLastDeletingItems.clear();
                mSwipeDeletingItems.clear();
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            Object obj;
            if (animation instanceof AnimatorSet) {
                AnimatorSet set = (AnimatorSet) animation;
                obj = ((ObjectAnimator) set.getChildAnimations().get(0)).getTarget();
            } else {
                obj = ((ObjectAnimator) animation).getTarget();
            }
            updateAnimatingConversationItems(obj, mSwipeDeletingItems);
            updateAnimatingConversationItems(obj, mDeletingItems);
            updateAnimatingConversationItems(obj, mSwipeUndoingItems);
            updateAnimatingConversationItems(obj, mUndoingItems);
            if (hasFadeLeaveBehinds() && obj instanceof LeaveBehindItem) {
                LeaveBehindItem objItem = (LeaveBehindItem) obj;
                clearLeaveBehind(objItem.getConversationId());
                objItem.commit();
                if (!hasFadeLeaveBehinds()) {
                    // Cancel any existing animations on the remaining leave behind
                    // item and start fading in text immediately.
                    LeaveBehindItem item = getLastLeaveBehindItem();
                    if (item != null) {
                        boolean cancelled = item.cancelFadeInTextAnimationIfNotStarted();
                        if (cancelled) {
                            item.startFadeInTextAnimation(0 /* delay start */);
                        }
                    }
                }
                // The view types have changed, since the animating views are gone.
                notifyDataSetChanged();
            }

            if (!isAnimating()) {
                mActivity.onAnimationEnd(AnimatedAdapter.this);
            }
        }

    };

    /**
     * The next action to perform. Do not read or write this. All accesses should
     * be in {@link #performAndSetNextAction(SwipeableListView.ListItemsRemovedListener)} which
     * commits the previous action, if any.
     */
    private ListItemsRemovedListener mPendingDestruction;

    /**
     * A destructive action that refreshes the list and performs no other action.
     */
    private final ListItemsRemovedListener mRefreshAction = new ListItemsRemovedListener() {
        @Override
        public void onListItemsRemoved() {
            notifyDataSetChanged();
        }
    };

    public interface Listener {
        void onAnimationEnd(AnimatedAdapter adapter);
    }

    private View mFooter;
    private boolean mShowFooter;
    private Folder mFolder;
    private final SwipeableListView mListView;
    private boolean mSwipeEnabled;
    private final HashMap<Long, LeaveBehindItem> mLeaveBehindItems = Maps.newHashMap();
    /** True if priority inbox markers are enabled, false otherwise. */
    private boolean mPriorityMarkersEnabled;
    private final ControllableActivity mActivity;
    private final ConversationListListener mConversationListListener;
    private final AccountObserver mAccountListener = new AccountObserver() {
        @Override
        public void onChanged(Account newAccount) {
            if (setAccount(newAccount)) {
                notifyDataSetChanged();
            }
        }
    };

    /**
     * A list of all views that are not conversations. These include temporary views from
     * {@link #mFleetingViews} and child folders from {@link #mFolderViews}.
     */
    private final SparseArray<ConversationSpecialItemView> mSpecialViews;

    private final CoordinatesCache mCoordinatesCache = new CoordinatesCache();

    /**
     * Temporary views insert at specific positions relative to conversations. These can be
     * related to showing new features (on-boarding) or showing information about new mailboxes
     * that have been added by the system.
     */
    private final List<ConversationSpecialItemView> mFleetingViews;

    /**
     * @return <code>true</code> if a relevant part of the account has changed, <code>false</code>
     *         otherwise
     */
    private boolean setAccount(Account newAccount) {
        final boolean accountChanged;
        if (mAccount != null && mAccount.uri.equals(newAccount.uri)
                && mAccount.settings.priorityArrowsEnabled ==
                        newAccount.settings.priorityArrowsEnabled
                && mAccount.supportsCapability(UIProvider.AccountCapabilities.UNDO) ==
                        newAccount.supportsCapability(UIProvider.AccountCapabilities.UNDO)
                && mAccount.settings.convListIcon == newAccount.settings.convListIcon
                && mAccount.settings.convListAttachmentPreviews ==
                        newAccount.settings.convListAttachmentPreviews) {
            accountChanged = false;
        } else {
            accountChanged = true;
        }

        mAccount = newAccount;
        mPriorityMarkersEnabled = mAccount.settings.priorityArrowsEnabled;
        mSwipeEnabled = mAccount.supportsCapability(UIProvider.AccountCapabilities.UNDO);

        Analytics.getInstance().setCustomDimension(Analytics.CD_INDEX_SENDER_IMAGES_ENABLED, Boolean
                .toString(newAccount.settings.convListIcon == ConversationListIcon.SENDER_IMAGE));
        Analytics.getInstance().setCustomDimension(Analytics.CD_INDEX_ATTACHMENT_PREVIEWS_ENABLED,
                Boolean.toString(newAccount.settings.convListAttachmentPreviews));
        Analytics.getInstance().setCustomDimension(Analytics.CD_INDEX_REPLY_ALL_SETTING,
                (newAccount.settings.replyBehavior == UIProvider.DefaultReplyBehavior.REPLY)
                ? "reply"
                : "reply_all");

        return accountChanged;
    }

    private static final String LOG_TAG = LogTag.getLogTag();
    private static final int INCREASE_WAIT_COUNT = 2;

    private static final int BITMAP_CACHE_TARGET_SIZE_BYTES = 0; // TODO: enable cache
    /**
     * This is the fractional portion of the total cache size above that's dedicated to non-pooled
     * bitmaps. (This is basically the portion of cache dedicated to GIFs.)
     */
    private static final float BITMAP_CACHE_NON_POOLED_FRACTION = 0.1f;

    public AnimatedAdapter(Context context, ConversationCursor cursor,
            ConversationSelectionSet batch, ControllableActivity activity,
            final ConversationListListener conversationListListener, SwipeableListView listView,
            final List<ConversationSpecialItemView> specialViews) {
        super(context, -1, cursor, UIProvider.CONVERSATION_PROJECTION, null, 0);
        mContext = context;
        mBatchConversations = batch;
        setAccount(mAccountListener.initialize(activity.getAccountController()));
        mActivity = activity;
        mConversationListListener = conversationListListener;
        mShowFooter = false;
        mListView = listView;

        mBitmapCache = new AltBitmapCache(BITMAP_CACHE_TARGET_SIZE_BYTES,
                BITMAP_CACHE_NON_POOLED_FRACTION);
        mDecodeAggregator = new DecodeAggregator();

        mHandler = new Handler();
        if (sDismissAllShortDelay == -1) {
            final Resources r = context.getResources();
            sDismissAllShortDelay = r.getInteger(R.integer.dismiss_all_leavebehinds_short_delay);
            sDismissAllLongDelay = r.getInteger(R.integer.dismiss_all_leavebehinds_long_delay);
        }
        if (specialViews != null) {
            mFleetingViews = new ArrayList<ConversationSpecialItemView>(specialViews);
        } else {
            mFleetingViews = new ArrayList<ConversationSpecialItemView>(0);
        }
        /** Total number of special views */
        final int size = mFleetingViews.size();
        mSpecialViews = new SparseArray<ConversationSpecialItemView>(size);

        // Only set the adapter in teaser views. Folder views don't care about the adapter.
        for (final ConversationSpecialItemView view : mFleetingViews) {
            view.setAdapter(this);
        }
        updateSpecialViews();
    }

    public void cancelDismissCounter() {
        cancelLeaveBehindFadeInAnimation();
        mHandler.removeCallbacks(mCountDown);
    }

    public void startDismissCounter() {
        if (mLeaveBehindItems.size() > INCREASE_WAIT_COUNT) {
            mHandler.postDelayed(mCountDown, sDismissAllLongDelay);
        } else {
            mHandler.postDelayed(mCountDown, sDismissAllShortDelay);
        }
    }

    public final void destroy() {
        // Set a null cursor in the adapter
        swapCursor(null);
        mAccountListener.unregisterAndDestroy();
    }

    @Override
    public int getCount() {
        // mSpecialViews only contains the views that are currently being displayed
        final int specialViewCount = mSpecialViews.size();

        final int count = super.getCount() + specialViewCount;
        return mShowFooter ? count + 1 : count;
    }

    /**
     * Add a conversation to the undo set, but only if its deletion is still cached. If the
     * deletion has already been written through and the cursor doesn't have it anymore, we can't
     * handle it here, and should instead rely on the cursor refresh to restore the item.
     * @param item id for the conversation that is being undeleted.
     * @return true if the conversation is still cached and therefore we will handle the undo.
     */
    private boolean addUndoingItem(final long item) {
        if (getConversationCursor().getUnderlyingPosition(item) >= 0) {
            mUndoingItems.add(item);
            return true;
        }
        return false;
    }

    public void setUndo(boolean undo) {
        if (undo) {
            boolean itemAdded = false;
            if (!mLastDeletingItems.isEmpty()) {
                for (Long item : mLastDeletingItems) {
                    itemAdded |= addUndoingItem(item);
                }
                mLastDeletingItems.clear();
            }
            if (mLastLeaveBehind != -1) {
                itemAdded |= addUndoingItem(mLastLeaveBehind);
                mLastLeaveBehind = -1;
            }
            // Start animation, only if we're handling the undo.
            if (itemAdded) {
                notifyDataSetChanged();
                performAndSetNextAction(mRefreshAction);
            }
        }
    }

    public void setSwipeUndo(boolean undo) {
        if (undo) {
            if (!mLastDeletingItems.isEmpty()) {
                mSwipeUndoingItems.addAll(mLastDeletingItems);
                mLastDeletingItems.clear();
            }
            if (mLastLeaveBehind != -1) {
                mSwipeUndoingItems.add(mLastLeaveBehind);
                mLastLeaveBehind = -1;
            }
            // Start animation
            notifyDataSetChanged();
            performAndSetNextAction(mRefreshAction);
        }
    }

    public View createConversationItemView(SwipeableConversationItemView view, Context context,
            Conversation conv) {
        if (view == null) {
            view = new SwipeableConversationItemView(context, mAccount.name);
        }
        view.bind(conv, mActivity, mConversationListListener, mBatchConversations, mFolder,
                getCheckboxSetting(), getAttachmentPreviewsSetting(),
                getParallaxSpeedAlternativeSetting(), getParallaxDirectionAlternativeSetting(),
                mSwipeEnabled, mPriorityMarkersEnabled, this);
        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getViewTypeCount() {
        // TYPE_VIEW_CONVERSATION, TYPE_VIEW_DELETING, TYPE_VIEW_UNDOING, and
        // TYPE_VIEW_FOOTER, TYPE_VIEW_LEAVEBEHIND.
        return 5;
    }

    @Override
    public int getItemViewType(int position) {
        // Try to recycle views.
        if (mShowFooter && position == getCount() - 1) {
            return TYPE_VIEW_FOOTER;
        } else if (hasLeaveBehinds() || isAnimating()) {
            // Setting as type -1 means the recycler won't take this view and
            // return it in get view. This is a bit of a "hammer" in that it
            // won't let even safe views be recycled here,
            // but its safer and cheaper than trying to determine individual
            // types. In a future release, use position/id map to try to make
            // this cleaner / faster to determine if the view is animating.
            return TYPE_VIEW_DONT_RECYCLE;
        } else if (mSpecialViews.get(position) != null) {
            // Don't recycle the special views
            return TYPE_VIEW_DONT_RECYCLE;
        }
        return TYPE_VIEW_CONVERSATION;
    }

    /**
     * Deletes the selected conversations from the conversation list view with a
     * translation and then a shrink. These conversations <b>must</b> have their
     * {@link Conversation#position} set to the position of these conversations
     * among the list. This will only remove the element from the list. The job
     * of deleting the actual element is left to the the listener. This listener
     * will be called when the animations are complete and is required to delete
     * the conversation.
     * @param conversations
     * @param listener
     */
    public void swipeDelete(Collection<Conversation> conversations,
            ListItemsRemovedListener listener) {
        delete(conversations, listener, mSwipeDeletingItems);
    }


    /**
     * Deletes the selected conversations from the conversation list view by
     * shrinking them away. These conversations <b>must</b> have their
     * {@link Conversation#position} set to the position of these conversations
     * among the list. This will only remove the element from the list. The job
     * of deleting the actual element is left to the the listener. This listener
     * will be called when the animations are complete and is required to delete
     * the conversation.
     * @param conversations
     * @param listener
     */
    public void delete(Collection<Conversation> conversations, ListItemsRemovedListener listener) {
        delete(conversations, listener, mDeletingItems);
    }

    private void delete(Collection<Conversation> conversations, ListItemsRemovedListener listener,
            HashSet<Long> list) {
        // Clear out any remaining items and add the new ones
        mLastDeletingItems.clear();
        // Since we are deleting new items, clear any remaining undo items
        mUndoingItems.clear();

        final int startPosition = mListView.getFirstVisiblePosition();
        final int endPosition = mListView.getLastVisiblePosition();

        // Only animate visible items
        for (Conversation c: conversations) {
            if (c.position >= startPosition && c.position <= endPosition) {
                mLastDeletingItems.add(c.id);
                list.add(c.id);
            }
        }

        if (list.isEmpty()) {
            // If we have no deleted items on screen, skip the animation
            listener.onListItemsRemoved();
        } else {
            performAndSetNextAction(listener);
        }
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mShowFooter && position == getCount() - 1) {
            return mFooter;
        }

        // Check if this is a special view
        final ConversationSpecialItemView specialView = mSpecialViews.get(position);
        if (specialView != null) {
            specialView.onGetView();
            return (View) specialView;
        }

        Utils.traceBeginSection("AA.getView");

        final ConversationCursor cursor = (ConversationCursor) getItem(position);
        final Conversation conv = cursor.getConversation();

        // Notify the provider of this change in the position of Conversation cursor
        cursor.notifyUIPositionChange();

        if (isPositionUndoing(conv.id)) {
            return getUndoingView(position - getPositionOffset(position), conv, parent,
                    false /* don't show swipe background */);
        } if (isPositionUndoingSwipe(conv.id)) {
            return getUndoingView(position - getPositionOffset(position), conv, parent,
                    true /* show swipe background */);
        } else if (isPositionDeleting(conv.id)) {
            return getDeletingView(position - getPositionOffset(position), conv, parent, false);
        } else if (isPositionSwipeDeleting(conv.id)) {
            return getDeletingView(position - getPositionOffset(position), conv, parent, true);
        }
        if (hasFadeLeaveBehinds()) {
            if(isPositionFadeLeaveBehind(conv)) {
                LeaveBehindItem fade  = getFadeLeaveBehindItem(position, conv);
                fade.startShrinkAnimation(mAnimatorListener);
                Utils.traceEndSection();
                return fade;
            }
        }
        if (hasLeaveBehinds()) {
            if (isPositionLeaveBehind(conv)) {
                final LeaveBehindItem fadeIn = getLeaveBehindItem(conv);
                if (conv.id == mLastLeaveBehind) {
                    // If it looks like the person is doing a lot of rapid
                    // swipes, wait patiently before animating
                    if (mLeaveBehindItems.size() > INCREASE_WAIT_COUNT) {
                        if (fadeIn.isAnimating()) {
                            fadeIn.increaseFadeInDelay(sDismissAllLongDelay);
                        } else {
                            fadeIn.startFadeInTextAnimation(sDismissAllLongDelay);
                        }
                    } else {
                        // Otherwise, assume they are just doing 1 and wait less time
                        fadeIn.startFadeInTextAnimation(sDismissAllShortDelay /* delay start */);
                    }
                }
                Utils.traceEndSection();
                return fadeIn;
            }
        }

        if (convertView != null && !(convertView instanceof SwipeableConversationItemView)) {
            LogUtils.w(LOG_TAG, "Incorrect convert view received; nulling it out");
            convertView = newView(mContext, cursor, parent);
        } else if (convertView != null) {
            ((SwipeableConversationItemView) convertView).reset();
        }
        final View v = createConversationItemView((SwipeableConversationItemView) convertView,
                mContext, conv);
        Utils.traceEndSection();
        return v;
    }

    private boolean hasLeaveBehinds() {
        return !mLeaveBehindItems.isEmpty();
    }

    private boolean hasFadeLeaveBehinds() {
        return !mFadeLeaveBehindItems.isEmpty();
    }

    public LeaveBehindItem setupLeaveBehind(Conversation target, ToastBarOperation undoOp,
            int deletedRow, int viewHeight) {
        cancelLeaveBehindFadeInAnimation();
        mLastLeaveBehind = target.id;
        fadeOutLeaveBehindItems();

        final LeaveBehindItem leaveBehind = (LeaveBehindItem) LayoutInflater.from(mContext)
                .inflate(R.layout.swipe_leavebehind, mListView, false);
        leaveBehind.bind(deletedRow, mAccount, this, undoOp, target, mFolder, viewHeight);
        mLeaveBehindItems.put(target.id, leaveBehind);
        mLastDeletingItems.add(target.id);
        return leaveBehind;
    }

    public void fadeOutSpecificLeaveBehindItem(long id) {
        if (mLastLeaveBehind == id) {
            mLastLeaveBehind = -1;
        }
        startFadeOutLeaveBehindItemsAnimations();
    }

    // This should kick off a timer such that there is a minimum time each item
    // shows up before being dismissed. That way if the user is swiping away
    // items in rapid succession, their finger position is maintained.
    public void fadeOutLeaveBehindItems() {
        if (mCountDown == null) {
            mCountDown = new Runnable() {
                @Override
                public void run() {
                    startFadeOutLeaveBehindItemsAnimations();
                }
            };
        } else {
            mHandler.removeCallbacks(mCountDown);
        }
        // Clear all the text since these are no longer clickable
        Iterator<Entry<Long, LeaveBehindItem>> i = mLeaveBehindItems.entrySet().iterator();
        LeaveBehindItem item;
        while (i.hasNext()) {
            item = i.next().getValue();
            Conversation conv = item.getData();
            if (mLastLeaveBehind == -1 || conv.id != mLastLeaveBehind) {
                item.cancelFadeInTextAnimation();
                item.makeInert();
            }
        }
        startDismissCounter();
    }

    protected void startFadeOutLeaveBehindItemsAnimations() {
        final int startPosition = mListView.getFirstVisiblePosition();
        final int endPosition = mListView.getLastVisiblePosition();

        if (hasLeaveBehinds()) {
            // If the item is visible, fade it out. Otherwise, just remove
            // it.
            Iterator<Entry<Long, LeaveBehindItem>> i = mLeaveBehindItems.entrySet().iterator();
            LeaveBehindItem item;
            while (i.hasNext()) {
                item = i.next().getValue();
                Conversation conv = item.getData();
                if (mLastLeaveBehind == -1 || conv.id != mLastLeaveBehind) {
                    if (conv.position >= startPosition && conv.position <= endPosition) {
                        mFadeLeaveBehindItems.put(conv.id, item);
                    } else {
                        item.commit();
                    }
                    i.remove();
                }
            }
            cancelLeaveBehindFadeInAnimation();
        }
        if (!mLastDeletingItems.isEmpty()) {
            mLastDeletingItems.clear();
        }
        notifyDataSetChanged();
    }

    private void cancelLeaveBehindFadeInAnimation() {
        LeaveBehindItem leaveBehind = getLastLeaveBehindItem();
        if (leaveBehind != null) {
            leaveBehind.cancelFadeInTextAnimation();
        }
    }

    public CoordinatesCache getCoordinatesCache() {
        return mCoordinatesCache;
    }

    public SwipeableListView getListView() {
        return mListView;
    }

    public void commitLeaveBehindItems(boolean animate) {
        // Remove any previously existing leave behinds.
        boolean changed = false;
        if (hasLeaveBehinds()) {
            for (LeaveBehindItem item : mLeaveBehindItems.values()) {
                if (animate) {
                    mFadeLeaveBehindItems.put(item.getConversationId(), item);
                } else {
                    item.commit();
                }
            }
            changed = true;
            mLastLeaveBehind = -1;
            mLeaveBehindItems.clear();
        }
        if (hasFadeLeaveBehinds() && !animate) {
            // Find any fading leave behind items and commit them all, too.
            for (LeaveBehindItem item : mFadeLeaveBehindItems.values()) {
                item.commit();
            }
            mFadeLeaveBehindItems.clear();
            changed = true;
        }
        if (!mLastDeletingItems.isEmpty()) {
            mLastDeletingItems.clear();
            changed = true;
        }
        if (changed) {
            notifyDataSetChanged();
        }
    }

    private LeaveBehindItem getLeaveBehindItem(Conversation target) {
        return mLeaveBehindItems.get(target.id);
    }

    private LeaveBehindItem getFadeLeaveBehindItem(int position, Conversation target) {
        return mFadeLeaveBehindItems.get(target.id);
    }

    @Override
    public long getItemId(int position) {
        if (mShowFooter && position == getCount() - 1) {
            return -1;
        }

        final ConversationSpecialItemView specialView = mSpecialViews.get(position);
        if (specialView != null) {
            // TODO(skennedy) We probably want something better than this
            return specialView.hashCode();
        }

        final int cursorPos = position - getPositionOffset(position);
        // advance the cursor to the right position and read the cached conversation, if present
        //
        // (no need to have CursorAdapter check mDataValid because in our incarnation without
        // FLAG_REGISTER_CONTENT_OBSERVER, mDataValid is effectively identical to mCursor being
        // non-null)
        final ConversationCursor cursor = getConversationCursor();
        if (cursor != null && cursor.moveToPosition(cursorPos)) {
            final Conversation conv = cursor.getCachedConversation();
            if (conv != null) {
                return conv.id;
            }
        }
        return super.getItemId(cursorPos);
    }

    /**
     * @param position The position in the cursor
     */
    private View getDeletingView(int position, Conversation conversation, ViewGroup parent,
            boolean swipe) {
        conversation.position = position;
        SwipeableConversationItemView deletingView = mAnimatingViews.get(conversation.id);
        if (deletingView == null) {
            // The undo animation consists of fading in the conversation that
            // had been destroyed.
            deletingView = newConversationItemView(position, parent, conversation);
            deletingView.startDeleteAnimation(mAnimatorListener, swipe);
        }
        return deletingView;
    }

    /**
     * @param position The position in the cursor
     */
    private View getUndoingView(int position, Conversation conv, ViewGroup parent, boolean swipe) {
        conv.position = position;
        SwipeableConversationItemView undoView = mAnimatingViews.get(conv.id);
        if (undoView == null) {
            // The undo animation consists of fading in the conversation that
            // had been destroyed.
            undoView = newConversationItemView(position, parent, conv);
            undoView.startUndoAnimation(mAnimatorListener, swipe);
        }
        return undoView;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new SwipeableConversationItemView(context, mAccount.name);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // no-op. we only get here from newConversationItemView(), which will immediately bind
        // on its own.
    }

    private SwipeableConversationItemView newConversationItemView(int position, ViewGroup parent,
            Conversation conversation) {
        SwipeableConversationItemView view = (SwipeableConversationItemView) super.getView(
                position, null, parent);
        view.reset();
        view.bind(conversation, mActivity, mConversationListListener, mBatchConversations, mFolder,
                getCheckboxSetting(), getAttachmentPreviewsSetting(),
                getParallaxSpeedAlternativeSetting(), getParallaxDirectionAlternativeSetting(),
                mSwipeEnabled, mPriorityMarkersEnabled, this);
        mAnimatingViews.put(conversation.id, view);
        return view;
    }

    private int getCheckboxSetting() {
        return mAccount != null ? mAccount.settings.convListIcon :
            ConversationListIcon.DEFAULT;
    }

    private boolean getAttachmentPreviewsSetting() {
        return mAccount == null || mAccount.settings.convListAttachmentPreviews;
    }

    private boolean getParallaxSpeedAlternativeSetting() {
        return MailPrefs.get(mContext).getParallaxSpeedAlternative();
    }

    private boolean getParallaxDirectionAlternativeSetting() {
        return MailPrefs.get(mContext).getParallaxDirectionAlternative();
    }

    @Override
    public Object getItem(int position) {
        if (mShowFooter && position == getCount() - 1) {
            return mFooter;
        } else if (mSpecialViews.get(position) != null) {
            return mSpecialViews.get(position);
        }
        return super.getItem(position - getPositionOffset(position));
    }

    private boolean isPositionDeleting(long id) {
        return mDeletingItems.contains(id);
    }

    private boolean isPositionSwipeDeleting(long id) {
        return mSwipeDeletingItems.contains(id);
    }

    private boolean isPositionUndoing(long id) {
        return mUndoingItems.contains(id);
    }

    private boolean isPositionUndoingSwipe(long id) {
        return mSwipeUndoingItems.contains(id);
    }

    private boolean isPositionLeaveBehind(Conversation conv) {
        return hasLeaveBehinds()
                && mLeaveBehindItems.containsKey(conv.id)
                && conv.isMostlyDead();
    }

    private boolean isPositionFadeLeaveBehind(Conversation conv) {
        return hasFadeLeaveBehinds()
                && mFadeLeaveBehindItems.containsKey(conv.id)
                && conv.isMostlyDead();
    }

    /**
     * Performs the pending destruction, if any and assigns the next pending action.
     * @param next The next action that is to be performed, possibly null (if no next action is
     * needed).
     */
    private void performAndSetNextAction(ListItemsRemovedListener next) {
        if (mPendingDestruction != null) {
            mPendingDestruction.onListItemsRemoved();
        }
        mPendingDestruction = next;
    }

    private void updateAnimatingConversationItems(Object obj, HashSet<Long> items) {
        if (!items.isEmpty()) {
            if (obj instanceof ConversationItemView) {
                final ConversationItemView target = (ConversationItemView) obj;
                final long id = target.getConversation().id;
                items.remove(id);
                mAnimatingViews.remove(id);
                if (items.isEmpty()) {
                    performAndSetNextAction(null);
                    notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        // The animating items and some special views are not enabled.
        return false;
    }

    @Override
    public boolean isEnabled(final int position) {
        final ConversationSpecialItemView view = mSpecialViews.get(position);
        if (view != null) {
            final boolean enabled = view.acceptsUserTaps();
            LogUtils.d(LOG_TAG, "AA.isEnabled(%d) = %b", position, enabled);
            return enabled;
        }
        return !isPositionDeleting(position) && !isPositionUndoing(position);
    }

    public void setFooterVisibility(boolean show) {
        if (mShowFooter != show) {
            mShowFooter = show;
            notifyDataSetChanged();
        }
    }

    public void addFooter(View footerView) {
        mFooter = footerView;
    }

    public void setFolder(Folder folder) {
        mFolder = folder;
    }

    public void clearLeaveBehind(long itemId) {
        if (hasLeaveBehinds() && mLeaveBehindItems.containsKey(itemId)) {
            mLeaveBehindItems.remove(itemId);
        } else if (hasFadeLeaveBehinds()) {
            mFadeLeaveBehindItems.remove(itemId);
        } else {
            LogUtils.d(LOG_TAG, "Trying to clear a non-existant leave behind");
        }
        if (mLastLeaveBehind == itemId) {
            mLastLeaveBehind = -1;
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        long[] lastDeleting = new long[mLastDeletingItems.size()];
        for (int i = 0; i < lastDeleting.length; i++) {
            lastDeleting[i] = mLastDeletingItems.get(i);
        }
        outState.putLongArray(LAST_DELETING_ITEMS, lastDeleting);
        if (hasLeaveBehinds()) {
            if (mLastLeaveBehind != -1) {
                outState.putParcelable(LEAVE_BEHIND_ITEM_DATA,
                        mLeaveBehindItems.get(mLastLeaveBehind).getLeaveBehindData());
                outState.putLong(LEAVE_BEHIND_ITEM_ID, mLastLeaveBehind);
            }
            for (LeaveBehindItem item : mLeaveBehindItems.values()) {
                if (mLastLeaveBehind == -1 || item.getData().id != mLastLeaveBehind) {
                    item.commit();
                }
            }
        }
    }

    public void onRestoreInstanceState(Bundle outState) {
        if (outState.containsKey(LAST_DELETING_ITEMS)) {
            final long[] lastDeleting = outState.getLongArray(LAST_DELETING_ITEMS);
            for (final long aLastDeleting : lastDeleting) {
                mLastDeletingItems.add(aLastDeleting);
            }
        }
        if (outState.containsKey(LEAVE_BEHIND_ITEM_DATA)) {
            LeaveBehindData left =
                    (LeaveBehindData) outState.getParcelable(LEAVE_BEHIND_ITEM_DATA);
            mLeaveBehindItems.put(outState.getLong(LEAVE_BEHIND_ITEM_ID),
                    setupLeaveBehind(left.data, left.op, left.data.position, left.height));
        }
    }

    /**
     * Return if the adapter is in the process of animating anything.
     */
    public boolean isAnimating() {
        return !mUndoingItems.isEmpty()
                || !mSwipeUndoingItems.isEmpty()
                || hasFadeLeaveBehinds()
                || !mDeletingItems.isEmpty()
                || !mSwipeDeletingItems.isEmpty();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append(super.toString());
        sb.append(" mUndoingItems=");
        sb.append(mUndoingItems);
        sb.append(" mSwipeUndoingItems=");
        sb.append(mSwipeUndoingItems);
        sb.append(" mDeletingItems=");
        sb.append(mDeletingItems);
        sb.append(" mSwipeDeletingItems=");
        sb.append(mSwipeDeletingItems);
        sb.append(" mLeaveBehindItems=");
        sb.append(mLeaveBehindItems);
        sb.append(" mFadeLeaveBehindItems=");
        sb.append(mFadeLeaveBehindItems);
        sb.append(" mLastDeletingItems=");
        sb.append(mLastDeletingItems);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Get the ConversationCursor associated with this adapter.
     */
    public ConversationCursor getConversationCursor() {
        return (ConversationCursor) getCursor();
    }

    /**
     * Get the currently visible leave behind item.
     */
    public LeaveBehindItem getLastLeaveBehindItem() {
        if (mLastLeaveBehind != -1) {
            return mLeaveBehindItems.get(mLastLeaveBehind);
        }
        return null;
    }

    /**
     * Cancel fading out the text displayed in the leave behind item currently
     * shown.
     */
    public void cancelFadeOutLastLeaveBehindItemText() {
        LeaveBehindItem item = getLastLeaveBehindItem();
        if (item != null) {
            item.cancelFadeOutText();
        }
    }

    /**
     * Updates special (non-conversation view) when either {@link #mFolderViews} or
     * {@link #mFleetingViews} changed
     */
    private void updateSpecialViews() {
        // We recreate all the special views using mFolderViews and mFleetingViews (in that order).
        mSpecialViews.clear();

        // Fleeting (temporary) views go after this. They specify a position,which is 0-indexed and
        // has to be adjusted for the number of folders above it.
        for (final ConversationSpecialItemView specialView : mFleetingViews) {
            specialView.onUpdate(mFolder, getConversationCursor());

            if (specialView.getShouldDisplayInList()) {
                // If the special view asks for position 0, it wants to be at the top.
                int position = (specialView.getPosition());

                // insert the special view into the position, but if there is
                // already an item occupying that position, move that item back
                // one position, and repeat
                ConversationSpecialItemView insert = specialView;
                while (insert != null) {
                    final ConversationSpecialItemView kickedOut = mSpecialViews.get(position);
                    mSpecialViews.put(position, insert);
                    insert = kickedOut;
                    position++;
                }
            }
        }
    }

    /**
     * Gets the position of the specified {@link ConversationSpecialItemView}, as determined by
     * the adapter.
     *
     * @return The position in the list, or a negative value if it could not be found
     */
    public int getSpecialViewPosition(final ConversationSpecialItemView view) {
        return mSpecialViews.indexOfValue(view);
    }

    @Override
    public void notifyDataSetChanged() {
        // This may be a temporary catch for a problem, or we may leave it here.
        // b/9527863
        if (Looper.getMainLooper() != Looper.myLooper()) {
            LogUtils.wtf(LOG_TAG, "notifyDataSetChanged() called off the main thread");
        }

        updateSpecialViews();
        super.notifyDataSetChanged();
    }

    @Override
    public void changeCursor(final Cursor cursor) {
        super.changeCursor(cursor);
        updateSpecialViews();
    }

    @Override
    public void changeCursorAndColumns(final Cursor c, final String[] from, final int[] to) {
        super.changeCursorAndColumns(c, from, to);
        updateSpecialViews();
    }

    @Override
    public Cursor swapCursor(final Cursor c) {
        final Cursor oldCursor = super.swapCursor(c);
        updateSpecialViews();

        return oldCursor;
    }

    public BitmapCache getBitmapCache() {
        return mBitmapCache;
    }

    public DecodeAggregator getDecodeAggregator() {
        return mDecodeAggregator;
    }

    /**
     * Gets the offset for the given position in the underlying cursor, based on any special views
     * that may be above it.
     */
    public int getPositionOffset(final int position) {
        int viewsAbove = 0;

        for (int i = 0, size = mSpecialViews.size(); i < size; i++) {
            final int bidPosition = mSpecialViews.keyAt(i);
            // If the view bid for a position above the cursor position,
            // it is above the conversation.
            if (bidPosition <= position) {
                viewsAbove++;
            }
        }

        return viewsAbove;
    }

    public void cleanup() {
        // Only clean up teaser views. Folder views don't care about clean up.
        for (final ConversationSpecialItemView view : mFleetingViews) {
            view.cleanup();
        }
    }

    public void onConversationSelected() {
        // Only notify teaser views. Folder views don't care about selected conversations.
        for (final ConversationSpecialItemView specialView : mFleetingViews) {
            specialView.onConversationSelected();
        }
    }

    public void onCabModeEntered() {
        for (final ConversationSpecialItemView specialView : mFleetingViews) {
            specialView.onCabModeEntered();
        }
    }

    public void onCabModeExited() {
        for (final ConversationSpecialItemView specialView : mFleetingViews) {
            specialView.onCabModeExited();
        }
    }

    public void onConversationListVisibilityChanged(final boolean visible) {
        for (final ConversationSpecialItemView specialView : mFleetingViews) {
            specialView.onConversationListVisibilityChanged(visible);
        }
    }

    public void onScrollStateChanged(final int scrollState) {
        final boolean scrolling = scrollState != OnScrollListener.SCROLL_STATE_IDLE;
        mBitmapCache.setBlocking(scrolling);
    }

    public int getViewMode() {
        return mActivity.getViewMode().getMode();
    }

    public boolean isInCabMode() {
        // If we have conversation in our selected set, we're in CAB mode
        return !mBatchConversations.isEmpty();
    }

    public void saveSpecialItemInstanceState(final Bundle outState) {
        for (final ConversationSpecialItemView specialView : mFleetingViews) {
            specialView.saveInstanceState(outState);
        }
    }
}
