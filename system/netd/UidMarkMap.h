/*
 * Copyright (C) 2013 The Android Open Source Project
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

#ifndef _NETD_UIDMARKMAP_H
#define _NETD_UIDMARKMAP_H

#include <stddef.h>
#include <stdint.h>
#include <List.h>
#include <utils/RWLock.h>

class UidMarkMap {
public:
    bool add(int uid_start, int uid_end, int mark);
    bool remove(int uid_start, int uid_end, int mark);
    int getMark(int uid);
    bool anyRulesForMark(int mark);

private:
    struct UidMarkEntry {
        int uid_start;
        int uid_end;
        int mark;
        UidMarkEntry(int uid_start, int uid_end, int mark);
    };

    android::RWLock mRWLock;
    android::netd::List<UidMarkEntry*> mMap;
};
#endif
