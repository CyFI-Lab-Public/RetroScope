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

import android.content.res.Resources;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.test.AndroidTestCase;

public class ContactsContract_CommonDataKinds_EmailTest extends AndroidTestCase {

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
    }

    public void testGetTypeLabel() {
        assertGetTypeLabel(Email.TYPE_HOME);
        assertGetTypeLabel(Email.TYPE_MOBILE);
        assertGetTypeLabel(Email.TYPE_OTHER);
        assertGetTypeLabel(Email.TYPE_WORK);
        assertGetTypeLabel(Email.TYPE_CUSTOM);
        assertCustomTypeLabel("Custom Label");
    }

    private void assertGetTypeLabel(int type) {
        int res = Email.getTypeLabelResource(type);
        assertTrue(res != 0);

        String label = mResources.getString(res);
        assertEquals(label, Email.getTypeLabel(mResources, type, ""));
    }

    private void assertCustomTypeLabel(String label) {
        int res = Email.getTypeLabelResource(Email.TYPE_CUSTOM);
        assertTrue(res != 0);
        assertEquals(label, Email.getTypeLabel(mResources, Email.TYPE_CUSTOM, label));
    }
}
