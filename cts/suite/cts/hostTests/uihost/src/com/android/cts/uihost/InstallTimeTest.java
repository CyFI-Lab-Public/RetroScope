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
import com.android.cts.tradefed.util.HostReportLog;
import com.android.cts.util.MeasureRun;
import com.android.cts.util.MeasureTime;
import com.android.cts.util.ResultType;
import com.android.cts.util.ResultUnit;
import com.android.cts.util.ReportLog;
import com.android.cts.util.Stat;
import com.android.ddmlib.Log;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.testtype.IBuildReceiver;

import java.io.File;


/**
 * Test to measure installation time of a APK.
 */
public class InstallTimeTest extends DeviceTestCase implements IBuildReceiver {
    private CtsBuildHelper mBuild;
    private ITestDevice mDevice;

    private static final String TAG = "InstallTimeTest";
    static final String PACKAGE = "com.replica.replicaisland";
    static final String APK = "com.replica.replicaisland.apk";
    private static final double OUTLIER_THRESHOLD = 0.1;

    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mDevice = getDevice();
    }


    @Override
    protected void tearDown() throws Exception {
        mDevice.uninstallPackage(PACKAGE);
        super.tearDown();
    }

    public void testInstallTime() throws Exception {
        HostReportLog report =
                new HostReportLog(mDevice.getSerialNumber(), ReportLog.getClassMethodNames());
        final int NUMBER_REPEAT = 10;
        final CtsBuildHelper build = mBuild;
        final ITestDevice device = mDevice;
        double[] result = MeasureTime.measure(NUMBER_REPEAT, new MeasureRun() {
            @Override
            public void prepare(int i) throws Exception {
                device.uninstallPackage(PACKAGE);
            }
            @Override
            public void run(int i) throws Exception {
                File app = build.getTestApp(APK);
                device.installPackage(app, false);
            }
        });
        report.printArray("install time", result, ResultType.LOWER_BETTER,
                ResultUnit.MS);
        Stat.StatResult stat = Stat.getStatWithOutlierRejection(result, OUTLIER_THRESHOLD);
        if (stat.mDataCount != result.length) {
            Log.w(TAG, "rejecting " + (result.length - stat.mDataCount) + " outliers");
        }
        report.printSummary("install time", stat.mAverage, ResultType.LOWER_BETTER,
                ResultUnit.MS);
        report.deliverReportToHost();
    }

}
