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
import android.text.InputFilter.AllCaps;

public class InputFilter_AllCapsTest extends AndroidTestCase {

    public void testFilter() {

        // Implicitly invoked
        CharSequence source = "Caps";
        SpannableStringBuilder dest = new SpannableStringBuilder("AllTest");
        AllCaps allCaps = new AllCaps();
        InputFilter[] filters = {allCaps};
        dest.setFilters(filters);

        String expectedString1 = "AllCAPSTest";
        dest.insert(3, source);
        assertEquals(expectedString1 , dest.toString());

        String expectedString2 = "AllCAPSCAPS";
        dest.replace(7, 11, source);
        assertEquals(expectedString2, dest.toString());

        dest.delete(0, 4);
        String expectedString3 = "APSCAPS";
        assertEquals(expectedString3, dest.toString());

        // Explicitly invoked
        CharSequence beforeFilterSource = "TestFilter";
        String expectedAfterFilter = "STFIL";
        CharSequence actualAfterFilter =
            allCaps.filter(beforeFilterSource, 2, 7, dest, 0, beforeFilterSource.length());
        assertEquals(expectedAfterFilter, actualAfterFilter);
    }
}
