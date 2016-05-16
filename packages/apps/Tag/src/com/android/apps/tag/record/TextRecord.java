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
import com.google.common.primitives.Bytes;

import android.app.Activity;
import android.content.Context;
import android.nfc.NdefRecord;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;

/**
 * An NFC Text Record
 */
public class TextRecord extends ParsedNdefRecord {

    public static final String RECORD_TYPE = "TextRecord";

    /** ISO/IANA language code */
    private final String mLanguageCode;
    private final String mText;

    private TextRecord(String languageCode, String text) {
        mLanguageCode = Preconditions.checkNotNull(languageCode);
        mText = Preconditions.checkNotNull(text);
    }

    @Override
    public View getView(Activity activity, LayoutInflater inflater, ViewGroup parent, int offset) {
        TextView text = (TextView) inflater.inflate(R.layout.tag_text, parent, false);
        text.setText(mText);
        return text;
    }

    @Override
    public String getSnippet(Context context, Locale locale) {
        return mText;
    }

    public String getText() {
        return mText;
    }

    /**
     * Returns the ISO/IANA language code associated with this text element.
     *
     * TODO: this should return a {@link java.util.Locale}
     */
    @VisibleForTesting
    public String getLanguageCode() {
        return mLanguageCode;
    }

    // TODO: deal with text fields which span multiple NdefRecords
    public static TextRecord parse(NdefRecord record) {
        Preconditions.checkArgument(record.getTnf() == NdefRecord.TNF_WELL_KNOWN);
        Preconditions.checkArgument(Arrays.equals(record.getType(), NdefRecord.RTD_TEXT));
        try {

            byte[] payload = record.getPayload();
            Preconditions.checkArgument(payload.length > 0);

            /*
             * payload[0] contains the "Status Byte Encodings" field, per
             * the NFC Forum "Text Record Type Definition" section 3.2.1.
             *
             * bit7 is the Text Encoding Field.
             *
             * if (Bit_7 == 0): The text is encoded in UTF-8
             * if (Bit_7 == 1): The text is encoded in UTF16
             *
             * Bit_6 is reserved for future use and must be set to zero.
             *
             * Bits 5 to 0 are the length of the IANA language code.
             */

            String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 0077;
            Preconditions.checkArgument(payload.length - languageCodeLength - 1 >= 0);

            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            String text = new String(payload,
                    languageCodeLength + 1,
                    payload.length - languageCodeLength - 1,
                    textEncoding);
            return new TextRecord(languageCode, text);

        } catch (UnsupportedEncodingException e) {
            // should never happen unless we get a malformed tag.
            throw new IllegalArgumentException(e);
        }
    }

    public static boolean isText(NdefRecord record) {
        try {
            parse(record);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @VisibleForTesting
    public static NdefRecord newTextRecord(String text, Locale locale) {
        return newTextRecord(text, locale, true);
    }

    public static NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(locale);

        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
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
}
