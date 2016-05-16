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

package com.android.camera.data;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

/**
 * An abstract {@link LocalDataAdapter} implementation to wrap another
 * {@link LocalDataAdapter}. All implementations related to data id is not
 * addressed in this abstract class since wrapping another data adapter
 * surely makes things different for data id.
 *
 * @see FixedFirstDataAdapter
 * @see FixedLastDataAdapter
 */
public abstract class AbstractLocalDataAdapterWrapper implements LocalDataAdapter {

    protected final LocalDataAdapter mAdapter;
    protected int mSuggestedWidth;
    protected int mSuggestedHeight;

    /**
     * Constructor.
     *
     * @param wrappedAdapter  The {@link LocalDataAdapter} to be wrapped.
     */
    AbstractLocalDataAdapterWrapper(LocalDataAdapter wrappedAdapter) {
        if (wrappedAdapter == null) {
            throw new AssertionError("data adapter is null");
        }
        mAdapter = wrappedAdapter;
    }

    @Override
    public void suggestViewSizeBound(int w, int h) {
        mSuggestedWidth = w;
        mSuggestedHeight = h;
        mAdapter.suggestViewSizeBound(w, h);
    }

    @Override
    public void setListener(Listener listener) {
        mAdapter.setListener(listener);
    }

    @Override
    public void requestLoad(ContentResolver resolver) {
        mAdapter.requestLoad(resolver);
    }

    @Override
    public void addNewVideo(ContentResolver resolver, Uri uri) {
        mAdapter.addNewVideo(resolver, uri);
    }

    @Override
    public void addNewPhoto(ContentResolver resolver, Uri uri) {
        mAdapter.addNewPhoto(resolver, uri);
    }

    @Override
    public void insertData(LocalData data) {
        mAdapter.insertData(data);
    }

    @Override
    public void flush() {
        mAdapter.flush();
    }

    @Override
    public boolean executeDeletion(Context context) {
        return mAdapter.executeDeletion(context);
    }

    @Override
    public boolean undoDataRemoval() {
        return mAdapter.undoDataRemoval();
    }

    @Override
    public void refresh(ContentResolver resolver, Uri uri) {
        mAdapter.refresh(resolver, uri);
    }
}
