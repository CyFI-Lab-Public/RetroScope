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

package com.android.providers.contacts;

import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;
import android.util.Log;

import com.android.providers.contacts.HanziToPinyin.Token;

import junit.framework.TestCase;

import java.util.ArrayList;

@SmallTest
public class HanziToPinyinTest extends TestCase {
    private final static String ONE_HANZI = "\u675C";
    private final static String TWO_HANZI = "\u675C\u9D51";
    private final static String ASSIC = "test";
    private final static String ONE_UNKNOWN = "\uFF71";
    private final static String MISC = "test\u675C   Test with space\uFF71\uFF71\u675C";

    private boolean hasChineseTransliterator() {
        return HanziToPinyin.getInstance().hasChineseTransliterator();
    }

    private void test(final char hanzi, final String expectedPinyin) throws Exception {
        final String hanziString = Character.toString(hanzi);
        ArrayList<Token> tokens = HanziToPinyin.getInstance().get(hanziString);
        assertEquals(tokens.size(), 1);
        final String newString = tokens.get(0).target;
        if (TextUtils.isEmpty(expectedPinyin)) {
            assertEquals("Expected no transliteration for '" + hanziString
                         + "' but got '" + newString + "'",
                         tokens.get(0).type, Token.UNKNOWN);
            assertTrue("Expected to get back original string for '"
                       + hanziString  + "' but got '" + newString + "'",
                       newString.equals(hanziString));
        } else {
            assertEquals("Expected transliteration for '" + hanziString
                         + "' of '" + expectedPinyin + "' but got none",
                         tokens.get(0).type, Token.PINYIN);
            assertTrue("Expected transliteration for '" + hanziString + "' of '"
                       + expectedPinyin + "' but got '" + newString + "'",
                       newString.equalsIgnoreCase(expectedPinyin));
        }
    }

    @SmallTest
    public void testGetToken() throws Exception {
        if (!hasChineseTransliterator()) {
            return;
        }
        ArrayList<Token> tokens = HanziToPinyin.getInstance().get(ONE_HANZI);
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).type, Token.PINYIN);
        assertTrue(tokens.get(0).target.equalsIgnoreCase("DU"));

        tokens = HanziToPinyin.getInstance().get(TWO_HANZI);
        assertEquals(tokens.size(), 2);
        assertEquals(tokens.get(0).type, Token.PINYIN);
        assertEquals(tokens.get(1).type, Token.PINYIN);
        assertTrue(tokens.get(0).target.equalsIgnoreCase("DU"));
        assertTrue(tokens.get(1).target.equalsIgnoreCase("JUAN"));

        tokens = HanziToPinyin.getInstance().get(ASSIC);
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).type, Token.LATIN);

        tokens = HanziToPinyin.getInstance().get(ONE_UNKNOWN);
        assertEquals(tokens.size(), 1);
        assertEquals(tokens.get(0).type, Token.UNKNOWN);

        tokens = HanziToPinyin.getInstance().get(MISC);
        assertEquals(tokens.size(), 7);
        assertEquals(tokens.get(0).type, Token.LATIN);
        assertEquals(tokens.get(1).type, Token.PINYIN);
        assertEquals(tokens.get(2).type, Token.LATIN);
        assertEquals(tokens.get(3).type, Token.LATIN);
        assertEquals(tokens.get(4).type, Token.LATIN);
        assertEquals(tokens.get(5).type, Token.UNKNOWN);
        assertEquals(tokens.get(6).type, Token.PINYIN);
    }

    /**
     * Test each supported han against expected pinyin from transliterator.
     */
    @SmallTest
    public void testTransliterator() throws Exception {
        if (!hasChineseTransliterator()) {
            return;
        }
        test('\u4e00', "YI");
        test('\u5201', "DIAO");
        test('\u5602', "JIAO");
        test('\u5a03', "WA");
        test('\u5e04', "DING");
        test('\u6205', "GANG");
        test('\u6606', "KUN");
        test('\u6a07', "XIU");
        test('\u6e08', "JI");
        test('\u7209', "LA");
        test('\u79ff', "FU");
        test('\u7a00', "XI");
        test('\u7e01', "YUAN");
        test('\u8202', "CHONG");
        test('\u8603', "RUI");
        test('\u8a04', "QIU");
        test('\u8e05', "XUE");
        test('\u9206', "QIAN");
        test('\u9607', "DU");
        test('\u9a08', "PIAN");
        test('\u9e09', "YANG");
    }
}
