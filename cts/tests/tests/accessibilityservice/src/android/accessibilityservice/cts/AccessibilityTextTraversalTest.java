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

import android.app.UiAutomation;
import android.os.Bundle;
import android.test.suitebuilder.annotation.MediumTest;
import android.text.Selection;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.android.cts.accessibilityservice.R;

/**
 * Test cases for testing the accessibility APIs for traversing the text content of
 * a View at several granularities.
 */
public class AccessibilityTextTraversalTest
        extends AccessibilityActivityTestCase<AccessibilityTextTraversalActivity>{

    public AccessibilityTextTraversalTest() {
        super(AccessibilityTextTraversalActivity.class);
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityCharacterOverContentDescription()
            throws Exception {
        final View view = getActivity().findViewById(R.id.view);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                view.setContentDescription(getString(R.string.a_b));
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.a_b)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);

        // Move to the next character and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                        arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                        arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                        arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                 AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Make sure there is no next.
        assertFalse(text.performAction(AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                arguments));

        // Move to the previous character and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Move to the previous character and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityWordOverContentDescription()
            throws Exception {
        final View view = getActivity().findViewById(R.id.view);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                view.setContentDescription(getString(R.string.foo_bar_baz));
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.foo_bar_baz)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);

        // Move to the next character and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                               AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Move to the next character and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Move to the next character and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(View.class.getName())
                        && event.getContentDescription().toString().equals(
                                getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityCharacterOverText()
            throws Exception {
        final TextView textView = (TextView) getActivity().findViewById(R.id.text);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(getString(R.string.a_b));
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                       getString(R.string.a_b)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);

        // Move to the next character and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(1, Selection.getSelectionStart(textView.getText()));
        assertEquals(1, Selection.getSelectionEnd(textView.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(textView.getText()));
        assertEquals(2, Selection.getSelectionEnd(textView.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(textView.getText()));
        assertEquals(3, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(textView.getText()));
        assertEquals(3, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(textView.getText()));
        assertEquals(2, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(1, Selection.getSelectionStart(textView.getText()));
        assertEquals(1, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent seventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(seventhExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityCharacterOverTextExtend()
            throws Exception {
        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setText(getString(R.string.a_b));
                Selection.removeSelection(editText.getText());
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                       getString(R.string.a_b)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
        arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);

        // Move to the next character and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(1, Selection.getSelectionEnd(editText.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(2, Selection.getSelectionEnd(editText.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(3, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(3, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(2, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(1, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Focus the view so we can change selection.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setFocusable(true);
                editText.requestFocus();
            }
        });

        // Put selection at the end of the text.
        Bundle setSelectionArgs = new Bundle();
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 3);
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 3);
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, setSelectionArgs));

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(3, Selection.getSelectionEnd(editText.getText()));

        // Unfocus the view so we can get rid of the soft-keyboard.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.clearFocus();
                editText.setFocusable(false);
            }
        });

        // Move to the previous character and wait for an event.
        AccessibilityEvent seventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(seventhExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(2, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent eightExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eightExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(1, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent ninethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(ninethExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent tenthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 1
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(tenthExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(1, Selection.getSelectionEnd(editText.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent eleventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 1
                        && event.getToIndex() == 2
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eleventhExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(2, Selection.getSelectionEnd(editText.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent twelvethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.a_b))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(twelvethExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(3, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(editText.getText()));
        assertEquals(3, Selection.getSelectionEnd(editText.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityWordOverText() throws Exception {
        final TextView textView = (TextView) getActivity().findViewById(R.id.text);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(getString(R.string.foo_bar_baz));
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.foo_bar_baz)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);

        // Move to the next word and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(3, Selection.getSelectionStart(textView.getText()));
        assertEquals(3, Selection.getSelectionEnd(textView.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(7, Selection.getSelectionStart(textView.getText()));
        assertEquals(7, Selection.getSelectionEnd(textView.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                 AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(textView.getText()));
        assertEquals(11, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(textView.getText()));
        assertEquals(11, Selection.getSelectionEnd(textView.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(8, Selection.getSelectionStart(textView.getText()));
        assertEquals(8, Selection.getSelectionEnd(textView.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(4, Selection.getSelectionStart(textView.getText()));
        assertEquals(4, Selection.getSelectionEnd(textView.getText()));

        // Move to the next character and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityWordOverEditTextWithContentDescription()
            throws Exception {

        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setText(getString(R.string.foo_bar_baz));
                editText.setContentDescription(getString(R.string.android_wiki));
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.foo_bar_baz)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);

        // Move to the next word and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getContentDescription().equals(getString(R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(3, editText.getSelectionStart());
        assertEquals(3, editText.getSelectionEnd());

        // Move to the next word and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getContentDescription().equals(getString(R.string.android_wiki))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(7, editText.getSelectionStart());
        assertEquals(7, editText.getSelectionEnd());

        // Move to the next word and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getContentDescription().equals(getString(R.string.android_wiki))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                 AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(11, editText.getSelectionStart());
        assertEquals(11, editText.getSelectionEnd());

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(11, editText.getSelectionStart());
        assertEquals(11, editText.getSelectionEnd());

        // Move to the next word and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getContentDescription().equals(getString(R.string.android_wiki))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(8, editText.getSelectionStart());
        assertEquals(8, editText.getSelectionEnd());

        // Move to the next word and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getContentDescription().equals(getString(R.string.android_wiki))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(4, editText.getSelectionStart());
        assertEquals(4, editText.getSelectionEnd());

        // Move to the next character and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getContentDescription().equals(getString(R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, editText.getSelectionStart());
        assertEquals(0, editText.getSelectionEnd());

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, editText.getSelectionStart());
        assertEquals(0, editText.getSelectionEnd());
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityWordOverTextExtend() throws Exception {
        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setText(getString(R.string.foo_bar_baz));
                Selection.removeSelection(editText.getText());
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.foo_bar_baz)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
        arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);

        // Move to the next word and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(3, Selection.getSelectionEnd(editText.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(7, Selection.getSelectionEnd(editText.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(11, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Move to the previous word and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(8, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous word and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(4, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Focus the view so we can change selection.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setFocusable(true);
                editText.requestFocus();
            }
        });

        // Put selection at the end of the text.
        Bundle setSelectionArgs = new Bundle();
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 11);
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 11);
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, setSelectionArgs));

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(11, Selection.getSelectionEnd(editText.getText()));

        // Unfocus the view so we can get rid of the soft-keyboard.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.clearFocus();
                editText.setFocusable(false);
            }
        });

        // Move to the previous word and wait for an event.
        AccessibilityEvent seventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(seventhExpected);

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(8, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous word and wait for an event.
        AccessibilityEvent eightExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eightExpected);

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(4, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous character and wait for an event.
        AccessibilityEvent ninethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(ninethExpected);

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent tenthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 3
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(tenthExpected);

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(3, Selection.getSelectionEnd(editText.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent eleventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 4
                        && event.getToIndex() == 7
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eleventhExpected);

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(7, Selection.getSelectionEnd(editText.getText()));

        // Move to the next word and wait for an event.
        AccessibilityEvent twelvthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(R.string.foo_bar_baz))
                        && event.getFromIndex() == 8
                        && event.getToIndex() == 11
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(twelvthExpected);

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(11, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(11, Selection.getSelectionStart(editText.getText()));
        assertEquals(11, Selection.getSelectionEnd(editText.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityLineOverText() throws Exception {
        final TextView textView = (TextView) getActivity().findViewById(R.id.text);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(getString(R.string.android_wiki_short));
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.android_wiki_short)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);

        // Move to the next line and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 25
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(25, Selection.getSelectionStart(textView.getText()));
        assertEquals(25, Selection.getSelectionEnd(textView.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 25
                        && event.getToIndex() == 53
                        && event.getMovementGranularity() ==
                                 AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(textView.getText()));
        assertEquals(53, Selection.getSelectionEnd(textView.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 53
                        && event.getToIndex() == 60
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(60, Selection.getSelectionStart(textView.getText()));
        assertEquals(60, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(60, Selection.getSelectionStart(textView.getText()));
        assertEquals(60, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 53
                        && event.getToIndex() == 60
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(textView.getText()));
        assertEquals(53, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 25
                        && event.getToIndex() == 53
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(25, Selection.getSelectionStart(textView.getText()));
        assertEquals(25, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(TextView.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 25
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(textView.getText()));
        assertEquals(0, Selection.getSelectionEnd(textView.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityLineOverTextExtend() throws Exception {
        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setText(getString(R.string.android_wiki_short));
                Selection.removeSelection(editText.getText());
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.android_wiki_short)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
        arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);

        // Move to the next line and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 19
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(19, Selection.getSelectionEnd(editText.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 19
                        && event.getToIndex() == 35
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(35, Selection.getSelectionEnd(editText.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 35
                        && event.getToIndex() == 53
                        && event.getMovementGranularity() ==
                                 AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(53, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 35
                        && event.getToIndex() == 53
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(35, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 19
                        && event.getToIndex() == 35
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(19, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                               AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 19
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Focus the view so we can change selection.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setFocusable(true);
                editText.requestFocus();
            }
        });

        // Put selection at the end of the text.
        Bundle setSelectionArgs = new Bundle();
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 53);
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 53);
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, setSelectionArgs));

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(editText.getText()));
        assertEquals(53, Selection.getSelectionEnd(editText.getText()));

        // Unocus the view so we can hide the keyboard.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.clearFocus();
                editText.setFocusable(false);
            }
        });

        // Move to the previous line and wait for an event.
        AccessibilityEvent seventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 35
                        && event.getToIndex() == 53
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(seventhExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(editText.getText()));
        assertEquals(35, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent eightExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 19
                        && event.getToIndex() == 35
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eightExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(editText.getText()));
        assertEquals(19, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous line and wait for an event.
        AccessibilityEvent ninethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                               AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 19
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(ninethExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent tenthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 19
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(tenthExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(editText.getText()));
        assertEquals(19, Selection.getSelectionEnd(editText.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent eleventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 19
                        && event.getToIndex() == 35
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eleventhExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(editText.getText()));
        assertEquals(35, Selection.getSelectionEnd(editText.getText()));

        // Move to the next line and wait for an event.
        AccessibilityEvent twelvethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_short))
                        && event.getFromIndex() == 35
                        && event.getToIndex() == 53
                        && event.getMovementGranularity() ==
                                 AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(twelvethExpected);

        // Verify the selection position.
        assertEquals(53, Selection.getSelectionStart(editText.getText()));
        assertEquals(53, Selection.getSelectionEnd(editText.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityPageOverText() throws Exception {
        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setText(getString(R.string.android_wiki));
                Selection.removeSelection(editText.getText());
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.android_wiki)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        // Move to the next page and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 139
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(139, Selection.getSelectionStart(editText.getText()));
        assertEquals(139, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 139
                        && event.getToIndex() == 285
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(285, Selection.getSelectionStart(editText.getText()));
        assertEquals(285, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 285
                        && event.getToIndex() == 436
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(436, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(436, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 285
                        && event.getToIndex() == 436
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(285, Selection.getSelectionStart(editText.getText()));
        assertEquals(285, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 139
                        && event.getToIndex() == 285
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(139, Selection.getSelectionStart(editText.getText()));
        assertEquals(139, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 139
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityPageOverTextExtend() throws Exception {
        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setText(getString(R.string.android_wiki));
                Selection.removeSelection(editText.getText());
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.android_wiki)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
        arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);

        // Move to the next page and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 139
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(139, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 139
                        && event.getToIndex() == 285
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(285, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 285
                        && event.getToIndex() == 436
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(436, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Move to the previous page and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                 AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 285
                        && event.getToIndex() == 436
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(285, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 139
                        && event.getToIndex() == 285
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(139, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 139
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(0, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Focus the view so we can change selection.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setFocusable(true);
                editText.requestFocus();
            }
        });

        // Put selection at the end of the text.
        Bundle setSelectionArgs = new Bundle();
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 436);
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 436);
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, setSelectionArgs));

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(436, Selection.getSelectionEnd(editText.getText()));

        // Unfocus the view so we can hide the soft-keyboard.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.clearFocus();
                editText.setFocusable(false);
            }
        });

        // Move to the previous page and wait for an event.
        AccessibilityEvent seventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                 AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 285
                        && event.getToIndex() == 436
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(seventhExpected);

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(285, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent eightExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 139
                        && event.getToIndex() == 285
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eightExpected);

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(139, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous page and wait for an event.
        AccessibilityEvent ninethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 139
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(ninethExpected);

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(0, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent tenthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 0
                        && event.getToIndex() == 139
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(tenthExpected);

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(139, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent eleventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 139
                        && event.getToIndex() == 285
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eleventhExpected);

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(285, Selection.getSelectionEnd(editText.getText()));

        // Move to the next page and wait for an event.
        AccessibilityEvent twelvethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki))
                        && event.getFromIndex() == 285
                        && event.getToIndex() == 436
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(twelvethExpected);

        // Verify the selection position.
        assertEquals(436, Selection.getSelectionStart(editText.getText()));
        assertEquals(436, Selection.getSelectionEnd(editText.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityParagraphOverText() throws Exception {
        final TextView textView = (TextView) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                textView.setText(getString(R.string.android_wiki_paragraphs));
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.android_wiki_short)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 104
                        && event.getMovementGranularity() ==
                                 AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(104, Selection.getSelectionStart(textView.getText()));
        assertEquals(104, Selection.getSelectionEnd(textView.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                 AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 106
                        && event.getToIndex() == 267
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(267, Selection.getSelectionStart(textView.getText()));
        assertEquals(267, Selection.getSelectionEnd(textView.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 268
                        && event.getToIndex() == 582
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(textView.getText()));
        assertEquals(582, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(textView.getText()));
        assertEquals(582, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 268
                        && event.getToIndex() == 582
                        && event.getMovementGranularity() ==
                                 AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(268, Selection.getSelectionStart(textView.getText()));
        assertEquals(268, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 106
                        && event.getToIndex() == 267
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(106, Selection.getSelectionStart(textView.getText()));
        assertEquals(106, Selection.getSelectionEnd(textView.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 104
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(textView.getText()));
        assertEquals(2, Selection.getSelectionEnd(textView.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(textView.getText()));
        assertEquals(2, Selection.getSelectionEnd(textView.getText()));
    }

    @MediumTest
    public void testActionNextAndPreviousAtGranularityParagraphOverTextExtend() throws Exception {
        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setText(getString(R.string.android_wiki_paragraphs));
                Selection.removeSelection(editText.getText());
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(getString(
                       R.string.android_wiki_short)).get(0);

        final int granularities = text.getMovementGranularities();
        assertEquals(granularities, AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH
                | AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE);

        final Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
        arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, true);

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent firstExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 104
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(firstExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(editText.getText()));
        assertEquals(104, Selection.getSelectionEnd(editText.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent secondExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 106
                        && event.getToIndex() == 267
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(secondExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(editText.getText()));
        assertEquals(267, Selection.getSelectionEnd(editText.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent thirdExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 268
                        && event.getToIndex() == 582
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(thirdExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(editText.getText()));
        assertEquals(582, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(editText.getText()));
        assertEquals(582, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent fourthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 268
                        && event.getToIndex() == 582
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fourthExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(editText.getText()));
        assertEquals(268, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent fifthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 106
                        && event.getToIndex() == 267
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(fifthExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(editText.getText()));
        assertEquals(106, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent sixthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 104
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(sixthExpected);

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(editText.getText()));
        assertEquals(2, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(2, Selection.getSelectionStart(editText.getText()));
        assertEquals(2, Selection.getSelectionEnd(editText.getText()));

        // Focus the view so we can change selection.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setFocusable(true);
                editText.requestFocus();
            }
        });

        // Put selection at the end of the text.
        Bundle setSelectionArgs = new Bundle();
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 582);
        setSelectionArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 582);
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, setSelectionArgs));

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(582, Selection.getSelectionEnd(editText.getText()));

        // Unfocus the view so we can get rid of the soft-keyboard.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.clearFocus();
                editText.setFocusable(false);
            }
        });

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent seventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 268
                        && event.getToIndex() == 582
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(seventhExpected);

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(268, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent eightExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 106
                        && event.getToIndex() == 267
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eightExpected);

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(106, Selection.getSelectionEnd(editText.getText()));

        // Move to the previous paragraph and wait for an event.
        AccessibilityEvent ninethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 104
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(ninethExpected);

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(2, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no previous.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(2, Selection.getSelectionEnd(editText.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent tenthExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 2
                        && event.getToIndex() == 104
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(tenthExpected);

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(104, Selection.getSelectionEnd(editText.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent eleventhExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 106
                        && event.getToIndex() == 267
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(eleventhExpected);

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(267, Selection.getSelectionEnd(editText.getText()));

        // Move to the next paragraph and wait for an event.
        AccessibilityEvent twlevethExpected = getInstrumentation().getUiAutomation()
                .executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                text.performAction(
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments);
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return
                (event.getEventType() ==
                    AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY
                        && event.getAction() ==
                                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                        && event.getPackageName().equals(getActivity().getPackageName())
                        && event.getClassName().equals(EditText.class.getName())
                        && event.getText().size() > 0
                        && event.getText().get(0).toString().equals(getString(
                                R.string.android_wiki_paragraphs))
                        && event.getFromIndex() == 268
                        && event.getToIndex() == 582
                        && event.getMovementGranularity() ==
                                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Make sure we got the expected event.
        assertNotNull(twlevethExpected);

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(582, Selection.getSelectionEnd(editText.getText()));

        // Make sure there is no next.
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY, arguments));

        // Verify the selection position.
        assertEquals(582, Selection.getSelectionStart(editText.getText()));
        assertEquals(582, Selection.getSelectionEnd(editText.getText()));
    }

    @MediumTest
    public void testTextEditingActions() throws Exception {
        final EditText editText = (EditText) getActivity().findViewById(R.id.edit);
        final String textContent = getString(R.string.foo_bar_baz);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                editText.setFocusable(true);
                editText.requestFocus();
                editText.setText(getString(R.string.foo_bar_baz));
                Selection.removeSelection(editText.getText());
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                       getString(R.string.foo_bar_baz)).get(0);

        // Select all text.
        Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                editText.getText().length());
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments));

        // Copy the selected text.
        text.performAction( AccessibilityNodeInfo.ACTION_COPY);

        // Set selection at the end.
        final int textLength = editText.getText().length();
        // Verify the selection position.
        assertEquals(textLength, Selection.getSelectionStart(editText.getText()));
        assertEquals(textLength, Selection.getSelectionEnd(editText.getText()));

        // Paste the selected text.
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_PASTE));

        // Verify the content.
        assertEquals(editText.getText().toString(), textContent + " " + textContent);

        // Select all text.
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                editText.getText().length());
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments));

        // Cut the selected text.
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_CUT));

        // Verify the content.
        assertTrue(TextUtils.isEmpty(editText.getText()));
    }

    public void testSetSelectionInContentDescription() throws Exception {
        final View view = getActivity().findViewById(R.id.view);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                view.setContentDescription(getString(R.string.foo_bar_baz));
            }
        });

        AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                       getString(R.string.foo_bar_baz)).get(0);

        // Set the cursor position.
        Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 4);
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 4);
        assertTrue(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments));

        // Try and fail to set the selection longer than zero.
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 4);
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 5);
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments));
    }

    public void testSelectionPositionForNonEditableView() throws Exception {
        final View view = getActivity().findViewById(R.id.view);

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                view.setContentDescription(getString(R.string.foo_bar_baz));
            }
        });

        final AccessibilityNodeInfo text = getInstrumentation().getUiAutomation()
               .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                       getString(R.string.foo_bar_baz)).get(0);

        // Check the initial node properties.
        assertFalse(text.isEditable());
        assertSame(text.getTextSelectionStart(), -1);
        assertSame(text.getTextSelectionEnd(), -1);

        // Set the cursor position.
        getInstrumentation().getUiAutomation().executeAndWaitForEvent(new Runnable() {
            @Override
            public void run() {
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 4);
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 4);
                assertTrue(text.performAction(
                        AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments));
            }
        }, new UiAutomation.AccessibilityEventFilter() {
            @Override
            public boolean accept(AccessibilityEvent event) {
                return (event.getEventType()
                        == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
            }
        }, TIMEOUT_ASYNC_PROCESSING);

        // Refresh the node.
        AccessibilityNodeInfo refreshedText = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.foo_bar_baz)).get(0);

        // Check the related node properties.
        assertFalse(refreshedText.isEditable());
        assertSame(refreshedText.getTextSelectionStart(), 4);
        assertSame(refreshedText.getTextSelectionEnd(), 4);

        // Try to set to an invalid cursor position.
        Bundle arguments = new Bundle();
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 4);
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, 5);
        assertFalse(text.performAction(
                AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments));

        // Refresh the node.
        refreshedText = getInstrumentation().getUiAutomation()
                .getRootInActiveWindow().findAccessibilityNodeInfosByText(
                        getString(R.string.foo_bar_baz)).get(0);

        // Check the related node properties.
        assertFalse(refreshedText.isEditable());
        assertSame(refreshedText.getTextSelectionStart(), 4);
        assertSame(refreshedText.getTextSelectionEnd(), 4);
    }
}
