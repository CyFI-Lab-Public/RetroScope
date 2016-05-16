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
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.test.AndroidTestCase;

public class ContactsContract_CommonDataKinds_StructuredPostalTest extends AndroidTestCase {

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
    }

    public void testGetTypeLabel() {
        assertGetTypeLabel(StructuredPostal.TYPE_HOME);
        assertGetTypeLabel(StructuredPostal.TYPE_OTHER);
        assertGetTypeLabel(StructuredPostal.TYPE_WORK);
        assertGetTypeLabel(StructuredPostal.TYPE_CUSTOM);
        assertCustomTypeLabel("Custom Label");
    }

    private void assertGetTypeLabel(int type) {
        int res = StructuredPostal.getTypeLabelResource(type);
        assertTrue(res != 0);

        String label = mResources.getString(res);
        assertEquals(label, StructuredPostal.getTypeLabel(mResources, type, ""));
    }

    private void assertCustomTypeLabel(String label) {
        int res = StructuredPostal.getTypeLabelResource(StructuredPostal.TYPE_CUSTOM);
        assertTrue(res != 0);
        assertEquals(label, StructuredPostal.getTypeLabel(mResources,
                StructuredPostal.TYPE_CUSTOM, label));
    }
}
