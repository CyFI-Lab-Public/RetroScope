/*******************************************************************************
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.providers;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.android.mail.content.CursorCreator;
import com.android.mail.content.ObjectCursorLoader;
import com.android.mail.providers.UIProvider.FolderType;
import com.android.mail.utils.FolderUri;
import com.android.mail.utils.LogTag;
import com.android.mail.utils.LogUtils;
import com.android.mail.utils.Utils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A folder is a collection of conversations, and perhaps other folders.
 */
// TODO: make most of these fields final
public class Folder implements Parcelable, Comparable<Folder> {

    @Deprecated
    public static final String SPLITTER = "^*^";
    @Deprecated
    private static final Pattern SPLITTER_REGEX = Pattern.compile("\\^\\*\\^");

    private static final String FOLDER_UNINITIALIZED = "Uninitialized!";

    // TODO: remove this once we figure out which folder is returning a "null" string as the
    // conversation list uri
    private static final String NULL_STRING_URI = "null";
    private static final String LOG_TAG = LogTag.getLogTag();

    // Try to match the order of members with the order of constants in UIProvider.

    /**
     * Unique id of this folder.
     */
    public int id;

    /**
     * Persistent (across installations) id of this folder.
     */
    public String persistentId;

    /**
     * The content provider URI that returns this folder for this account.
     */
    public FolderUri folderUri;

    /**
     * The human visible name for this folder.
     */
    public String name;

    /**
     * The possible capabilities that this folder supports.
     */
    public int capabilities;

    /**
     * Whether or not this folder has children folders.
     */
    public boolean hasChildren;

    /**
     * How large the synchronization window is: how many days worth of data is retained on the
     * device.
     */
    public int syncWindow;

    /**
     * The content provider URI to return the list of conversations in this
     * folder.
     */
    public Uri conversationListUri;

    /**
     * The content provider URI to return the list of child folders of this folder.
     */
    public Uri childFoldersListUri;

    /**
     * The number of messages that are unseen in this folder.
     */
    public int unseenCount;

    /**
     * The number of messages that are unread in this folder.
     */
    public int unreadCount;

    /**
     * The total number of messages in this folder.
     */
    public int totalCount;

    /**
     * The content provider URI to force a refresh of this folder.
     */
    public Uri refreshUri;

    /**
     * The current sync status of the folder
     */
    public int syncStatus;

    /**
     * A packed integer containing the last synced result, and the request code. The
     * value is (requestCode << 4) | syncResult
     * syncResult is a value from {@link UIProvider.LastSyncResult}
     * requestCode is a value from: {@link UIProvider.SyncStatus},
     */
    public int lastSyncResult;

    /**
     * Folder type bit mask. 0 is default.
     * @see FolderType
     */
    public int type;

    /**
     * Icon for this folder; 0 implies no icon.
     */
    public int iconResId;

    /**
     * Notification icon for this folder; 0 implies no icon.
     */
    public int notificationIconResId;

    public String bgColor;
    public String fgColor;

    public int bgColorInt;
    public int fgColorInt;

    /**
     * The content provider URI to request additional conversations
     */
    public Uri loadMoreUri;

    /**
     * The possibly empty name of this folder with full hierarchy.
     * The expected format is: parent/folder1/folder2/folder3/folder4
     */
    public String hierarchicalDesc;

    /**
     * Parent folder of this folder, or null if there is none.
     */
    public Uri parent;

    /**
     * The time at which the last message was received.
     */
    public long lastMessageTimestamp;

    /**
     * A string of unread senders sorted by date, so we don't have to fetch this in multiple queries
     */
    public String unreadSenders;

    /** An immutable, empty conversation list */
    public static final Collection<Folder> EMPTY = Collections.emptyList();

    public static final class Builder {
        private int mId;
        private String mPersistentId;
        private Uri mUri;
        private String mName;
        private int mCapabilities;
        private boolean mHasChildren;
        private int mSyncWindow;
        private Uri mConversationListUri;
        private Uri mChildFoldersListUri;
        private int mUnseenCount;
        private int mUnreadCount;
        private int mTotalCount;
        private Uri mRefreshUri;
        private int mSyncStatus;
        private int mLastSyncResult;
        private int mType;
        private int mIconResId;
        private int mNotificationIconResId;
        private String mBgColor;
        private String mFgColor;
        private Uri mLoadMoreUri;
        private String mHierarchicalDesc;
        private Uri mParent;
        private long mLastMessageTimestamp;
        private String mUnreadSenders;

