/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
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
package com.android.mail.browse;

import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

class AttachmentCommandHandler extends AsyncQueryHandler {
    public AttachmentCommandHandler(Context context) {
        super(context.getContentResolver());
    }

    /**
     * Asynchronously begin an update() on a ContentProvider.
     *
     */
    public void sendCommand(Uri uri, ContentValues params) {
        startUpdate(0, null, uri, params, null, null);
    }
}
