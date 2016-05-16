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

package android.view.accessibility.cts;

import android.os.Message;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityRecord;

import junit.framework.TestCase;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

/**
 * Class for testing {@link AccessibilityRecord}.
 */
public class AccessibilityRecordTest extends AndroidTestCase {

    /** The number of properties of the {@link AccessibilityEvent} class. */
    private static final int NON_STATIC_FIELD_COUNT = 22;

    /**
     * Test that no new fields have been added without updating the
     * marshaling tests. Note that the marshaling tests are in
     * AccessibilityEventTests since it is a super class that is
     * responsible for marshaling and unmarshaling.
     */
    @SmallTest
    public void testNoNewFieldsAddedWithoutUpdadingMarshallTests() {
        assertNoNewNonStaticFieldsAdded(AccessibilityRecord.class, NON_STATIC_FIELD_COUNT);
    }

    /**
     * Tests the cloning obtain method.
     */
    @SmallTest
    public void testObtain() {
        AccessibilityRecord originalRecord = AccessibilityRecord.obtain();
        fullyPopulateAccessibilityRecord(originalRecord);
        AccessibilityRecord cloneRecord = AccessibilityRecord.obtain(originalRecord);
        assertEqualAccessibilityRecord(originalRecord, cloneRecord);
    }

   /**
    * Tests if {@link AccessibilityRecord}s are properly recycled.
    */
   @SmallTest
   public void testRecycle() {
       // obtain and populate an event
       AccessibilityRecord populatedRecord = AccessibilityRecord.obtain();
       fullyPopulateAccessibilityRecord(populatedRecord);

       // recycle and obtain the same recycled instance
       populatedRecord.recycle();
       AccessibilityRecord recycledRecord = AccessibilityRecord.obtain();

       // check expectations
       assertAccessibilityRecordCleared(recycledRecord);
   }

   /**
    * Asserts that an {@link AccessibilityRecord} is cleared.
    */
   static void assertAccessibilityRecordCleared(AccessibilityRecord record) {
       TestCase.assertEquals("addedCount not properly recycled", -1, record.getAddedCount());
       TestCase.assertNull("beforeText not properly recycled", record.getBeforeText());
       TestCase.assertFalse("checked not properly recycled", record.isChecked());
       TestCase.assertNull("className not properly recycled", record.getClassName());
       TestCase.assertNull("contentDescription not properly recycled",
               record.getContentDescription());
       TestCase.assertEquals("currentItemIndex not properly recycled", -1,
               record.getCurrentItemIndex());
       TestCase.assertFalse("enabled not properly recycled", record.isEnabled());
       TestCase.assertEquals("fromIndex not properly recycled", -1, record.getFromIndex());
       TestCase.assertFalse("fullScreen not properly recycled", record.isFullScreen());
       TestCase.assertEquals("itemCount not properly recycled", -1, record.getItemCount());
       TestCase.assertNull("parcelableData not properly recycled", record.getParcelableData());
       TestCase.assertFalse("password not properly recycled", record.isPassword());
       TestCase.assertEquals("removedCount not properly recycled", -1, record.getRemovedCount());
       TestCase.assertTrue("text not properly recycled", record.getText().isEmpty());
       TestCase.assertFalse("scrollable not properly recycled", record.isScrollable());
       TestCase.assertSame("maxScrollX not properly recycled", -1, record.getMaxScrollX());
       TestCase.assertSame("maxScrollY not properly recycled", -1, record.getMaxScrollY());
       TestCase.assertSame("scrollX not properly recycled", -1, record.getScrollX());
       TestCase.assertSame("scrollY not properly recycled", -1, record.getScrollY());
       TestCase.assertSame("toIndex not properly recycled", -1, record.getToIndex());
   }

    /**
     * Fully populates the {@link AccessibilityRecord}.
     *
     * @param record The record to populate.
     */
    static void fullyPopulateAccessibilityRecord(AccessibilityRecord record) {
        record.setAddedCount(1);
        record.setBeforeText("BeforeText");
        record.setChecked(true);
        record.setClassName("foo.bar.baz.Class");
        record.setContentDescription("ContentDescription");
        record.setCurrentItemIndex(1);
        record.setEnabled(true);
        record.setFromIndex(1);
        record.setFullScreen(true);
        record.setItemCount(1);
        record.setParcelableData(Message.obtain(null, 1, 2, 3));
        record.setPassword(true);
        record.setRemovedCount(1);
        record.getText().add("Foo");
        record.setMaxScrollX(1);
        record.setMaxScrollY(1);
        record.setScrollX(1);
        record.setScrollY(1);
        record.setToIndex(1);
        record.setScrollable(true);
    }

