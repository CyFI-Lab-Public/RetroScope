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
package android.util.cts;

import android.test.AndroidTestCase;
import android.util.StringBuilderPrinter;

public class StringBuilderPrinterTest extends AndroidTestCase{
    public void testStringBuilderPrinter(){
        StringBuilder strBuilder = new StringBuilder("Hello");
        StringBuilderPrinter strBuilderPrinter = new StringBuilderPrinter(strBuilder);
        assertEquals("Hello", strBuilder.toString());

        strBuilderPrinter.println(" Android");

        String str = strBuilder.toString();
        assertTrue(str.startsWith("Hello"));
        assertEquals(' ', str.charAt(5));
        assertEquals('A', str.charAt(6));
        assertEquals('n', str.charAt(7));
        assertEquals('d', str.charAt(8));
        assertEquals('r', str.charAt(9));
        assertEquals('o', str.charAt(10));
        assertEquals('i', str.charAt(11));
        assertEquals('d', str.charAt(12));
    }
}
