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

import junit.framework.TestCase;
import android.graphics.Paint.Join;
import android.graphics.Paint;

public class Paint_JoinTest extends TestCase {

    public void testValueOf() {
        assertEquals(Join.BEVEL, Join.valueOf("BEVEL"));
        assertEquals(Join.MITER, Join.valueOf("MITER"));
        assertEquals(Join.ROUND, Join.valueOf("ROUND"));
    }

    public void testValues() {
        // set the actual value
        Join[] actual = Join.values();

        assertEquals(3, actual.length);
        assertEquals(Join.MITER, actual[0]);
        assertEquals(Join.ROUND, actual[1]);
        assertEquals(Join.BEVEL, actual[2]);

        // Here we use Join as the param of setStrokeJoin
        // and get the setting result by getStrokeJoin
        Paint p = new Paint();
        p.setStrokeJoin(actual[0]);
        assertEquals(Join.MITER, p.getStrokeJoin());
        p.setStrokeJoin(actual[1]);
        assertEquals(Join.ROUND, p.getStrokeJoin());
        p.setStrokeJoin(actual[2]);
        assertEquals(Join.BEVEL, p.getStrokeJoin());
    }
}
