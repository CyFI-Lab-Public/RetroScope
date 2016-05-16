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

import android.graphics.Matrix.ScaleToFit;
import android.test.AndroidTestCase;

public class Matrix_ScaleToFitTest extends AndroidTestCase {


    public void testValues() {
        ScaleToFit[] scaleToFits = ScaleToFit.values();
        assertEquals(ScaleToFit.FILL,scaleToFits[0]);
        assertEquals( ScaleToFit.START,scaleToFits[1]);
        assertEquals( ScaleToFit.CENTER,scaleToFits[2]);
        assertEquals( ScaleToFit.END,scaleToFits[3]);
    }

    public void testValueOf() {
        assertEquals(ScaleToFit.FILL,ScaleToFit.valueOf("FILL"));
        assertEquals( ScaleToFit.START,ScaleToFit.valueOf("START"));
        assertEquals( ScaleToFit.CENTER,ScaleToFit.valueOf("CENTER"));
        assertEquals(ScaleToFit.END,ScaleToFit.valueOf("END") );
    }

    public void testValueOf2() {
        assertEquals(ScaleToFit.FILL, ScaleToFit.valueOf(ScaleToFit.class,
                "FILL"));
        assertEquals(ScaleToFit.START, ScaleToFit.valueOf(ScaleToFit.class,
                "START"));
        assertEquals(ScaleToFit.CENTER, ScaleToFit.valueOf(ScaleToFit.class,
                "CENTER"));
        assertEquals(ScaleToFit.END, ScaleToFit
                .valueOf(ScaleToFit.class, "END"));
    }
}
