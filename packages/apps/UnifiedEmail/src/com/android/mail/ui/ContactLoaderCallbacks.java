/*
 * Copyright (C) 2013 Google Inc.
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

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Bundle;

import com.android.mail.ContactInfo;
import com.android.mail.ContactInfoSource;
import com.android.mail.SenderInfoLoader;
import com.google.common.collect.ImmutableMap;

import java.util.Set;

/**
 * Asynchronously loads contact data for all senders in the conversation,
 * and notifies observers when the data is ready.
 */
public class ContactLoaderCallbacks implements ContactInfoSource,
        LoaderManager.LoaderCallbacks<ImmutableMap<String, ContactInfo>> {

    private Set<String> mSenders;
    private ImmutableMap<String, ContactInfo> mContactInfoMap;
    private DataSetObservable mObservable = new DataSetObservable();

    private Context mContext;

    public ContactLoaderCallbacks(Context context) {
        mContext = context;
    }

    public void setSenders(Set<String> emailAddresses) {
        mSenders = emailAddresses;
    }

    @Override
    public Loader<ImmutableMap<String, ContactInfo>> onCreateLoader(int id, Bundle args) {
        return new SenderInfoLoader(mContext, mSenders);
    }

    @Override
    public void onLoadFinished(Loader<ImmutableMap<String, ContactInfo>> loader,
            ImmutableMap<String, ContactInfo> data) {
        mContactInfoMap = data;
        mObservable.notifyChanged();
    }

    @Override
    public void onLoaderReset(Loader<ImmutableMap<String, ContactInfo>> loader) {
    }

    @Override
    public ContactInfo getContactInfo(String email) {
        if (mContactInfoMap == null) {
            return null;
        }
        return mContactInfoMap.get(email);
    }

    @Override
    public void registerObserver(DataSetObserver observer) {
        mObservable.registerObserver(observer);
    }

    @Override
    public void unregisterObserver(DataSetObserver observer) {
        mObservable.unregisterObserver(observer);
    }
}
