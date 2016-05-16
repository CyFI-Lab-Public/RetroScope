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

package com.android.photos;

import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Files.FileColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.android.gallery3d.R;
import com.android.photos.adapters.AlbumSetCursorAdapter;
import com.android.photos.data.AlbumSetLoader;
import com.android.photos.shims.LoaderCompatShim;
import com.android.photos.shims.MediaSetLoader;

import java.util.ArrayList;


public class AlbumSetFragment extends MultiSelectGridFragment implements LoaderCallbacks<Cursor> {

    private AlbumSetCursorAdapter mAdapter;
    private LoaderCompatShim<Cursor> mLoaderCompatShim;

    private static final int LOADER_ALBUMSET = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        mAdapter = new AlbumSetCursorAdapter(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        getLoaderManager().initLoader(LOADER_ALBUMSET, null, this);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getGridView().setColumnWidth(getActivity().getResources()
                .getDimensionPixelSize(R.dimen.album_set_item_width));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // TODO: Switch to AlbumSetLoader
        MediaSetLoader loader = new MediaSetLoader(getActivity());
        mAdapter.setDrawableFactory(loader);
        mLoaderCompatShim = loader;
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader,
            Cursor data) {
        mAdapter.swapCursor(data);
        setAdapter(mAdapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        if (mLoaderCompatShim == null) {
            // Not fully initialized yet, discard
            return;
        }
        Cursor item = (Cursor) getItemAtPosition(position);
        Context context = getActivity();
        Intent intent = new Intent(context, AlbumActivity.class);
        intent.putExtra(AlbumActivity.KEY_ALBUM_URI,
                mLoaderCompatShim.getPathForItem(item).toString());
        intent.putExtra(AlbumActivity.KEY_ALBUM_TITLE,
                item.getString(AlbumSetLoader.INDEX_TITLE));
        context.startActivity(intent);
    }

    @Override
    public int getItemMediaType(Object item) {
        return FileColumns.MEDIA_TYPE_NONE;
    }

    @Override
    public int getItemSupportedOperations(Object item) {
        return ((Cursor) item).getInt(AlbumSetLoader.INDEX_SUPPORTED_OPERATIONS);
    }

    @Override
    public ArrayList<Uri> getSubItemUrisForItem(Object item) {
        return mLoaderCompatShim.urisForSubItems((Cursor) item);
    }

    @Override
    public void deleteItemWithPath(Object itemPath) {
        mLoaderCompatShim.deleteItemWithPath(itemPath);
    }

    @Override
    public Uri getItemUri(Object item) {
        return mLoaderCompatShim.uriForItem((Cursor) item);
    }

    @Override
    public Object getPathForItem(Object item) {
        return mLoaderCompatShim.getPathForItem((Cursor) item);
    }
}
