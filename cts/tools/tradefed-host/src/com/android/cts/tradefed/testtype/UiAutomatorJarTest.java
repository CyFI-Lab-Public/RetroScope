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
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.testtype.UiAutomatorTest;

import java.io.FileNotFoundException;
import java.util.Arrays;

import junit.framework.Assert;

/**
 * A {@link UiAutomatorTest} that will install a uiautomator jar before test
 * execution, and uninstall on execution completion.
 */
public class UiAutomatorJarTest extends UiAutomatorTest implements IBuildReceiver {

    // TODO: expose this in parent
    private static final String SHELL_EXE_BASE = "/data/local/tmp/";

    /** the file names of the CTS jar to install */
    private String mTestJarFileName;

    private CtsBuildHelper mCtsBuild = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuild(IBuildInfo build) {
        mCtsBuild  = CtsBuildHelper.createBuildHelper(build);
    }

    /**
     * Setter for CTS build files needed to perform the test. 
     *
     * @param testJarName the file name of the jar containing the uiautomator tests
     */
    public void setInstallArtifacts(String testJarName) {
        mTestJarFileName = testJarName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final ITestInvocationListener listener)
            throws DeviceNotAvailableException {
        Assert.assertNotNull("missing device", getDevice());
        Assert.assertNotNull("missing build", mCtsBuild);
        Assert.assertNotNull("missing jar to install", mTestJarFileName);

        installJar();

        super.run(listener);

        uninstallJar();
    }

    private void installJar() throws DeviceNotAvailableException {
        CLog.d("Installing %s on %s", mTestJarFileName, getDevice().getSerialNumber());
        String fullJarPath = String.format("%s%s", SHELL_EXE_BASE, mTestJarFileName);
        try {
            boolean result = getDevice().pushFile(mCtsBuild.getTestApp(mTestJarFileName),
                    fullJarPath);
            Assert.assertTrue(String.format("Failed to push file to %s", fullJarPath), result);
            setTestJarPaths(Arrays.asList(fullJarPath));
        }  catch (FileNotFoundException e) {
            Assert.fail(String.format("Could not find file %s", mTestJarFileName));
        }
    }

    private void uninstallJar() throws DeviceNotAvailableException {
        CLog.d("Uninstalling %s on %s", mTestJarFileName, getDevice().getSerialNumber());
        String fullJarPath = String.format("%s%s", SHELL_EXE_BASE, mTestJarFileName);
        getDevice().executeShellCommand(String.format("rm %s", fullJarPath));
    }
}
