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

#include "UidMarkMap.h"

UidMarkMap::UidMarkEntry::UidMarkEntry(int start, int end, int new_mark) :
                                            uid_start(start),
                                            uid_end(end),
                                            mark(new_mark) {
};

bool UidMarkMap::add(int uid_start, int uid_end, int mark) {
    android::RWLock::AutoWLock lock(mRWLock);
    if (uid_start > uid_end) {
        return false;
    }
    android::netd::List<UidMarkEntry*>::iterator it;
    for (it = mMap.begin(); it != mMap.end(); it++) {
        UidMarkEntry *entry = *it;
        if (entry->uid_start <= uid_end && uid_start <= entry->uid_end) {
            return false;
        }
    }

    UidMarkEntry *e = new UidMarkEntry(uid_start, uid_end, mark);
    mMap.push_back(e);
    return true;
};

bool UidMarkMap::remove(int uid_start, int uid_end, int mark) {
    android::RWLock::AutoWLock lock(mRWLock);
    android::netd::List<UidMarkEntry*>::iterator it;
    for (it = mMap.begin(); it != mMap.end(); it++) {
        UidMarkEntry *entry = *it;
        if (entry->uid_start == uid_start && entry->uid_end == uid_end && entry->mark == mark) {
            mMap.erase(it);
            delete entry;
            return true;
        }
    }
    return false;
};

int UidMarkMap::getMark(int uid) {
    android::RWLock::AutoRLock lock(mRWLock);
    android::netd::List<UidMarkEntry*>::iterator it;
    for (it = mMap.begin(); it != mMap.end(); it++) {
        UidMarkEntry *entry = *it;
        if (entry->uid_start <= uid && entry->uid_end >= uid) {
            return entry->mark;
        }
    }
    return -1;
};

bool UidMarkMap::anyRulesForMark(int mark) {
    android::RWLock::AutoRLock lock(mRWLock);
    android::netd::List<UidMarkEntry*>::iterator it;
    for (it = mMap.begin(); it != mMap.end(); it++) {
        UidMarkEntry *entry = *it;
        if (entry->mark == mark) {
            return true;
        }
    }
    return false;
}
