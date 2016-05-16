/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.launch;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ide.eclipse.adt.internal.launch.DeviceChooserDialog.DeviceChooserResponse;
import com.android.sdklib.internal.avd.AvdInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link DeviceChoiceCache} maps a launch configuration name to the device selected for use
 * in that launch configuration by the {@link DeviceChooserDialog}.
 */
public class DeviceChoiceCache {
    private static final Map<String, String> sDeviceUsedForLaunch = new HashMap<String, String>();

    public static IDevice get(String launchConfigName) {
        // obtain the cached entry
        String deviceName = sDeviceUsedForLaunch.get(launchConfigName);
        if (deviceName == null) {
            return null;
        }

        // verify that the device is still online
        for (IDevice device : getOnlineDevices()) {
            if (deviceName.equals(device.getAvdName()) ||
                    deviceName.equals(device.getSerialNumber())) {
                return device;
            }
        }

        // remove from cache if device is not online anymore
        sDeviceUsedForLaunch.remove(launchConfigName);

        return null;
    }

    public static void put(String launchConfigName, DeviceChooserResponse response) {
        if (!response.useDeviceForFutureLaunches()) {
            return;
        }

        AvdInfo avd = response.getAvdToLaunch();
        String device = null;
        if (avd != null) {
            device = avd.getName();
        } else {
            device = response.getDeviceToUse().getSerialNumber();
        }

        sDeviceUsedForLaunch.put(launchConfigName, device);
    }

    private static IDevice[] getOnlineDevices() {
        AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
        if (bridge != null) {
            return bridge.getDevices();
        } else {
            return new IDevice[0];
        }
    }
}
