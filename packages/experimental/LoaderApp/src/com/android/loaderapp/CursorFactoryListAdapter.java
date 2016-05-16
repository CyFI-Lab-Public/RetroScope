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

package com.android.loaderapp;

import com.android.loaderapp.R;
import com.android.loaderapp.model.ContactsListLoader;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class CursorFactoryListAdapter extends CursorAdapter {
    ViewFactory mViewFactory;

    public interface ViewFactory {
        public View newView(Context context, ViewGroup parent);
        public void bindView(View view, Context context, Cursor cursor);
    }

    /**
     * A simple view factory that inflates the views from XML and puts the display
     * name in @id/name.
     */
    public static class ResourceViewFactory implements ViewFactory {
        private int mResId;

        public ResourceViewFactory(int resId) {
            mResId = resId;
        }

        public View newView(Context context, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(mResId, parent, false);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            TextView name = (TextView) view.findViewById(R.id.name);
            name.setText(cursor.getString(ContactsListLoader.COLUMN_NAME));
        }
    }

    public CursorFactoryListAdapter(Context context, ViewFactory factory) {
        super(context, null, /* disable content observers for the cursor */0);
        mViewFactory = factory;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = mViewFactory.newView(context, parent);
        mViewFactory.bindView(view, context, cursor); 
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        mViewFactory.bindView(view, context, cursor);
    }
}
