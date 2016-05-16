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

package android.text.cts;

import android.os.Parcel;
import android.test.AndroidTestCase;
import android.text.Annotation;

public class AnnotationTest extends AndroidTestCase {

    private static final String KEY1 = "name";
    private static final String KEY2 = "family name";
    private static final String VALUE1 = "John";
    private static final String VALUE2 = "Smith";
    private static final int NOFLAG = 0;
    private Annotation mAnnotation;

    @Override
    protected void setUp() throws Exception {

        super.setUp();
        mAnnotation = null;
    }

    public void testConstructor() {
        // new the Annotation instance
        new Annotation(KEY1, VALUE1);
    }

    public void testGetValue() {
        // new the Annotation instance
        mAnnotation = new Annotation(KEY1, VALUE1);
        assertEquals(VALUE1, mAnnotation.getValue());
        mAnnotation = new Annotation(KEY2, VALUE2);
        assertEquals(VALUE2, mAnnotation.getValue());
    }

    public void testGetKey() {
        // new the Annotation instance
        mAnnotation = new Annotation(KEY1, VALUE1);
        assertEquals(KEY1, mAnnotation.getKey());
        mAnnotation = new Annotation(KEY2, VALUE2);
        assertEquals(KEY2, mAnnotation.getKey());
    }

    public void testGetSpanTypeId() {
        mAnnotation = new Annotation(KEY1, VALUE1);
        // Because of the return value is a hide value, we only can assert the return value isn't 0.
        assertTrue(mAnnotation.getSpanTypeId() != 0);
    }

    public void testWriteToParcel() {
        Parcel dest = Parcel.obtain();
        try {
            mAnnotation = new Annotation(KEY1, VALUE1);
            mAnnotation.writeToParcel(dest, NOFLAG);
            dest.setDataPosition(0);
            Annotation out = new Annotation(dest);
            assertEquals(out.getKey(), mAnnotation.getKey());
            assertEquals(out.getValue(), mAnnotation.getValue());

            assertEquals(0, out.describeContents());
        } finally {
            dest.recycle();
        }
    }
}
