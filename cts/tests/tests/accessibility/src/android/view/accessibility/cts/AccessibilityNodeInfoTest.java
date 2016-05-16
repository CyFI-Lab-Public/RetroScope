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

import android.graphics.Rect;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Class for testing {@link AccessibilityNodeInfo}.
 */
public class AccessibilityNodeInfoTest extends AndroidTestCase {

    /** The number of properties of the {@link AccessibilityNodeInfo} class. */
    private static final int NON_STATIC_FIELD_COUNT = 26;

    @SmallTest
    public void testMarshaling() throws Exception {
        // no new fields, so we are testing marshaling of all such
        AccessibilityRecordTest.assertNoNewNonStaticFieldsAdded(AccessibilityNodeInfo.class,
                NON_STATIC_FIELD_COUNT);

        // fully populate the node info to marshal
        AccessibilityNodeInfo sentInfo = AccessibilityNodeInfo.obtain(new View(getContext()));
        fullyPopulateAccessibilityNodeInfo(sentInfo);

        // marshal and unmarshal the node info
        Parcel parcel = Parcel.obtain();
        sentInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        AccessibilityNodeInfo receivedInfo = AccessibilityNodeInfo.CREATOR.createFromParcel(parcel);

        // make sure all fields properly marshaled
        assertEqualsAccessiblityNodeInfo(sentInfo, receivedInfo);
    }

    /**
     * Tests if {@link AccessibilityNodeInfo}s are properly reused.
     */
    @SmallTest
    public void testReuse() {
        AccessibilityEvent firstInfo = AccessibilityEvent.obtain();
        firstInfo.recycle();
        AccessibilityEvent secondInfo = AccessibilityEvent.obtain();
        assertSame("AccessibilityNodeInfo not properly reused", firstInfo, secondInfo);
    }

    /**
     * Tests if {@link AccessibilityNodeInfo} are properly recycled.
     */
    @SmallTest
    public void testRecycle() {
        // obtain and populate an node info
        AccessibilityNodeInfo populatedInfo = AccessibilityNodeInfo.obtain();
        fullyPopulateAccessibilityNodeInfo(populatedInfo);

        // recycle and obtain the same recycled instance
        populatedInfo.recycle();
        AccessibilityNodeInfo recycledInfo = AccessibilityNodeInfo.obtain();

        // check expectations
        assertAccessibilityNodeInfoCleared(recycledInfo);
    }

    /**
     * Tests whether the event describes its contents consistently.
     */
    @SmallTest
    public void testDescribeContents() {
        AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
        assertSame("Accessibility node infos always return 0 for this method.", 0,
                info.describeContents());
        fullyPopulateAccessibilityNodeInfo(info);
        assertSame("Accessibility node infos always return 0 for this method.", 0,
                info.describeContents());
    }

    /**
     * Fully populates the {@link AccessibilityNodeInfo} to marshal.
     *
     * @param info The node info to populate.
     */
    private void fullyPopulateAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        info.setParent(new View(getContext()));
        info.setSource(new View(getContext()));
        info.addChild(new View(getContext()));
        info.addChild(new View(getContext()), 1);
        info.setBoundsInParent(new Rect(1,1,1,1));
        info.setBoundsInScreen(new Rect(2,2,2,2));
        info.setClassName("foo.bar.baz.Class");
        info.setContentDescription("content description");
        info.setPackageName("foo.bar.baz");
        info.setText("text");
        info.setCheckable(true);
        info.setChecked(true);
        info.setClickable(true);
        info.setEnabled(true);
        info.setFocusable(true);
        info.setFocused(true);
        info.setLongClickable(true);
        info.setPassword(true);
        info.setScrollable(true);
        info.setSelected(true);
        info.addAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
        info.setAccessibilityFocused(true);
        info.setMovementGranularities(AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
        info.setLabeledBy(new View(getContext()));
        info.setLabelFor(new View(getContext()));
        info.setViewIdResourceName("foo.bar:id/baz");
    }

