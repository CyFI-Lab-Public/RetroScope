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


import android.text.LoginFilter.PasswordFilterGMail;

import junit.framework.TestCase;

public class LoginFilter_PasswordFilterGMailTest extends TestCase {

    public void testConstructor() {
        new PasswordFilterGMail();
        new PasswordFilterGMail(true);
        new PasswordFilterGMail(false);
    }

    public void testIsAllowed() {
        PasswordFilterGMail passwordFilterGMail = new PasswordFilterGMail();

        assertTrue(passwordFilterGMail.isAllowed('a'));
        assertTrue(passwordFilterGMail.isAllowed((char) 200));
        assertFalse(passwordFilterGMail.isAllowed('\n'));
        assertFalse(passwordFilterGMail.isAllowed((char) 150));

        // boundary test
        assertFalse(passwordFilterGMail.isAllowed((char) 0));

        assertFalse(passwordFilterGMail.isAllowed((char) 31));
        assertTrue(passwordFilterGMail.isAllowed((char) 32));

        assertTrue(passwordFilterGMail.isAllowed((char) 127));
        assertFalse(passwordFilterGMail.isAllowed((char) 128));

        assertFalse(passwordFilterGMail.isAllowed((char) 159));
        assertTrue(passwordFilterGMail.isAllowed((char) 160));

        assertTrue(passwordFilterGMail.isAllowed((char) 255));
    }
}
