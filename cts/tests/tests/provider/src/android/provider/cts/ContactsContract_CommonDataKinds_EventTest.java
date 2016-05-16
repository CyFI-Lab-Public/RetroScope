/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.provider.cts;

import android.provider.ContactsContract.CommonDataKinds.Event;
import android.test.AndroidTestCase;

public class ContactsContract_CommonDataKinds_EventTest extends AndroidTestCase {

    public void testGetTypeLabel() {
        assertGetTypeLabel(Event.TYPE_ANNIVERSARY);
        assertGetTypeLabel(Event.TYPE_BIRTHDAY);
        assertGetTypeLabel(Event.TYPE_OTHER);
        assertGetTypeLabel(Event.TYPE_CUSTOM);
        assertGetTypeLabel(null);
    }

    private void assertGetTypeLabel(Integer type) {
        int res = Event.getTypeResource(type);
        assertTrue(res != 0);
    }
}
