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
package com.android.cts.tradefed.targetprep;

import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;

/**
 * {@link SettingsToggler} sets settings by using the "adb shell content" command.
 */
public class SettingsToggler {
    private static final String GROUP_SECURE = "secure";
    private static final String GROUP_GLOBAL = "global";

    /** Sets a setting by deleting and then inserting the string value. */
    public static void setString(ITestDevice device, String group, String name, String value)
            throws DeviceNotAvailableException {
        deleteSetting(device, group, name);
        device.executeShellCommand(
                "content insert"
                + " --uri content://settings/" + group
                + " --bind name:s:" + name
                + " --bind value:s:" + value);
    }

    /** Sets a secure setting by deleting and then inserting the string value. */
    public static void setSecureString(ITestDevice device, String name, String value)
            throws DeviceNotAvailableException {
        setString(device, GROUP_SECURE, name, value);
    }

    /** Sets a global setting by deleting and then inserting the string value. */
    public static void setGlobalString(ITestDevice device, String name, String value)
            throws DeviceNotAvailableException {
        setString(device, GROUP_GLOBAL, name, value);
    }

    /** Sets a setting by deleting and then inserting the int value. */
    public static void setInt(ITestDevice device, String group, String name, int value)
            throws DeviceNotAvailableException {
        deleteSetting(device, group, name);
        device.executeShellCommand(
                "content insert"
                + " --uri content://settings/" + group
                + " --bind name:s:" + name
                + " --bind value:i:" + value);
    }

    /** Sets a secure setting by deleting and then inserting the int value. */
    public static void setSecureInt(ITestDevice device, String name, int value)
            throws DeviceNotAvailableException {
        setInt(device, GROUP_SECURE, name, value);
    }

    /** Sets a global setting by deleting and then inserting the int value. */
    public static void setGlobalInt(ITestDevice device, String name, int value)
            throws DeviceNotAvailableException {
        setInt(device, GROUP_GLOBAL, name, value);
    }

    public static void updateString(ITestDevice device, String group, String name, String value)
            throws DeviceNotAvailableException {
        device.executeShellCommand(
                "content update"
                + " --uri content://settings/" + group
                + " --bind value:s:" + value
                + " --where \"name='" + name + "'\"");
    }

    public static void updateSecureString(ITestDevice device, String name, String value)
            throws DeviceNotAvailableException {
        updateString(device, GROUP_SECURE, name, value);
    }

    public static void updateGlobalString(ITestDevice device, String name, String value)
            throws DeviceNotAvailableException {
        updateString(device, GROUP_GLOBAL, name, value);
    }

    public static void updateInt(ITestDevice device, String group, String name, int value)
            throws DeviceNotAvailableException {
        device.executeShellCommand(
                "content update"
                + " --uri content://settings/" + group
                + " --bind value:i:" + value
                + " --where \"name='" + name + "'\"");
    }

    public static void updateSecureInt(ITestDevice device, String name, int value)
            throws DeviceNotAvailableException {
        updateInt(device, GROUP_SECURE, name, value);
    }

    public static void updateGlobalInt(ITestDevice device, String name, int value)
            throws DeviceNotAvailableException {
        updateInt(device, GROUP_GLOBAL, name, value);
    }

    private static void deleteSetting(ITestDevice device, String group, String name)
            throws DeviceNotAvailableException {
        device.executeShellCommand(
                "content delete"
                + " --uri content://settings/" + group
                + " --where \"name='" + name + "'\"");
    }
}
