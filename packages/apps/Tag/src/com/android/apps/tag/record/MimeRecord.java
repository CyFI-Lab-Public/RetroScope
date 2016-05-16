/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apps.tag.record;

import com.android.apps.tag.R;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import android.app.Activity;
import android.content.Context;
import android.nfc.NdefRecord;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

/**
 * A {@link ParsedNdefRecord} corresponding to a MIME object.
 */
public class MimeRecord extends ParsedNdefRecord {
    private final String mType;
    private final byte[] mContent;

    private MimeRecord(String mimeType, byte[] content) {
        mType = Preconditions.checkNotNull(mimeType);
        Preconditions.checkNotNull(content);
        mContent = Arrays.copyOf(content, content.length);
    }

    @VisibleForTesting
    public String getMimeType() {
        return mType;
    }

    @VisibleForTesting
    public byte[] getContent() {
        return Arrays.copyOf(mContent, mContent.length);
    }

    @Override
    public View getView(Activity activity, LayoutInflater inflater, ViewGroup parent, int offset) {
        TextView text = (TextView) inflater.inflate(R.layout.tag_text, parent, false);
        text.setText(mType);
        return text;
    }

    @Override
    public String getSnippet(Context context, Locale locale) {
        return mType;
    }

    public static MimeRecord parse(NdefRecord record) {
        Preconditions.checkArgument(record.toMimeType() != null);
        return new MimeRecord(record.toMimeType(), record.getPayload());
    }

    public static boolean isMime(NdefRecord record) {
        return record.toMimeType() != null;
    }

    public static NdefRecord newMimeRecord(String type, byte[] data) {
        return NdefRecord.createMime(type,  data);
    }
}
