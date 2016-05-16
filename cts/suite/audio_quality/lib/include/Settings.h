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


#ifndef CTSAUDIO_SETTINGS_H
#define CTSAUDIO_SETTINGS_H

#include <utils/String8.h>

class Settings {
public:
    static Settings* Instance();
    static void Finalize();
    enum SettingType {
        EADB            = 0, // adb device serial number
        EREPORT_TIME    = 1,
        EREPORT_FILE    = 2,
        EDEVICE_INFO    = 3,
        ETEST_XML       = 4, // name of test description xml
        EMAX_SETTINGS   = 4  // not real setting
    };
    void addSetting(SettingType type, const android::String8 setting);
    const android::String8& getSetting(SettingType type);
private:
    static Settings* mInstance;
    android::String8 mSettings[EMAX_SETTINGS + 1];
};


#endif // CTSAUDIO_SETTINGS_H
