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


#ifndef CTSAUDIO_CLIENTIMPL_H
#define CTSAUDIO_CLIENTIMPL_H

#include <utils/StrongPointer.h>
#include "ClientInterface.h"
#include "ClientSocket.h"
#include "audio/RemoteAudio.h"

class ClientImpl: public ClientInterface {
public:
    static const int HOST_TCP_PORT = 15001;
    static const int CLIENT_TCP_PORT = 15001;
    ClientImpl();
    virtual ~ClientImpl();
    virtual bool init(const android::String8& param);

    virtual ClientSocket& getSocket() {
        return mSocket;
    };

    virtual android::sp<RemoteAudio>& getAudio() {
        return mAudio;
    };

private:
    ClientSocket mSocket;
    android::sp<RemoteAudio> mAudio;

};


#endif // CTSAUDIO_CLIENTIMPL_H
