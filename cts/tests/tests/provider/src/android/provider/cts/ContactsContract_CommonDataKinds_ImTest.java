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
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.test.AndroidTestCase;

public class ContactsContract_CommonDataKinds_ImTest extends AndroidTestCase {

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
    }

    public void testGetProtocolLabel() {
        assertGetProtocolLabel(Im.PROTOCOL_AIM);
        assertGetProtocolLabel(Im.PROTOCOL_CUSTOM);
        assertGetProtocolLabel(Im.PROTOCOL_GOOGLE_TALK);
        assertGetProtocolLabel(Im.PROTOCOL_ICQ);
        assertGetProtocolLabel(Im.PROTOCOL_JABBER);
        assertGetProtocolLabel(Im.PROTOCOL_MSN);
        assertGetProtocolLabel(Im.PROTOCOL_NETMEETING);
        assertGetProtocolLabel(Im.PROTOCOL_QQ);
        assertGetProtocolLabel(Im.PROTOCOL_SKYPE);
        assertGetProtocolLabel(Im.PROTOCOL_YAHOO);
        assertCustomProtocolLabel("Custom Label");
    }

    public void testGetTypeLabel() {
        assertGetTypeLabel(Im.TYPE_HOME);
        assertGetTypeLabel(Im.TYPE_WORK);
        assertGetTypeLabel(Im.TYPE_OTHER);
        assertGetTypeLabel(Im.TYPE_CUSTOM);
        assertCustomTypeLabel("Custom Label");
    }

    private void assertGetProtocolLabel(int type) {
        int res = Im.getProtocolLabelResource(type);
        assertTrue(res != 0);

        String label = mResources.getString(res);
        assertEquals(label, Im.getProtocolLabel(mResources, type, ""));
    }

    private void assertCustomProtocolLabel(String label) {
        int res = Im.getProtocolLabelResource(Im.PROTOCOL_CUSTOM);
        assertTrue(res != 0);
        assertEquals(label, Im.getProtocolLabel(mResources, Im.PROTOCOL_CUSTOM, label));
    }

    private void assertGetTypeLabel(int type) {
        int res = Im.getTypeLabelResource(type);
        assertTrue(res != 0);

        String label = mResources.getString(res);
        assertEquals(label, Im.getTypeLabel(mResources, type, ""));
    }

    private void assertCustomTypeLabel(String label) {
        int res = Im.getTypeLabelResource(Im.TYPE_CUSTOM);
        assertTrue(res != 0);
        assertEquals(label, Im.getTypeLabel(mResources, Im.TYPE_CUSTOM, label));
    }
}
