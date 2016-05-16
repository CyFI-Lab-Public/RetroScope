/**
 * Copyright (c) 2013, The Android Open Source Project
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

#include <stdint.h>
#include <sys/types.h>

#include <utils/Singleton.h>

namespace android {

class ConnectivityManager : public Singleton<ConnectivityManager> {
    // Keep this in sync with IConnectivityManager.aidl
    static const int TRANSACTION_markSocketAsUser = IBinder::FIRST_CALL_TRANSACTION;
    static const String16 DESCRIPTOR;

    friend class Singleton<ConnectivityManager>;
    sp<IBinder> mConnectivityService;

    ConnectivityManager();

    void markSocketAsUserImpl(int fd, uid_t uid);

public:
    static void markSocketAsUser(int fd, uid_t uid) {
        ConnectivityManager::getInstance().markSocketAsUserImpl(fd, uid);
    }
};

};
