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
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.gallery3d.R;
import com.android.photos.data.AlbumSetLoader;
import com.android.photos.shims.LoaderCompatShim;

import java.util.Date;

public class AlbumSetCursorAdapter extends CursorAdapter {

    private LoaderCompatShim<Cursor> mDrawableFactory;

    public void setDrawableFactory(LoaderCompatShim<Cursor> factory) {
        mDrawableFactory = factory;
    }

    public AlbumSetCursorAdapter(Context context) {
        super(context, null, false);
    }

    @Override
    public void bindView(View v, Context context, Cursor cursor) {
        TextView titleTextView = (TextView) v.findViewById(
                R.id.album_set_item_title);
        titleTextView.setText(cursor.getString(AlbumSetLoader.INDEX_TITLE));

        TextView countTextView = (TextView) v.findViewById(
                R.id.album_set_item_count);
        int count = cursor.getInt(AlbumSetLoader.INDEX_COUNT);
        countTextView.setText(context.getResources().getQuantityString(
                R.plurals.number_of_photos, count, count));

        ImageView thumbImageView = (ImageView) v.findViewById(
                R.id.album_set_item_image);
        Drawable recycle = thumbImageView.getDrawable();
        Drawable drawable = mDrawableFactory.drawableForItem(cursor, recycle);
        if (recycle != drawable) {
            thumbImageView.setImageDrawable(drawable);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(
                R.layout.album_set_item, parent, false);
    }
}