        public Folder build() {
            return new Folder(mId, mPersistentId, mUri, mName, mCapabilities,
                    mHasChildren, mSyncWindow, mConversationListUri, mChildFoldersListUri,
                    mUnseenCount, mUnreadCount, mTotalCount, mRefreshUri, mSyncStatus,
                    mLastSyncResult, mType, mIconResId, mNotificationIconResId, mBgColor,
                    mFgColor, mLoadMoreUri, mHierarchicalDesc, mParent,
                    mLastMessageTimestamp, mUnreadSenders);
        }

        public Builder setId(final int id) {
            mId = id;
            return this;
        }
        public Builder setPersistentId(final String persistentId) {
            mPersistentId = persistentId;
            return this;
        }
        public Builder setUri(final Uri uri) {
            mUri = uri;
            return this;
        }
        public Builder setName(final String name) {
            mName = name;
            return this;
        }
        public Builder setCapabilities(final int capabilities) {
            mCapabilities = capabilities;
            return this;
        }
        public Builder setHasChildren(final boolean hasChildren) {
            mHasChildren = hasChildren;
            return this;
        }
        public Builder setSyncWindow(final int syncWindow) {
            mSyncWindow = syncWindow;
            return this;
        }
        public Builder setConversationListUri(final Uri conversationListUri) {
            mConversationListUri = conversationListUri;
            return this;
        }
        public Builder setChildFoldersListUri(final Uri childFoldersListUri) {
            mChildFoldersListUri = childFoldersListUri;
            return this;
        }
        public Builder setUnseenCount(final int unseenCount) {
            mUnseenCount = unseenCount;
            return this;
        }
        public Builder setUnreadCount(final int unreadCount) {
            mUnreadCount = unreadCount;
            return this;
        }
        public Builder setTotalCount(final int totalCount) {
            mTotalCount = totalCount;
            return this;
        }
        public Builder setRefreshUri(final Uri refreshUri) {
            mRefreshUri = refreshUri;
            return this;
        }
        public Builder setSyncStatus(final int syncStatus) {
            mSyncStatus = syncStatus;
            return this;
        }
        public Builder setLastSyncResult(final int lastSyncResult) {
            mLastSyncResult = lastSyncResult;
            return this;
        }
        public Builder setType(final int type) {
            mType = type;
            return this;
        }
        public Builder setIconResId(final int iconResId) {
            mIconResId = iconResId;
            return this;
        }
        public Builder setNotificationIconResId(final int notificationIconResId) {
            mNotificationIconResId = notificationIconResId;
            return this;
        }
        public Builder setBgColor(final String bgColor) {
            mBgColor = bgColor;
            return this;
        }
        public Builder setFgColor(final String fgColor) {
            mFgColor = fgColor;
            return this;
        }
        public Builder setLoadMoreUri(final Uri loadMoreUri) {
            mLoadMoreUri = loadMoreUri;
            return this;
        }
        public Builder setHierarchicalDesc(final String hierarchicalDesc) {
            mHierarchicalDesc = hierarchicalDesc;
            return this;
        }
        public Builder setParent(final Uri parent) {
            mParent = parent;
            return this;
        }
        public Builder setLastMessageTimestamp(final long lastMessageTimestamp) {
            mLastMessageTimestamp = lastMessageTimestamp;
            return this;
        }
        public Builder setUnreadSenders(final String unreadSenders) {
            mUnreadSenders = unreadSenders;
            return this;
        }
    }

    public Folder(int id, String persistentId, Uri uri, String name, int capabilities,
            boolean hasChildren, int syncWindow, Uri conversationListUri, Uri childFoldersListUri,
            int unseenCount, int unreadCount, int totalCount, Uri refreshUri, int syncStatus,
            int lastSyncResult, int type, int iconResId, int notificationIconResId, String bgColor,
            String fgColor, Uri loadMoreUri, String hierarchicalDesc, Uri parent,
            final long lastMessageTimestamp, final String unreadSenders) {
        this.id = id;
        this.persistentId = persistentId;
        this.folderUri = new FolderUri(uri);
        this.name = name;
        this.capabilities = capabilities;
        this.hasChildren = hasChildren;
        this.syncWindow = syncWindow;
        this.conversationListUri = conversationListUri;
        this.childFoldersListUri = childFoldersListUri;
        this.unseenCount = unseenCount;
        this.unreadCount = unreadCount;
        this.totalCount = totalCount;
        this.refreshUri = refreshUri;
        this.syncStatus = syncStatus;
        this.lastSyncResult = lastSyncResult;
        this.type = type;
        this.iconResId = iconResId;
        this.notificationIconResId = notificationIconResId;
        this.bgColor = bgColor;
        this.fgColor = fgColor;
        if (bgColor != null) {
            this.bgColorInt = Integer.parseInt(bgColor);
        }
        if (fgColor != null) {
            this.fgColorInt = Integer.parseInt(fgColor);
        }
        this.loadMoreUri = loadMoreUri;
        this.hierarchicalDesc = hierarchicalDesc;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.parent = parent;
        this.unreadSenders = unreadSenders;
    }

