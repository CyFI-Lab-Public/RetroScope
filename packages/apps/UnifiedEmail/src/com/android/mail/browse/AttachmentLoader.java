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

import com.google.common.collect.Maps;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;

import com.android.mail.providers.Attachment;
import com.android.mail.providers.UIProvider;

import java.util.Map;

public class AttachmentLoader extends CursorLoader {

    public AttachmentLoader(Context c, Uri uri) {
        super(c, uri, UIProvider.ATTACHMENT_PROJECTION, null, null, null);
    }

    @Override
    public Cursor loadInBackground() {
        return new AttachmentCursor(super.loadInBackground());
    }

    public static class AttachmentCursor extends CursorWrapper {

        private Map<String, Attachment> mCache = Maps.newHashMap();

        private AttachmentCursor(Cursor inner) {
            super(inner);
        }

        public Attachment get() {
            final String uri = getWrappedCursor().getString(UIProvider.ATTACHMENT_URI_COLUMN);
            Attachment m = mCache.get(uri);
            if (m == null) {
                m = new Attachment(this);
                mCache.put(uri, m);
            }
            return m;
        }
    }
}
