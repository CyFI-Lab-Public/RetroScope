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
package com.android.dialer.list;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactTileLoaderFactory;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.list.ContactEntry;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.ContactTileView;
import com.android.dialer.DialtactsActivity;
import com.android.dialer.R;
import com.android.dialer.calllog.CallLogQuery;
import com.android.dialer.calllog.ContactInfoHelper;
import com.android.dialer.calllog.CallLogAdapter;
import com.android.dialer.calllog.CallLogQueryHandler;
import com.android.dialer.list.PhoneFavoritesTileAdapter.ContactTileRow;
import com.android.dialerbind.ObjectFactory;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Fragment for Phone UI's favorite screen.
 *
 * This fragment contains three kinds of contacts in one screen: "starred", "frequent", and "all"
 * contacts. To show them at once, this merges results from {@link com.android.contacts.common.list.ContactTileAdapter} and
 * {@link com.android.contacts.common.list.PhoneNumberListAdapter} into one unified list using {@link PhoneFavoriteMergedAdapter}.
 * A contact filter header is also inserted between those adapters' results.
 */
public class PhoneFavoriteFragment extends Fragment implements OnItemClickListener,
        CallLogQueryHandler.Listener, CallLogAdapter.CallFetcher,
        PhoneFavoritesTileAdapter.OnDataSetChangedForAnimationListener {

    /**
     * By default, the animation code assumes that all items in a list view are of the same height
     * when animating new list items into view (e.g. from the bottom of the screen into view).
     * This can cause incorrect translation offsets when a item that is larger or smaller than
     * other list item is removed from the list. This key is used to provide the actual height
     * of the removed object so that the actual translation appears correct to the user.
     */
    private static final long KEY_REMOVED_ITEM_HEIGHT = Long.MAX_VALUE;

    private static final String TAG = PhoneFavoriteFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private int mAnimationDuration;

    /**
     * Used with LoaderManager.
     */
    private static int LOADER_ID_CONTACT_TILE = 1;
    private static int MISSED_CALL_LOADER = 2;

    private static final String KEY_LAST_DISMISSED_CALL_SHORTCUT_DATE =
            "key_last_dismissed_call_shortcut_date";

    public interface OnShowAllContactsListener {
        public void onShowAllContacts();
    }

    public interface Listener {
        public void onContactSelected(Uri contactUri);
        public void onCallNumberDirectly(String phoneNumber);
    }

    private class MissedCallLogLoaderListener implements LoaderManager.LoaderCallbacks<Cursor> {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            final Uri uri = CallLog.Calls.CONTENT_URI;
            final String[] projection = new String[] {CallLog.Calls.TYPE};
            final String selection = CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE +
                    " AND " + CallLog.Calls.IS_READ + " = 0";
            return new CursorLoader(getActivity(), uri, projection, selection, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
            mCallLogAdapter.setMissedCalls(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
    }

    private class ContactTileLoaderListener implements LoaderManager.LoaderCallbacks<Cursor> {
        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            if (DEBUG) Log.d(TAG, "ContactTileLoaderListener#onCreateLoader.");
            return ContactTileLoaderFactory.createStrequentPhoneOnlyLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (DEBUG) Log.d(TAG, "ContactTileLoaderListener#onLoadFinished");
            mContactTileAdapter.setContactCursor(data);
            setEmptyViewVisibility(mContactTileAdapter.getCount() == 0);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            if (DEBUG) Log.d(TAG, "ContactTileLoaderListener#onLoaderReset. ");
        }
    }

    private class ContactTileAdapterListener implements ContactTileView.Listener {
        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            if (mListener != null) {
                mListener.onContactSelected(contactUri);
            }
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            if (mListener != null) {
                mListener.onCallNumberDirectly(phoneNumber);
            }
        }

        @Override
        public int getApproximateTileWidth() {
            return getView().getWidth() / mContactTileAdapter.getColumnCount();
        }
    }

    private class ScrollListener implements ListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view,
                int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            mActivityScrollListener.onListFragmentScrollStateChange(scrollState);
        }
    }

    private Listener mListener;

    private OnListFragmentScrolledListener mActivityScrollListener;
    private OnShowAllContactsListener mShowAllContactsListener;
    private PhoneFavoriteMergedAdapter mAdapter;
    private PhoneFavoritesTileAdapter mContactTileAdapter;

    private CallLogAdapter mCallLogAdapter;
    private CallLogQueryHandler mCallLogQueryHandler;

    private View mParentView;

    private PhoneFavoriteListView mListView;

    private View mShowAllContactsButton;
    private View mShowAllContactsInEmptyViewButton;
    private View mContactTileFrame;

    private TileInteractionTeaserView mTileInteractionTeaserView;

    private final HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
    private final HashMap<Long, Integer> mItemIdLeftMap = new HashMap<Long, Integer>();

    /**
     * Layout used when there are no favorites.
     */
    private View mEmptyView;

    /**
     * Call shortcuts older than this date (persisted in shared preferences) will not show up in
     * at the top of the screen
     */
    private long mLastCallShortcutDate = 0;

    /**
     * The date of the current call shortcut that is showing on screen.
     */
    private long mCurrentCallShortcutDate = 0;

    private final ContactTileView.Listener mContactTileAdapterListener =
            new ContactTileAdapterListener();
    private final LoaderManager.LoaderCallbacks<Cursor> mContactTileLoaderListener =
            new ContactTileLoaderListener();
    private final ScrollListener mScrollListener = new ScrollListener();

    @Override
    public void onAttach(Activity activity) {
        if (DEBUG) Log.d(TAG, "onAttach()");
        super.onAttach(activity);

        // Construct two base adapters which will become part of PhoneFavoriteMergedAdapter.
        // We don't construct the resultant adapter at this moment since it requires LayoutInflater
        // that will be available on onCreateView().
        mContactTileAdapter = new PhoneFavoritesTileAdapter(activity, mContactTileAdapterListener,
                this,
                getResources().getInteger(R.integer.contact_tile_column_count_in_favorites_new),
                1);
        mContactTileAdapter.setPhotoLoader(ContactPhotoManager.getInstance(activity));
    }

    @Override
    public void onCreate(Bundle savedState) {
        if (DEBUG) Log.d(TAG, "onCreate()");
        super.onCreate(savedState);

        mAnimationDuration = getResources().getInteger(R.integer.fade_duration);
        mCallLogQueryHandler = new CallLogQueryHandler(getActivity().getContentResolver(),
                this, 1);
        final String currentCountryIso = GeoUtil.getCurrentCountryIso(getActivity());
        mCallLogAdapter = ObjectFactory.newCallLogAdapter(getActivity(), this,
                new ContactInfoHelper(getActivity(), currentCountryIso), true, false);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences prefs = getActivity().getSharedPreferences(
                DialtactsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);

        mLastCallShortcutDate = prefs.getLong(KEY_LAST_DISMISSED_CALL_SHORTCUT_DATE, 0);

        fetchCalls();
        mCallLogAdapter.setLoading(true);
        getLoaderManager().getLoader(LOADER_ID_CONTACT_TILE).forceLoad();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mParentView = inflater.inflate(R.layout.phone_favorites_fragment, container, false);

        mListView = (PhoneFavoriteListView) mParentView.findViewById(R.id.contact_tile_list);
        mListView.setItemsCanFocus(true);
        mListView.setOnItemClickListener(this);
        mListView.setVerticalScrollBarEnabled(false);
        mListView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
        mListView.setOnItemSwipeListener(mContactTileAdapter);
        mListView.setOnDragDropListener(mContactTileAdapter);

        final ImageView dragShadowOverlay =
                (ImageView) mParentView.findViewById(R.id.contact_tile_drag_shadow_overlay);
        mListView.setDragShadowOverlay(dragShadowOverlay);

        mEmptyView = mParentView.findViewById(R.id.phone_no_favorites_view);

        mShowAllContactsInEmptyViewButton = mParentView.findViewById(
                R.id.show_all_contact_button_in_nofav);
        prepareAllContactsButton(mShowAllContactsInEmptyViewButton);

        mShowAllContactsButton = inflater.inflate(R.layout.show_all_contact_button, mListView,
                false);

        prepareAllContactsButton(mShowAllContactsButton);

        mContactTileFrame = mParentView.findViewById(R.id.contact_tile_frame);

        mTileInteractionTeaserView = (TileInteractionTeaserView) inflater.inflate(
                R.layout.tile_interactions_teaser_view, mListView, false);

        mAdapter = new PhoneFavoriteMergedAdapter(getActivity(), this, mContactTileAdapter,
                mCallLogAdapter, mShowAllContactsButton, mTileInteractionTeaserView);

        mTileInteractionTeaserView.setAdapter(mAdapter);

        mListView.setAdapter(mAdapter);

        mListView.setOnScrollListener(mScrollListener);
        mListView.setFastScrollEnabled(false);
        mListView.setFastScrollAlwaysVisible(false);

        return mParentView;
    }

    public boolean hasFrequents() {
        if (mContactTileAdapter == null) return false;
        return mContactTileAdapter.getNumFrequents() > 0;
    }

    /* package */ void setEmptyViewVisibility(final boolean visible) {
        final int previousVisibility = mEmptyView.getVisibility();
        final int newVisibility = visible ? View.VISIBLE : View.GONE;

        if (previousVisibility != newVisibility) {
            final RelativeLayout.LayoutParams params = (LayoutParams) mContactTileFrame
                    .getLayoutParams();
            params.height = visible ? LayoutParams.WRAP_CONTENT : LayoutParams.MATCH_PARENT;
            mContactTileFrame.setLayoutParams(params);
            mEmptyView.setVisibility(newVisibility);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        final Activity activity = getActivity();

        try {
            mActivityScrollListener = (OnListFragmentScrolledListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnListFragmentScrolledListener");
        }

        try {
            mShowAllContactsListener = (OnShowAllContactsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnShowAllContactsListener");
        }

        // Use initLoader() instead of restartLoader() to refraining unnecessary reload.
        // This method call implicitly assures ContactTileLoaderListener's onLoadFinished() will
        // be called, on which we'll check if "all" contacts should be reloaded again or not.
        getLoaderManager().initLoader(LOADER_ID_CONTACT_TILE, null, mContactTileLoaderListener);
        getLoaderManager().initLoader(MISSED_CALL_LOADER, null, new MissedCallLogLoaderListener());
    }

    /**
     * {@inheritDoc}
     *
     * This is only effective for elements provided by {@link #mContactTileAdapter}.
     * {@link #mContactTileAdapter} has its own logic for click events.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final int contactTileAdapterCount = mContactTileAdapter.getCount();
        if (position <= contactTileAdapterCount) {
            Log.e(TAG, "onItemClick() event for unexpected position. "
                    + "The position " + position + " is before \"all\" section. Ignored.");
        }
    }

    /**
     * Gets called when user click on the show all contacts button.
     */
    private void showAllContacts() {
        mShowAllContactsListener.onShowAllContacts();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onVoicemailStatusFetched(Cursor statusCursor) {
        // no-op
    }

    @Override
    public void onCallsFetched(Cursor cursor) {
        animateListView();
        mCallLogAdapter.setLoading(false);

        // Save the date of the most recent call log item
        if (cursor != null && cursor.moveToFirst()) {
            mCurrentCallShortcutDate = cursor.getLong(CallLogQuery.DATE);
        }

        mCallLogAdapter.changeCursor(cursor);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void fetchCalls() {
        mCallLogQueryHandler.fetchCalls(CallLogQueryHandler.CALL_TYPE_ALL, mLastCallShortcutDate);
    }

    @Override
    public void onPause() {
        // If there are any pending contact entries that are to be removed, remove them
        mContactTileAdapter.removePendingContactEntry();
        // Wipe the cache to refresh the call shortcut item. This is not that expensive because
        // it only contains one item.
        mCallLogAdapter.invalidateCache();
        super.onPause();
    }

    /**
     * Saves the current view offsets into memory
     */
    @SuppressWarnings("unchecked")
    private void saveOffsets(int removedItemHeight) {
        final int firstVisiblePosition = mListView.getFirstVisiblePosition();
        if (DEBUG) {
            Log.d(TAG, "Child count : " + mListView.getChildCount());
        }
        for (int i = 0; i < mListView.getChildCount(); i++) {
            final View child = mListView.getChildAt(i);
            final int position = firstVisiblePosition + i;
            final long itemId = mAdapter.getItemId(position);
            final int itemViewType = mAdapter.getItemViewType(position);
            if (itemViewType == PhoneFavoritesTileAdapter.ViewTypes.TOP) {
                // This is a tiled row, so save horizontal offsets instead
                saveHorizontalOffsets((ContactTileRow) child, (ArrayList<ContactEntry>)
                        mAdapter.getItem(position));
            }
            if (DEBUG) {
                Log.d(TAG, "Saving itemId: " + itemId + " for listview child " + i + " Top: "
                        + child.getTop());
            }
            mItemIdTopMap.put(itemId, child.getTop());
        }

        mItemIdTopMap.put(KEY_REMOVED_ITEM_HEIGHT, removedItemHeight);
    }

    private void saveHorizontalOffsets(ContactTileRow row, ArrayList<ContactEntry> list) {
        for (int i = 0; i < list.size(); i++) {
            final View child = row.getChildAt(i);
            final ContactEntry entry = list.get(i);
            final long itemId = mContactTileAdapter.getAdjustedItemId(entry.id);
            if (DEBUG) {
                Log.d(TAG, "Saving itemId: " + itemId + " for tileview child " + i + " Left: "
                        + child.getTop());
            }
            mItemIdLeftMap.put(itemId, child.getLeft());
        }
    }

    /*
     * Performs a animations for a row of tiles
     */
    private void performHorizontalAnimations(ContactTileRow row, ArrayList<ContactEntry> list,
            long[] idsInPlace) {
        if (mItemIdLeftMap.isEmpty()) {
            return;
        }
        final AnimatorSet animSet = new AnimatorSet();
        final ArrayList<Animator> animators = new ArrayList<Animator>();
        for (int i = 0; i < list.size(); i++) {
            final View child = row.getChildAt(i);
            final ContactEntry entry = list.get(i);
            final long itemId = mContactTileAdapter.getAdjustedItemId(entry.id);

            if (containsId(idsInPlace, itemId)) {
                animators.add(ObjectAnimator.ofFloat(
                        child, "alpha", 0.0f, 1.0f));
                break;
            } else {
                Integer startLeft = mItemIdLeftMap.get(itemId);
                int left = child.getLeft();
                if (startLeft != null) {
                    if (startLeft != left) {
                        int delta = startLeft - left;
                        if (DEBUG) {
                            Log.d(TAG, "Found itemId: " + itemId + " for tileview child " + i +
                                    " Left: " + left +
                                    " Delta: " + delta);
                        }
                        animators.add(ObjectAnimator.ofFloat(
                                child, "translationX", delta, 0.0f));
                    }
                } else {
                    // In case the last square row is pushed up from the non-square section.
                    animators.add(ObjectAnimator.ofFloat(
                            child, "translationX", left, 0.0f));
                }
            }
        }
        if (animators.size() > 0) {
            animSet.setDuration(mAnimationDuration).playTogether(animators);
            animSet.start();
        }
    }

    /*
     * Performs animations for the list view. If the list item is a row of tiles, horizontal
     * animations will be performed instead.
     */
    private void animateListView(final long... idsInPlace) {
        if (mItemIdTopMap.isEmpty()) {
            // Don't do animations if the database is being queried for the first time and
            // the previous item offsets have not been cached, or the user hasn't done anything
            // (dragging, swiping etc) that requires an animation.
            return;
        }

        final int removedItemHeight = mItemIdTopMap.get(KEY_REMOVED_ITEM_HEIGHT);

        final ViewTreeObserver observer = mListView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                final int firstVisiblePosition = mListView.getFirstVisiblePosition();
                final AnimatorSet animSet = new AnimatorSet();
                final ArrayList<Animator> animators = new ArrayList<Animator>();
                for (int i = 0; i < mListView.getChildCount(); i++) {
                    final View child = mListView.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    final int itemViewType = mAdapter.getItemViewType(position);
                    if (itemViewType == PhoneFavoritesTileAdapter.ViewTypes.TOP) {
                        // This is a tiled row, so perform horizontal animations instead
                        performHorizontalAnimations((ContactTileRow) child, (
                                ArrayList<ContactEntry>) mAdapter.getItem(position), idsInPlace);
                    }

                    final long itemId = mAdapter.getItemId(position);

                    if (containsId(idsInPlace, itemId)) {
                        animators.add(ObjectAnimator.ofFloat(
                                child, "alpha", 0.0f, 1.0f));
                        break;
                    } else {
                        Integer startTop = mItemIdTopMap.get(itemId);
                        final int top = child.getTop();
                        int delta = 0;
                        if (startTop != null) {
                            if (startTop != top) {
                                delta = startTop - top;
                            }
                        } else if (!mItemIdLeftMap.containsKey(itemId)) {
                            // Animate new views along with the others. The catch is that they did
                            // not exist in the start state, so we must calculate their starting
                            // position based on neighboring views.

                            final int itemHeight;
                            if (removedItemHeight == 0) {
                                itemHeight = child.getHeight() + mListView.getDividerHeight();
                            } else {
                                itemHeight = removedItemHeight;
                            }
                            startTop = top + (i > 0 ? itemHeight : -itemHeight);
                            delta = startTop - top;
                        } else {
                            // In case the first non-square row is pushed down
                            // from the square section.
                            animators.add(ObjectAnimator.ofFloat(
                                    child, "alpha", 0.0f, 1.0f));
                        }
                        if (DEBUG) {
                            Log.d(TAG, "Found itemId: " + itemId + " for listview child " + i +
                                    " Top: " + top +
                                    " Delta: " + delta);
                        }

                        if (delta != 0) {
                            animators.add(ObjectAnimator.ofFloat(
                                    child, "translationY", delta, 0.0f));
                        }
                    }
                }

                if (animators.size() > 0) {
                    animSet.setDuration(mAnimationDuration).playTogether(animators);
                    animSet.start();
                }

                mItemIdTopMap.clear();
                mItemIdLeftMap.clear();
                return true;
            }
        });
    }

    private boolean containsId(long[] ids, long target) {
        // Linear search on array is fine because this is typically only 0-1 elements long
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == target) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDataSetChangedForAnimation(long... idsInPlace) {
        animateListView(idsInPlace);
    }

    @Override
    public void cacheOffsetsForDatasetChange() {
        saveOffsets(0);
    }

    public void dismissShortcut(int height) {
        saveOffsets(height);
        mLastCallShortcutDate = mCurrentCallShortcutDate;
        final SharedPreferences prefs = getActivity().getSharedPreferences(
                DialtactsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_DISMISSED_CALL_SHORTCUT_DATE, mLastCallShortcutDate)
                .apply();
        fetchCalls();
    }

    /**
     * Returns a view that is laid out and styled to look like a regular contact, with the correct
     * click behavior (to launch the all contacts activity when it is clicked).
     */
    private View prepareAllContactsButton(View v) {
        final ContactListItemView view = (ContactListItemView) v;
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showAllContacts();
            }
        });

        view.setPhotoPosition(ContactListItemView.PhotoPosition.LEFT);
        final Resources resources = getResources();
        view.setBackgroundResource(R.drawable.contact_list_item_background);

        view.setPaddingRelative(
                resources.getDimensionPixelSize(R.dimen.favorites_row_start_padding),
                resources.getDimensionPixelSize(R.dimen.favorites_row_end_padding),
                resources.getDimensionPixelSize(R.dimen.favorites_row_top_padding),
                resources.getDimensionPixelSize(R.dimen.favorites_row_bottom_padding));

        view.setDisplayName(resources.getString(R.string.show_all_contacts_button_text));
        view.setDrawableResource(R.drawable.list_item_avatar_bg,
                R.drawable.ic_menu_all_contacts_dk);
        return view;
    }
}
