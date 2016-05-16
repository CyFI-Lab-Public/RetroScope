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

package android.graphics.cts;

import android.graphics.PathDashPathEffect;
import android.graphics.PathDashPathEffect.Style;
import android.test.AndroidTestCase;

public class PathDashPathEffect_StyleTest extends AndroidTestCase {


    public void testValueOf() {
        assertEquals(Style.TRANSLATE, Style.valueOf("TRANSLATE"));
        assertEquals(Style.ROTATE, Style.valueOf("ROTATE"));
        assertEquals(Style.MORPH, Style.valueOf("MORPH"));
        // Every Style element will be tested somewhere else.
    }

    public void testValues() {
        // set the expected value
        Style[] expected = {
                Style.TRANSLATE,
                Style.ROTATE,
                Style.MORPH};
        Style[] actual = Style.values();
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < actual.length; i ++) {
            assertEquals(expected[i], actual[i]);
        }
    }

}
