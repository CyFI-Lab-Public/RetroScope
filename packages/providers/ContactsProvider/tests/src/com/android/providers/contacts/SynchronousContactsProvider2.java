/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.providers.contacts;

import android.accounts.Account;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import junit.framework.Assert;

import java.util.Locale;

/**
 * A version of {@link ContactsProvider2} class that performs aggregation
 * synchronously and wipes all data at construction time.
 */
public class SynchronousContactsProvider2 extends ContactsProvider2 {
    public static final String READ_ONLY_ACCOUNT_TYPE = "ro";

    private static Boolean sDataWiped = false;
    private static ContactsDatabaseHelper sDbHelper;
    private boolean mDataWipeEnabled = true;
    private Account mAccount;
    private boolean mNetworkNotified;
    private boolean mIsPhone = true;
    private boolean mIsVoiceCapable = true;

    @Override
    protected ContactsDatabaseHelper getDatabaseHelper(final Context context) {
        if (sDbHelper == null) {
            sDbHelper = ContactsDatabaseHelper.getNewInstanceForTest(context);
        }
        return sDbHelper;
    }

    @Override
    public ProfileProvider newProfileProvider() {
        return new SynchronousProfileProvider(this);
    }

    public void setDataWipeEnabled(boolean flag) {
        mDataWipeEnabled = flag;
    }

    @Override
    public void onBegin() {
        super.onBegin();
        mNetworkNotified = false;
    }

    @Override
    protected void notifyChange(boolean syncToNetwork) {
        mNetworkNotified |= syncToNetwork;
    }

    public boolean isNetworkNotified() {
        return mNetworkNotified;
    }

    public void setIsPhone(boolean flag) {
        mIsPhone = flag;
    }

    @Override
    public boolean isPhone() {
        return mIsPhone;
    }

    public void setIsVoiceCapable(boolean flag) {
        mIsVoiceCapable = flag;
    }

    @Override
    public boolean isVoiceCapable() {
        return mIsVoiceCapable;
    }

    @Override
    public boolean onCreate() {
        boolean created = super.onCreate();
        if (mDataWipeEnabled) {
            synchronized (sDataWiped) {
                if (!sDataWiped) {
                    sDataWiped = true;
                    wipeData();
                }
            }
        }
        return created;
    }

    @Override
    protected boolean shouldThrowExceptionForInitializationError() {
        return true;
    }

    /** We'll use a static size for unit tests */
    @Override
    public int getMaxThumbnailDim() {
        return 96;
    }

    /** We'll use a static size for unit tests */
    @Override
    public int getMaxDisplayPhotoDim() {
        return 256;
    }

    @Override
    protected void scheduleBackgroundTask(int task) {
        performBackgroundTask(task, null);
    }

    @Override
    protected void scheduleBackgroundTask(int task, Object arg) {
        performBackgroundTask(task, arg);
    }

    @Override
    protected void updateLocaleInBackground() {
    }

    @Override
    protected void updateDirectoriesInBackground(boolean rescan) {
    }

    @Override
    protected Account getDefaultAccount() {
        if (mAccount == null) {
            mAccount = new Account("androidtest@gmail.com", "com.google");
        }
        return mAccount;
    }

    @Override
    protected boolean isContactsAccount(Account account) {
        return true;
    }

    /**
     * Creates a mock PhotoPriorityResolver
     */
    @Override
    PhotoPriorityResolver createPhotoPriorityResolver(Context context) {
        return new PhotoPriorityResolver(context) {
            @Override
            public synchronized int getPhotoPriority(String accountType) {
                if ("cupcake".equals(accountType)) {
                    return 3;
                }
                if ("donut".equals(accountType)) {
                    return 2;
                }
                if ("froyo".equals(accountType)) {
                    return 1;
                }
                return 0;
            }
        };
    }

    @Override
    protected Locale getLocale() {
        return Locale.US;
    }

    @Override
    public boolean isWritableAccountWithDataSet(String accountType) {
        return !READ_ONLY_ACCOUNT_TYPE.equals(accountType);
    }

    public void prepareForFullAggregation(int maxContact) {
        SQLiteDatabase db = getDatabaseHelper(getContext()).getWritableDatabase();
        db.execSQL("UPDATE raw_contacts SET aggregation_mode=0,aggregation_needed=1;");
        long rowId =
            db.compileStatement("SELECT _id FROM raw_contacts LIMIT 1 OFFSET " + maxContact)
                .simpleQueryForLong();
        db.execSQL("DELETE FROM raw_contacts WHERE _id > " + rowId + ";");
    }

