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

package com.android.cts.verifier.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.android.cts.verifier.R;

/**
 * This is the service that provides the factory to be bound to the collection
 * service.
 */
public class WidgetCtsService extends RemoteViewsService {
    public static final int NUM_ITEMS = 50;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new CtsRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class CtsRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    Context mContext;

    public CtsRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
    }

    public void onCreate() {
    }

    public void onDestroy() {
    }

    public int getCount() {
        return WidgetCtsService.NUM_ITEMS;
    }

    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.list_item);
        String text = "List Item " + (position + 1);
        rv.setTextViewText(R.id.list_item_text, text);
        return rv;
    }

    public RemoteViews getLoadingView() {
        // We aren't going to return a default loading view in this sample
        return null;
    }

    public int getViewTypeCount() {
        // Technically, we have two types of views (the dark and light
        // background views)
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
    }
}
