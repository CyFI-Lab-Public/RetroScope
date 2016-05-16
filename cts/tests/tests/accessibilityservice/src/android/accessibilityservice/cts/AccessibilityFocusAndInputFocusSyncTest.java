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

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS;

import android.app.UiAutomation;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.cts.accessibilityservice.R;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Test cases for testing the accessibility focus APIs exposed to accessibility
 * services. These APIs allow moving accessibility focus in the view tree from
 * an AccessiiblityService. Specifically, this activity is for verifying the the
 * sync between accessibility and input focus.
 */
public class AccessibilityFocusAndInputFocusSyncTest
        extends AccessibilityActivityTestCase<AccessibilityFocusAndInputFocusSyncActivity>{

    public AccessibilityFocusAndInputFocusSyncTest() {
        super(AccessibilityFocusAndInputFocusSyncActivity.class);
    }

    @MediumTest
    public void testFindAccessibilityFocus() throws Exception {
        // Get the view that has input and accessibility focus.
        final AccessibilityNodeInfo expected = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.firstEditText)).get(0);
        assertNotNull(expected);
        assertFalse(expected.isAccessibilityFocused());
        assertTrue(expected.isFocused());

        getInstrumentation().getUiAutomation().executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                // Perform a focus action and check for success.
                assertTrue(expected.performAction(ACTION_ACCESSIBILITY_FOCUS));
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Get the second expected node info.
        AccessibilityNodeInfo received = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        assertNotNull(received);
        assertTrue(received.isAccessibilityFocused());

        // Make sure we got the expected focusable.
        assertEquals(expected, received);
    }

    @MediumTest
    public void testInitialStateNoAccessibilityFocus() throws Exception {
        // Get the root which is only accessibility focused.
        AccessibilityNodeInfo focused = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        assertNull(focused);
    }

    @MediumTest
    public void testActionAccessibilityFocus() throws Exception {
        // Get the root linear layout info.
        final AccessibilityNodeInfo rootLinearLayout = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.rootLinearLayout)).get(0);
        assertNotNull(rootLinearLayout);
        assertFalse(rootLinearLayout.isAccessibilityFocused());

        getInstrumentation().getUiAutomation().executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                // Perform a focus action and check for success.
                assertTrue(rootLinearLayout.performAction(ACTION_ACCESSIBILITY_FOCUS));
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Get the node info again.
        rootLinearLayout.refresh();

        // Check if the node info is focused.
        assertTrue(rootLinearLayout.isAccessibilityFocused());
    }

    @MediumTest
    public void testActionClearAccessibilityFocus() throws Exception {
        // Get the root linear layout info.
        final AccessibilityNodeInfo rootLinearLayout = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.rootLinearLayout)).get(0);
        assertNotNull(rootLinearLayout);

        getInstrumentation().getUiAutomation().executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                // Perform a focus action and check for success.
                assertTrue(rootLinearLayout.performAction(ACTION_ACCESSIBILITY_FOCUS));
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Refresh the node info.
        rootLinearLayout.refresh();

        // Check if the node info is focused.
        assertTrue(rootLinearLayout.isAccessibilityFocused());

        getInstrumentation().getUiAutomation().executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                // Perform a clear focus action and check for success.
                assertTrue(rootLinearLayout.performAction(ACTION_CLEAR_ACCESSIBILITY_FOCUS));
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return event.getEventType()
                        == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED;
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Refresh the node info.
        rootLinearLayout.refresh();

        // Check if the node info is not focused.
        assertFalse(rootLinearLayout.isAccessibilityFocused());
    }

    @MediumTest
    public void testOnlyOneNodeHasAccessibilityFocus() throws Exception {
        // Get the first not focused edit text.
        final AccessibilityNodeInfo firstEditText = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.firstEditText)).get(0);
        assertNotNull(firstEditText);
        assertTrue(firstEditText.isFocusable());
        assertTrue(firstEditText.isFocused());
        assertFalse(firstEditText.isAccessibilityFocused());

        getInstrumentation().getUiAutomation().executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                // Perform a set focus action and check for success.
                assertTrue(firstEditText.performAction(ACTION_ACCESSIBILITY_FOCUS));
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Get the second not focused edit text.
        final AccessibilityNodeInfo secondEditText = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.secondEditText)).get(0);
        assertNotNull(secondEditText);
        assertTrue(secondEditText.isFocusable());
        assertFalse(secondEditText.isFocused());
        assertFalse(secondEditText.isAccessibilityFocused());

        getInstrumentation().getUiAutomation().executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                // Perform a set focus action and check for success.
                assertTrue(secondEditText.performAction(ACTION_ACCESSIBILITY_FOCUS));
                
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return event.getEventType() == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Get the node info again.
        secondEditText.refresh();

        // Make sure no other node has accessibility focus.
        AccessibilityNodeInfo root = getInstrumentation().getUiAutomation().getRootInActiveWindow();
        Queue<AccessibilityNodeInfo> workQueue = new LinkedList<AccessibilityNodeInfo>();
        workQueue.add(root);
        while (!workQueue.isEmpty()) {
            AccessibilityNodeInfo current = workQueue.poll();
            if (current.isAccessibilityFocused() && !current.equals(secondEditText)) {
                fail();
            }
            final int childCount = current.getChildCount();
            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo child = current.getChild(i);
                if (child != null) {
                    workQueue.offer(child);
                }
            }
        }
    }
}
