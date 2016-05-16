/*
 * Copyright (C) 2011 The Android Open Source Project

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
 * limitations under the License
 */

package com.android.providers.contacts;

import static android.Manifest.permission.ADD_VOICEMAIL;
import static com.android.providers.contacts.Manifest.permission.READ_WRITE_ALL_VOICEMAIL;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.provider.CallLog.Calls;
import android.provider.VoicemailContract;
import android.provider.VoicemailContract.Status;
import android.provider.VoicemailContract.Voicemails;
import android.util.Log;

import com.android.common.io.MoreCloseables;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.util.DbQueryUtils;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An implementation of {@link DatabaseModifier} for voicemail related tables which additionally
 * generates necessary notifications after the modification operation is performed.
 * The class generates notifications for both voicemail as well as call log URI depending on which
 * of then got affected by the change.
 */
public class DbModifierWithNotification implements DatabaseModifier {
    private static final String TAG = "DbModifierWithVmNotification";

    private static final String[] PROJECTION = new String[] {
            VoicemailContract.SOURCE_PACKAGE_FIELD
    };
    private static final int SOURCE_PACKAGE_COLUMN_INDEX = 0;
    private static final String NON_NULL_SOURCE_PACKAGE_SELECTION =
            VoicemailContract.SOURCE_PACKAGE_FIELD + " IS NOT NULL";

    private final String mTableName;
    private final SQLiteDatabase mDb;
    private final InsertHelper mInsertHelper;
    private final Context mContext;
    private final Uri mBaseUri;
    private final boolean mIsCallsTable;
    private final VoicemailPermissions mVoicemailPermissions;

    public DbModifierWithNotification(String tableName, SQLiteDatabase db, Context context) {
        this(tableName, db, null, context);
    }

    public DbModifierWithNotification(String tableName, InsertHelper insertHelper,
            Context context) {
        this(tableName, null, insertHelper, context);
    }

    private DbModifierWithNotification(String tableName, SQLiteDatabase db,
            InsertHelper insertHelper, Context context) {
        mTableName = tableName;
        mDb = db;
        mInsertHelper = insertHelper;
        mContext = context;
        mBaseUri = mTableName.equals(Tables.VOICEMAIL_STATUS) ?
                Status.CONTENT_URI : Voicemails.CONTENT_URI;
        mIsCallsTable = mTableName.equals(Tables.CALLS);
        mVoicemailPermissions = new VoicemailPermissions(mContext);
    }

    @Override
    public long insert(String table, String nullColumnHack, ContentValues values) {
        Set<String> packagesModified = getModifiedPackages(values);
        long rowId = mDb.insert(table, nullColumnHack, values);
        if (rowId > 0 && packagesModified.size() != 0) {
            notifyVoicemailChangeOnInsert(ContentUris.withAppendedId(mBaseUri, rowId),
                    packagesModified);
        }
        if (rowId > 0 && mIsCallsTable) {
            notifyCallLogChange();
        }
        return rowId;
    }

    @Override
    public long insert(ContentValues values) {
        Set<String> packagesModified = getModifiedPackages(values);
        long rowId = mInsertHelper.insert(values);
        if (rowId > 0 && packagesModified.size() != 0) {
            notifyVoicemailChangeOnInsert(
                    ContentUris.withAppendedId(mBaseUri, rowId), packagesModified);
        }
        if (rowId > 0 && mIsCallsTable) {
            notifyCallLogChange();
        }
        return rowId;
    }

    private void notifyCallLogChange() {
        mContext.getContentResolver().notifyChange(Calls.CONTENT_URI, null, false);
    }

    private void notifyVoicemailChangeOnInsert(Uri notificationUri, Set<String> packagesModified) {
        if (mIsCallsTable) {
            notifyVoicemailChange(notificationUri, packagesModified,
                    VoicemailContract.ACTION_NEW_VOICEMAIL, Intent.ACTION_PROVIDER_CHANGED);
        } else {
            notifyVoicemailChange(notificationUri, packagesModified,
                    Intent.ACTION_PROVIDER_CHANGED);
        }
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        Set<String> packagesModified = getModifiedPackages(whereClause, whereArgs);
        packagesModified.addAll(getModifiedPackages(values));
        int count = mDb.update(table, values, whereClause, whereArgs);
        if (count > 0 && packagesModified.size() != 0) {
            notifyVoicemailChange(mBaseUri, packagesModified, Intent.ACTION_PROVIDER_CHANGED);
        }
        if (count > 0 && mIsCallsTable) {
            notifyCallLogChange();
        }
        return count;
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        Set<String> packagesModified = getModifiedPackages(whereClause, whereArgs);
        int count = mDb.delete(table, whereClause, whereArgs);
        if (count > 0 && packagesModified.size() != 0) {
            notifyVoicemailChange(mBaseUri, packagesModified, Intent.ACTION_PROVIDER_CHANGED);
        }
        if (count > 0 && mIsCallsTable) {
            notifyCallLogChange();
        }
        return count;
    }

