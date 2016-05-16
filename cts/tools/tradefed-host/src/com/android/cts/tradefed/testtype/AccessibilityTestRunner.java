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

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.targetprep.SettingsToggler;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.util.FileUtil;

import junit.framework.TestCase;

import java.io.File;

/**
 * Running the accessibility tests requires modification of secure
 * settings. Secure settings cannot be changed from device CTS tests
 * since system signature permission is required. Such settings can
 * be modified by the shell user, so a host side test is used for
 * installing a package with some accessibility services, enabling
 * these services, running the tests, disabling the services, and removing
 * the accessibility services package.
 */
public class AccessibilityTestRunner extends InstrumentationApkTest {

    private static final String SOME_ACCESSIBLITY_SERVICES_PACKAGE_NAME =
        "android.view.accessibility.services";

    private static final String SPEAKING_ACCESSIBLITY_SERVICE_NAME =
        "android.view.accessibility.services.SpeakingAccessibilityService";

    private static final String VIBRATING_ACCESSIBLITY_SERVICE_NAME =
        "android.view.accessibility.services.VibratingAccessibilityService";

    private static final String SOME_ACCESSIBLITY_SERVICES_APK =
        "CtsSomeAccessibilityServices.apk";

    private CtsBuildHelper mCtsBuild;

    @Override
    public void setBuild(IBuildInfo build) {
        super.setBuild(build);
        mCtsBuild  = CtsBuildHelper.createBuildHelper(build);
    }

    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        beforeTest();
        super.run(listener);
        afterTest();
    }

    private void beforeTest() throws DeviceNotAvailableException {
        installApkAndAssert(SOME_ACCESSIBLITY_SERVICES_APK);
        enableAccessibilityAndServices();
    }

    private void afterTest() throws DeviceNotAvailableException {
        disableAccessibilityAndServices(getDevice());
        uninstallAndAssert(SOME_ACCESSIBLITY_SERVICES_PACKAGE_NAME);
    }

    private void installApkAndAssert(String apkName) throws DeviceNotAvailableException {
        File file = FileUtil.getFileForPath(mCtsBuild.getTestCasesDir(), apkName);
        String errorMessage = getDevice().installPackage(file, true);
        TestCase.assertNull("Error installing: " + apkName, errorMessage);
    }

    private void uninstallAndAssert(String packageName) throws DeviceNotAvailableException {
        String errorMessage = getDevice().uninstallPackage(packageName);
        TestCase.assertNull("Error uninstalling: " + packageName, errorMessage);
    }

    private void enableAccessibilityAndServices() throws DeviceNotAvailableException {
        String enabledServicesValue =
              SOME_ACCESSIBLITY_SERVICES_PACKAGE_NAME + "/" + SPEAKING_ACCESSIBLITY_SERVICE_NAME
            + ":"
            + SOME_ACCESSIBLITY_SERVICES_PACKAGE_NAME + "/" + VIBRATING_ACCESSIBLITY_SERVICE_NAME;
        enableAccessibilityAndServices(getDevice(), enabledServicesValue);
    }

    static void enableAccessibilityAndServices(ITestDevice device, String value)
            throws DeviceNotAvailableException {
        SettingsToggler.setSecureString(device, "enabled_accessibility_services", value);
        SettingsToggler.setSecureString(device,
                "touch_exploration_granted_accessibility_services", value);
        SettingsToggler.setSecureInt(device, "accessibility_enabled", 1);
    }

    static void disableAccessibilityAndServices(ITestDevice device)
            throws DeviceNotAvailableException {
        SettingsToggler.updateSecureString(device, "enabled_accessibility_services", "");
        SettingsToggler.updateSecureString(device,
                "touch_exploration_granted_accessibility_services", "");
        SettingsToggler.updateSecureInt(device, "accessibility_enabled", 0);
    }
}
