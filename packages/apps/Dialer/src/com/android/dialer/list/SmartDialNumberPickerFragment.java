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

import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Directory;
import android.util.Log;

import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.list.PhoneNumberPickerFragment;
import com.android.dialer.dialpad.SmartDialCursorLoader;

/**
 * Implements a fragment to load and display SmartDial search results.
 */
public class SmartDialNumberPickerFragment extends PhoneNumberPickerFragment {

    private static final String TAG = SmartDialNumberPickerFragment.class.getSimpleName();

    /**
     * Creates a SmartDialListAdapter to display and operate on search results.
     * @return
     */
    @Override
    protected ContactEntryListAdapter createListAdapter() {
        SmartDialNumberListAdapter adapter = new SmartDialNumberListAdapter(getActivity());
        adapter.setDisplayPhotos(true);
        adapter.setUseCallableUri(super.usesCallableUri());
        return adapter;
    }

    /**
     * Creates a SmartDialCursorLoader object to load query results.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /** SmartDial does not support Directory Load, falls back to normal search instead. */
        if (id == getDirectoryLoaderId()) {
            Log.v(TAG, "Directory load");
            return super.onCreateLoader(id, args);
        } else {
            Log.v(TAG, "Creating loader");
            final SmartDialNumberListAdapter adapter = (SmartDialNumberListAdapter) getAdapter();
            SmartDialCursorLoader loader = new SmartDialCursorLoader(super.getContext());
            adapter.configureLoader(loader);
            return loader;
        }
    }

    /**
     * Gets the Phone Uri of an entry for calling.
     * @param position Location of the data of interest.
     * @return Phone Uri to establish a phone call.
     */
    @Override
    protected Uri getPhoneUri(int position) {
        final SmartDialNumberListAdapter adapter = (SmartDialNumberListAdapter) getAdapter();
        return adapter.getDataUri(position);
    }
}
