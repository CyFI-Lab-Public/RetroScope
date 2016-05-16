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

package android.content.res.cts;

import org.xmlpull.v1.XmlPullParser;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.Theme;
import android.test.AndroidTestCase;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.util.Xml;

import com.android.cts.stub.R;


public class Resources_ThemeTest extends AndroidTestCase {

    private Resources.Theme mResTheme;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mResTheme = getContext().getResources().newTheme();
    }

    public void testSetMethods() {
        // call a native method, and have no way to get the style
        mResTheme.applyStyle(R.raw.testmp3, false);
        // call a native method, this method is just for debug to the log
        mResTheme.dump(1, "hello", "world");
        // call a native method
        final Theme other = getContext().getTheme();
        mResTheme.setTo(other);
    }

    public void testObtainStyledAttributes() {
        final int[] attrs = new int[1];
        attrs[0] = R.raw.testmp3;

        TypedArray testTypedArray = mResTheme.obtainStyledAttributes(attrs);
        assertNotNull(testTypedArray);
        assertTrue(testTypedArray.length() > 0);
        testTypedArray.recycle();

        testTypedArray = mResTheme.obtainStyledAttributes(R.raw.testmp3, attrs);
        assertNotNull(testTypedArray);
        assertTrue(testTypedArray.length() > 0);
        testTypedArray.recycle();

        XmlPullParser parser = getContext().getResources().getXml(R.xml.colors);
        AttributeSet set = Xml.asAttributeSet(parser);
        attrs[0] = R.xml.colors;
        testTypedArray =mResTheme.obtainStyledAttributes(set, attrs, 0, 0);
        assertNotNull(testTypedArray);
        assertTrue(testTypedArray.length() > 0);
        testTypedArray.recycle();
    }

    public void testResolveAttribute() {
        final TypedValue value = new TypedValue();
        getContext().getResources().getValue(R.raw.testmp3, value, true);
        assertFalse(mResTheme.resolveAttribute(R.raw.testmp3, value, false));
    }

}
