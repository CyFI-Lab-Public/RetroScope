/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.accessibilityservice.cts;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Parcel;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.accessibility.AccessibilityEvent;

/**
 * Class for testing {@link AccessibilityServiceInfo}.
 */
public class AccessibilityServiceInfoTest extends AndroidTestCase {

    @MediumTest
    public void testMarshalling() throws Exception {

        // fully populate the service info to marshal
        AccessibilityServiceInfo sentInfo = new AccessibilityServiceInfo();
        fullyPopulateSentAccessibilityServiceInfo(sentInfo);

        // marshal and unmarshal the service info
        Parcel parcel = Parcel.obtain();
        sentInfo.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        AccessibilityServiceInfo receivedInfo = AccessibilityServiceInfo.CREATOR
                .createFromParcel(parcel);

        // make sure all fields properly marshaled
        assertAllFieldsProperlyMarshalled(sentInfo, receivedInfo);
    }

    /**
     * Tests whether the service info describes its contents consistently.
     */
    @MediumTest
    public void testDescribeContents() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        assertSame("Accessibility service info always return 0 for this method.", 0,
                info.describeContents());
        fullyPopulateSentAccessibilityServiceInfo(info);
        assertSame("Accessibility service infos always return 0 for this method.", 0,
                info.describeContents());
    }

    /**
     * Tests whether a feedback type is correctly transformed to a string.
     */
    @MediumTest
    public void testFeedbackTypeToString() {
        assertEquals("[FEEDBACK_AUDIBLE]", AccessibilityServiceInfo.feedbackTypeToString(
                AccessibilityServiceInfo.FEEDBACK_AUDIBLE));
        assertEquals("[FEEDBACK_GENERIC]", AccessibilityServiceInfo.feedbackTypeToString(
                AccessibilityServiceInfo.FEEDBACK_GENERIC));
        assertEquals("[FEEDBACK_HAPTIC]", AccessibilityServiceInfo.feedbackTypeToString(
                AccessibilityServiceInfo.FEEDBACK_HAPTIC));
        assertEquals("[FEEDBACK_SPOKEN]", AccessibilityServiceInfo.feedbackTypeToString(
                AccessibilityServiceInfo.FEEDBACK_SPOKEN));
        assertEquals("[FEEDBACK_VISUAL]", AccessibilityServiceInfo.feedbackTypeToString(
                AccessibilityServiceInfo.FEEDBACK_VISUAL));
        assertEquals("[FEEDBACK_BRAILLE]", AccessibilityServiceInfo.feedbackTypeToString(
                AccessibilityServiceInfo.FEEDBACK_BRAILLE));
        assertEquals("[FEEDBACK_SPOKEN, FEEDBACK_HAPTIC, FEEDBACK_AUDIBLE, FEEDBACK_VISUAL,"
                + " FEEDBACK_GENERIC, FEEDBACK_BRAILLE]",
                AccessibilityServiceInfo.feedbackTypeToString(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK));
    }

    /**
     * Tests whether a flag is correctly transformed to a string.
     */
    @MediumTest
    public void testFlagToString() {
        assertEquals("DEFAULT", AccessibilityServiceInfo.flagToString(
                AccessibilityServiceInfo.DEFAULT));
        assertEquals("FLAG_INCLUDE_NOT_IMPORTANT_VIEWS", AccessibilityServiceInfo.flagToString(
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS));
        assertEquals("FLAG_REPORT_VIEW_IDS", AccessibilityServiceInfo.flagToString(
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS));
        assertEquals("FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY", AccessibilityServiceInfo
                .flagToString(AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY));
        assertEquals("FLAG_REQUEST_FILTER_KEY_EVENTS", AccessibilityServiceInfo.flagToString(
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS));
        assertEquals("FLAG_REQUEST_TOUCH_EXPLORATION_MODE", AccessibilityServiceInfo.flagToString(
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE));
        
    }

    /**
     * Fully populates the {@link AccessibilityServiceInfo} to marshal.
     *
     * @param sentInfo The service info to populate.
     */
    private void fullyPopulateSentAccessibilityServiceInfo(AccessibilityServiceInfo sentInfo) {
        sentInfo.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED;
        sentInfo.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        sentInfo.flags = AccessibilityServiceInfo.DEFAULT;
        sentInfo.notificationTimeout = 1000;
        sentInfo.packageNames = new String[] {
            "foo.bar.baz"
        };
    }

    /**
     * Compares all properties of the <code>sentInfo</code> and the
     * <code>receviedInfo</code> to make sure marshaling is correctly
     * implemented.
     */
    private void assertAllFieldsProperlyMarshalled(AccessibilityServiceInfo sentInfo,
            AccessibilityServiceInfo receivedInfo) {
        assertEquals("eventTypes not marshalled properly", sentInfo.eventTypes,
                receivedInfo.eventTypes);
        assertEquals("feedbackType not marshalled properly", sentInfo.feedbackType,
                receivedInfo.feedbackType);
        assertEquals("flags not marshalled properly", sentInfo.flags, receivedInfo.flags);
        assertEquals("notificationTimeout not marshalled properly", sentInfo.notificationTimeout,
                receivedInfo.notificationTimeout);
        assertEquals("packageNames not marshalled properly", sentInfo.packageNames.length,
                receivedInfo.packageNames.length);
        assertEquals("packageNames not marshalled properly", sentInfo.packageNames[0],
                receivedInfo.packageNames[0]);
    }
}
