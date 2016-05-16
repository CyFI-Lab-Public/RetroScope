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

package android.text.method.cts;

import android.view.KeyEvent;

import junit.framework.Assert;

public class TextMethodUtils {
    /**
     * Assert that two char arrays are equal.
     *
     * @param expected the expected char array.
     * @param actual the actual char array.
     */
    public static void assertEquals(char[] expected, char[] actual) {
        if (expected != actual) {
            if (expected == null || actual == null) {
                Assert.fail("the char arrays are not equal");
            }

            Assert.assertEquals(String.valueOf(expected), String.valueOf(actual));
        }
    }

    /**
     * Get an unaccepted key code.
     *
     * @param acceptedChars accepted chars array.
     * @return return key code if we find unaccepted one, or return -1.
     */
    public static int getUnacceptedKeyCode(char[] acceptedChars) {
        for (int keyCode = KeyEvent.KEYCODE_A; keyCode <= KeyEvent.KEYCODE_Z; keyCode++) {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            if ('\0' == event.getMatch(acceptedChars)) {
                return keyCode;
            }
        }
        return -1;
    }
}
