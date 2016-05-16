/*
 * Copyright (C) 2011 The Android Open Source Project
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
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.testtype.IDeviceTest;
import com.android.tradefed.testtype.IRemoteTest;

import java.io.File;

/**
 * Test runner for native gTests.
 *
 * TODO: This is similar to Tradefed's existing GTest, but it doesn't confirm
 *       each directory segment exists using ddmlib's file service. This was
 *       a problem since /data is not visible on a user build, but it is
 *       executable. It's also a lot more verbose when it comes to errors.
 */
public class GeeTest implements IBuildReceiver, IDeviceTest, IRemoteTest {

    private static final String NATIVE_TESTS_DIRECTORY = "/data/local/tmp/cts-native-tests";

    private int mMaxTestTimeMs = 1 * 60 * 1000;

    private CtsBuildHelper mCtsBuild;
    private ITestDevice mDevice;

    private final String mPackageName;
    private final String mExeName;

    public GeeTest(String packageName, String exeName) {
        mPackageName = packageName;
        mExeName = exeName;
    }

    @Override
    public void run(ITestInvocationListener listener) throws DeviceNotAvailableException {
        if (installTest()) {
            runTest(listener);
        } else {
            CLog.e("Failed to install native tests");
        }
    }

    private boolean installTest() throws DeviceNotAvailableException {
        if (!createRemoteDir(NATIVE_TESTS_DIRECTORY)) {
            CLog.e("Could not create directory for native tests: " + NATIVE_TESTS_DIRECTORY);
            return false;
        }

        File nativeExe = new File(mCtsBuild.getTestCasesDir(), mExeName);
        if (!nativeExe.exists()) {
            CLog.e("Native test not found: " + nativeExe);
            return false;
        }

        File devicePath = new File(NATIVE_TESTS_DIRECTORY, mExeName);
        if (!mDevice.pushFile(nativeExe, devicePath.toString())) {
            CLog.e("Failed to push native test to device");
            return false;
        }
        return true;
    }

    private boolean createRemoteDir(String remoteFilePath) throws DeviceNotAvailableException {
        if (mDevice.doesFileExist(remoteFilePath)) {
            return true;
        }
        File remoteFile = new File(remoteFilePath);
        String parentPath = remoteFile.getParent();
        if (parentPath != null) {
            if (!createRemoteDir(parentPath)) {
                return false;
            }
        }
        mDevice.executeShellCommand(String.format("mkdir %s", remoteFilePath));
        return mDevice.doesFileExist(remoteFilePath);
    }

    void runTest(ITestRunListener listener) throws DeviceNotAvailableException {
        GeeTestResultParser resultParser = new GeeTestResultParser(mPackageName, listener);
        resultParser.setFakePackagePrefix(mPackageName + ".");

        String fullPath = NATIVE_TESTS_DIRECTORY + File.separator + mExeName;
        String flags = "";
        CLog.v("Running gtest %s %s on %s", fullPath, flags, mDevice.getSerialNumber());
        // force file to be executable
        CLog.v("%s", mDevice.executeShellCommand(String.format("chmod 755 %s", fullPath)));

        try {
            mDevice.executeShellCommand(String.format("%s %s", fullPath, flags), resultParser,
                    mMaxTestTimeMs /* maxTimeToShellOutputResponse */,
                    0 /* retryAttempts */);
        } catch (DeviceNotAvailableException e) {
            resultParser.flush();
            throw e;
        } catch (RuntimeException e) {
            resultParser.flush();
            throw e;
        }
    }


    @Override
    public void setBuild(IBuildInfo buildInfo) {
        mCtsBuild = CtsBuildHelper.createBuildHelper(buildInfo);
    }

    @Override
    public void setDevice(ITestDevice device) {
        mDevice = device;
    }

    @Override
    public ITestDevice getDevice() {
        return mDevice;
    }
}
