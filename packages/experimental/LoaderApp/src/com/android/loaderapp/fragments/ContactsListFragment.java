/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.loaderapp.fragments;

import com.android.loaderapp.CursorFactoryListAdapter;
import com.android.loaderapp.R;
import com.android.loaderapp.CursorFactoryListAdapter.ResourceViewFactory;
import com.android.loaderapp.model.ContactsListLoader;

import android.app.LoaderManagingFragment;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class ContactsListFragment extends LoaderManagingFragment<Cursor>
        implements OnItemClickListener {
    private static final int LOADER_LIST = 1;

    public static final int MODE_NULL = 0;
    public static final int MODE_VISIBLE = 1;
    public static final int MODE_STREQUENT = 2;
    public static final int MODE_GROUP = 3;

    private static final int DEFAULT_MODE = MODE_VISIBLE;
    
    public interface Controller {
        public void onContactSelected(Uri contact);
    }
    
    Controller mController;
    ListView mList;
    CursorFactoryListAdapter mAdapter;
    int mMode;
    String mGroupName;

    public ContactsListFragment() {
        super();
        mMode = DEFAULT_MODE;
    }

    public ContactsListFragment(int mode) {
        super();
        mMode = mode;
    }

    @Override
    public void onInitializeLoaders() {
        if (mMode != MODE_NULL) {
            startLoading(LOADER_LIST, null);
        }
    }

    @Override
    protected Loader onCreateLoader(int id, Bundle args) {
        switch (mMode) {
            case MODE_GROUP:
                return ContactsListLoader.newContactGroupLoader(getActivity(), mGroupName);
            case MODE_STREQUENT:
                return ContactsListLoader.newStrequentContactsLoader(getActivity());
            default:
                return ContactsListLoader.newVisibleContactsLoader(getActivity());
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        ListView list = (ListView) inflater.inflate(R.layout.contacts_list, container, false);
        list.setOnItemClickListener(this);
        mAdapter = new CursorFactoryListAdapter(getActivity(),
                new ResourceViewFactory(getListItemResId()));
        list.setAdapter(mAdapter);
        mList = list;
        return list;
    }

    public void setMode(int mode) {
        boolean reload = mode != mMode;
        mMode = mode;
        if (reload) {
            startLoading(LOADER_LIST, null);
        }
    }

    public void setGroupMode(String groupName) {
        boolean reload = (MODE_GROUP != mMode) || !groupName.equals(mGroupName);
        mMode = MODE_GROUP;
        mGroupName = groupName;
        if (reload) {
            startLoading(LOADER_LIST, null);
        }
    }

    public void setController(Controller controller) {
        mController = controller;
    }

    public int getMode() {
        return mMode;
    }

    /**
     * Build the {@link Contacts#CONTENT_LOOKUP_URI} for the given
     * {@link ListView} position.
     */
    public Uri getContactUri(int position) {
        if (position == ListView.INVALID_POSITION) {
            throw new IllegalArgumentException("Position not in list bounds");
        }

        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        if (cursor == null) {
            return null;
        }

        // Build and return soft, lookup reference
        final long contactId = cursor.getLong(ContactsListLoader.COLUMN_ID);
        final String lookupKey = cursor.getString(ContactsListLoader.COLUMN_LOOKUP_KEY);
        return Contacts.getLookupUri(contactId, lookupKey);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // The user clicked on an item in the left side pane, start loading the data for it
        if (mController != null) {
            mController.onContactSelected(getContactUri(position));
        }
    }

    private int getListItemResId() {
        // This should be done using the resource system, but for now we want to override
        // the configuration for running xlarge UIs on normal screens and vice versa
        Configuration config = getActivity().getResources().getConfiguration();
        int screenLayoutSize = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (screenLayoutSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return R.layout.xlarge_list_item;
        } else {
            return R.layout.normal_list_item;
        }
    }
}
