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

import junit.framework.TestCase;
import android.util.FloatMath;

public class FloatMathTest extends TestCase {
    public void testFloatMathMethods() {
        // ceil
        assertEquals(8.0f, FloatMath.ceil(7.2f));
        assertEquals(-6.0f, FloatMath.ceil(-6.3f));

        // floor
        assertEquals(7.0f, FloatMath.floor(7.2f));
        assertEquals(-7.0f, FloatMath.floor(-6.3f));

        // sin
        assertEquals(-0.26237485f, FloatMath.sin(50));
        assertEquals(-0.71487643f, FloatMath.sin(150));
        assertEquals(0.26237485f, FloatMath.sin(-50));

        // cos
        assertEquals(0.964966f, FloatMath.cos(50));
        assertEquals(0.69925081f, FloatMath.cos(150));
        assertEquals(0.964966f, FloatMath.cos(-50));

        // sqrt
        assertEquals(5.0f, FloatMath.sqrt(25));
    }

}
