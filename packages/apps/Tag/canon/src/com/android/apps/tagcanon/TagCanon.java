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
 * limitations under the License
 */

package com.android.apps.tagcanon;

import com.android.apps.tag.MockNdefMessages;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * A test activity that launches tags as if they had been scanned.
 */
public class TagCanon extends ListActivity {
    static final String TAG = "TagCanon";
    static final byte[] UID = new byte[] { 0x05, 0x00, 0x03, 0x08 };

    ArrayAdapter<TagDescription> mAdapter;

    public static NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(locale);

        byte[] langBytes = locale.getLanguage().getBytes(StandardCharsets.US_ASCII);

        Charset utfEncoding = encodeInUtf8 ? StandardCharsets.UTF_8 : StandardCharsets.UTF_16;
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = Bytes.concat(
           new byte[] { (byte) status },
           langBytes,
           textBytes
        );

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    public static NdefRecord newMimeRecord(String type, byte[] data) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(data);

        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);

        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, typeBytes, new byte[0], data);
    }

    final NdefMessage[] buildImageMessages() {
        Resources res = getResources();
        Drawable drawable = res.getDrawable(R.drawable.ic_launcher_nfc);
        Bitmap photo = ((BitmapDrawable) drawable).getBitmap();
        final int size = photo.getWidth() * photo.getHeight() * 4;
        final ByteArrayOutputStream out = new ByteArrayOutputStream(size);

        try {
            photo.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            byte[] payload = out.toByteArray();
            out.close();

            NdefRecord text = newTextRecord("There's an image below this text!", Locale.US, true);
            NdefRecord image = newMimeRecord("image/png", payload);
            NdefMessage[] msgs = new NdefMessage[] { 
                    new NdefMessage(new NdefRecord[] { text, image }) };

            return msgs;
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress image", e);
        }
    }

    static final class TagDescription {
        public String title;
        NdefMessage[] msgs;

        public TagDescription(String title, byte[] bytes) {
            this.title = title;
            try {
                msgs = new NdefMessage[] { new NdefMessage(bytes) };
            } catch (Exception e) {
                throw new RuntimeException("Failed to create tag description", e);
            }
        }

        public TagDescription(String title, NdefMessage[] msgs) {
            this.title = title;
            this.msgs = msgs;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        ArrayAdapter<TagDescription> adapter = new ArrayAdapter<TagDescription>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1);
        adapter.add(new TagDescription("Image", buildImageMessages()));
        adapter.add(new TagDescription("Real NFC message", MockNdefMessages.REAL_NFC_MSG));
        adapter.add(new TagDescription("Call Google", MockNdefMessages.CALL_GOOGLE));
        adapter.add(new TagDescription("English text", MockNdefMessages.ENGLISH_PLAIN_TEXT));
        adapter.add(new TagDescription("Send text message", MockNdefMessages.SEND_TEXT_MESSAGE));
        adapter.add(new TagDescription("SmartPoster URL & text", MockNdefMessages.SMART_POSTER_URL_AND_TEXT));
        adapter.add(new TagDescription("SmartPoster URL", MockNdefMessages.SMART_POSTER_URL_NO_TEXT));
        adapter.add(new TagDescription("VCARD", MockNdefMessages.VCARD));
        adapter.add(new TagDescription("URI", MockNdefMessages.URI));
        setListAdapter(adapter);
        mAdapter = adapter;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        TagDescription description = mAdapter.getItem(position);
        Intent intent = new Intent(NfcAdapter.ACTION_TAG_DISCOVERED);
        intent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, description.msgs);
        startActivity(intent);
    }
}
