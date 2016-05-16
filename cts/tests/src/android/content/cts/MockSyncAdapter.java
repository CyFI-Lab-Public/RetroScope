/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.content.cts;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ISyncAdapter;
import android.content.ISyncContext;
import android.os.Bundle;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class MockSyncAdapter extends ISyncAdapter.Stub {

    private static MockSyncAdapter sSyncAdapter = null;

    private ArrayList<Account> mAccounts = new ArrayList<Account>();
    private String mAuthority;
    private Bundle mExtras;
    private boolean mInitialized;
    private boolean mStartSync;
    private boolean mCancelSync;
    private CountDownLatch mLatch;

    public ArrayList<Account> getAccounts() {
        return mAccounts;
    }

    public String getAuthority() {
        return mAuthority;
    }

    public Bundle getExtras() {
        return mExtras;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public boolean isStartSync() {
        return mStartSync;
    }

    public boolean isCancelSync() {
        return mCancelSync;
    }

    public void clearData() {
        mAccounts.clear();
        mAuthority = null;
        mExtras = null;
        mInitialized = false;
        mStartSync = false;
        mCancelSync = false;
        mLatch = null;
    }

    public void setLatch(CountDownLatch mLatch) {
        this.mLatch = mLatch;
    }

    public void startSync(ISyncContext syncContext, String authority, Account account,
            Bundle extras) throws RemoteException {

        mAccounts.add(account);
        mAuthority = authority;
        mExtras = extras;

        if (null != extras && extras.getBoolean(ContentResolver.SYNC_EXTRAS_INITIALIZE)) {
            mInitialized = true;
            mStartSync = false;
            mCancelSync = false;
        } else {
            mInitialized = false;
            mStartSync = true;
            mCancelSync = false;
        }

        if (null != mLatch) {
            mLatch.countDown();
        }
    }

    public void cancelSync(ISyncContext syncContext) throws RemoteException {
        mAccounts.clear();
        mAuthority = null;
        mExtras = null;

        mInitialized = false;
        mStartSync = false;
        mCancelSync = true;

        if (null != mLatch) {
            mLatch.countDown();
        }
    }

    public void initialize(android.accounts.Account account, java.lang.String authority)
            throws android.os.RemoteException {

        mAccounts.add(account);
        mAuthority = authority;

        mInitialized = true;
        mStartSync = false;
        mCancelSync = false;

        if (null != mLatch) {
            mLatch.countDown();
        }
    }

    public static MockSyncAdapter getMockSyncAdapter() {
        if (null == sSyncAdapter) {
            sSyncAdapter = new MockSyncAdapter();
        }
        return sSyncAdapter;
    }
}