    /**
     * Compares all properties of the <code>expectedInfo</code> and the
     * <code>receviedInfo</code> to verify that the received node info is
     * the one that is expected.
     */
    public static void assertEqualsAccessiblityNodeInfo(AccessibilityNodeInfo expectedInfo,
            AccessibilityNodeInfo receivedInfo) {
        Rect expectedBounds = new Rect();
        Rect receivedBounds = new Rect();
        expectedInfo.getBoundsInParent(expectedBounds);
        receivedInfo.getBoundsInParent(receivedBounds);
        assertEquals("boundsInParent has incorrect value", expectedBounds, receivedBounds);
        expectedInfo.getBoundsInScreen(expectedBounds);
        receivedInfo.getBoundsInScreen(receivedBounds);
        assertEquals("boundsInScreen has incorrect value", expectedBounds, receivedBounds);
        assertEquals("className has incorrect value", expectedInfo.getClassName(),
                receivedInfo.getClassName());
        assertEquals("contentDescription has incorrect value", expectedInfo.getContentDescription(),
                receivedInfo.getContentDescription());
        assertEquals("packageName has incorrect value", expectedInfo.getPackageName(),
                receivedInfo.getPackageName());
        assertEquals("text has incorrect value", expectedInfo.getText(), receivedInfo.getText());
        assertSame("checkable has incorrect value", expectedInfo.isCheckable(),
                receivedInfo.isCheckable());
        assertSame("checked has incorrect value", expectedInfo.isChecked(),
                receivedInfo.isChecked());
        assertSame("clickable has incorrect value", expectedInfo.isClickable(),
                receivedInfo.isClickable());
        assertSame("enabled has incorrect value", expectedInfo.isEnabled(),
                receivedInfo.isEnabled());
        assertSame("focusable has incorrect value", expectedInfo.isFocusable(),
                receivedInfo.isFocusable());
        assertSame("focused has incorrect value", expectedInfo.isFocused(),
                receivedInfo.isFocused());
        assertSame("longClickable has incorrect value", expectedInfo.isLongClickable(),
                receivedInfo.isLongClickable());
        assertSame("password has incorrect value", expectedInfo.isPassword(),
                receivedInfo.isPassword());
        assertSame("scrollable has incorrect value", expectedInfo.isScrollable(),
                receivedInfo.isScrollable());
        assertSame("selected has incorrect value", expectedInfo.isSelected(),
                receivedInfo.isSelected());
        assertSame("actions has incorrect value", expectedInfo.getActions(),
                receivedInfo.getActions());
        assertSame("childCount has incorrect value", expectedInfo.getChildCount(),
                receivedInfo.getChildCount());
        assertSame("childCount has incorrect value", expectedInfo.getChildCount(),
                receivedInfo.getChildCount());
        assertSame("accessibilityFocused has incorrect value",
                expectedInfo.isAccessibilityFocused(),
                receivedInfo.isAccessibilityFocused());
        assertSame("movementGranularities has incorrect value",
                expectedInfo.getMovementGranularities(),
                receivedInfo.getMovementGranularities());
        assertEquals("viewId has incorrect value", expectedInfo.getViewIdResourceName(),
                receivedInfo.getViewIdResourceName());
    }

    /**
     * Asserts that an {@link AccessibilityNodeInfo} is cleared.
     *
     * @param info The node info to check.
     */
    public static void assertAccessibilityNodeInfoCleared(AccessibilityNodeInfo info) {
        Rect bounds = new Rect();
        info.getBoundsInParent(bounds);
        assertTrue("boundsInParent not properly recycled", bounds.isEmpty());
        info.getBoundsInScreen(bounds);
        assertTrue("boundsInScreen not properly recycled", bounds.isEmpty());
        assertNull("className not properly recycled", info.getClassName());
        assertNull("contentDescription not properly recycled", info.getContentDescription());
        assertNull("packageName not properly recycled", info.getPackageName());
        assertNull("text not properly recycled", info.getText());
        assertFalse("checkable not properly recycled", info.isCheckable());
        assertFalse("checked not properly recycled", info.isChecked());
        assertFalse("clickable not properly recycled", info.isClickable());
        assertFalse("enabled not properly recycled", info.isEnabled());
        assertFalse("focusable not properly recycled", info.isFocusable());
        assertFalse("focused not properly recycled", info.isFocused());
        assertFalse("longClickable not properly recycled", info.isLongClickable());
        assertFalse("password not properly recycled", info.isPassword());
        assertFalse("scrollable not properly recycled", info.isScrollable());
        assertFalse("selected not properly recycled", info.isSelected());
        assertSame("actions not properly recycled", 0, info.getActions());
        assertFalse("accessibilityFocused not properly recycled", info.isAccessibilityFocused());
        assertSame("movementGranularities not properly recycled", 0,
                info.getMovementGranularities());
        assertNull("viewId not properly recycled", info.getViewIdResourceName());
    }
}
