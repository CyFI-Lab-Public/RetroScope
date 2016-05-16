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
package com.android.cts.tradefed.device;

import com.android.ddmlib.Log;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.InstrumentationTest;

import java.io.File;

/**
 * Collects info from device under test.
 * <p/>
 * This class simply serves as a conduit for grabbing info from device using the device info
 * collector apk, and forwarding that data directly to the {@link ITestInvocationListener} as run
 * metrics.
 */
public class DeviceInfoCollector {

    private static final String LOG_TAG = "DeviceInfoCollector";
    private static final String APK_NAME = "TestDeviceSetup";
    public static final String APP_PACKAGE_NAME = "android.tests.devicesetup";
    private static final String INSTRUMENTATION_NAME = "android.tests.getinfo.DeviceInfoInstrument";

    /**
     * Installs and runs the device info collector instrumentation, and forwards results
     * to the <var>listener</var>
     *
     * @param device
     * @param listener
     * @throws DeviceNotAvailableException
     */
    public static void collectDeviceInfo(ITestDevice device, File testApkDir,
            ITestInvocationListener listener) throws DeviceNotAvailableException {
        File apkFile = new File(testApkDir, String.format("%s.apk", APK_NAME));
        if (!apkFile.exists()) {
            Log.e(LOG_TAG, String.format("Could not find %s", apkFile.getAbsolutePath()));
        }
        // collect the instrumentation bundle results using instrumentation test
        // should work even though no tests will actually be run
        InstrumentationTest instrTest = new InstrumentationTest();
        instrTest.setDevice(device);
        instrTest.setInstallFile(apkFile);
        // no need to collect tests and re-run
        instrTest.setRerunMode(false);
        instrTest.setPackageName(APP_PACKAGE_NAME);
        instrTest.setRunnerName(INSTRUMENTATION_NAME);
        instrTest.run(listener);
    }
}
