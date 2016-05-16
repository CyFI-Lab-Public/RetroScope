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
package com.android.cts.tradefed.targetprep;

import com.android.cts.tradefed.build.CtsBuildHelper;
import com.android.tradefed.build.IBuildInfo;
import com.android.tradefed.build.IFolderBuildInfo;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.targetprep.DeviceSetup;
import com.android.tradefed.targetprep.ITargetPreparer;
import com.android.tradefed.targetprep.TargetSetupError;

import java.io.FileNotFoundException;

/**
 * A {@link ITargetPreparer} that attempts to automatically perform the CTS-specific manual steps
 * for setting up a device for CTS testing.
 * <p/>
 * This class is NOT intended for 'official' CTS runs against a production device as the steps
 * performed by this class require a debug build (aka 'adb root' must succeed).
 * <p/>
 * This class currently performs the 'Allow mock locations' and 'accessibililty setup' steps
 * documented in the CTS user manual. It is intended to be used in conjunction with
 * a {@link DeviceSetup} which will enable the 'Stay Awake' setting and verify that external
 * storage is present.
 */
public class CtsRootDeviceSetup implements ITargetPreparer {

    private static final String DEVICE_ADMIN_APK_FILE_NAME = "CtsDeviceAdmin.apk";

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp(ITestDevice device, IBuildInfo buildInfo) throws TargetSetupError,
            DeviceNotAvailableException {
        if (!(buildInfo instanceof IFolderBuildInfo)) {
            throw new IllegalArgumentException("Provided buildInfo is not a IFolderBuildInfo");
        }
        CLog.i("Setting up %s to run CTS tests", device.getSerialNumber());

        IFolderBuildInfo ctsBuild = (IFolderBuildInfo)buildInfo;
        try {
            CtsBuildHelper buildHelper = new CtsBuildHelper(ctsBuild.getRootDir());

            if (!device.enableAdbRoot()) {
                throw new TargetSetupError(String.format(
                        "Failed to set root on device %s.", device.getSerialNumber()));
            }

            // perform CTS setup steps that only work if adb is root
            SettingsToggler.setSecureInt(device, "mock_location", 1);
            enableDeviceAdmin(device, buildHelper);
            // This is chrome specific setting to disable the first screen.
            // For other browser, it will not do anything.
            device.executeShellCommand(
                    "echo \"chrome --disable-fre\" > /data/local/chrome-command-line");
            // end root setup steps
        } catch (FileNotFoundException e) {
            throw new TargetSetupError("Invalid CTS installation", e);
        }
    }

    private void enableDeviceAdmin(ITestDevice device, CtsBuildHelper ctsBuild)
            throws DeviceNotAvailableException, TargetSetupError, FileNotFoundException {
        String errorCode = device.installPackage(ctsBuild.getTestApp(DEVICE_ADMIN_APK_FILE_NAME),
                true);
        if (errorCode != null) {
            // TODO: retry ?
            throw new TargetSetupError(String.format(
                    "Failed to install %s on device %s. Reason: %s",
                    DEVICE_ADMIN_APK_FILE_NAME, device.getSerialNumber(), errorCode));
        }
        // TODO: enable device admin Settings
    }
}
