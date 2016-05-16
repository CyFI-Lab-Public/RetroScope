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
import android.widget.ImageButton;

public class ImageButtonTest extends AndroidTestCase {
    public void testConstructor() {
        XmlPullParser parser = getContext().getResources().getXml(R.layout.imagebutton_test);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        assertNotNull(attrs);

        new ImageButton(getContext());

        new ImageButton(getContext(), attrs);

        new ImageButton(getContext(), attrs, 0);

        try {
            new ImageButton(null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new ImageButton(null, null);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }

        try {
            new ImageButton(null, null, -1);
            fail("should throw NullPointerException.");
        } catch (NullPointerException e) {
        }
    }

    public void testOnSetAlpha() {
        // Do not test, it's controlled by View. Implementation details.
    }
}
