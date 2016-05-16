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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.android.gallery3d.app.GalleryActivity;
import com.android.photos.adapters.PhotoThumbnailAdapter;
import com.android.photos.data.PhotoSetLoader;
import com.android.photos.shims.LoaderCompatShim;
import com.android.photos.shims.MediaItemsLoader;

import java.util.ArrayList;

public class PhotoSetFragment extends MultiSelectGridFragment implements LoaderCallbacks<Cursor> {

    private static final int LOADER_PHOTOSET = 1;

    private LoaderCompatShim<Cursor> mLoaderCompatShim;
    private PhotoThumbnailAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        mAdapter = new PhotoThumbnailAdapter(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        getLoaderManager().initLoader(LOADER_PHOTOSET, null, this);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO: Remove once UI stabilizes
        getGridView().setColumnWidth(MediaItemsLoader.getThumbnailSize());
    }

    @Override
    public void onGridItemClick(GridView g, View v, int position, long id) {
        if (mLoaderCompatShim == null) {
            // Not fully initialized yet, discard
            return;
        }
        Cursor item = (Cursor) getItemAtPosition(position);
        Uri uri = mLoaderCompatShim.uriForItem(item);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClass(getActivity(), GalleryActivity.class);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // TODO: Switch to PhotoSetLoader
        MediaItemsLoader loader = new MediaItemsLoader(getActivity());
        mLoaderCompatShim = loader;
        mAdapter.setDrawableFactory(mLoaderCompatShim);
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
    public int getItemMediaType(Object item) {
        return ((Cursor) item).getInt(PhotoSetLoader.INDEX_MEDIA_TYPE);
    }

    @Override
    public int getItemSupportedOperations(Object item) {
        return ((Cursor) item).getInt(PhotoSetLoader.INDEX_SUPPORTED_OPERATIONS);
    }

    private ArrayList<Uri> mSubItemUriTemp = new ArrayList<Uri>(1);
    @Override
    public ArrayList<Uri> getSubItemUrisForItem(Object item) {
        mSubItemUriTemp.clear();
        mSubItemUriTemp.add(mLoaderCompatShim.uriForItem((Cursor) item));
        return mSubItemUriTemp;
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
