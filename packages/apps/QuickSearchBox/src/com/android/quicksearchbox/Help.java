/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.quicksearchbox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Handles app help.
 */
public class Help {

    private final Context mContext;
    private final Config mConfig;

    public Help(Context context, Config config) {
        mContext = context;
        mConfig = config;
    }

    public void addHelpMenuItem(Menu menu, String activityName) {
        addHelpMenuItem(menu, activityName, false);
    }

    public void addHelpMenuItem(Menu menu, String activityName, boolean showAsAction) {
        Intent helpIntent = getHelpIntent(activityName);
        if (helpIntent != null) {
            MenuInflater inflater = new MenuInflater(mContext);
            inflater.inflate(R.menu.help, menu);
            MenuItem item = menu.findItem(R.id.menu_help);
            item.setIntent(helpIntent);
            if (showAsAction) {
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }
    }

    private Intent getHelpIntent(String activityName) {
        Uri helpUrl = mConfig.getHelpUrl(activityName);
        if (helpUrl == null) return null;
        return new Intent(Intent.ACTION_VIEW, helpUrl);
    }

}