    static void assertEqualAccessibilityRecord(AccessibilityRecord expectedRecord,
            AccessibilityRecord receivedRecord) {
        assertEquals("addedCount has incorrect value", expectedRecord.getAddedCount(),
                receivedRecord.getAddedCount());
        assertEquals("beforeText has incorrect value", expectedRecord.getBeforeText(),
                receivedRecord.getBeforeText());
        assertEquals("checked has incorrect value", expectedRecord.isChecked(),
                receivedRecord.isChecked());
        assertEquals("className has incorrect value", expectedRecord.getClassName(),
                receivedRecord.getClassName());
        assertEquals("contentDescription has incorrect value",
                expectedRecord.getContentDescription(), receivedRecord.getContentDescription());
        assertEquals("currentItemIndex has incorrect value", expectedRecord.getCurrentItemIndex(),
                receivedRecord.getCurrentItemIndex());
        assertEquals("enabled has incorrect value", expectedRecord.isEnabled(),
                receivedRecord.isEnabled());
        assertEquals("fromIndex has incorrect value", expectedRecord.getFromIndex(),
                receivedRecord.getFromIndex());
        assertEquals("fullScreen has incorrect value", expectedRecord.isFullScreen(),
                receivedRecord.isFullScreen());
        assertEquals("itemCount has incorrect value", expectedRecord.getItemCount(),
                receivedRecord.getItemCount());
        assertEquals("password has incorrect value", expectedRecord.isPassword(),
                receivedRecord.isPassword());
        assertEquals("removedCount has incorrect value", expectedRecord.getRemovedCount(),
                receivedRecord.getRemovedCount());
        assertEqualsText(expectedRecord.getText(), receivedRecord.getText());
        assertSame("maxScrollX has incorect value", expectedRecord.getMaxScrollX(),
                receivedRecord.getMaxScrollX());
        assertSame("maxScrollY has incorect value", expectedRecord.getMaxScrollY(),
                receivedRecord.getMaxScrollY());
        assertSame("scrollX has incorect value", expectedRecord.getScrollX(),
                receivedRecord.getScrollX());
        assertSame("scrollY has incorect value", expectedRecord.getScrollY(),
                receivedRecord.getScrollY());
        assertSame("toIndex has incorect value", expectedRecord.getToIndex(),
                receivedRecord.getToIndex());
        assertSame("scrollable has incorect value", expectedRecord.isScrollable(),
                receivedRecord.isScrollable());
        assertSame("parcelableData has incorect value",
                ((Message) expectedRecord.getParcelableData()).what,
                ((Message) receivedRecord.getParcelableData()).what);
    }

    /**
     * Compares the text of the <code>expectedEvent</code> and
     * <code>receivedEvent</code> by comparing the string representation of the
     * corresponding {@link CharSequence}s.
     */
    static void assertEqualsText(List<CharSequence> expectedText,
            List<CharSequence> receivedText ) {
        String message = "text has incorrect value";

        TestCase.assertEquals(message, expectedText.size(), receivedText.size());

        Iterator<CharSequence> expectedTextIterator = expectedText.iterator();
        Iterator<CharSequence> receivedTextIterator = receivedText.iterator();

        for (int i = 0; i < expectedText.size(); i++) {
            // compare the string representation
            TestCase.assertEquals(message, expectedTextIterator.next().toString(),
                    receivedTextIterator.next().toString());
        }
    }

    /**
     * Asserts that no new fields have been added, so we are testing marshaling
     * of all such.
     */
    static void assertNoNewNonStaticFieldsAdded(Class<?> clazz, int expectedCount) {
        int nonStaticFieldCount = 0;

        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if ((field.getModifiers() & Modifier.STATIC) == 0) {
                    nonStaticFieldCount++;
                }
            }
            clazz = clazz.getSuperclass();
        }

        String message = "New fields have been added, so add code to test marshaling them.";
        assertEquals(message, expectedCount, nonStaticFieldCount);
    }
}
