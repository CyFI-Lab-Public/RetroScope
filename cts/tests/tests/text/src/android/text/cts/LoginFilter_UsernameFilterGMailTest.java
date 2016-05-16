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


import android.text.LoginFilter.UsernameFilterGMail;

import junit.framework.TestCase;

public class LoginFilter_UsernameFilterGMailTest extends TestCase {
    public void testConstructor() {
        new UsernameFilterGMail();
        new UsernameFilterGMail(true);
        new UsernameFilterGMail(false);
    }

    public void testIsAllowed() {
        UsernameFilterGMail usernameFilterGMail = new UsernameFilterGMail();

        assertTrue(usernameFilterGMail.isAllowed('c'));
        assertTrue(usernameFilterGMail.isAllowed('C'));
        assertTrue(usernameFilterGMail.isAllowed('3'));
        assertFalse(usernameFilterGMail.isAllowed('#'));
        assertFalse(usernameFilterGMail.isAllowed('%'));

        // boundary test
        assertTrue(usernameFilterGMail.isAllowed('0'));
        assertTrue(usernameFilterGMail.isAllowed('9'));

        assertTrue(usernameFilterGMail.isAllowed('a'));
        assertTrue(usernameFilterGMail.isAllowed('z'));

        assertTrue(usernameFilterGMail.isAllowed('A'));
        assertTrue(usernameFilterGMail.isAllowed('Z'));

        assertTrue(usernameFilterGMail.isAllowed('.'));

        assertFalse(usernameFilterGMail.isAllowed('/'));
        assertFalse(usernameFilterGMail.isAllowed(':'));
        assertFalse(usernameFilterGMail.isAllowed('`'));
        assertFalse(usernameFilterGMail.isAllowed('{'));
        assertFalse(usernameFilterGMail.isAllowed('@'));
        assertFalse(usernameFilterGMail.isAllowed('['));
    }
}
