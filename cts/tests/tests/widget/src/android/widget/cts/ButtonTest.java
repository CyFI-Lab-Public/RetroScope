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

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.Xml;
import android.widget.Button;

import com.android.cts.stub.R;


public class ButtonTest extends AndroidTestCase {
    public void testConstructor() {
        XmlPullParser parser = mContext.getResources().getXml(R.layout.togglebutton_layout);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        new Button(mContext, attrs, 0);
        new Button(mContext, attrs);
        new Button(mContext);

        try {
            new Button(null, null, -1);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new Button(null, null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }

        try {
            new Button(null);
            fail("Should throw NullPointerException.");
        } catch (NullPointerException e) {
            // expected, test success.
        }
    }
}
