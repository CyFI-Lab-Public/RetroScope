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

import android.animation.ObjectAnimator;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PinnedPositions;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactTileLoaderFactory;
import com.android.contacts.common.R;
import com.android.contacts.common.list.ContactEntry;
import com.android.contacts.common.list.ContactTileAdapter.DisplayType;
import com.android.contacts.common.list.ContactTileView;
import com.android.dialer.list.SwipeHelper.OnItemGestureListener;
import com.android.dialer.list.SwipeHelper.SwipeHelperCallback;
import com.android.internal.annotations.VisibleForTesting;

import com.google.common.collect.ComparisonChain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Also allows for a configurable number of columns as well as a maximum row of tiled contacts.
 *
 * This adapter has been rewritten to only support a maximum of one row for favorites.
 *
 */
public class PhoneFavoritesTileAdapter extends BaseAdapter implements
        SwipeHelper.OnItemGestureListener, PhoneFavoriteListView.OnDragDropListener {
    private static final String TAG = PhoneFavoritesTileAdapter.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final int ROW_LIMIT_DEFAULT = 1;

    private ContactTileView.Listener mListener;
    private OnDataSetChangedForAnimationListener mDataSetChangedListener;

    private Context mContext;
    private Resources mResources;

    /** Contact data stored in cache. This is used to populate the associated view. */
    protected ArrayList<ContactEntry> mContactEntries = null;
    /** Back up of the temporarily removed Contact during dragging. */
    private ContactEntry mDraggedEntry = null;
    /** Position of the temporarily removed contact in the cache. */
    private int mDraggedEntryIndex = -1;
    /** New position of the temporarily removed contact in the cache. */
    private int mDropEntryIndex = -1;
    /** New position of the temporarily entered contact in the cache. */
    private int mDragEnteredEntryIndex = -1;
    /** Position of the contact pending removal. */
    private int mPotentialRemoveEntryIndex = -1;
    private long mIdToKeepInPlace = -1;

    private boolean mAwaitingRemove = false;

    private ContactPhotoManager mPhotoManager;
    protected int mNumFrequents;
    protected int mNumStarred;

    protected int mColumnCount;
    private int mMaxTiledRows = ROW_LIMIT_DEFAULT;
    private int mStarredIndex;

    protected int mIdIndex;
    protected int mLookupIndex;
    protected int mPhotoUriIndex;
    protected int mNameIndex;
    protected int mPresenceIndex;
    protected int mStatusIndex;

    private int mPhoneNumberIndex;
    private int mPhoneNumberTypeIndex;
    private int mPhoneNumberLabelIndex;
    private int mIsDefaultNumberIndex;
    protected int mPinnedIndex;
    protected int mContactIdIndex;

    private final int mPaddingInPixels;

    /** Indicates whether a drag is in process. */
    private boolean mInDragging = false;

    public static final int PIN_LIMIT = 20;

    /**
     * The soft limit on how many contact tiles to show.
     * NOTE This soft limit would not restrict the number of starred contacts to show, rather
     * 1. If the count of starred contacts is less than this limit, show 20 tiles total.
     * 2. If the count of starred contacts is more than or equal to this limit,
     * show all starred tiles and no frequents.
     */
    private static final int TILES_SOFT_LIMIT = 20;

    final Comparator<ContactEntry> mContactEntryComparator = new Comparator<ContactEntry>() {
        @Override
        public int compare(ContactEntry lhs, ContactEntry rhs) {
            return ComparisonChain.start()
                    .compare(lhs.pinned, rhs.pinned)
                    .compare(lhs.name, rhs.name)
                    .result();
        }
    };

    public interface OnDataSetChangedForAnimationListener {
        public void onDataSetChangedForAnimation(long... idsInPlace);
        public void cacheOffsetsForDatasetChange();
    };

    public PhoneFavoritesTileAdapter(Context context, ContactTileView.Listener listener,
            OnDataSetChangedForAnimationListener dataSetChangedListener,
            int numCols, int maxTiledRows) {
        mDataSetChangedListener = dataSetChangedListener;
        mListener = listener;
        mContext = context;
        mResources = context.getResources();
        mColumnCount = numCols;
        mNumFrequents = 0;
        mMaxTiledRows = maxTiledRows;
        mContactEntries = new ArrayList<ContactEntry>();
        // Converting padding in dips to padding in pixels
        mPaddingInPixels = mContext.getResources()
                .getDimensionPixelSize(R.dimen.contact_tile_divider_padding);

        bindColumnIndices();
    }

    public void setPhotoLoader(ContactPhotoManager photoLoader) {
        mPhotoManager = photoLoader;
    }

    public void setMaxRowCount(int maxRows) {
        mMaxTiledRows = maxRows;
    }

    public void setColumnCount(int columnCount) {
        mColumnCount = columnCount;
    }

    /**
     * Indicates whether a drag is in process.
     *
     * @param inDragging Boolean variable indicating whether there is a drag in process.
     */
    public void setInDragging(boolean inDragging) {
        mInDragging = inDragging;
    }

    /** Gets whether the drag is in process. */
    public boolean getInDragging() {
        return mInDragging;
    }

    /**
     * Sets the column indices for expected {@link Cursor}
     * based on {@link DisplayType}.
     */
    protected void bindColumnIndices() {
        mIdIndex = ContactTileLoaderFactory.CONTACT_ID;
        mLookupIndex = ContactTileLoaderFactory.LOOKUP_KEY;
        mPhotoUriIndex = ContactTileLoaderFactory.PHOTO_URI;
        mNameIndex = ContactTileLoaderFactory.DISPLAY_NAME;
        mStarredIndex = ContactTileLoaderFactory.STARRED;
        mPresenceIndex = ContactTileLoaderFactory.CONTACT_PRESENCE;
        mStatusIndex = ContactTileLoaderFactory.CONTACT_STATUS;

        mPhoneNumberIndex = ContactTileLoaderFactory.PHONE_NUMBER;
        mPhoneNumberTypeIndex = ContactTileLoaderFactory.PHONE_NUMBER_TYPE;
        mPhoneNumberLabelIndex = ContactTileLoaderFactory.PHONE_NUMBER_LABEL;
        mIsDefaultNumberIndex = ContactTileLoaderFactory.IS_DEFAULT_NUMBER;
        mPinnedIndex = ContactTileLoaderFactory.PINNED;
        mContactIdIndex = ContactTileLoaderFactory.CONTACT_ID_FOR_DATA;
    }

    /**
     * Gets the number of frequents from the passed in cursor.
     *
     * This methods is needed so the GroupMemberTileAdapter can override this.
     *
     * @param cursor The cursor to get number of frequents from.
     */
    protected void saveNumFrequentsFromCursor(Cursor cursor) {
        mNumFrequents = cursor.getCount() - mNumStarred;
    }

    /**
     * Creates {@link ContactTileView}s for each item in {@link Cursor}.
     *
     * Else use {@link ContactTileLoaderFactory}
     */
    public void setContactCursor(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            mNumStarred = getNumStarredContacts(cursor);
            if (mAwaitingRemove) {
                mDataSetChangedListener.cacheOffsetsForDatasetChange();
            }

            saveNumFrequentsFromCursor(cursor);
            saveCursorToCache(cursor);
            // cause a refresh of any views that rely on this data
            notifyDataSetChanged();
            // about to start redraw
            if (mIdToKeepInPlace != -1) {
                mDataSetChangedListener.onDataSetChangedForAnimation(mIdToKeepInPlace);
            } else {
                mDataSetChangedListener.onDataSetChangedForAnimation();
            }
            mIdToKeepInPlace = -1;
        }
    }

    /**
     * Saves the cursor data to the cache, to speed up UI changes.
     *
     * @param cursor Returned cursor with data to populate the view.
     */
    private void saveCursorToCache(Cursor cursor) {
        mContactEntries.clear();

        cursor.moveToPosition(-1);

        final LongSparseArray<Object> duplicates = new LongSparseArray<Object>(cursor.getCount());

        // Track the length of {@link #mContactEntries} and compare to {@link #TILES_SOFT_LIMIT}.
        int counter = 0;

        while (cursor.moveToNext()) {

            final int starred = cursor.getInt(mStarredIndex);
            final long id;

            // We display a maximum of TILES_SOFT_LIMIT contacts, or the total number of starred
            // whichever is greater.
            if (starred < 1 && counter >= TILES_SOFT_LIMIT) {
                break;
            } else {
                id = cursor.getLong(mContactIdIndex);
            }

            final ContactEntry existing = (ContactEntry) duplicates.get(id);
            if (existing != null) {
                // Check if the existing number is a default number. If not, clear the phone number
                // and label fields so that the disambiguation dialog will show up.
                if (!existing.isDefaultNumber) {
                    existing.phoneLabel = null;
                    existing.phoneNumber = null;
                }
                continue;
            }

            final String photoUri = cursor.getString(mPhotoUriIndex);
            final String lookupKey = cursor.getString(mLookupIndex);
            final int pinned = cursor.getInt(mPinnedIndex);
            final String name = cursor.getString(mNameIndex);
            final boolean isStarred = cursor.getInt(mStarredIndex) > 0;
            final boolean isDefaultNumber = cursor.getInt(mIsDefaultNumberIndex) > 0;

            final ContactEntry contact = new ContactEntry();

            contact.id = id;
            contact.name = (!TextUtils.isEmpty(name)) ? name :
                    mResources.getString(R.string.missing_name);
            contact.photoUri = (photoUri != null ? Uri.parse(photoUri) : null);
            contact.lookupKey = ContentUris.withAppendedId(
                    Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey), id);
            contact.isFavorite = isStarred;
            contact.isDefaultNumber = isDefaultNumber;

            // Set phone number and label
            final int phoneNumberType = cursor.getInt(mPhoneNumberTypeIndex);
            final String phoneNumberCustomLabel = cursor.getString(mPhoneNumberLabelIndex);
            contact.phoneLabel = (String) Phone.getTypeLabel(mResources, phoneNumberType,
                    phoneNumberCustomLabel);
            contact.phoneNumber = cursor.getString(mPhoneNumberIndex);

            contact.pinned = pinned;
            mContactEntries.add(contact);

            duplicates.put(id, contact);

            counter++;
        }

        mAwaitingRemove = false;

        arrangeContactsByPinnedPosition(mContactEntries);

        notifyDataSetChanged();
    }

    /**
     * Iterates over the {@link Cursor}
     * Returns position of the first NON Starred Contact
     * Returns -1 if {@link DisplayType#STARRED_ONLY}
     * Returns 0 if {@link DisplayType#FREQUENT_ONLY}
     */
    protected int getNumStarredContacts(Cursor cursor) {
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (cursor.getInt(mStarredIndex) == 0) {
                return cursor.getPosition();
            }
        }

        // There are not NON Starred contacts in cursor
        // Set divider positon to end
        return cursor.getCount();
    }

    /**
     * Loads a contact from the cached list.
     *
     * @param position Position of the Contact.
     * @return Contact at the requested position.
     */
    protected ContactEntry getContactEntryFromCache(int position) {
        if (mContactEntries.size() <= position) return null;
        return mContactEntries.get(position);
    }

    /**
     * Returns the number of frequents that will be displayed in the list.
     */
    public int getNumFrequents() {
        return mNumFrequents;
    }

    @Override
    public int getCount() {
        if (mContactEntries == null || mContactEntries.isEmpty()) {
            return 0;
        }

        int total = mContactEntries.size();
        // The number of contacts that don't show up as tiles
        final int nonTiledRows = Math.max(0, total - getMaxContactsInTiles());
        // The number of tiled rows
        final int tiledRows = getRowCount(total - nonTiledRows);
        return nonTiledRows + tiledRows;
    }

    public int getMaxTiledRows() {
        return mMaxTiledRows;
    }

    /**
     * Returns the number of rows required to show the provided number of entries
     * with the current number of columns.
     */
    protected int getRowCount(int entryCount) {
        if (entryCount == 0) return 0;
        final int nonLimitedRows = ((entryCount - 1) / mColumnCount) + 1;
        return Math.min(mMaxTiledRows, nonLimitedRows);
    }

    private int getMaxContactsInTiles() {
        return mColumnCount * mMaxTiledRows;
    }

    public int getRowIndex(int entryIndex) {
        if (entryIndex < mMaxTiledRows * mColumnCount) {
            return entryIndex / mColumnCount;
        } else {
            return entryIndex - mMaxTiledRows * mColumnCount + mMaxTiledRows;
        }
    }

    public int getColumnCount() {
        return mColumnCount;
    }

    /**
     * Returns an ArrayList of the {@link ContactEntry}s that are to appear
     * on the row for the given position.
     */
    @Override
    public ArrayList<ContactEntry> getItem(int position) {
        ArrayList<ContactEntry> resultList = new ArrayList<ContactEntry>(mColumnCount);

        final int entryIndex = getFirstContactEntryIndexForPosition(position);

        final int viewType = getItemViewType(position);

        final int columnCount;
        if (viewType == ViewTypes.TOP) {
            columnCount = mColumnCount;
        } else {
            columnCount = 1;
        }

        for (int i = 0; i < columnCount; i++) {
            final ContactEntry entry = getContactEntryFromCache(entryIndex + i);
            if (entry == null) break; // less than mColumnCount contacts
            resultList.add(entry);
        }

        return resultList;
    }

    /*
     * Given a position in the adapter, returns the index of the first contact entry that is to be
     * in that row.
     */
    private int getFirstContactEntryIndexForPosition(int position) {
        final int maxContactsInTiles = getMaxContactsInTiles();
        if (position < getRowCount(maxContactsInTiles)) {
            // Contacts that appear as tiles
            return position * mColumnCount;
        } else {
            // Contacts that appear as rows
            // The actual position of the contact in the cursor is simply total the number of
            // tiled contacts + the given position
            return maxContactsInTiles + position - mMaxTiledRows;
        }
    }

    /**
     * For the top row of tiled contacts, the item id is the position of the row of
     * contacts.
     * For frequent contacts, the item id is the maximum number of rows of tiled contacts +
     * the actual contact id. Since contact ids are always greater than 0, this guarantees that
     * all items within this adapter will always have unique ids.
     */
    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == ViewTypes.FREQUENT) {
            return getAdjustedItemId(getItem(position).get(0).id);
        } else {
            return position;
        }
    }

    /**
     * Calculates the stable itemId for a particular entry based on its contactID
     */
    public long getAdjustedItemId(long id) {
        return mMaxTiledRows + id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override

    public boolean areAllItemsEnabled() {
        // No dividers, so all items are enabled.
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return getCount() > 0;
    }

    @Override
    public void notifyDataSetChanged() {
        if (DEBUG) {
            Log.v(TAG, "notifyDataSetChanged");
        }
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (DEBUG) {
            Log.v(TAG, "get view for " + String.valueOf(position));
        }

        int itemViewType = getItemViewType(position);

        ContactTileRow contactTileRowView = null;

        if (convertView instanceof  ContactTileRow) {
            contactTileRowView  = (ContactTileRow) convertView;
        }

        ArrayList<ContactEntry> contactList = getItem(position);

        if (contactTileRowView == null) {
            // Creating new row if needed
            contactTileRowView = new ContactTileRow(mContext, itemViewType, position);
        }

        contactTileRowView.configureRow(contactList, position, position == getCount() - 1);

        return contactTileRowView;
    }

    private int getLayoutResourceId(int viewType) {
        switch (viewType) {
            case ViewTypes.FREQUENT:
                return R.layout.phone_favorite_regular_row_view;
            case ViewTypes.TOP:
                return R.layout.phone_favorite_tile_view;
            default:
                throw new IllegalArgumentException("Unrecognized viewType " + viewType);
        }
    }
    @Override
    public int getViewTypeCount() {
        return ViewTypes.COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < getRowCount(getMaxContactsInTiles())) {
            return ViewTypes.TOP;
        } else {
            return ViewTypes.FREQUENT;
        }
    }

    /**
     * Temporarily removes a contact from the list for UI refresh. Stores data for this contact
     * in the back-up variable.
     *
     * @param index Position of the contact to be removed.
     */
    public void popContactEntry(int index) {
        if (isIndexInBound(index)) {
            mDraggedEntry = mContactEntries.get(index);
            mDraggedEntryIndex = index;
            mDragEnteredEntryIndex = index;
            markDropArea(mDragEnteredEntryIndex);
        }
    }

    /**
     * @param itemIndex Position of the contact in {@link #mContactEntries}.
     * @return True if the given index is valid for {@link #mContactEntries}.
     */
    private boolean isIndexInBound(int itemIndex) {
        return itemIndex >= 0 && itemIndex < mContactEntries.size();
    }

    /**
     * Mark the tile as drop area by given the item index in {@link #mContactEntries}.
     *
     * @param itemIndex Position of the contact in {@link #mContactEntries}.
     */
    private void markDropArea(int itemIndex) {
        if (isIndexInBound(mDragEnteredEntryIndex) && isIndexInBound(itemIndex)) {
            mDataSetChangedListener.cacheOffsetsForDatasetChange();
            // Remove the old placeholder item and place the new placeholder item.
            final int oldIndex = mDragEnteredEntryIndex;
            mContactEntries.remove(mDragEnteredEntryIndex);
            mDragEnteredEntryIndex = itemIndex;
            mContactEntries.add(mDragEnteredEntryIndex, ContactEntry.BLANK_ENTRY);
            ContactEntry.BLANK_ENTRY.id = mDraggedEntry.id;
            mDataSetChangedListener.onDataSetChangedForAnimation();
            notifyDataSetChanged();
        }
    }

    /**
     * Drops the temporarily removed contact to the desired location in the list.
     */
    public void handleDrop() {
        boolean changed = false;
        if (mDraggedEntry != null) {
            if (isIndexInBound(mDragEnteredEntryIndex) &&
                    mDragEnteredEntryIndex != mDraggedEntryIndex) {
                // Don't add the ContactEntry here (to prevent a double animation from occuring).
                // When we receive a new cursor the list of contact entries will automatically be
                // populated with the dragged ContactEntry at the correct spot.
                mDropEntryIndex = mDragEnteredEntryIndex;
                mContactEntries.set(mDropEntryIndex, mDraggedEntry);
                mIdToKeepInPlace = getAdjustedItemId(mDraggedEntry.id);
                mDataSetChangedListener.cacheOffsetsForDatasetChange();
                changed = true;
            } else if (isIndexInBound(mDraggedEntryIndex)) {
                // If {@link #mDragEnteredEntryIndex} is invalid,
                // falls back to the original position of the contact.
                mContactEntries.remove(mDragEnteredEntryIndex);
                mContactEntries.add(mDraggedEntryIndex, mDraggedEntry);
                mDropEntryIndex = mDraggedEntryIndex;
                notifyDataSetChanged();
            }

            if (changed && mDropEntryIndex < PIN_LIMIT) {
                final ContentValues cv = getReflowedPinnedPositions(mContactEntries, mDraggedEntry,
                        mDraggedEntryIndex, mDropEntryIndex);
                final Uri pinUri = PinnedPositions.UPDATE_URI.buildUpon().build();
                // update the database here with the new pinned positions
                mContext.getContentResolver().update(pinUri, cv, null, null);
            }
            mDraggedEntry = null;
        }
    }

    /**
     * Invoked when the dragged item is dropped to unsupported location. We will then move the
     * contact back to where it was dragged from.
     */
    public void dropToUnsupportedView() {
        if (isIndexInBound(mDragEnteredEntryIndex)) {
            mContactEntries.remove(mDragEnteredEntryIndex);
            mContactEntries.add(mDraggedEntryIndex, mDraggedEntry);
            notifyDataSetChanged();
        }
    }

    /**
     * Sets an item to for pending removal. If the user does not click the undo button, the item
     * will be removed at the next interaction.
     *
     * @param index Index of the item to be removed.
     */
    public void setPotentialRemoveEntryIndex(int index) {
        mPotentialRemoveEntryIndex = index;
    }

    /**
     * Removes a contact entry from the list.
     *
     * @return True is an item is removed. False is there is no item to be removed.
     */
    public boolean removePendingContactEntry() {
        boolean removed = false;
        if (isIndexInBound(mPotentialRemoveEntryIndex)) {
            final ContactEntry entry = mContactEntries.get(mPotentialRemoveEntryIndex);
            unstarAndUnpinContact(entry.lookupKey);
            removed = true;
            mAwaitingRemove = true;
        }
        cleanTempVariables();
        return removed;
    }

    /**
     * Resets the item for pending removal.
     */
    public void undoPotentialRemoveEntryIndex() {
        mPotentialRemoveEntryIndex = -1;
    }

    public boolean hasPotentialRemoveEntryIndex() {
        return isIndexInBound(mPotentialRemoveEntryIndex);
    }

    /**
     * Clears all temporary variables at a new interaction.
     */
    public void cleanTempVariables() {
        mDraggedEntryIndex = -1;
        mDropEntryIndex = -1;
        mDragEnteredEntryIndex = -1;
        mDraggedEntry = null;
        mPotentialRemoveEntryIndex = -1;
    }

    /**
     * Acts as a row item composed of {@link ContactTileView}
     *
     */
    public class ContactTileRow extends FrameLayout implements SwipeHelperCallback {
        public static final int CONTACT_ENTRY_INDEX_TAG = R.id.contact_entry_index_tag;

        private int mItemViewType;
        private int mLayoutResId;
        private final int mRowPaddingStart;
        private final int mRowPaddingEnd;
        private final int mRowPaddingTop;
        private final int mRowPaddingBottom;
        private int mPosition;
        private SwipeHelper mSwipeHelper;
        private OnItemGestureListener mOnItemSwipeListener;

        public ContactTileRow(Context context, int itemViewType, int position) {
            super(context);
            mItemViewType = itemViewType;
            mLayoutResId = getLayoutResourceId(mItemViewType);
            mPosition = position;

            final Resources resources = mContext.getResources();

            if (mItemViewType == ViewTypes.TOP) {
                // For tiled views, we still want padding to be set on the ContactTileRow.
                // Otherwise the padding would be set around each of the tiles, which we don't want
                mRowPaddingTop = resources.getDimensionPixelSize(
                        R.dimen.favorites_row_top_padding);
                mRowPaddingBottom = resources.getDimensionPixelSize(
                        R.dimen.favorites_row_bottom_padding);
                mRowPaddingStart = resources.getDimensionPixelSize(
                        R.dimen.favorites_row_start_padding);
                mRowPaddingEnd = resources.getDimensionPixelSize(
                        R.dimen.favorites_row_end_padding);

                setBackgroundResource(R.drawable.bottom_border_background);
            } else {
                // For row views, padding is set on the view itself.
                mRowPaddingTop = 0;
                mRowPaddingBottom = 0;
                mRowPaddingStart = 0;
                mRowPaddingEnd = 0;
            }

            setPaddingRelative(mRowPaddingStart, mRowPaddingTop, mRowPaddingEnd,
                    mRowPaddingBottom);

            // Remove row (but not children) from accessibility node tree.
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

            if (mItemViewType == ViewTypes.FREQUENT) {
                // ListView handles swiping for this item
                SwipeHelper.setSwipeable(this, true);
            } else if (mItemViewType == ViewTypes.TOP) {
                // The contact tile row has its own swipe helpers, that makes each individual
                // tile swipeable.
                final float densityScale = getResources().getDisplayMetrics().density;
                final float pagingTouchSlop = ViewConfiguration.get(context)
                        .getScaledPagingTouchSlop();
                mSwipeHelper = new SwipeHelper(context, SwipeHelper.X, this, densityScale,
                        pagingTouchSlop);
                // Increase swipe thresholds for square tiles since they are relatively small.
                mSwipeHelper.setChildSwipedFarEnoughFactor(0.9f);
                mSwipeHelper.setChildSwipedFastEnoughFactor(0.1f);
                mOnItemSwipeListener = PhoneFavoritesTileAdapter.this;
            }
        }

        /**
         * Configures the row to add {@link ContactEntry}s information to the views
         */
        public void configureRow(ArrayList<ContactEntry> list, int position, boolean isLastRow) {
            int columnCount = mItemViewType == ViewTypes.FREQUENT ? 1 : mColumnCount;
            mPosition = position;

            // Adding tiles to row and filling in contact information
            for (int columnCounter = 0; columnCounter < columnCount; columnCounter++) {
                ContactEntry entry =
                        columnCounter < list.size() ? list.get(columnCounter) : null;
                addTileFromEntry(entry, columnCounter, isLastRow);
            }
            if (columnCount == 1) {
                if (list.get(0) == ContactEntry.BLANK_ENTRY) {
                    setVisibility(View.INVISIBLE);
                } else {
                    setVisibility(View.VISIBLE);
                }
            }
        }

        private void addTileFromEntry(ContactEntry entry, int childIndex, boolean isLastRow) {
            final PhoneFavoriteTileView contactTile;

            if (getChildCount() <= childIndex) {

                contactTile = (PhoneFavoriteTileView) inflate(mContext, mLayoutResId, null);
                // Note: the layoutparam set here is only actually used for FREQUENT.
                // We override onMeasure() for STARRED and we don't care the layout param there.
                final Resources resources = mContext.getResources();
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

                params.setMargins(
                        resources.getDimensionPixelSize(R.dimen.detail_item_side_margin), 0,
                        resources.getDimensionPixelSize(R.dimen.detail_item_side_margin), 0);
                contactTile.setLayoutParams(params);
                contactTile.setPhotoManager(mPhotoManager);
                contactTile.setListener(mListener);
                addView(contactTile);
            } else {
                contactTile = (PhoneFavoriteTileView) getChildAt(childIndex);
            }
            contactTile.loadFromContact(entry);

            int entryIndex = -1;
            switch (mItemViewType) {
                case ViewTypes.TOP:
                    // Setting divider visibilities
                    contactTile.setPaddingRelative(0, 0,
                            childIndex >= mColumnCount - 1 ? 0 : mPaddingInPixels, 0);
                    entryIndex = getFirstContactEntryIndexForPosition(mPosition) + childIndex;
                    SwipeHelper.setSwipeable(contactTile, false);
                    break;
                case ViewTypes.FREQUENT:
                    contactTile.setHorizontalDividerVisibility(
                            isLastRow ? View.GONE : View.VISIBLE);
                    entryIndex = getFirstContactEntryIndexForPosition(mPosition);
                    SwipeHelper.setSwipeable(this, true);
                    break;
                default:
                    break;
            }
            // tag the tile with the index of the contact entry it is associated with
            if (entryIndex != -1) {
                contactTile.setTag(CONTACT_ENTRY_INDEX_TAG, entryIndex);
            }
            contactTile.setupFavoriteContactCard();
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            switch (mItemViewType) {
                case ViewTypes.TOP:
                    onLayoutForTiles();
                    return;
                default:
                    super.onLayout(changed, left, top, right, bottom);
                    return;
            }
        }

        private void onLayoutForTiles() {
            final int count = getChildCount();

            // Just line up children horizontally.
            int childLeft = getPaddingStart();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);

                // Note MeasuredWidth includes the padding.
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, getPaddingTop(), childLeft + childWidth,
                        getPaddingTop() + child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            switch (mItemViewType) {
                case ViewTypes.TOP:
                    onMeasureForTiles(widthMeasureSpec);
                    return;
                default:
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    return;
            }
        }

        private void onMeasureForTiles(int widthMeasureSpec) {
            final int width = MeasureSpec.getSize(widthMeasureSpec);

            final int childCount = getChildCount();
            if (childCount == 0) {
                // Just in case...
                setMeasuredDimension(width, 0);
                return;
            }

            // 1. Calculate image size.
            //      = ([total width] - [total padding]) / [child count]
            //
            // 2. Set it to width/height of each children.
            //    If we have a remainder, some tiles will have 1 pixel larger width than its height.
            //
            // 3. Set the dimensions of itself.
            //    Let width = given width.
            //    Let height = image size + bottom paddding.

            final int totalPaddingsInPixels = (mColumnCount - 1) * mPaddingInPixels
                    + mRowPaddingStart + mRowPaddingEnd;

            // Preferred width / height for images (excluding the padding).
            // The actual width may be 1 pixel larger than this if we have a remainder.
            final int imageSize = (width - totalPaddingsInPixels) / mColumnCount;
            final int remainder = width - (imageSize * mColumnCount) - totalPaddingsInPixels;

            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final int childWidth = imageSize + child.getPaddingRight()
                        // Compensate for the remainder
                        + (i < remainder ? 1 : 0);
                final int childHeight = imageSize;
                child.measure(
                        MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
                        );
            }
            setMeasuredDimension(width, imageSize + getPaddingTop() + getPaddingBottom());
        }

        /**
         * Gets the index of the item at the specified coordinates.
         *
         * @param itemX X-coordinate of the selected item.
         * @param itemY Y-coordinate of the selected item.
         * @return Index of the selected item in the cached array.
         */
        public int getItemIndex(float itemX, float itemY) {
            if (mPosition < mMaxTiledRows) {
                if (DEBUG) {
                    Log.v(TAG, String.valueOf(itemX) + " " + String.valueOf(itemY));
                }
                for (int i = 0; i < getChildCount(); ++i) {
                    /** If the row contains multiple tiles, checks each tile to see if the point
                     * is contained in the tile. */
                    final View child = getChildAt(i);
                    /** The coordinates passed in are based on the ListView,
                     * translate for each child first */
                    final int xInListView = child.getLeft() + getLeft();
                    final int yInListView = child.getTop() + getTop();
                    final int distanceX = (int) itemX - xInListView;
                    final int distanceY = (int) itemY - yInListView;
                    if ((distanceX > 0 && distanceX < child.getWidth()) &&
                            (distanceY > 0 && distanceY < child.getHeight())) {
                        /** If the point is contained in the rectangle, computes the index of the
                         * item in the cached array. */
                        return i + (mPosition) * mColumnCount;
                    }
                }
            } else {
                /** If the selected item is one of the rows, compute the index. */
                return getRegularRowItemIndex();
            }
            return -1;
        }

        /**
         * Gets the index of the regular row item.
         *
         * @return Index of the selected item in the cached array.
         */
        public int getRegularRowItemIndex() {
            return (mPosition - mMaxTiledRows) + mColumnCount * mMaxTiledRows;
        }

        public PhoneFavoritesTileAdapter getTileAdapter() {
            return PhoneFavoritesTileAdapter.this;
        }

        public int getPosition() {
            return mPosition;
        }

        /**
         * Find the view under the pointer.
         */
        public View getViewAtPosition(int x, int y) {
            // find the view under the pointer, accounting for GONE views
            final int count = getChildCount();
            View view;
            for (int childIdx = 0; childIdx < count; childIdx++) {
                view = getChildAt(childIdx);
                if (x >= view.getLeft() && x <= view.getRight()) {
                    return view;
                }
            }
            return null;
        }

        @Override
        public View getChildAtPosition(MotionEvent ev) {
            final View view = getViewAtPosition((int) ev.getX(), (int) ev.getY());
            if (view != null &&
                    SwipeHelper.isSwipeable(view) &&
                    view.getVisibility() != GONE) {
                // If this view is swipable, then return it. If not, because the removal
                // dialog is currently showing, then return a null view, which will simply
                // be ignored by the swipe helper.
                return view;
            }
            return null;
        }

        @Override
        public View getChildContentView(View v) {
            return v.findViewById(R.id.contact_favorite_card);
        }

        @Override
        public void onScroll() {}

        @Override
        public boolean canChildBeDismissed(View v) {
            return true;
        }

        @Override
        public void onBeginDrag(View v) {
            removePendingContactEntry();
            final int index = indexOfChild(v);

            /*
            if (index > 0) {
                detachViewFromParent(index);
                attachViewToParent(v, 0, v.getLayoutParams());
            }*/

            // We do this so the underlying ScrollView knows that it won't get
            // the chance to intercept events anymore
            requestDisallowInterceptTouchEvent(true);
        }

        @Override
        public void onChildDismissed(View v) {
            if (v != null) {
                if (mOnItemSwipeListener != null) {
                    mOnItemSwipeListener.onSwipe(v);
                }
            }
        }

        @Override
        public void onDragCancelled(View v) {}

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (mSwipeHelper != null && isSwipeEnabled()) {
                return mSwipeHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev);
            } else {
                return super.onInterceptTouchEvent(ev);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (mSwipeHelper != null && isSwipeEnabled()) {
                return mSwipeHelper.onTouchEvent(ev) || super.onTouchEvent(ev);
            } else {
                return super.onTouchEvent(ev);
            }
        }

        public int getItemViewType() {
            return mItemViewType;
        }

        public void setOnItemSwipeListener(OnItemGestureListener listener) {
            mOnItemSwipeListener = listener;
        }
    }

    /**
     * Used when a contact is swiped away. This will both unstar and set pinned position of the
     * contact to PinnedPosition.DEMOTED so that it doesn't show up anymore in the favorites list.
     */
    private void unstarAndUnpinContact(Uri contactUri) {
        final ContentValues values = new ContentValues(2);
        values.put(Contacts.STARRED, false);
        values.put(Contacts.PINNED, PinnedPositions.DEMOTED);
        mContext.getContentResolver().update(contactUri, values, null, null);
    }

    /**
     * Given a list of contacts that each have pinned positions, rearrange the list (destructive)
     * such that all pinned contacts are in their defined pinned positions, and unpinned contacts
     * take the spaces between those pinned contacts. Demoted contacts should not appear in the
     * resulting list.
     *
     * This method also updates the pinned positions of pinned contacts so that they are all
     * unique positive integers within range from 0 to toArrange.size() - 1. This is because
     * when the contact entries are read from the database, it is possible for them to have
     * overlapping pin positions due to sync or modifications by third party apps.
     */
    @VisibleForTesting
    /* package */ void arrangeContactsByPinnedPosition(ArrayList<ContactEntry> toArrange) {
        final PriorityQueue<ContactEntry> pinnedQueue =
                new PriorityQueue<ContactEntry>(PIN_LIMIT, mContactEntryComparator);

        final List<ContactEntry> unpinnedContacts = new LinkedList<ContactEntry>();

        final int length = toArrange.size();
        for (int i = 0; i < length; i++) {
            final ContactEntry contact = toArrange.get(i);
            // Decide whether the contact is hidden(demoted), pinned, or unpinned
            if (contact.pinned > PIN_LIMIT) {
                unpinnedContacts.add(contact);
            } else if (contact.pinned > PinnedPositions.DEMOTED) {
                // Demoted or contacts with negative pinned positions are ignored.
                // Pinned contacts go into a priority queue where they are ranked by pinned
                // position. This is required because the contacts provider does not return
                // contacts ordered by pinned position.
                pinnedQueue.add(contact);
            }
        }

        final int maxToPin = Math.min(PIN_LIMIT, pinnedQueue.size() + unpinnedContacts.size());

        toArrange.clear();
        for (int i = 0; i < maxToPin; i++) {
            if (!pinnedQueue.isEmpty() && pinnedQueue.peek().pinned <= i) {
                final ContactEntry toPin = pinnedQueue.poll();
                toPin.pinned = i;
                toArrange.add(toPin);
            } else if (!unpinnedContacts.isEmpty()) {
                toArrange.add(unpinnedContacts.remove(0));
            }
        }

        // If there are still contacts in pinnedContacts at this point, it means that the pinned
        // positions of these pinned contacts exceed the actual number of contacts in the list.
        // For example, the user had 10 frequents, starred and pinned one of them at the last spot,
        // and then cleared frequents. Contacts in this situation should become unpinned.
        while (!pinnedQueue.isEmpty()) {
            final ContactEntry entry = pinnedQueue.poll();
            entry.pinned = PinnedPositions.UNPINNED;
            toArrange.add(entry);
        }

        // Any remaining unpinned contacts that weren't in the gaps between the pinned contacts
        // now just get appended to the end of the list.
        toArrange.addAll(unpinnedContacts);
    }

    /**
     * Given an existing list of contact entries and a single entry that is to be pinned at a
     * particular position, return a ContentValues object that contains new pinned positions for
     * all contacts that are forced to be pinned at new positions, trying as much as possible to
     * keep pinned contacts at their original location.
     *
     * At this point in time the pinned position of each contact in the list has already been
     * updated by {@link #arrangeContactsByPinnedPosition}, so we can assume that all pinned
     * positions(within {@link #PIN_LIMIT} are unique positive integers.
     */
    @VisibleForTesting
    /* package */ ContentValues getReflowedPinnedPositions(ArrayList<ContactEntry> list,
            ContactEntry entryToPin, int oldPos, int newPinPos) {

        final ContentValues cv = new ContentValues();
        final int lowerBound = Math.min(oldPos, newPinPos);
        final int upperBound = Math.max(oldPos, newPinPos);
        for (int i = lowerBound; i <= upperBound; i++) {
            final ContactEntry entry = list.get(i);
            if (entry.pinned == i) continue;
            cv.put(String.valueOf(entry.id), i);
        }
        return cv;
    }

    protected static class ViewTypes {
        public static final int FREQUENT = 0;
        public static final int TOP = 1;
        public static final int COUNT = 2;
    }

    @Override
    public void onSwipe(View view) {
        final PhoneFavoriteTileView tileView = (PhoneFavoriteTileView) view.findViewById(
                R.id.contact_tile);
        // When the view is in the removal dialog, it should no longer be swipeable
        SwipeHelper.setSwipeable(view, false);
        tileView.displayRemovalDialog();

        final Integer entryIndex = (Integer) tileView.getTag(
                ContactTileRow.CONTACT_ENTRY_INDEX_TAG);

        setPotentialRemoveEntryIndex(entryIndex);
    }

    @Override
    public void onTouch() {
        removePendingContactEntry();
        return;
    }

    @Override
    public boolean isSwipeEnabled() {
        return !mAwaitingRemove;
    }

    @Override
    public void onDragStarted(int itemIndex) {
        setInDragging(true);
        popContactEntry(itemIndex);
    }

    @Override
    public void onDragHovered(int itemIndex) {
        if (mInDragging &&
                mDragEnteredEntryIndex != itemIndex &&
                isIndexInBound(itemIndex) &&
                itemIndex < PIN_LIMIT) {
            markDropArea(itemIndex);
        }
    }

    @Override
    public void onDragFinished() {
        setInDragging(false);
        handleDrop();
    }
}
