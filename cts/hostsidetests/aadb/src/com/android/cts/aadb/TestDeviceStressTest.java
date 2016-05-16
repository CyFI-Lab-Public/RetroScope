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
package com.android.cts.aadb;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.result.CollectingTestListener;
import com.android.tradefed.testtype.DeviceTestCase;
import com.android.tradefed.util.FileUtil;

import java.io.File;
import java.io.IOException;

/**
 * Long running functional tests for {@link TestDevice} that verify an operation can be run
 * many times in sequence
 * <p/>
 * Requires a physical device to be connected.
 */
public class TestDeviceStressTest extends DeviceTestCase {

    private int mIterations = 50;

    private static final String LOG_TAG = "TestDeviceStressTest";
    private static final int TEST_FILE_COUNT= 200;
    private ITestDevice mTestDevice;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestDevice = getDevice();
    }

    private File createTempTestFiles() throws IOException {
        File tmpDir = null;
        File tmpFile = null;

        tmpDir = FileUtil.createTempDir("testDir");

        final String fileContents = "this is the test file contents";
        for (int i = 0; i < TEST_FILE_COUNT; i++) {
            tmpFile = FileUtil.createTempFile(String.format("tmp_%d", i), ".txt", tmpDir);
            FileUtil.writeToFile(fileContents, tmpFile);
        }
        return tmpDir;

    }

    /**
     * Stress test to push a folder which contains 200 text file to device
     * internal storage.
     */
    public void testPushFolderWithManyFiles() throws IOException, DeviceNotAvailableException {
        File tmpDir = null;
        String deviceFilePath = null;
        String externalStorePath = mTestDevice.getMountPoint(IDevice.MNT_EXTERNAL_STORAGE);
        assertNotNull(externalStorePath);
        deviceFilePath = String.format("%s/%s", externalStorePath, "testDir");

        // start the stress test
        try {
            // Create the test folder and make sure the test folder doesn't exist in
            // device before the test start.
            tmpDir = createTempTestFiles();
            for (int i = 0; i < mIterations; i++) {
                mTestDevice.executeShellCommand(String.format("rm -r %s", deviceFilePath));
                assertFalse(String.format("%s exists", deviceFilePath),
                        mTestDevice.doesFileExist(deviceFilePath));
                assertTrue(mTestDevice.pushDir(tmpDir, deviceFilePath));
                assertTrue(mTestDevice.doesFileExist(deviceFilePath));
            }
        } finally {
            if (tmpDir != null) {
                FileUtil.recursiveDelete(tmpDir);
            }
            mTestDevice.executeShellCommand(String.format("rm -r %s", deviceFilePath));
            assertFalse(String.format("%s exists", deviceFilePath),
                    mTestDevice.doesFileExist(deviceFilePath));
        }
    }
}