    public Folder(Cursor cursor) {
        id = cursor.getInt(UIProvider.FOLDER_ID_COLUMN);
        persistentId = cursor.getString(UIProvider.FOLDER_PERSISTENT_ID_COLUMN);
        folderUri =
                new FolderUri(Uri.parse(cursor.getString(UIProvider.FOLDER_URI_COLUMN)));
        name = cursor.getString(UIProvider.FOLDER_NAME_COLUMN);
        capabilities = cursor.getInt(UIProvider.FOLDER_CAPABILITIES_COLUMN);
        // 1 for true, 0 for false.
        hasChildren = cursor.getInt(UIProvider.FOLDER_HAS_CHILDREN_COLUMN) == 1;
        syncWindow = cursor.getInt(UIProvider.FOLDER_SYNC_WINDOW_COLUMN);
        String convList = cursor.getString(UIProvider.FOLDER_CONVERSATION_LIST_URI_COLUMN);
        conversationListUri = !TextUtils.isEmpty(convList) ? Uri.parse(convList) : null;
        String childList = cursor.getString(UIProvider.FOLDER_CHILD_FOLDERS_LIST_COLUMN);
        childFoldersListUri = (hasChildren && !TextUtils.isEmpty(childList)) ? Uri.parse(childList)
                : null;
        unseenCount = cursor.getInt(UIProvider.FOLDER_UNSEEN_COUNT_COLUMN);
        unreadCount = cursor.getInt(UIProvider.FOLDER_UNREAD_COUNT_COLUMN);
        totalCount = cursor.getInt(UIProvider.FOLDER_TOTAL_COUNT_COLUMN);
        String refresh = cursor.getString(UIProvider.FOLDER_REFRESH_URI_COLUMN);
        refreshUri = !TextUtils.isEmpty(refresh) ? Uri.parse(refresh) : null;
        syncStatus = cursor.getInt(UIProvider.FOLDER_SYNC_STATUS_COLUMN);
        lastSyncResult = cursor.getInt(UIProvider.FOLDER_LAST_SYNC_RESULT_COLUMN);
        type = cursor.getInt(UIProvider.FOLDER_TYPE_COLUMN);
        iconResId = cursor.getInt(UIProvider.FOLDER_ICON_RES_ID_COLUMN);
        notificationIconResId = cursor.getInt(UIProvider.FOLDER_NOTIFICATION_ICON_RES_ID_COLUMN);
        bgColor = cursor.getString(UIProvider.FOLDER_BG_COLOR_COLUMN);
        fgColor = cursor.getString(UIProvider.FOLDER_FG_COLOR_COLUMN);
        if (bgColor != null) {
            bgColorInt = Integer.parseInt(bgColor);
        }
        if (fgColor != null) {
            fgColorInt = Integer.parseInt(fgColor);
        }
        String loadMore = cursor.getString(UIProvider.FOLDER_LOAD_MORE_URI_COLUMN);
        loadMoreUri = !TextUtils.isEmpty(loadMore) ? Uri.parse(loadMore) : null;
        hierarchicalDesc = cursor.getString(UIProvider.FOLDER_HIERARCHICAL_DESC_COLUMN);
        lastMessageTimestamp = cursor.getLong(UIProvider.FOLDER_LAST_MESSAGE_TIMESTAMP_COLUMN);
        // A null parent URI means that this is a top-level folder.
        final String parentString = cursor.getString(UIProvider.FOLDER_PARENT_URI_COLUMN);
        parent = parentString == null ? Uri.EMPTY : Uri.parse(parentString);
        final int unreadSendersColumn =
                cursor.getColumnIndex(UIProvider.FolderColumns.UNREAD_SENDERS);
        if (unreadSendersColumn != -1) {
            unreadSenders = cursor.getString(unreadSendersColumn);
        } else {
            unreadSenders = null;
        }
    }