    /**
     * Returns the set of packages affected when a modify operation is run for the specified
     * where clause. When called from an insert operation an empty set returned by this method
     * implies (indirectly) that this does not affect any voicemail entry, as a voicemail entry is
     * always expected to have the source package field set.
     */
    private Set<String> getModifiedPackages(String whereClause, String[] whereArgs) {
        Set<String> modifiedPackages = new HashSet<String>();
        Cursor cursor = mDb.query(mTableName, PROJECTION,
                DbQueryUtils.concatenateClauses(NON_NULL_SOURCE_PACKAGE_SELECTION, whereClause),
                whereArgs, null, null, null);
        while(cursor.moveToNext()) {
            modifiedPackages.add(cursor.getString(SOURCE_PACKAGE_COLUMN_INDEX));
        }
        MoreCloseables.closeQuietly(cursor);
        return modifiedPackages;
    }

    /**
     * Returns the source package that gets affected (in an insert/update operation) by the supplied
     * content values. An empty set returned by this method also implies (indirectly) that this does
     * not affect any voicemail entry, as a voicemail entry is always expected to have the source
     * package field set.
     */
    private Set<String> getModifiedPackages(ContentValues values) {
        Set<String> impactedPackages = new HashSet<String>();
        if(values.containsKey(VoicemailContract.SOURCE_PACKAGE_FIELD)) {
            impactedPackages.add(values.getAsString(VoicemailContract.SOURCE_PACKAGE_FIELD));
        }
        return impactedPackages;
    }

    private void notifyVoicemailChange(Uri notificationUri, Set<String> modifiedPackages,
            String... intentActions) {
        // Notify the observers.
        // Must be done only once, even if there are multiple broadcast intents.
        mContext.getContentResolver().notifyChange(notificationUri, null, true);
        Collection<String> callingPackages = getCallingPackages();
        // Now fire individual intents.
        for (String intentAction : intentActions) {
            // self_change extra should be included only for provider_changed events.
            boolean includeSelfChangeExtra = intentAction.equals(Intent.ACTION_PROVIDER_CHANGED);
            for (ComponentName component :
                    getBroadcastReceiverComponents(intentAction, notificationUri)) {
                // Ignore any package that is not affected by the change and don't have full access
                // either.
                if (!modifiedPackages.contains(component.getPackageName()) &&
                        !mVoicemailPermissions.packageHasFullAccess(component.getPackageName())) {
                    continue;
                }

                Intent intent = new Intent(intentAction, notificationUri);
                intent.setComponent(component);
                if (includeSelfChangeExtra && callingPackages != null) {
                    intent.putExtra(VoicemailContract.EXTRA_SELF_CHANGE,
                            callingPackages.contains(component.getPackageName()));
                }
                String permissionNeeded = modifiedPackages.contains(component.getPackageName()) ?
                        ADD_VOICEMAIL : READ_WRITE_ALL_VOICEMAIL;
                mContext.sendBroadcast(intent, permissionNeeded);
                Log.v(TAG, String.format("Sent intent. act:%s, url:%s, comp:%s, perm:%s," +
                        " self_change:%s", intent.getAction(), intent.getData(),
                        component.getClassName(), permissionNeeded,
                        intent.hasExtra(VoicemailContract.EXTRA_SELF_CHANGE) ?
                                intent.getBooleanExtra(VoicemailContract.EXTRA_SELF_CHANGE, false) :
                                        null));
            }
        }
    }

    /** Determines the components that can possibly receive the specified intent. */
    private List<ComponentName> getBroadcastReceiverComponents(String intentAction, Uri uri) {
        Intent intent = new Intent(intentAction, uri);
        List<ComponentName> receiverComponents = new ArrayList<ComponentName>();
        // For broadcast receivers ResolveInfo.activityInfo is the one that is populated.
        for (ResolveInfo resolveInfo :
                mContext.getPackageManager().queryBroadcastReceivers(intent, 0)) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            receiverComponents.add(new ComponentName(activityInfo.packageName, activityInfo.name));
        }
        return receiverComponents;
    }

    /**
     * Returns the package names of the calling process. If the calling process has more than
     * one packages, this returns them all
     */
    private Collection<String> getCallingPackages() {
        int caller = Binder.getCallingUid();
        if (caller == 0) {
            return null;
        }
        return Lists.newArrayList(mContext.getPackageManager().getPackagesForUid(caller));
    }
}
