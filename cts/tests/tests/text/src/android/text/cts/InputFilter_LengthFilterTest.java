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

package android.text.cts;

import android.test.AndroidTestCase;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.InputFilter.LengthFilter;

public class InputFilter_LengthFilterTest extends AndroidTestCase {

    public void testFilter() {
        // Define the variables
        CharSequence source;
        SpannableStringBuilder dest;
        // Constructor to create a LengthFilter
        LengthFilter lengthFilter = new LengthFilter(10);
        InputFilter[] filters = {lengthFilter};

        // filter() implicitly invoked. If the total length > filter length, the filter will
        // cut off the source CharSequence from beginning to fit the filter length.
        source = "abc";
        dest = new SpannableStringBuilder("abcdefgh");
        dest.setFilters(filters);

        dest.insert(1, source);
        String expectedString1 = "aabbcdefgh";
        assertEquals(expectedString1, dest.toString());

        dest.replace(5, 8, source);
        String expectedString2 = "aabbcabcgh";
        assertEquals(expectedString2, dest.toString());

        dest.delete(1, 3);
        String expectedString3 = "abcabcgh";
        assertEquals(expectedString3, dest.toString());

        // filter() explicitly invoked
        dest = new SpannableStringBuilder("abcdefgh");
        CharSequence beforeFilterSource = "TestLengthFilter";
        String expectedAfterFilter = "TestLength";
        CharSequence actualAfterFilter = lengthFilter.filter(beforeFilterSource, 0,
                beforeFilterSource.length(), dest, 0, dest.length());
        assertEquals(expectedAfterFilter, actualAfterFilter);
    }
}
