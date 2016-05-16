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

package android.net.cts;

import android.net.Credentials;
import android.test.AndroidTestCase;

public class CredentialsTest extends AndroidTestCase {

    public void testCredentials() {
        // new the Credentials instance
        // Test with zero inputs
        Credentials cred = new Credentials(0, 0, 0);
        assertEquals(0, cred.getGid());
        assertEquals(0, cred.getPid());
        assertEquals(0, cred.getUid());

        // Test with big integer
        cred = new Credentials(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, cred.getGid());
        assertEquals(Integer.MAX_VALUE, cred.getPid());
        assertEquals(Integer.MAX_VALUE, cred.getUid());

        // Test with big negative integer
        cred = new Credentials(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, cred.getGid());
        assertEquals(Integer.MIN_VALUE, cred.getPid());
        assertEquals(Integer.MIN_VALUE, cred.getUid());
    }
}
