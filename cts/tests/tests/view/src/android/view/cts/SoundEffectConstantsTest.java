/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.view.cts;

import android.test.AndroidTestCase;
import android.view.SoundEffectConstants;
import android.view.View;

public class SoundEffectConstantsTest extends AndroidTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    public void testgetContantForFocusDirection() {

        assertEquals(SoundEffectConstants.NAVIGATION_RIGHT,
                SoundEffectConstants
                        .getContantForFocusDirection(View.FOCUS_RIGHT));
        assertEquals(SoundEffectConstants.NAVIGATION_DOWN, SoundEffectConstants
                .getContantForFocusDirection(View.FOCUS_DOWN));
        assertEquals(SoundEffectConstants.NAVIGATION_LEFT, SoundEffectConstants
                .getContantForFocusDirection(View.FOCUS_LEFT));
        assertEquals(SoundEffectConstants.NAVIGATION_UP, SoundEffectConstants
                .getContantForFocusDirection(View.FOCUS_UP));

        assertEquals(SoundEffectConstants.NAVIGATION_DOWN, SoundEffectConstants
                .getContantForFocusDirection(View.FOCUS_FORWARD));

        assertEquals(SoundEffectConstants.NAVIGATION_UP, SoundEffectConstants
                .getContantForFocusDirection(View.FOCUS_BACKWARD));
        try {
            SoundEffectConstants.getContantForFocusDirection(-1);
            fail("should throw exception");
        } catch (RuntimeException e) {

        }
    }
}
