/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "RemoteDisplay.h"

#include "source/WifiDisplaySource.h"

#include <media/IRemoteDisplayClient.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/foundation/ANetworkSession.h>

namespace android {

RemoteDisplay::RemoteDisplay(
        const sp<IRemoteDisplayClient> &client,
        const char *iface)
    : mLooper(new ALooper),
      mNetSession(new ANetworkSession) {
    mLooper->setName("wfd_looper");

    mSource = new WifiDisplaySource(mNetSession, client);
    mLooper->registerHandler(mSource);

    mNetSession->start();
    mLooper->start();

    mSource->start(iface);
}

RemoteDisplay::~RemoteDisplay() {
}

status_t RemoteDisplay::pause() {
    return mSource->pause();
}

status_t RemoteDisplay::resume() {
    return mSource->resume();
}

status_t RemoteDisplay::dispose() {
    mSource->stop();
    mSource.clear();

    mLooper->stop();
    mNetSession->stop();

    return OK;
}

}  // namespace android
