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

package android.content.cts;

import android.content.ComponentName;
import android.content.Context;
import android.os.Parcel;
import android.test.AndroidTestCase;

/**
 * Test {@link ComponentName}.
 */
public class ComponentNameTest extends AndroidTestCase {
    public void testConstructor() {
        // new the ComponentName instance
        new ComponentName("com.android.app", "com.android.app.InstrumentationTestActivity");

        // Test null string
        try {
            new ComponentName((String) null, (String) null);
            fail("ComponentName's constructor (String, Stirng) can not accept null input values.");
        } catch (NullPointerException e) {
            // expected
        }

        // new the ComponentName instance: test real Context , real class name string
        new ComponentName(mContext, "ActivityTestCase");

        // Test null Context, real class name string input, return should be null
        try {
            new ComponentName((Context) null, "ActivityTestCase");
            fail("class name is null, the constructor should throw a exception");
        } catch (NullPointerException e) {
            // expected
        }

        // Test real Context, null name string input, return should not be null
        try {
            new ComponentName(mContext, (String) null);
            fail("Constructor should not accept null class name.");
        } catch (NullPointerException e) {
            // expected
        }

        // new the ComponentName instance: real Context, real class input, return shouldn't be null
        new ComponentName(mContext, this.getClass());

        // new the ComponentName instance: real Context, null class input, return shouldn't be null
        try {
            new ComponentName(mContext, (Class<?>) null);
            fail("If class name is null, contructor should throw a exception");
        } catch (NullPointerException e) {
            // expected
        }

        // new the ComponentName instance, Test null Parcel
        try {
            new ComponentName((Parcel) null);
            fail("Constructor should not accept null Parcel input.");
        } catch (NullPointerException e) {
            // expected
        }

        // new the ComponentName instance, Test null Parcel
        final Parcel parcel = Parcel.obtain();

        final ComponentName componentName = getComponentName();
        componentName.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        new ComponentName(parcel);
    }

    public void testFlattenToString() {
        assertEquals("com.android.cts.stub/android.content.cts.ComponentNameTest",
                getComponentName().flattenToString());
    }

    public void testGetShortClassName() {
        // set the expected value, test normal value
        String actual = getComponentName().getShortClassName();
        assertEquals("android.content.cts.ComponentNameTest", actual);

        // Test class name which can be abbreviated
        ComponentName componentName = new ComponentName("com.android.view",
                "com.android.view.View");
        final String className = componentName.getClassName();
        // First, check the string return by getClassName().
        assertEquals("com.android.view.View", className);
        actual = componentName.getShortClassName();
        // Then, check the string return by getShortClassName().
        assertEquals(".View", actual);
    }

    public void testReadFromParcel() {
        ComponentName expected = getComponentName();
        final Parcel parcel1 = Parcel.obtain();
        expected.writeToParcel(parcel1, 0);
        parcel1.setDataPosition(0);
        ComponentName actual = ComponentName.readFromParcel(parcel1);
        assertEquals(expected, actual);

        // Test empty data
        final Parcel parcel2 = Parcel.obtain();
        expected = ComponentName.readFromParcel(parcel2);
        assertNull(expected);
    }

    public void testGetPackageName() {
        final String actual = getComponentName().getPackageName();
        assertEquals("com.android.cts.stub", actual);
    }

    public void testUnflattenFromString() {
        final ComponentName componentName = getComponentName();
        final String flattenString = getComponentName().flattenToString();
        assertNotNull(flattenString);
        ComponentName actual = ComponentName.unflattenFromString(flattenString);
        assertEquals(componentName, actual);
    }

    public void testFlattenToShortString() {
        // Test normal
        String actual = getComponentName().flattenToShortString();
        assertEquals("com.android.cts.stub/android.content.cts.ComponentNameTest", actual);

        // Test long class name
        final ComponentName componentName = new ComponentName("com.android.view",
                "com.android.view.View");
        final String falttenString = componentName.flattenToString();
        // First, compare the string return by flattenToString().
        assertEquals("com.android.view/com.android.view.View", falttenString);
        actual = componentName.flattenToShortString();
        // Then, compare the string return by flattenToShortString().
        assertEquals("com.android.view/.View", actual);
    }

    public void testEquals() {
        // new the ComponentName instances, both are the same.
        final ComponentName componentName1 = getComponentName();
        ComponentName componentName2 = new ComponentName(componentName1.getPackageName(),
                componentName1.getShortClassName());
        assertTrue(componentName1.equals(componentName2));

        // new the ComponentName instances, are not the same.
        componentName2 = new ComponentName(componentName1.getPackageName(),
                componentName1.getShortClassName() + "different name");
        assertFalse(componentName1.equals(componentName2));
    }

    public void testToString() {
        assertNotNull(getComponentName().toString());
    }

    public void testToShortString() {
        // Test normal string
        final String shortString = getComponentName().toShortString();
        assertEquals("{com.android.cts.stub/android.content.cts.ComponentNameTest}", shortString);
    }

    public void testGetClassName() {
        // set the expected value
        final String className = getComponentName().getClassName();
        assertEquals("android.content.cts.ComponentNameTest", className);
    }

    public void testHashCode() {
        final ComponentName componentName = getComponentName();

        final int hashCode1 = componentName.hashCode();
        assertFalse(0 == hashCode1);

        final ComponentName componentName2 = new ComponentName(componentName.getPackageName(),
                componentName.getClassName());
        final int hashCode2 = componentName2.hashCode();
        assertEquals(hashCode1, hashCode2);
    }

    public void testWriteToParcel() {
        // Test normal status
        final ComponentName componentName = getComponentName();
        Parcel parcel = Parcel.obtain();
        ComponentName.writeToParcel(componentName, parcel);
        parcel.setDataPosition(0);
        assertFalse(0 == parcel.dataAvail());
        assertEquals("com.android.cts.stub", parcel.readString());
        assertEquals("android.content.cts.ComponentNameTest", parcel.readString());

        // Test null data
        parcel = Parcel.obtain();
        ComponentName.writeToParcel(null, parcel);
        assertEquals(0, parcel.dataAvail());
    }

    public void testDescribeContents() {
        assertEquals(0, getComponentName().describeContents());
    }

    private ComponentName getComponentName() {
        final ComponentName componentName = new ComponentName(mContext, this.getClass());
        return componentName;
    }
}
