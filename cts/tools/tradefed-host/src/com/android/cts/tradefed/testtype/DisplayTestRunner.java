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

package com.android.cts.tradefed.testtype;

import com.android.cts.tradefed.targetprep.SettingsToggler;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.result.ITestInvocationListener;

/**
 * Running the display tests requires modification of secure settings to create an overlay display.
 * Secure settings cannot be changed from device CTS tests since system signature permission is
 * required. Such settings can be modified by the shell user, so a host side test is used.
 */
public class DisplayTestRunner extends InstrumentationApkTest {
    private static final String OVERLAY_DISPLAY_DEVICES_SETTING_NAME = "overlay_display_devices";

    // Use a non-standard pattern, must match values in tests/tests/display/.../DisplayTest.java
    private static final String OVERLAY_DISPLAY_DEVICES_SETTING_VALUE = "1281x721/214";

    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        // CLog.e("run: About to enable overlay display.");
        SettingsToggler.setGlobalString(getDevice(), OVERLAY_DISPLAY_DEVICES_SETTING_NAME,
                OVERLAY_DISPLAY_DEVICES_SETTING_VALUE);

        super.run(listener);

        // Tear down overlay display.
        // CLog.e("run: About to disable overlay display.");
        SettingsToggler.setGlobalString(getDevice(), OVERLAY_DISPLAY_DEVICES_SETTING_NAME,
                "");
    }
}
