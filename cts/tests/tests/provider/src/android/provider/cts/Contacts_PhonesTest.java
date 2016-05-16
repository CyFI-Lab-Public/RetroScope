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

package android.provider.cts;

import android.content.Context;
import android.provider.Contacts.Phones;
import android.test.AndroidTestCase;

public class Contacts_PhonesTest extends AndroidTestCase {
    public void testGetDisplayLabel() {
        CharSequence label = "label";
        String display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_CUSTOM, label).toString();
        assertEquals(label, display);

        CharSequence[] labels = getContext().getResources().getTextArray(
                com.android.internal.R.array.phoneTypes);
        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_HOME, label).toString();
        assertEquals(labels[Phones.TYPE_HOME - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_MOBILE, label).toString();
        assertEquals(labels[Phones.TYPE_MOBILE - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_WORK, label).toString();
        assertEquals(labels[Phones.TYPE_WORK - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_FAX_WORK, label).toString();
        assertEquals(labels[Phones.TYPE_FAX_WORK - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_FAX_HOME, label).toString();
        assertEquals(labels[Phones.TYPE_FAX_HOME - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_PAGER, label).toString();
        assertEquals(labels[Phones.TYPE_PAGER - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_OTHER, label).toString();
        assertEquals(labels[Phones.TYPE_OTHER - 1], display);
    }

    public void testGetDisplayLabelCharSequenceArray() {
        CharSequence label = "label";
        CharSequence[] labelArray = new CharSequence[] {
                "1 home",
                "2 mobile",
                "3 work",
                "4 fax work",
                "5 fax home",
                "6 pager",
                "7 other"};

        String display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_CUSTOM, label, labelArray).toString();
        assertEquals(label, display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_HOME, label, labelArray).toString();
        assertEquals(labelArray[Phones.TYPE_HOME - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_MOBILE, label, labelArray).toString();
        assertEquals(labelArray[Phones.TYPE_MOBILE - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_WORK, label, labelArray).toString();
        assertEquals(labelArray[Phones.TYPE_WORK - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_FAX_WORK, label, labelArray).toString();
        assertEquals(labelArray[Phones.TYPE_FAX_WORK - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_FAX_HOME, label, labelArray).toString();
        assertEquals(labelArray[Phones.TYPE_FAX_HOME - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_PAGER, label, labelArray).toString();
        assertEquals(labelArray[Phones.TYPE_PAGER - 1], display);

        display = Phones.getDisplayLabel(getContext(),
                Phones.TYPE_OTHER, label, labelArray).toString();
        assertEquals(labelArray[Phones.TYPE_OTHER - 1], display);
    }
}