    /**
     * Public object that knows how to construct Folders given Cursors.
     */
    public static final CursorCreator<Folder> FACTORY = new CursorCreator<Folder>() {
        @Override
        public Folder createFromCursor(Cursor c) {
            return new Folder(c);
        }

        @Override
        public String toString() {
            return "Folder CursorCreator";
        }
    };

    public Folder(Parcel in, ClassLoader loader) {
        id = in.readInt();
        persistentId = in.readString();
        folderUri = new FolderUri((Uri) in.readParcelable(loader));
        name = in.readString();
        capabilities = in.readInt();
        // 1 for true, 0 for false.
        hasChildren = in.readInt() == 1;
        syncWindow = in.readInt();
        conversationListUri = in.readParcelable(loader);
        childFoldersListUri = in.readParcelable(loader);
        unseenCount = in.readInt();
        unreadCount = in.readInt();
        totalCount = in.readInt();
        refreshUri = in.readParcelable(loader);
        syncStatus = in.readInt();
        lastSyncResult = in.readInt();
        type = in.readInt();
        iconResId = in.readInt();
        notificationIconResId = in.readInt();
        bgColor = in.readString();
        fgColor = in.readString();
        if (bgColor != null) {
            bgColorInt = Integer.parseInt(bgColor);
        }
        if (fgColor != null) {
            fgColorInt = Integer.parseInt(fgColor);
        }
        loadMoreUri = in.readParcelable(loader);
        hierarchicalDesc = in.readString();
        parent = in.readParcelable(loader);
        lastMessageTimestamp = in.readLong();
        parent = in.readParcelable(loader);
        unreadSenders = in.readString();
     }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(persistentId);
        dest.writeParcelable(folderUri != null ? folderUri.fullUri : null, 0);
        dest.writeString(name);
        dest.writeInt(capabilities);
        // 1 for true, 0 for false.
        dest.writeInt(hasChildren ? 1 : 0);
        dest.writeInt(syncWindow);
        dest.writeParcelable(conversationListUri, 0);
        dest.writeParcelable(childFoldersListUri, 0);
        dest.writeInt(unseenCount);
        dest.writeInt(unreadCount);
        dest.writeInt(totalCount);
        dest.writeParcelable(refreshUri, 0);
        dest.writeInt(syncStatus);
        dest.writeInt(lastSyncResult);
        dest.writeInt(type);
        dest.writeInt(iconResId);
        dest.writeInt(notificationIconResId);
        dest.writeString(bgColor);
        dest.writeString(fgColor);
        dest.writeParcelable(loadMoreUri, 0);
        dest.writeString(hierarchicalDesc);
        dest.writeParcelable(parent, 0);
        dest.writeLong(lastMessageTimestamp);
        dest.writeParcelable(parent, 0);
        dest.writeString(unreadSenders);
    }

    /**
     * Construct a folder that queries for search results. Do not call on the UI
     * thread.
     */
    public static ObjectCursorLoader<Folder> forSearchResults(Account account, String query,
            Context context) {
        if (account.searchUri != null) {
            final Uri.Builder searchBuilder = account.searchUri.buildUpon();
            searchBuilder.appendQueryParameter(UIProvider.SearchQueryParameters.QUERY, query);
            final Uri searchUri = searchBuilder.build();
            return new ObjectCursorLoader<Folder>(context, searchUri, UIProvider.FOLDERS_PROJECTION,
                    FACTORY);
        }
        return null;
    }

    public static HashMap<Uri, Folder> hashMapForFolders(List<Folder> rawFolders) {
        final HashMap<Uri, Folder> folders = new HashMap<Uri, Folder>();
        for (Folder f : rawFolders) {
            folders.put(f.folderUri.getComparisonUri(), f);
        }
        return folders;
    }

    /**
     * Constructor that leaves everything uninitialized.
     */
    private Folder() {
        name = FOLDER_UNINITIALIZED;
    }

    /**
     * Creates a new instance of a folder object that is <b>not</b> initialized.  The caller is
     * expected to fill in the details. Used only for testing.
     * @return a new instance of an unsafe folder.
     */
    @VisibleForTesting
    public static Folder newUnsafeInstance() {
        return new Folder();
    }

    public static final ClassLoaderCreator<Folder> CREATOR = new ClassLoaderCreator<Folder>() {
        @Override
        public Folder createFromParcel(Parcel source) {
            return new Folder(source, null);
        }

        @Override
        public Folder createFromParcel(Parcel source, ClassLoader loader) {
            return new Folder(source, loader);
        }

        @Override
        public Folder[] newArray(int size) {
            return new Folder[size];
        }
    };

    @Override
    public int describeContents() {
        // Return a sort of version number for this parcelable folder. Starting with zero.
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Folder)) {
            return false;
        }
        return Objects.equal(folderUri, ((Folder) o).folderUri);
    }

    @Override
    public int hashCode() {
        return folderUri == null ? 0 : folderUri.hashCode();
    }

    @Override
    public String toString() {
        // log extra info at DEBUG level or finer
        final StringBuilder sb = new StringBuilder("[folder id=");
        sb.append(id);
        if (LogUtils.isLoggable(LOG_TAG, LogUtils.DEBUG)) {
            sb.append(", uri=");
            sb.append(folderUri);
            sb.append(", name=");
            sb.append(name);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public int compareTo(Folder other) {
        return name.compareToIgnoreCase(other.name);
    }

    /**
     * Returns a boolean indicating whether network activity (sync) is occuring for this folder.
     */
    public boolean isSyncInProgress() {
        return UIProvider.SyncStatus.isSyncInProgress(syncStatus);
    }

    public boolean supportsCapability(int capability) {
        return (capabilities & capability) != 0;
    }

    // Show black text on a transparent swatch for system folders, effectively hiding the
    // swatch (see bug 2431925).
    public static void setFolderBlockColor(Folder folder, View colorBlock) {
        if (colorBlock == null) {
            return;
        }
        boolean showBg =
                !TextUtils.isEmpty(folder.bgColor) && (folder.type & FolderType.INBOX_SECTION) == 0;
        final int backgroundColor = showBg ? Integer.parseInt(folder.bgColor) : 0;
        if (backgroundColor == Utils.getDefaultFolderBackgroundColor(colorBlock.getContext())) {
            showBg = false;
        }
        if (!showBg) {
            colorBlock.setBackgroundDrawable(null);
            colorBlock.setVisibility(View.GONE);
        } else {
            PaintDrawable paintDrawable = new PaintDrawable();
            paintDrawable.getPaint().setColor(backgroundColor);
            colorBlock.setBackgroundDrawable(paintDrawable);
            colorBlock.setVisibility(View.VISIBLE);
        }
    }

    public static void setIcon(Folder folder, ImageView iconView) {
        if (iconView == null) {
            return;
        }
        final int icon = folder.iconResId;
        if (icon > 0) {
            iconView.setImageResource(icon);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }
    }

    /**
     * Return if the type of the folder matches a provider defined folder.
     */
    public boolean isProviderFolder() {
        return !isType(UIProvider.FolderType.DEFAULT);
    }

    public int getBackgroundColor(int defaultColor) {
        return bgColor != null ? bgColorInt : defaultColor;
    }

    public int getForegroundColor(int defaultColor) {
        return fgColor != null ? fgColorInt : defaultColor;
    }

    /**
     * Get just the uri's from an arraylist of folders.
     */
    public static String[] getUriArray(List<Folder> folders) {
        if (folders == null || folders.size() == 0) {
            return new String[0];
        }
        final String[] folderUris = new String[folders.size()];
        int i = 0;
        for (Folder folder : folders) {
            folderUris[i] = folder.folderUri.toString();
            i++;
        }
        return folderUris;
    }

    /**
     * Returns a boolean indicating whether this Folder object has been initialized
     */
    public boolean isInitialized() {
        return !name.equals(FOLDER_UNINITIALIZED) && conversationListUri != null &&
                !NULL_STRING_URI.equals(conversationListUri.toString());
    }

    public boolean isType(final int folderType) {
        return isType(type, folderType);
    }

    /**
     * Checks if <code>typeMask</code> is of the specified {@link FolderType}
     *
     * @return <code>true</code> if the mask contains the specified
     *         {@link FolderType}, <code>false</code> otherwise
     */
    public static boolean isType(final int typeMask, final int folderType) {
        return (typeMask & folderType) != 0;
    }

    public boolean isInbox() {
        return isType(UIProvider.FolderType.INBOX);
    }

    /**
     * Return if this is the trash folder.
     */
    public boolean isTrash() {
        return isType(UIProvider.FolderType.TRASH);
    }

    /**
     * Return if this is a draft folder.
     */
    public boolean isDraft() {
        return isType(UIProvider.FolderType.DRAFT);
    }

    /**
     * Whether this folder supports only showing important messages.
     */
    public boolean isImportantOnly() {
        return supportsCapability(
                UIProvider.FolderCapabilities.ONLY_IMPORTANT);
    }

    /**
     * Whether this is the special folder just used to display all mail for an account.
     */
    public boolean isViewAll() {
        return isType(UIProvider.FolderType.ALL_MAIL);
    }

    /**
     * @return a non-user facing English string describing this folder's type
     */
    public String getTypeDescription() {
        final String desc;
        if (isType(FolderType.INBOX_SECTION)) {
            desc = "inbox_section:" + persistentId;
        } else if (isInbox()) {
            desc = "inbox:" + persistentId;
        } else if (isDraft()) {
            desc = "draft";
        } else if (isImportantOnly()) {
            desc = "important";
        } else if (isType(FolderType.OUTBOX)) {
            desc = "outbox";
        } else if (isType(FolderType.SENT)) {
            desc = "sent";
        } else if (isType(FolderType.SPAM)) {
            desc = "spam";
        } else if (isType(FolderType.STARRED)) {
            desc = "starred";
        } else if (isTrash()) {
            desc = "trash";
        } else if (isType(FolderType.UNREAD)) {
            desc = "unread";
        } else if (isViewAll()) {
            desc = "all_mail";
        } else if (isProviderFolder()) {
            desc = "other:" + persistentId;
        } else {
            desc = "user_folder";
        }
        return desc;
    }

    /**
     * True if the previous sync was successful, false otherwise.
     * @return
     */
    public final boolean wasSyncSuccessful() {
        return ((lastSyncResult & 0x0f) == UIProvider.LastSyncResult.SUCCESS);
    }

    /**
     * Returns true if unread count should be suppressed for this folder. This is done for folders
     * where the unread count is meaningless: trash or drafts, for instance.
     * @return true if unread count should be suppressed for this object.
     */
    public final boolean isUnreadCountHidden() {
        return (isDraft() || isTrash() || isType(FolderType.OUTBOX));
    }

    /**
     * This method is only used for parsing folders out of legacy intent extras, and only the
     * folderUri and conversationListUri fields are actually read before the object is discarded.
     * TODO: replace this with a parsing function that just directly returns those values
     * @param inString UR8 or earlier EXTRA_FOLDER intent extra string
     * @return Constructed folder object
     */
    @Deprecated
    public static Folder fromString(String inString) {
        if (TextUtils.isEmpty(inString)) {
            return null;
        }
        final Folder f = new Folder();
        int indexOf = inString.indexOf(SPLITTER);
        int id = -1;
        if (indexOf != -1) {
            id = Integer.valueOf(inString.substring(0, indexOf));
        } else {
            // If no separator was found, we can't parse this folder and the
            // TextUtils.split call would also fail. Return null.
            return null;
        }
        final String[] split = TextUtils.split(inString, SPLITTER_REGEX);
        if (split.length < 20) {
            LogUtils.e(LOG_TAG, "split.length %d", split.length);
            return null;
        }
        f.id = id;
        int index = 1;
        f.folderUri = new FolderUri(Folder.getValidUri(split[index++]));
        f.name = split[index++];
        f.hasChildren = Integer.parseInt(split[index++]) != 0;
        f.capabilities = Integer.parseInt(split[index++]);
        f.syncWindow = Integer.parseInt(split[index++]);
        f.conversationListUri = getValidUri(split[index++]);
        f.childFoldersListUri = getValidUri(split[index++]);
        f.unreadCount = Integer.parseInt(split[index++]);
        f.totalCount = Integer.parseInt(split[index++]);
        f.refreshUri = getValidUri(split[index++]);
        f.syncStatus = Integer.parseInt(split[index++]);
        f.lastSyncResult = Integer.parseInt(split[index++]);
        f.type = Integer.parseInt(split[index++]);
        f.iconResId = Integer.parseInt(split[index++]);
        f.bgColor = split[index++];
        f.fgColor = split[index++];
        if (f.bgColor != null) {
            f.bgColorInt = Integer.parseInt(f.bgColor);
        }
        if (f.fgColor != null) {
            f.fgColorInt = Integer.parseInt(f.fgColor);
        }
        f.loadMoreUri = getValidUri(split[index++]);
        f.hierarchicalDesc = split[index++];
        f.parent = Folder.getValidUri(split[index++]);
        f.unreadSenders = null;

        return f;
    }

    private static Uri getValidUri(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return null;
        }
        return Uri.parse(uri);
    }
}
