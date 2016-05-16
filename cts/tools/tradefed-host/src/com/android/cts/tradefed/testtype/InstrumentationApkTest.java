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
import com.android.ddmlib.Log;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.result.ITestInvocationListener;
import com.android.tradefed.testtype.IBuildReceiver;
import com.android.tradefed.testtype.InstrumentationTest;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Assert;

/**
 * A {@link InstrumentationTest] that will install CTS apks before test execution,
 * and uninstall on execution completion.
 */
public class InstrumentationApkTest extends InstrumentationTest implements IBuildReceiver {

    private static final String LOG_TAG = "InstrumentationApkTest";

    /** the file names of the CTS apks to install */
    private Collection<String> mInstallFileNames = new ArrayList<String>();
    private Collection<String> mUninstallPackages = new ArrayList<String>();

    private CtsBuildHelper mCtsBuild = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBuild(IBuildInfo build) {
        mCtsBuild  = CtsBuildHelper.createBuildHelper(build);
    }

    /**
     * Add an apk to install.
     *
     * @param apkFileName the apk file name
     * @param packageName the apk's Android package name
     */
    public void addInstallApk(String apkFileName, String packageName) {
        mInstallFileNames.add(apkFileName);
        mUninstallPackages.add(packageName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final ITestInvocationListener listener)
            throws DeviceNotAvailableException {
        Assert.assertNotNull("missing device", getDevice());
        Assert.assertNotNull("missing build", mCtsBuild);

        for (String apkFileName : mInstallFileNames) {
            Log.d(LOG_TAG, String.format("Installing %s on %s", apkFileName,
                    getDevice().getSerialNumber()));
            try {
                String installCode = getDevice().installPackage(mCtsBuild.getTestApp(apkFileName),
                        true);
                Assert.assertNull(String.format("Failed to install %s on %s. Reason: %s",
                        apkFileName, getDevice().getSerialNumber(), installCode), installCode);

            } catch (FileNotFoundException e) {
                Assert.fail(String.format("Could not find file %s", apkFileName));
            }
        }
        super.run(listener);
        for (String packageName : mUninstallPackages) {
            Log.d(LOG_TAG, String.format("Uninstalling %s on %s", packageName,
                    getDevice().getSerialNumber()));
            getDevice().uninstallPackage(packageName);
        }
    }
}
