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
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.test.AndroidTestCase;

public class ContactsContract_CommonDataKinds_PhoneTest extends AndroidTestCase {

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
    }

    public void testGetTypeLabel() {
        assertGetTypeLabel(Phone.TYPE_ASSISTANT);
        assertGetTypeLabel(Phone.TYPE_CALLBACK);
        assertGetTypeLabel(Phone.TYPE_CAR);
        assertGetTypeLabel(Phone.TYPE_COMPANY_MAIN);
        assertGetTypeLabel(Phone.TYPE_FAX_HOME);
        assertGetTypeLabel(Phone.TYPE_FAX_WORK);
        assertGetTypeLabel(Phone.TYPE_HOME);
        assertGetTypeLabel(Phone.TYPE_ISDN);
        assertGetTypeLabel(Phone.TYPE_MAIN);
        assertGetTypeLabel(Phone.TYPE_MMS);
        assertGetTypeLabel(Phone.TYPE_MOBILE);
        assertGetTypeLabel(Phone.TYPE_OTHER);
        assertGetTypeLabel(Phone.TYPE_OTHER_FAX);
        assertGetTypeLabel(Phone.TYPE_PAGER);
        assertGetTypeLabel(Phone.TYPE_RADIO);
        assertGetTypeLabel(Phone.TYPE_TELEX);
        assertGetTypeLabel(Phone.TYPE_TTY_TDD);
        assertGetTypeLabel(Phone.TYPE_WORK);
        assertGetTypeLabel(Phone.TYPE_WORK_MOBILE);
        assertGetTypeLabel(Phone.TYPE_WORK_PAGER);
        assertGetTypeLabel(Phone.TYPE_CUSTOM);
        assertCustomTypeLabel("Custom Label");
    }

    private void assertGetTypeLabel(int type) {
        int res = Phone.getTypeLabelResource(type);
        assertTrue(res != 0);

        String label = mResources.getString(res);
        assertEquals(label, Phone.getTypeLabel(mResources, type, ""));
    }

    private void assertCustomTypeLabel(String label) {
        int res = Phone.getTypeLabelResource(Phone.TYPE_CUSTOM);
        assertTrue(res != 0);
        assertEquals(label, Phone.getTypeLabel(mResources, Phone.TYPE_CUSTOM, label));
    }
}
