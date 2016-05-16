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

package com.android.photos.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.android.gallery3d.R;
import com.android.photos.data.PhotoSetLoader;
import com.android.photos.shims.LoaderCompatShim;
import com.android.photos.views.GalleryThumbnailView.GalleryThumbnailAdapter;


public class PhotoThumbnailAdapter extends CursorAdapter implements GalleryThumbnailAdapter {
    private LayoutInflater mInflater;
    private LoaderCompatShim<Cursor> mDrawableFactory;

    public PhotoThumbnailAdapter(Context context) {
        super(context, null, false);
        mInflater = LayoutInflater.from(context);
    }

    public void setDrawableFactory(LoaderCompatShim<Cursor> factory) {
        mDrawableFactory = factory;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView iv = (ImageView) view.findViewById(R.id.thumbnail);
        Drawable recycle = iv.getDrawable();
        Drawable drawable = mDrawableFactory.drawableForItem(cursor, recycle);
        if (recycle != drawable) {
            iv.setImageDrawable(drawable);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.photo_set_item, parent, false);
        return view;
    }

    @Override
    public float getIntrinsicAspectRatio(int position) {
        Cursor cursor = getItem(position);
        float width = cursor.getInt(PhotoSetLoader.INDEX_WIDTH);
        float height = cursor.getInt(PhotoSetLoader.INDEX_HEIGHT);
        return width / height;
    }

    @Override
    public Cursor getItem(int position) {
        return (Cursor) super.getItem(position);
    }
}