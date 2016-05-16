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
package com.android.photos.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.photos.data.PhotoProvider.Accounts;
import com.android.photos.data.PhotoProvider.Albums;
import com.android.photos.data.PhotoProvider.Metadata;
import com.android.photos.data.PhotoProvider.Photos;

import java.util.ArrayList;
import java.util.List;

/**
 * Used in PhotoProvider to create and access the database containing
 * information about photo and video information stored on the server.
 */
public class PhotoDatabase extends SQLiteOpenHelper {
    @SuppressWarnings("unused")
    private static final String TAG = PhotoDatabase.class.getSimpleName();
    static final int DB_VERSION = 3;

    private static final String SQL_CREATE_TABLE = "CREATE TABLE ";

    private static final String[][] CREATE_PHOTO = {
        { Photos._ID, "INTEGER PRIMARY KEY AUTOINCREMENT" },
        // Photos.ACCOUNT_ID is a foreign key to Accounts._ID
        { Photos.ACCOUNT_ID, "INTEGER NOT NULL" },
        { Photos.WIDTH, "INTEGER NOT NULL" },
        { Photos.HEIGHT, "INTEGER NOT NULL" },
        { Photos.DATE_TAKEN, "INTEGER NOT NULL" },
        // Photos.ALBUM_ID is a foreign key to Albums._ID
        { Photos.ALBUM_ID, "INTEGER" },
        { Photos.MIME_TYPE, "TEXT NOT NULL" },
        { Photos.TITLE, "TEXT" },
        { Photos.DATE_MODIFIED, "INTEGER" },
        { Photos.ROTATION, "INTEGER" },
    };

    private static final String[][] CREATE_ALBUM = {
        { Albums._ID, "INTEGER PRIMARY KEY AUTOINCREMENT" },
        // Albums.ACCOUNT_ID is a foreign key to Accounts._ID
        { Albums.ACCOUNT_ID, "INTEGER NOT NULL" },
        // Albums.PARENT_ID is a foreign key to Albums._ID
        { Albums.PARENT_ID, "INTEGER" },
        { Albums.ALBUM_TYPE, "TEXT" },
        { Albums.VISIBILITY, "INTEGER NOT NULL" },
        { Albums.LOCATION_STRING, "TEXT" },
        { Albums.TITLE, "TEXT NOT NULL" },
        { Albums.SUMMARY, "TEXT" },
        { Albums.DATE_PUBLISHED, "INTEGER" },
        { Albums.DATE_MODIFIED, "INTEGER" },
        createUniqueConstraint(Albums.PARENT_ID, Albums.TITLE),
    };

    private static final String[][] CREATE_METADATA = {
        { Metadata._ID, "INTEGER PRIMARY KEY AUTOINCREMENT" },
        // Metadata.PHOTO_ID is a foreign key to Photos._ID
        { Metadata.PHOTO_ID, "INTEGER NOT NULL" },
        { Metadata.KEY, "TEXT NOT NULL" },
        { Metadata.VALUE, "TEXT NOT NULL" },
        createUniqueConstraint(Metadata.PHOTO_ID, Metadata.KEY),
    };

    private static final String[][] CREATE_ACCOUNT = {
        { Accounts._ID, "INTEGER PRIMARY KEY AUTOINCREMENT" },
        { Accounts.ACCOUNT_NAME, "TEXT UNIQUE NOT NULL" },
    };

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db, Accounts.TABLE, getAccountTableDefinition());
        createTable(db, Albums.TABLE, getAlbumTableDefinition());
        createTable(db, Photos.TABLE, getPhotoTableDefinition());
        createTable(db, Metadata.TABLE, getMetadataTableDefinition());
    }

    public PhotoDatabase(Context context, String dbName, int dbVersion) {
        super(context, dbName, null, dbVersion);
    }

    public PhotoDatabase(Context context, String dbName) {
        super(context, dbName, null, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        recreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        recreate(db);
    }

    private void recreate(SQLiteDatabase db) {
        dropTable(db, Metadata.TABLE);
        dropTable(db, Photos.TABLE);
        dropTable(db, Albums.TABLE);
        dropTable(db, Accounts.TABLE);
        onCreate(db);
    }

    protected List<String[]> getAlbumTableDefinition() {
        return tableCreationStrings(CREATE_ALBUM);
    }

    protected List<String[]> getPhotoTableDefinition() {
        return tableCreationStrings(CREATE_PHOTO);
    }

    protected List<String[]> getMetadataTableDefinition() {
        return tableCreationStrings(CREATE_METADATA);
    }

    protected List<String[]> getAccountTableDefinition() {
        return tableCreationStrings(CREATE_ACCOUNT);
    }

    protected static void createTable(SQLiteDatabase db, String table, List<String[]> columns) {
        StringBuilder create = new StringBuilder(SQL_CREATE_TABLE);
        create.append(table).append('(');
        boolean first = true;
        for (String[] column : columns) {
            if (!first) {
                create.append(',');
            }
            first = false;
            for (String val: column) {
                create.append(val).append(' ');
            }
        }
        create.append(')');
        db.beginTransaction();
        try {
            db.execSQL(create.toString());
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    protected static String[] createUniqueConstraint(String column1, String column2) {
        return new String[] {
                "UNIQUE(", column1, ",", column2, ")"
        };
    }

    protected static List<String[]> tableCreationStrings(String[][] createTable) {
        ArrayList<String[]> create = new ArrayList<String[]>(createTable.length);
        for (String[] line: createTable) {
            create.add(line);
        }
        return create;
    }

    protected static void addToTable(List<String[]> createTable, String[][] columns, String[][] constraints) {
        if (columns != null) {
            for (String[] column: columns) {
                createTable.add(0, column);
            }
        }
        if (constraints != null) {
            for (String[] constraint: constraints) {
                createTable.add(constraint);
            }
        }
    }

    protected static void dropTable(SQLiteDatabase db, String table) {
        db.beginTransaction();
        try {
            db.execSQL("drop table if exists " + table);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
