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
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.test.AndroidTestCase;

public class ContactsContract_CommonDataKinds_RelationTest extends AndroidTestCase {

    private Resources mResources;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResources = mContext.getResources();
    }

    public void testGetTypeLabel() {
        assertGetTypeLabel(Relation.TYPE_ASSISTANT);
        assertGetTypeLabel(Relation.TYPE_BROTHER);
        assertGetTypeLabel(Relation.TYPE_CHILD);
        assertGetTypeLabel(Relation.TYPE_DOMESTIC_PARTNER);
        assertGetTypeLabel(Relation.TYPE_FATHER);
        assertGetTypeLabel(Relation.TYPE_FRIEND);
        assertGetTypeLabel(Relation.TYPE_MANAGER);
        assertGetTypeLabel(Relation.TYPE_MOTHER);
        assertGetTypeLabel(Relation.TYPE_PARENT);
        assertGetTypeLabel(Relation.TYPE_PARTNER);
        assertGetTypeLabel(Relation.TYPE_REFERRED_BY);
        assertGetTypeLabel(Relation.TYPE_RELATIVE);
        assertGetTypeLabel(Relation.TYPE_SISTER);
        assertGetTypeLabel(Relation.TYPE_SPOUSE);
        assertGetTypeLabel(Relation.TYPE_CUSTOM);
        assertCustomTypeLabel("Custom Label");
    }

    private void assertGetTypeLabel(int type) {
        int res = Relation.getTypeLabelResource(type);
        assertTrue(res != 0);

        String label = mResources.getString(res);
        assertEquals(label, Relation.getTypeLabel(mResources, type, ""));
    }

    private void assertCustomTypeLabel(String label) {
        int res = Relation.getTypeLabelResource(Relation.TYPE_CUSTOM);
        assertTrue(res != 0);
        assertEquals(label, Relation.getTypeLabel(mResources, Relation.TYPE_CUSTOM, label));
    }
}
