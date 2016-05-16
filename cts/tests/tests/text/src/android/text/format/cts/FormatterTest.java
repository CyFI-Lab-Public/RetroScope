/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.text.format.cts;

import java.math.BigDecimal;
import java.math.MathContext;

import android.test.AndroidTestCase;
import android.text.format.Formatter;

public class FormatterTest extends AndroidTestCase {

    public void testFormatFileSize() {
        // test null Context
        assertEquals("", Formatter.formatFileSize(null, 0));

        MathContext mc = MathContext.DECIMAL64;
        BigDecimal bd = new BigDecimal((long) 1024, mc);

        // test different long values with various length
        assertEquals("0.00B", Formatter.formatFileSize(mContext, 0));

        assertEquals("899B", Formatter.formatFileSize(mContext, 899));

        assertEquals("1.00KB", Formatter.formatFileSize(mContext, bd.pow(1).longValue()));

        assertEquals("1.00MB", Formatter.formatFileSize(mContext, bd.pow(2).longValue()));

        assertEquals("1.00GB", Formatter.formatFileSize(mContext, bd.pow(3).longValue()));

        assertEquals("1.00TB", Formatter.formatFileSize(mContext, bd.pow(4).longValue()));

        assertEquals("1.00PB", Formatter.formatFileSize(mContext, bd.pow(5).longValue()));

        assertEquals("1024PB", Formatter.formatFileSize(mContext, bd.pow(6).longValue()));

        // test Negative value
        assertEquals("-1.00B", Formatter.formatFileSize(mContext, -1));
    }

    public void testFormatIpAddress() {
        assertEquals("1.0.168.192", Formatter.formatIpAddress(0xC0A80001));
        assertEquals("1.0.0.127", Formatter.formatIpAddress(0x7F000001));
        assertEquals("35.182.168.192", Formatter.formatIpAddress(0xC0A8B623));
        assertEquals("0.255.255.255", Formatter.formatIpAddress(0xFFFFFF00));
        assertEquals("222.5.15.10", Formatter.formatIpAddress(0x0A0F05DE));
    }
}
