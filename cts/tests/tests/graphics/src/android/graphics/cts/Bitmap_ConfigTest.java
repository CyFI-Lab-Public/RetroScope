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
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

public class Bitmap_ConfigTest extends TestCase {

    public void testValueOf(){
        assertEquals(Config.ALPHA_8, Config.valueOf("ALPHA_8"));
        assertEquals(Config.RGB_565, Config.valueOf("RGB_565"));
        assertEquals(Config.ARGB_4444, Config.valueOf("ARGB_4444"));
        assertEquals(Config.ARGB_8888, Config.valueOf("ARGB_8888"));
    }

    public void testValues(){
        Config[] config = Config.values();

        assertEquals(4, config.length);
        assertEquals(Config.ALPHA_8, config[0]);
        assertEquals(Config.RGB_565, config[1]);
        assertEquals(Config.ARGB_4444, config[2]);
        assertEquals(Config.ARGB_8888, config[3]);

        //Config is used as a argument here for all the methods that use it
        assertNotNull(Bitmap.createBitmap(10, 24, Config.ALPHA_8));
        assertNotNull(Bitmap.createBitmap(10, 24, Config.ARGB_4444));
        assertNotNull(Bitmap.createBitmap(10, 24, Config.ARGB_8888));
        assertNotNull(Bitmap.createBitmap(10, 24, Config.RGB_565));
    }
}
