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

package android.widget.cts;

import com.android.cts.stub.R;


import org.xmlpull.v1.XmlPullParser;

import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.widget.CheckBox;

public class CheckBoxTest extends AndroidTestCase {
    public void testConstructor() {
        XmlPullParser parser = mContext.getResources().getXml(R.layout.checkbox_layout);
        AttributeSet mAttrSet = Xml.asAttributeSet(parser);

        new CheckBox(mContext, mAttrSet, 0);
        new CheckBox(mContext, mAttrSet);
        new CheckBox(mContext);

        try {
            new CheckBox(null, null, -1);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new CheckBox(null, null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new CheckBox(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }
}
