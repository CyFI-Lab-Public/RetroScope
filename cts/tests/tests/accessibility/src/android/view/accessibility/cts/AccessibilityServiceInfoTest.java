/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

/**
 * Tests whether accessibility service infos are properly reported. Logically,
 * this test belongs to cts/test/test/accessibilityservice but the tests there
 * are using the new UiAutomation API which disables all currently active
 * accessibility service and the fake service used for implementing the UI
 * automation is not reported through the APIs.
 */
public class AccessibilityServiceInfoTest  extends AndroidTestCase {

    /**
     * Tests whether a service can that requested it can retrieve
     * window content.
     */
    @MediumTest
    @SuppressWarnings("deprecation")
    public void testAccessibilityServiceInfoForEnabledService() {
        AccessibilityManager accessibilityManager = (AccessibilityManager)
            getContext().getSystemService(Service.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_SPOKEN);
        assertSame("There should be one speaking service.", 1, enabledServices.size());
        AccessibilityServiceInfo speakingService = enabledServices.get(0);
        assertSame(AccessibilityEvent.TYPES_ALL_MASK, speakingService.eventTypes);
        assertSame(AccessibilityServiceInfo.FEEDBACK_SPOKEN, speakingService.feedbackType);
        assertSame(AccessibilityServiceInfo.DEFAULT
                | AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                | AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
                | AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
                | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS,
                speakingService.flags);
        assertSame(0l, speakingService.notificationTimeout);
        assertEquals("Some description", speakingService.getDescription());
        assertNull(speakingService.packageNames /*all packages*/);
        assertNotNull(speakingService.getId());
        assertSame(speakingService.getCapabilities(),
                AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_ENHANCED_WEB_ACCESSIBILITY
                | AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS
                | AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_TOUCH_EXPLORATION
                | AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT);
        assertEquals("foo.bar.Activity", speakingService.getSettingsActivityName());
        assertEquals("Some description", speakingService.loadDescription(
                getContext().getPackageManager()));
        assertNotNull(speakingService.getResolveInfo());
    }
}
