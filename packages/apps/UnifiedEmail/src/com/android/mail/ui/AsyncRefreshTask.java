/*******************************************************************************mFolder
 *      Copyright (C) 2012 Google Inc.
 *      Licensed to The Android Open Source Project.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *******************************************************************************/

package com.android.mail.ui;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

public class AsyncRefreshTask extends AsyncTask<Void, Void, Void> {
    private final Context mContext;
    private final Uri mRefreshUri;


    public AsyncRefreshTask(Context context, Uri refreshUri) {
        mContext = context;
        mRefreshUri = refreshUri;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (mRefreshUri != null) {
            mContext.getContentResolver().query(mRefreshUri, null, null, null, null);
        }
        return null;
    }
}
