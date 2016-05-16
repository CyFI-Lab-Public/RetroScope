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

/* AndroidConfiguration implementation */

#include "sles_allinclusive.h"


static SLresult IAndroidConfiguration_SetConfiguration(SLAndroidConfigurationItf self,
        const SLchar *configKey,
        const void *pConfigValue,
        SLuint32 valueSize)
{
    SL_ENTER_INTERFACE

    // object-specific code will check that valueSize is large enough for the key
    if (NULL == configKey || NULL == pConfigValue || valueSize == 0) {
        result = SL_RESULT_PARAMETER_INVALID;

    } else {
        IAndroidConfiguration *thiz = (IAndroidConfiguration *) self;
        interface_lock_exclusive(thiz);

        // route configuration to the appropriate object
        switch (IObjectToObjectID((thiz)->mThis)) {
        case SL_OBJECTID_AUDIORECORDER:
            SL_LOGV("SetConfiguration issued for AudioRecorder key=%s valueSize=%u",
                    configKey, valueSize);
            result = android_audioRecorder_setConfig((CAudioRecorder *) thiz->mThis, configKey,
                    pConfigValue, valueSize);
            break;
        case SL_OBJECTID_AUDIOPLAYER:
            SL_LOGV("SetConfiguration issued for AudioPlayer key=%s valueSize=%u",
                    configKey, valueSize);
            result = android_audioPlayer_setConfig((CAudioPlayer *) thiz->mThis, configKey,
                    pConfigValue, valueSize);
            break;
        default:
            result = SL_RESULT_FEATURE_UNSUPPORTED;
            break;
        }

        interface_unlock_exclusive(thiz);
    }

    SL_LEAVE_INTERFACE
}


static SLresult IAndroidConfiguration_GetConfiguration(SLAndroidConfigurationItf self,
        const SLchar *configKey,
        SLuint32 *pValueSize,
        void *pConfigValue)
{
    SL_ENTER_INTERFACE

    // non-NULL pValueSize is required, but a NULL pConfigValue is allowed, so
    // that we can report the actual value size without returning the value itself
    if (NULL == configKey || NULL == pValueSize) {
        result = SL_RESULT_PARAMETER_INVALID;
    } else {
        IAndroidConfiguration *thiz = (IAndroidConfiguration *) self;
        interface_lock_exclusive(thiz);

        // route configuration request to the appropriate object
        switch (IObjectToObjectID((thiz)->mThis)) {
        case SL_OBJECTID_AUDIORECORDER:
            result = android_audioRecorder_getConfig((CAudioRecorder *) thiz->mThis, configKey,
                    pValueSize, pConfigValue);
            break;
        case SL_OBJECTID_AUDIOPLAYER:
            result = android_audioPlayer_getConfig((CAudioPlayer *) thiz->mThis, configKey,
                    pValueSize, pConfigValue);
        default:
            result = SL_RESULT_FEATURE_UNSUPPORTED;
            break;
        }

        interface_unlock_exclusive(thiz);
    }

    SL_LEAVE_INTERFACE
}


static const struct SLAndroidConfigurationItf_ IAndroidConfiguration_Itf = {
    IAndroidConfiguration_SetConfiguration,
    IAndroidConfiguration_GetConfiguration
};

void IAndroidConfiguration_init(void *self)
{
    IAndroidConfiguration *thiz = (IAndroidConfiguration *) self;
    thiz->mItf = &IAndroidConfiguration_Itf;
}
