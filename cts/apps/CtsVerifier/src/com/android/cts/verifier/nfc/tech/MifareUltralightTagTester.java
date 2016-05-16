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

package com.android.cts.verifier.nfc.tech;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * {@link TagTester} for MIFARE Ultralight tags. It writes random bytes to the
 * tag's first user page and verifies that it matches when scanned later.
 */
public class MifareUltralightTagTester implements TagTester {

    private static final int USER_PAGE_OFFSET = 5;

    private static final int NUM_PAGES = 4;

    @Override
    public boolean isTestableTag(Tag tag) {
        if (tag != null) {
            for (String tech : tag.getTechList()) {
                if (tech.equals(MifareUltralight.class.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public TagVerifier writeTag(Tag tag) throws IOException {
        Random random = new Random();
        MifareUltralight ultralight = MifareUltralight.get(tag);
        ultralight.connect();

        final byte[] fourPages = new byte[NUM_PAGES * MifareUltralight.PAGE_SIZE];
        byte[] onePage = new byte[MifareUltralight.PAGE_SIZE];
        for (int i = 0; i < NUM_PAGES; i++) {
            random.nextBytes(onePage);
            System.arraycopy(onePage, 0, fourPages, i * onePage.length, onePage.length);
            ultralight.writePage(USER_PAGE_OFFSET + i, onePage);
        }

        final CharSequence expectedContent = NfcUtils.displayByteArray(fourPages);

        return new TagVerifier() {
            @Override
            public Result verifyTag(Tag tag) throws IOException {
                MifareUltralight ultralight = MifareUltralight.get(tag);
                if (ultralight != null) {
                    ultralight.connect();
                    byte[] actualFourPages = ultralight.readPages(USER_PAGE_OFFSET);
                    CharSequence actualContent = NfcUtils.displayByteArray(actualFourPages);
                    return new Result(expectedContent, actualContent,
                            Arrays.equals(fourPages, actualFourPages));
                } else {
                    return new Result(expectedContent, null, false);
                }
            }
        };
    }
}
