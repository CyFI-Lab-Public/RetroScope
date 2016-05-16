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

package com.android.cts.uihost;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.cts.tradefed.result.CtsReportUtil;
import com.android.cts.tradefed.util.CtsHostStore;
import com.android.cts.tradefed.util.HostReportLog;
import com.android.cts.util.ReportLog;
import com.android.cts.util.TimeoutReq;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.device.TestDeviceOptions;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.result.TestResult;
import com.android.tradefed.result.TestRunResult;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;
import java.util.Map;


/**
 * Measure time to taskswitching between two Apps: A & B
 * Actual test is done in device, but this host side code installs all necessary APKs
 * and starts device test which is in CtsDeviceTaskswitchingControl.
 */
public class TaskSwitchingTest extends DeviceTestCase implements IBuildReceiver {
    private static final String TAG = "TaskSwitchingTest";
    private final static String CTS_RUNNER = "android.test.InstrumentationCtsTestRunner";
    private CtsBuildHelper mBuild;
    private ITestDevice mDevice;
    private String mCtsReport = null;

    static final String[] PACKAGES = {
        "com.android.cts.taskswitching.control",
        "com.android.cts.taskswitching.appa",
        "com.android.cts.taskswitching.appb"
    };
    static final String[] APKS = {
        "CtsDeviceTaskswitchingControl.apk",
        "CtsDeviceTaskswitchingAppA.apk",
        "CtsDeviceTaskswitchingAppB.apk"
    };

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = getDevice();
        for (int i = 0; i < PACKAGES.length; i++) {
            mDevice.uninstallPackage(PACKAGES[i]);
            File app = mBuild.getTestApp(APKS[i]);
            mDevice.installPackage(app, false);
        }
    }


    @Override
    protected void tearDown() throws Exception {
        for (int i = 0; i < PACKAGES.length; i++) {
            mDevice.uninstallPackage(PACKAGES[i]);
        }
        super.tearDown();
    }

    @TimeoutReq(minutes = 30)
    public void testTaskswitching() throws Exception {
        HostReportLog report =
                new HostReportLog(mDevice.getSerialNumber(), ReportLog.getClassMethodNames());
        RemoteAndroidTestRunner testRunner = new RemoteAndroidTestRunner(PACKAGES[0], CTS_RUNNER,
                mDevice.getIDevice());
        LocalListener listener = new LocalListener();
        mDevice.runInstrumentationTests(testRunner, listener);
        TestRunResult result = listener.getCurrentRunResults();
        if (result.isRunFailure()) {
            fail(result.getRunFailureMessage());
        }
        assertNotNull("no performance data", mCtsReport);
        CtsHostStore.storeCtsResult(mDevice.getSerialNumber(),
                ReportLog.getClassMethodNames(), mCtsReport);

    }

    public class LocalListener extends CollectingTestListener {
        @Override
        public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
            // necessary as testMetrics passed from CollectingTestListerner is empty
            mCtsReport = CtsReportUtil.getCtsResultFromMetrics(testMetrics);
            super.testEnded(test, testMetrics);
        }
    }
}
