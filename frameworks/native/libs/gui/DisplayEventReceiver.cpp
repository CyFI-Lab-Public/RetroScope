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

#include <string.h>

#include <utils/Errors.h>

#include <gui/BitTube.h>
#include <gui/DisplayEventReceiver.h>
#include <gui/IDisplayEventConnection.h>
#include <gui/ISurfaceComposer.h>

#include <private/gui/ComposerService.h>

// ---------------------------------------------------------------------------

namespace android {

// ---------------------------------------------------------------------------

DisplayEventReceiver::DisplayEventReceiver() {
    sp<ISurfaceComposer> sf(ComposerService::getComposerService());
    if (sf != NULL) {
        mEventConnection = sf->createDisplayEventConnection();
        if (mEventConnection != NULL) {
            mDataChannel = mEventConnection->getDataChannel();
        }
    }
}

DisplayEventReceiver::~DisplayEventReceiver() {
}

status_t DisplayEventReceiver::initCheck() const {
    if (mDataChannel != NULL)
        return NO_ERROR;
    return NO_INIT;
}

int DisplayEventReceiver::getFd() const {
    if (mDataChannel == NULL)
        return NO_INIT;

    return mDataChannel->getFd();
}

status_t DisplayEventReceiver::setVsyncRate(uint32_t count) {
    if (int32_t(count) < 0)
        return BAD_VALUE;

    if (mEventConnection != NULL) {
        mEventConnection->setVsyncRate(count);
        return NO_ERROR;
    }
    return NO_INIT;
}

status_t DisplayEventReceiver::requestNextVsync() {
    if (mEventConnection != NULL) {
        mEventConnection->requestNextVsync();
        return NO_ERROR;
    }
    return NO_INIT;
}


ssize_t DisplayEventReceiver::getEvents(DisplayEventReceiver::Event* events,
        size_t count) {
    return DisplayEventReceiver::getEvents(mDataChannel, events, count);
}

ssize_t DisplayEventReceiver::getEvents(const sp<BitTube>& dataChannel,
        Event* events, size_t count)
{
    return BitTube::recvObjects(dataChannel, events, count);
}

ssize_t DisplayEventReceiver::sendEvents(const sp<BitTube>& dataChannel,
        Event const* events, size_t count)
{
    return BitTube::sendObjects(dataChannel, events, count);
}

// ---------------------------------------------------------------------------

}; // namespace android
