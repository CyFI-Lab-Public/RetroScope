/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.cts.jank.ui;

import com.android.cts.jank.CtsHostJankTest;

import java.io.File;

public class CtsHostJankUi extends CtsHostJankTest {

    private static final String APK_PACKAGE = "com.android.cts";
    private static final String APK = "CtsDeviceUi.apk";
    private static final String PACKAGE = "com.android.cts.jank.ui";
    private static final String HOST_CLASS = CtsHostJankUi.class.getName();
    private static final String DEVICE_CLASS = PACKAGE + ".CtsDeviceJankUi";
    private static final String JAR_NAME = "CtsDeviceJank.jar";

    public CtsHostJankUi() {
        super(JAR_NAME, DEVICE_CLASS, HOST_CLASS);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Install the app.
        mDevice.uninstallPackage(APK_PACKAGE);
        File app = mBuild.getTestApp(APK);
        mDevice.installPackage(app, false);
    }

    @Override
    protected void tearDown() throws Exception {
        // Uninstall the app.
        mDevice.uninstallPackage(APK_PACKAGE);
        super.tearDown();
    }

    public void testScrolling() throws Exception {
        runUiAutomatorTest("testScrolling");
    }
}
