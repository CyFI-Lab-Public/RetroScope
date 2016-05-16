/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.sax.cts;

import android.sax.RootElement;
import android.test.AndroidTestCase;

public class RootElementTest extends AndroidTestCase {

    private static final String ATOM_NAMESPACE = "http://www.w3.org/2005/Atom";
    private static final String FEED = "feed";

    public void testRoot() throws Exception {
        RootElement root = new RootElement(ATOM_NAMESPACE, FEED);
        assertNotNull(root.getContentHandler());

        RootElement root1 = new RootElement(FEED);
        assertNotNull(root1.getContentHandler());
    }
}
