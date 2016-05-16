/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.graphics.Shader;
import android.graphics.Shader.TileMode;

public class Shader_TileModeTest extends TestCase {

    public void testValueOf() {
        assertEquals(TileMode.CLAMP, TileMode.valueOf("CLAMP"));
        assertEquals(TileMode.MIRROR, TileMode.valueOf("MIRROR"));
        assertEquals(TileMode.REPEAT, TileMode.valueOf("REPEAT"));
    }

    public void testValues() {
        TileMode[] tileMode = TileMode.values();
        assertEquals(3, tileMode.length);
        assertEquals(TileMode.CLAMP, tileMode[0]);
        assertEquals(TileMode.REPEAT, tileMode[1]);
        assertEquals(TileMode.MIRROR, tileMode[2]);
    }
}
