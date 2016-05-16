/*
 * Copyright (C) 2010 Google Inc.
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

package com.android.loaderapp.fragments;

import com.android.loaderapp.CursorFactoryListAdapter;
import com.android.loaderapp.R;
import com.android.loaderapp.CursorFactoryListAdapter.ResourceViewFactory;
import com.android.loaderapp.model.GroupsListLoader;

import android.app.LoaderManagingFragment;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class GroupsListFragment extends LoaderManagingFragment<Cursor>
        implements OnItemClickListener {
    private static final long GROUP_ID_ALL_CONTACTS = -1;
    private static final long GROUP_ID_FAVORITES = -2;

    private static final int LOADER_GROUPS = 0; 
    
    Controller mController;
    ListView mList;
    CursorFactoryListAdapter mAdapter;

    public interface Controller {
        public void onAllContactsSelected();
        public void onFavoritesSelected();
        public void onGroupSelected(String name);
    }
    
    public void setController(Controller controller) {
        mController = controller;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        ListView list = (ListView) inflater.inflate(R.layout.contacts_list, container, false);
        list.setOnItemClickListener(this);
        mAdapter = new CursorFactoryListAdapter(getActivity(),
                new ResourceViewFactory(R.layout.xlarge_list_item));
        list.setAdapter(mAdapter);
        mList = list;
        return list;
    }

    @Override
    public void onInitializeLoaders() {
        startLoading(LOADER_GROUPS, null);
    }

    @Override
    protected Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_GROUPS: {
                return new GroupsListLoader(getActivity());
            }
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_GROUPS: {
                setData(data);
                break;
            }
        }
    }

    private void setData(Cursor groups) {
        MatrixCursor psuedoGroups = new MatrixCursor(new String[] { "_id", "name" });
        psuedoGroups.newRow().add(-1).add("All Contacts");
        psuedoGroups.newRow().add(-2).add("Favorites");
        mAdapter.changeCursor(new MergeCursor(new Cursor[] { psuedoGroups, groups }));
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (id == GROUP_ID_ALL_CONTACTS) {
            mController.onAllContactsSelected();
        } else if (id == GROUP_ID_FAVORITES) {
            mController.onFavoritesSelected();
        } else {
            Cursor cursor = (Cursor) mAdapter.getItem(position);
            mController.onGroupSelected(cursor.getString(GroupsListLoader.COLUMN_TITLE));
        }
    }
}