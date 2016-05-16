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

#include "Log.h"

#include "Adb.h"
#include "ClientImpl.h"


ClientImpl::ClientImpl()
    : mAudio(new RemoteAudio(mSocket))
{

}

ClientImpl::~ClientImpl()
{
    mAudio->release();
}

bool ClientImpl::init(const android::String8& param)
{
    Adb adb(param);
    if (!adb.setPortForwarding(HOST_TCP_PORT, CLIENT_TCP_PORT)) {
        LOGE("adb port forwarding failed");
        return false;
    }
    android::String8 clientBinary("client/CtsAudioClient.apk");
    android::String8 componentName("com.android.cts.audiotest/.CtsAudioClientActivity");
    if (!adb.launchClient(clientBinary, componentName)) {
        LOGE("cannot install or launch client");
        return false;
    }
    // now socket connection
    return mAudio->init(HOST_TCP_PORT);
}


