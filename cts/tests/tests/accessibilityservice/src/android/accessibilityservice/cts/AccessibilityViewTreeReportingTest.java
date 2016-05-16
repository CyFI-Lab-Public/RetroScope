/**
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.accessibilityservice.cts;

import android.test.suitebuilder.annotation.MediumTest;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.cts.accessibilityservice.R;

/**
 * Test cases for testing the accessibility focus APIs exposed to accessibility
 * services. This test checks how the view hierarchy is reported to accessibility
 * services.
 */
public class AccessibilityViewTreeReportingTest
        extends AccessibilityActivityTestCase<AccessibilityViewTreeReportingActivity>{

    public AccessibilityViewTreeReportingTest() {
        super(AccessibilityViewTreeReportingActivity.class);
    }

    @MediumTest
    public void testDescendantsOfNotImportantViewReportedInOrder1() throws Exception {
        AccessibilityNodeInfo firstFrameLayout = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.firstFrameLayout)).get(0);
        assertNotNull(firstFrameLayout);
        assertSame(3, firstFrameLayout.getChildCount());

        // Check if the first child is the right one.
        AccessibilityNodeInfo firstTextView = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                    R.string.firstTextView)).get(0);
        assertEquals(firstTextView, firstFrameLayout.getChild(0));

        // Check if the second child is the right one.
        AccessibilityNodeInfo firstEditText = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                    R.string.firstEditText)).get(0);
        assertEquals(firstEditText, firstFrameLayout.getChild(1));

        // Check if the third child is the right one.
        AccessibilityNodeInfo firstButton = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.firstButton)).get(0);
        assertEquals(firstButton, firstFrameLayout.getChild(2));
    }

    @MediumTest
    public void testDescendantsOfNotImportantViewReportedInOrder2() throws Exception {
        AccessibilityNodeInfo secondFrameLayout = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.secondFrameLayout)).get(0);
        assertNotNull(secondFrameLayout);
        assertSame(3, secondFrameLayout.getChildCount());

        // Check if the first child is the right one.
        AccessibilityNodeInfo secondTextView = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.secondTextView)).get(0);
        assertEquals(secondTextView, secondFrameLayout.getChild(0));

        // Check if the second child is the right one.
        AccessibilityNodeInfo secondEditText = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.secondEditText)).get(0);
        assertEquals(secondEditText, secondFrameLayout.getChild(1));

        // Check if the third child is the right one.
        AccessibilityNodeInfo secondButton = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.secondButton)).get(0);
        assertEquals(secondButton, secondFrameLayout.getChild(2));
    }

    @MediumTest
    public void testDescendantsOfNotImportantViewReportedInOrder3() throws Exception {
        AccessibilityNodeInfo rootLinearLayout = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.rootLinearLayout)).get(0);
        assertNotNull(rootLinearLayout);
        assertSame(4, rootLinearLayout.getChildCount());

        // Check if the first child is the right one.
        AccessibilityNodeInfo firstFrameLayout = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.firstFrameLayout)).get(0);
        assertEquals(firstFrameLayout, rootLinearLayout.getChild(0));

        // Check if the second child is the right one.
        AccessibilityNodeInfo secondTextView = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.secondTextView)).get(0);
        assertEquals(secondTextView, rootLinearLayout.getChild(1));

        // Check if the third child is the right one.
        AccessibilityNodeInfo secondEditText = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.secondEditText)).get(0);
        assertEquals(secondEditText, rootLinearLayout.getChild(2));

        // Check if the fourth child is the right one.
        AccessibilityNodeInfo secondButton = getInstrumentation().getUiAutomation()
            .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                    getString(R.string.secondButton)).get(0);
        assertEquals(secondButton, rootLinearLayout.getChild(3));
    }
}