    public long getRawContactCount() {
        SQLiteDatabase db = getDatabaseHelper(getContext()).getReadableDatabase();
        return db.compileStatement("SELECT COUNT(*) FROM raw_contacts").simpleQueryForLong();
    }

    public long getContactCount() {
        SQLiteDatabase db = getDatabaseHelper(getContext()).getReadableDatabase();
        return db.compileStatement("SELECT COUNT(*) FROM contacts").simpleQueryForLong();
    }

    @Override
    public void wipeData() {
        Log.i(TAG, "wipeData");
        super.wipeData();
        SQLiteDatabase db = getDatabaseHelper(getContext()).getWritableDatabase();
        db.execSQL("replace into SQLITE_SEQUENCE (name,seq) values('raw_contacts', 42)");
        db.execSQL("replace into SQLITE_SEQUENCE (name,seq) values('contacts', 2009)");
        db.execSQL("replace into SQLITE_SEQUENCE (name,seq) values('data', 777)");

        getContactDirectoryManagerForTest().scanAllPackages();
    }

    // Flags to remember which transaction callback has been called for which mode.
    private boolean mOnBeginTransactionInternalCalledInProfileMode;
    private boolean mOnCommitTransactionInternalCalledInProfileMode;
    private boolean mOnRollbackTransactionInternalCalledInProfileMode;

    private boolean mOnBeginTransactionInternalCalledInContactMode;
    private boolean mOnCommitTransactionInternalCalledInContactMode;
    private boolean mOnRollbackTransactionInternalCalledInContactMode;

    public void resetTrasactionCallbackCalledFlags() {
        mOnBeginTransactionInternalCalledInProfileMode = false;
        mOnCommitTransactionInternalCalledInProfileMode = false;
        mOnRollbackTransactionInternalCalledInProfileMode = false;

        mOnBeginTransactionInternalCalledInContactMode = false;
        mOnCommitTransactionInternalCalledInContactMode = false;
        mOnRollbackTransactionInternalCalledInContactMode = false;
    }

    @Override
    protected void onBeginTransactionInternal(boolean forProfile) {
        super.onBeginTransactionInternal(forProfile);
        if (forProfile) {
            mOnBeginTransactionInternalCalledInProfileMode = true;
        } else {
            mOnBeginTransactionInternalCalledInContactMode = true;
        }
    }

    @Override
    protected void onCommitTransactionInternal(boolean forProfile) {
        super.onCommitTransactionInternal(forProfile);
        if (forProfile) {
            mOnCommitTransactionInternalCalledInProfileMode = true;
        } else {
            mOnCommitTransactionInternalCalledInContactMode = true;
        }
    }

    @Override
    protected void onRollbackTransactionInternal(boolean forProfile) {
        super.onRollbackTransactionInternal(forProfile);
        if (forProfile) {
            mOnRollbackTransactionInternalCalledInProfileMode = true;
        } else {
            mOnRollbackTransactionInternalCalledInContactMode = true;
        }
    }

    public void assertCommitTransactionCalledForProfileMode() {
        Assert.assertTrue("begin", mOnBeginTransactionInternalCalledInProfileMode);
        Assert.assertTrue("commit", mOnCommitTransactionInternalCalledInProfileMode);
        Assert.assertFalse("rollback", mOnRollbackTransactionInternalCalledInProfileMode);
    }

    public void assertRollbackTransactionCalledForProfileMode() {
        Assert.assertTrue("begin", mOnBeginTransactionInternalCalledInProfileMode);
        Assert.assertFalse("commit", mOnCommitTransactionInternalCalledInProfileMode);
        Assert.assertTrue("rollback", mOnRollbackTransactionInternalCalledInProfileMode);
    }

    public void assertNoTransactionsForProfileMode() {
        Assert.assertFalse("begin", mOnBeginTransactionInternalCalledInProfileMode);
        Assert.assertFalse("commit", mOnCommitTransactionInternalCalledInProfileMode);
        Assert.assertFalse("rollback", mOnRollbackTransactionInternalCalledInProfileMode);
    }


    public void assertCommitTransactionCalledForContactMode() {
        Assert.assertTrue("begin", mOnBeginTransactionInternalCalledInContactMode);
        Assert.assertTrue("commit", mOnCommitTransactionInternalCalledInContactMode);
        Assert.assertFalse("rollback", mOnRollbackTransactionInternalCalledInContactMode);
    }

    public void assertRollbackTransactionCalledForContactMode() {
        Assert.assertTrue("begin", mOnBeginTransactionInternalCalledInContactMode);
        Assert.assertFalse("commit", mOnCommitTransactionInternalCalledInContactMode);
        Assert.assertTrue("rollback", mOnRollbackTransactionInternalCalledInContactMode);
    }
}
