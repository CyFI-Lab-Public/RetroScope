/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ti.fmrxapp;


import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter
{
    public static final String KEY_ROWID = "_id";
    public static final String ITEM_KEY = "id";
    public static final String ITEM_VALUE = "channel";
    public static final String ITEM_NAME = "name";
    private static final String TAG = "DBAdapter";
    private static final String DATABASE_NAME = "presets.db";
    private static final String DATABASE_TABLE = "presets";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE =
       "create table presets (_id integer primary key autoincrement, "
       + "id text not null, channel text not null, "
       + "name text not null);";

    private final Context context;

    private DatabaseHelper DBHelper;
    private SQLiteDatabase db;

    public DBAdapter(Context ctx)
    {
       this.context = ctx;
       DBHelper = new DatabaseHelper(context);
        Log.d(TAG," DBAdapter const");
    }

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
       DatabaseHelper(Context context)
       {

          super(context, DATABASE_NAME, null, DATABASE_VERSION);
           Log.d(TAG," DatabaseHelper con");
       }

       @Override
       public void onCreate(SQLiteDatabase db)
       {
           Log.d(TAG," DatabaseHelper onCreate");
          db.execSQL(DATABASE_CREATE);
       }

       @Override
       public void onUpgrade(SQLiteDatabase db, int oldVersion,
       int newVersion)
       {
          Log.w(TAG, "Upgrading database from version " + oldVersion
                + " to "
                + newVersion + ", which will destroy all old data");
          db.execSQL("DROP TABLE IF EXISTS presets");
          onCreate(db);
       }
    }

    //---opens the database---
    public DBAdapter open() throws SQLException
    {
       db = DBHelper.getWritableDatabase();
        Log.d(TAG," DatabaseHelper open");
       return this;
    }

    //---closes the database---
    public void close()
    {
       DBHelper.close();
        Log.d(TAG," DatabaseHelper close");
    }

    //---insert a station into the database---
    public long insertStation(String itemkey, String itemValue, String itemName)
    {

        Log.d(TAG," DatabaseHelper insertStation");
       ContentValues initialValues = new ContentValues();
       initialValues.put(ITEM_KEY, itemkey);
       initialValues.put(ITEM_VALUE, itemValue);
       initialValues.put(ITEM_NAME, itemName);
       return db.insert(DATABASE_TABLE, null, initialValues);
    }

    //---deletes a particular station---
    public boolean deleteStation(long rowId)
    {
        Log.d(TAG," DatabaseHelper deleteStation");
       return db.delete(DATABASE_TABLE, KEY_ROWID +
               "=" + rowId, null) > 0;
    }

    //---retrieves all the Stations---
    public Cursor getAllStations()
    {
        Log.d(TAG," DatabaseHelper getAllStations");
       return db.query(DATABASE_TABLE, new String[] {
               KEY_ROWID,
               ITEM_KEY,
               ITEM_VALUE,
               ITEM_NAME},
             null,
             null,
             null,
             null,
             null);
    }

    //---retrieves a particular station---
    public Cursor getStation(long rowId) throws SQLException
    {

        Log.d(TAG," DatabaseHelper getStation");
       Cursor mCursor =
             db.query(true, DATABASE_TABLE, new String[] {
                     KEY_ROWID,
                     ITEM_KEY,
                     ITEM_VALUE,
                     ITEM_NAME},
                     KEY_ROWID + "=" + rowId,
                     null,
                     null,
                     null,
                     null,
                     null);
       if (mCursor != null) {
          mCursor.moveToFirst();
       }
       return mCursor;
    }

    //---updates a station---
    public boolean updateStation(int index, String itemKey,
    String itemValue, String itemName)
    {
        Log.d(TAG," DatabaseHelper updateStation");
       ContentValues args = new ContentValues();
       args.put(ITEM_KEY, itemKey);
       args.put(ITEM_VALUE, itemValue);
       args.put(ITEM_NAME, itemName);
       return db.update(DATABASE_TABLE, args,
               KEY_ROWID + "=" + index, null) > 0;
    }


}
