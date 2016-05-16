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

#ifndef CTSAUDIO_ADB_H
#define CTSAUDIO_ADB_H

#include <utils/String8.h>

/** ADB interface to set port forwarding and launch client app */
class Adb {
public:
    /// device: device number typically passed in adb's -s argument.
    /// if device string is empty, adb command will be called without -s option.
    Adb(const android::String8& device);
    ~Adb();
    bool setPortForwarding(int hostPort, int devicePort);
    /// install given clientBinary to DUT and launch given component.
    bool launchClient(const android::String8& clientBinary, const android::String8& component);
private:
    int executeCommand(const android::String8& command);

private:
    android::String8 mDevice;
};

#endif // CTSAUDIO_ADB_H




