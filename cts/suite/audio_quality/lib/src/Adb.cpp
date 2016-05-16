/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
#include <stdlib.h>

#include <StringUtil.h>
#include "Adb.h"

Adb::Adb(const android::String8& device)
    : mDevice(device)
{

}

Adb::~Adb()
{

}

bool Adb::setPortForwarding(int hostPort, int devicePort)
{
    android::String8 command;
    if (command.appendFormat("forward tcp:%d tcp:%d", hostPort, devicePort) != 0) {
        return false;
    }
    if (executeCommand(command) != 0) {
        return false;
    }
    return true;
}

bool Adb::launchClient(const android::String8& clientBinary, const android::String8& component)
{
    android::String8 command;
    if (command.appendFormat("install -r %s", clientBinary.string()) != 0) {
        return false;
    }
    if (executeCommand(command) != 0) {
        return false;
    }
    command.clear();
    if (command.appendFormat("shell am start -W -n %s", component.string()) != 0) {
        return false;
    }
    if (executeCommand(command) != 0) {
        return false;
    }
    return true;
}

/** @param command ADB command except adb -s XYZW */
int Adb::executeCommand(const android::String8& command)
{
    android::String8 adbCommand;
    if (mDevice.empty()) {
        if (adbCommand.appendFormat("adb %s", command.string()) != 0) {
            return -1;
        }
    } else {
        if (adbCommand.appendFormat("adb -s %s %s", mDevice.string(),
                command.string()) != 0) {
            return -1;
        }
    }
    return system(adbCommand.string());
}

