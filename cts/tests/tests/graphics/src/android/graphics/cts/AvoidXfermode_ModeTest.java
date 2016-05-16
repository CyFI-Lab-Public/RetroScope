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

import android.graphics.AvoidXfermode;
import android.graphics.AvoidXfermode.Mode;
import android.test.AndroidTestCase;

public class AvoidXfermode_ModeTest extends AndroidTestCase{

    public void testValueOf(){
        assertEquals(Mode.AVOID, Mode.valueOf("AVOID"));
        assertEquals(Mode.TARGET, Mode.valueOf("TARGET"));
    }

    public void testValues(){
        Mode[] mode = Mode.values();

        assertEquals(2, mode.length);
        assertEquals(Mode.AVOID, mode[0]);
        assertEquals(Mode.TARGET, mode[1]);

        //Mode is used as a argument here for all the methods that use it
        assertNotNull(new AvoidXfermode(10, 24, Mode.AVOID));
        assertNotNull(new AvoidXfermode(10, 24, Mode.TARGET));
    }
}
