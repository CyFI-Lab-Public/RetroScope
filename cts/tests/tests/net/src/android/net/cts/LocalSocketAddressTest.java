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

import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.test.AndroidTestCase;

public class LocalSocketAddressTest extends AndroidTestCase {

    public void testNewLocalSocketAddressWithDefaultNamespace() {
        // default namespace
        LocalSocketAddress localSocketAddress = new LocalSocketAddress("name");
        assertEquals("name", localSocketAddress.getName());
        assertEquals(Namespace.ABSTRACT, localSocketAddress.getNamespace());

        // specify the namespace
        LocalSocketAddress localSocketAddress2 =
                new LocalSocketAddress("name2", Namespace.ABSTRACT);
        assertEquals("name2", localSocketAddress2.getName());
        assertEquals(Namespace.ABSTRACT, localSocketAddress2.getNamespace());

        LocalSocketAddress localSocketAddress3 =
                new LocalSocketAddress("name3", Namespace.FILESYSTEM);
        assertEquals("name3", localSocketAddress3.getName());
        assertEquals(Namespace.FILESYSTEM, localSocketAddress3.getNamespace());

        LocalSocketAddress localSocketAddress4 =
                new LocalSocketAddress("name4", Namespace.RESERVED);
        assertEquals("name4", localSocketAddress4.getName());
        assertEquals(Namespace.RESERVED, localSocketAddress4.getNamespace());
    }
}
