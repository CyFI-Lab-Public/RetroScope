/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.providers.partnerbookmarks;

import android.content.ContentUris;
import android.net.Uri;

/**
 * <p>
 * The contract between the partner bookmarks provider and applications.
 * Contains the definition for the supported URIs and columns.
 * </p>
 * <p>
 * Authority URI: content://com.android.partnerbookmarks
 * </p>
 * <p>
 * Partner bookmarks URI: content://com.android.partnerbookmarks/bookmarks
 * </p>
 * <p>
 * If the provider is found, and the set of bookmarks is non-empty, exactly one
 * top-level folder with “parent” set to {@link #BOOKMARK_PARENT_ROOT_ID}
 * shall be provided; more than one bookmark with “parent” set to
 * {@link #BOOKMARK_PARENT_ROOT_ID} will cause the import to fail.
 * </p>
 */
public class PartnerBookmarksContract {
    /** The authority for the partner bookmarks provider */
    public static final String AUTHORITY = "com.android.partnerbookmarks";

    /** A content:// style uri to the authority for the partner bookmarks provider */
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * A parameter for use when querying any table that allows specifying
     * a limit on the number of rows returned.
     */
    public static final String PARAM_LIMIT = "limit";

    /**
     * A parameter for use when querying any table that allows specifying
     * grouping of the rows returned.
     */
    public static final String PARAM_GROUP_BY = "groupBy";

    /**
     * The bookmarks table, which holds the partner bookmarks.
     */
    public static final class Bookmarks {
        /**
         * This utility class cannot be instantiated.
         */
        private Bookmarks() {}

        /**
         * The content:// style URI for this table
         */
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, "bookmarks");

        /**
         * The content:// style URI for the root partner bookmarks folder
         */
        public static final Uri CONTENT_URI_PARTNER_BOOKMARKS_FOLDER =
                Uri.withAppendedPath(CONTENT_URI, "folder");

        /**
         * Builds a URI that points to a specific folder.
         * @param folderId the ID of the folder to point to
         */
        public static final Uri buildFolderUri(long folderId) {
            return ContentUris.withAppendedId(CONTENT_URI_PARTNER_BOOKMARKS_FOLDER, folderId);
        }

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of bookmarks.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/partnerbookmark";

        /**
         * The MIME type of a {@link #CONTENT_URI} of a single bookmark.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/partnerbookmark";

        /**
         * Used in {@link #TYPE} column and indicates the row is a bookmark.
         */
        public static final int BOOKMARK_TYPE_BOOKMARK = 1;

        /**
         * Used in {@link #TYPE} column and indicates the row is a folder.
         */
        public static final int BOOKMARK_TYPE_FOLDER = 2;

        /**
         * Used in {@link #PARENT} column and indicates the row doesn't have a parent.
         */
        public static final int BOOKMARK_PARENT_ROOT_ID = 0;

        /**
         * The type of the item.
         * <p>Type: INTEGER</p>
         * <p>Allowed values are:</p>
         * <p>
         * <ul>
         * <li>{@link #BOOKMARK_TYPE_BOOKMARK}</li>
         * <li>{@link #BOOKMARK_TYPE_FOLDER}</li>
         * </ul>
         * </p>
         */
        public static final String TYPE = "type";

        /**
         * The unique ID for a row.  Cannot be BOOKMARK_PARENT_ROOT_ID.
         * <p>Type: INTEGER (long)</p>
         */
        public static final String ID = "_id";

        /**
         * This column is valid when the row is not a folder.
         * <p>Type: TEXT (URL)</p>
         */
        public static final String URL = "url";

        /**
         * The user visible title.
         * <p>Type: TEXT</p>
         */
        public static final String TITLE = "title";

        /**
         * The favicon of the bookmark, may be NULL.
         * Must decode via {@link BitmapFactory#decodeByteArray}.
         * <p>Type: BLOB (image)</p>
         */
        public static final String FAVICON = "favicon";

        /**
         * The touch icon for the web page, may be NULL.
         * Must decode via {@link BitmapFactory#decodeByteArray}.
         * <p>Type: BLOB (image)</p>
         */
        public static final String TOUCHICON = "touchicon";

        /**
         * The ID of the parent folder. BOOKMARK_PARENT_ROOT_ID is the root folder.
         * <p>Type: INTEGER (long) (reference to item in the same table)</p>
         */
        public static final String PARENT = "parent";
    }
}
