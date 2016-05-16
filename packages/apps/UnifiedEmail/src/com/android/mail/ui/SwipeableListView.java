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

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;

import com.android.mail.R;
import com.android.mail.analytics.Analytics;
import com.android.mail.analytics.AnalyticsUtils;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.browse.ConversationItemView;
import com.android.mail.browse.SwipeableConversationItemView;
import com.android.mail.providers.Account;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.providers.FolderList;
import com.android.mail.ui.SwipeHelper.Callback;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class SwipeableListView extends ListView implements Callback, OnScrollListener {
    private final SwipeHelper mSwipeHelper;
    private boolean mEnableSwipe = false;

    public static final String LOG_TAG = LogTag.getLogTag();
    /**
     * Set to false to prevent the FLING scroll state from pausing the photo manager loaders.
     */
    private final static boolean SCROLL_PAUSE_ENABLE = true;

    /**
     * Set to true to enable parallax effect for attachment previews as the scroll position varies.
     * This effect triggers invalidations on scroll (!) and requires more memory for attachment
     * preview bitmaps.
     */
    public static final boolean ENABLE_ATTACHMENT_PARALLAX = true;

    /**
     * Set to true to queue finished decodes in an aggregator so that we display decoded attachment
     * previews in an ordered fashion. This artificially delays updating the UI with decoded images,
     * since they may have to wait on another image to finish decoding first.
     */
    public static final boolean ENABLE_ATTACHMENT_DECODE_AGGREGATOR = true;

    /**
     * The amount of extra vertical space to decode in attachment previews so we have image data to
     * pan within. 1.0 implies no parallax effect.
     */
    public static final float ATTACHMENT_PARALLAX_MULTIPLIER_NORMAL = 1.5f;
    public static final float ATTACHMENT_PARALLAX_MULTIPLIER_ALTERNATIVE = 2.0f;

    private ConversationSelectionSet mConvSelectionSet;
    private int mSwipeAction;
    private Account mAccount;
    private Folder mFolder;
    private ListItemSwipedListener mSwipedListener;
    private boolean mScrolling;

    private SwipeListener mSwipeListener;

    // Instantiated through view inflation
    @SuppressWarnings("unused")
    public SwipeableListView(Context context) {
        this(context, null);
    }

    public SwipeableListView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SwipeableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnScrollListener(this);
        float densityScale = getResources().getDisplayMetrics().density;
        float pagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
        mSwipeHelper = new SwipeHelper(context, SwipeHelper.X, this, densityScale,
                pagingTouchSlop);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        float densityScale = getResources().getDisplayMetrics().density;
        mSwipeHelper.setDensityScale(densityScale);
        float pagingTouchSlop = ViewConfiguration.get(getContext()).getScaledPagingTouchSlop();
        mSwipeHelper.setPagingTouchSlop(pagingTouchSlop);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        LogUtils.d(Utils.VIEW_DEBUGGING_TAG,
                "START CLF-ListView.onFocusChanged layoutRequested=%s root.layoutRequested=%s",
                isLayoutRequested(), getRootView().isLayoutRequested());
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        LogUtils.d(Utils.VIEW_DEBUGGING_TAG, new Error(),
                "FINISH CLF-ListView.onFocusChanged layoutRequested=%s root.layoutRequested=%s",
                isLayoutRequested(), getRootView().isLayoutRequested());
    }

    /**
     * Enable swipe gestures.
     */
    public void enableSwipe(boolean enable) {
        mEnableSwipe = enable;
    }

    public void setSwipeAction(int action) {
        mSwipeAction = action;
    }

    public void setSwipedListener(ListItemSwipedListener listener) {
        mSwipedListener = listener;
    }

    public int getSwipeAction() {
        return mSwipeAction;
    }

    public void setSelectionSet(ConversationSelectionSet set) {
        mConvSelectionSet = set;
    }

    public void setCurrentAccount(Account account) {
        mAccount = account;
    }

    public void setCurrentFolder(Folder folder) {
        mFolder = folder;
    }

    @Override
    public ConversationSelectionSet getSelectionSet() {
        return mConvSelectionSet;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mScrolling || !mEnableSwipe) {
            return super.onInterceptTouchEvent(ev);
        } else {
            return mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mEnableSwipe) {
            return mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
        } else {
            return super.onTouchEvent(ev);
        }
    }

    @Override
    public View getChildAtPosition(MotionEvent ev) {
        // find the view under the pointer, accounting for GONE views
        final int count = getChildCount();
        final int touchY = (int) ev.getY();
        int childIdx = 0;
        View slidingChild;
        for (; childIdx < count; childIdx++) {
            slidingChild = getChildAt(childIdx);
            if (slidingChild.getVisibility() == GONE) {
                continue;
            }
            if (touchY >= slidingChild.getTop() && touchY <= slidingChild.getBottom()) {
                if (slidingChild instanceof SwipeableConversationItemView) {
                    return ((SwipeableConversationItemView) slidingChild).getSwipeableItemView();
                }
                return slidingChild;
            }
        }
        return null;
    }

    @Override
    public boolean canChildBeDismissed(SwipeableItemView v) {
        return v.canChildBeDismissed();
    }

    @Override
    public void onChildDismissed(SwipeableItemView v) {
        if (v != null) {
            v.dismiss();
        }
    }

    // Call this whenever a new action is taken; this forces a commit of any
    // existing destructive actions.
    public void commitDestructiveActions(boolean animate) {
        final AnimatedAdapter adapter = getAnimatedAdapter();
        if (adapter != null) {
            adapter.commitLeaveBehindItems(animate);
        }
    }

    public void dismissChild(final ConversationItemView target) {
        final ToastBarOperation undoOp;

        undoOp = new ToastBarOperation(1, mSwipeAction, ToastBarOperation.UNDO, false /* batch */,
                mFolder);
        Conversation conv = target.getConversation();
        target.getConversation().position = findConversation(target, conv);
        final AnimatedAdapter adapter = getAnimatedAdapter();
        if (adapter == null) {
            return;
        }
        adapter.setupLeaveBehind(conv, undoOp, conv.position, target.getHeight());
        ConversationCursor cc = (ConversationCursor) adapter.getCursor();
        Collection<Conversation> convList = Conversation.listOf(conv);
        ArrayList<Uri> folderUris;
        ArrayList<Boolean> adds;

        Analytics.getInstance().sendMenuItemEvent("list_swipe", mSwipeAction, null, 0);

        if (mSwipeAction == R.id.remove_folder) {
            FolderOperation folderOp = new FolderOperation(mFolder, false);
            HashMap<Uri, Folder> targetFolders = Folder
                    .hashMapForFolders(conv.getRawFolders());
            targetFolders.remove(folderOp.mFolder.folderUri.fullUri);
            final FolderList folders = FolderList.copyOf(targetFolders.values());
            conv.setRawFolders(folders);
            final ContentValues values = new ContentValues();
            folderUris = new ArrayList<Uri>();
            folderUris.add(mFolder.folderUri.fullUri);
            adds = new ArrayList<Boolean>();
            adds.add(Boolean.FALSE);
            ConversationCursor.addFolderUpdates(folderUris, adds, values);
            ConversationCursor.addTargetFolders(targetFolders.values(), values);
            cc.mostlyDestructiveUpdate(Conversation.listOf(conv), values);
        } else if (mSwipeAction == R.id.archive) {
            cc.mostlyArchive(convList);
        } else if (mSwipeAction == R.id.delete) {
            cc.mostlyDelete(convList);
        }
        if (mSwipedListener != null) {
            mSwipedListener.onListItemSwiped(convList);
        }
        adapter.notifyDataSetChanged();
        if (mConvSelectionSet != null && !mConvSelectionSet.isEmpty()
                && mConvSelectionSet.contains(conv)) {
            mConvSelectionSet.toggle(conv);
            // Don't commit destructive actions if the item we just removed from
            // the selection set is the item we just destroyed!
            if (!conv.isMostlyDead() && mConvSelectionSet.isEmpty()) {
                commitDestructiveActions(true);
            }
        }
    }

    @Override
    public void onBeginDrag(View v) {
        // We do this so the underlying ScrollView knows that it won't get
        // the chance to intercept events anymore
        requestDisallowInterceptTouchEvent(true);
        cancelDismissCounter();

        // Notifies {@link ConversationListView} to disable pull to refresh since once
        // an item in the list view has been picked up, we don't want any vertical movement
        // to also trigger refresh.
        if (mSwipeListener != null) {
            mSwipeListener.onBeginSwipe();
        }
    }

    @Override
    public void onDragCancelled(SwipeableItemView v) {
        final AnimatedAdapter adapter = getAnimatedAdapter();
        if (adapter != null) {
            adapter.startDismissCounter();
            adapter.cancelFadeOutLastLeaveBehindItemText();
        }
    }

    /**
     * Archive items using the swipe away animation before shrinking them away.
     */
    public boolean destroyItems(Collection<Conversation> convs,
            final ListItemsRemovedListener listener) {
        if (convs == null) {
            LogUtils.e(LOG_TAG, "SwipeableListView.destroyItems: null conversations.");
            return false;
        }
        final AnimatedAdapter adapter = getAnimatedAdapter();
        if (adapter == null) {
            LogUtils.e(LOG_TAG, "SwipeableListView.destroyItems: Cannot destroy: adapter is null.");
            return false;
        }
        adapter.swipeDelete(convs, listener);
        return true;
    }

    public int findConversation(ConversationItemView view, Conversation conv) {
        int position = INVALID_POSITION;
        long convId = conv.id;
        try {
            position = getPositionForView(view);
        } catch (Exception e) {
            position = INVALID_POSITION;
            LogUtils.w(LOG_TAG, e, "Exception finding position; using alternate strategy");
        }
        if (position == INVALID_POSITION) {
            // Try the other way!
            Conversation foundConv;
            long foundId;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child instanceof SwipeableConversationItemView) {
                    foundConv = ((SwipeableConversationItemView) child).getSwipeableItemView()
                            .getConversation();
                    foundId = foundConv.id;
                    if (foundId == convId) {
                        position = i + getFirstVisiblePosition();
                        break;
                    }
                }
            }
        }
        return position;
    }

    private AnimatedAdapter getAnimatedAdapter() {
        return (AnimatedAdapter) getAdapter();
    }

    @Override
    public boolean performItemClick(View view, int pos, long id) {
        final int previousPosition = getCheckedItemPosition();
        final boolean selectionSetEmpty = mConvSelectionSet.isEmpty();

        // Superclass method modifies the selection set
        final boolean handled = super.performItemClick(view, pos, id);

        // If we are in CAB mode then a click shouldn't
        // activate the new item, it should only add it to the selection set
        if (!selectionSetEmpty && previousPosition != -1) {
            setItemChecked(previousPosition, true);
        }
        // Commit any existing destructive actions when the user selects a
        // conversation to view.
        commitDestructiveActions(true);
        return handled;
    }

    @Override
    public void onScroll() {
        commitDestructiveActions(true);
    }

    public interface ListItemsRemovedListener {
        public void onListItemsRemoved();
    }

    public interface ListItemSwipedListener {
        public void onListItemSwiped(Collection<Conversation> conversations);
    }

    @Override
    public final void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (ENABLE_ATTACHMENT_PARALLAX) {
            for (int i = 0, len = getChildCount(); i < len; i++) {
                final View child = getChildAt(i);
                if (child instanceof OnScrollListener) {
                    ((OnScrollListener) child).onScroll(view, firstVisibleItem, visibleItemCount,
                            totalItemCount);
                }
            }
        }
    }

    @Override
    public void onScrollStateChanged(final AbsListView view, final int scrollState) {
        mScrolling = scrollState != OnScrollListener.SCROLL_STATE_IDLE;

        if (!mScrolling) {
            final Context c = getContext();
            if (c instanceof ControllableActivity) {
                final ControllableActivity activity = (ControllableActivity) c;
                activity.onAnimationEnd(null /* adapter */);
            } else {
                LogUtils.wtf(LOG_TAG, "unexpected context=%s", c);
            }
        }

        if (SCROLL_PAUSE_ENABLE) {
            AnimatedAdapter adapter = getAnimatedAdapter();
            if (adapter != null) {
                adapter.onScrollStateChanged(scrollState);
            }
            ConversationItemView.setScrollStateChanged(scrollState);
        }
    }

    public boolean isScrolling() {
        return mScrolling;
    }

    @Override
    public void cancelDismissCounter() {
        AnimatedAdapter adapter = getAnimatedAdapter();
        if (adapter != null) {
            adapter.cancelDismissCounter();
        }
    }

    @Override
    public LeaveBehindItem getLastSwipedItem() {
        AnimatedAdapter adapter = getAnimatedAdapter();
        if (adapter != null) {
            return adapter.getLastLeaveBehindItem();
        }
        return null;
    }

    public void setSwipeListener(SwipeListener swipeListener) {
        mSwipeListener = swipeListener;
    }

    public interface SwipeListener {
        public void onBeginSwipe();
    }
}
