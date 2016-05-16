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

#define LOG_TAG "ResolverController"
#define DBG 0

#include <cutils/log.h>

#include <net/if.h>

// NOTE: <resolv_iface.h> is a private C library header that provides
//       declarations for _resolv_set_default_iface() and others.
#include <resolv_iface.h>

#include "ResolverController.h"

int ResolverController::setDefaultInterface(const char* iface) {
    if (DBG) {
        ALOGD("setDefaultInterface iface = %s\n", iface);
    }

    _resolv_set_default_iface(iface);

    return 0;
}

int ResolverController::setInterfaceDnsServers(const char* iface, const char* domains,
        const char** servers, int numservers) {
    if (DBG) {
        ALOGD("setInterfaceDnsServers iface = %s\n", iface);
    }
    _resolv_set_nameservers_for_iface(iface, servers, numservers, domains);

    return 0;
}

int ResolverController::setInterfaceAddress(const char* iface, struct in_addr* addr) {
    if (DBG) {
        ALOGD("setInterfaceAddress iface = %s\n", iface);
    }

    _resolv_set_addr_of_iface(iface, addr);

    return 0;
}

int ResolverController::flushDefaultDnsCache() {
    if (DBG) {
        ALOGD("flushDefaultDnsCache\n");
    }

    _resolv_flush_cache_for_default_iface();

    return 0;
}

int ResolverController::flushInterfaceDnsCache(const char* iface) {
    if (DBG) {
        ALOGD("flushInterfaceDnsCache iface = %s\n", iface);
    }

    _resolv_flush_cache_for_iface(iface);

    return 0;
}

int ResolverController::setDnsInterfaceForPid(const char* iface, int pid) {
    if (DBG) {
        ALOGD("setDnsIfaceForPid iface = %s, pid = %d\n", iface, pid);
    }

    _resolv_set_iface_for_pid(iface, pid);

    return 0;
}

int ResolverController::clearDnsInterfaceForPid(int pid) {
    if (DBG) {
        ALOGD("clearDnsIfaceForPid pid = %d\n", pid);
    }

    _resolv_clear_iface_for_pid(pid);

    return 0;
}

int ResolverController::setDnsInterfaceForUidRange(const char* iface, int uid_start, int uid_end) {
    if (DBG) {
        ALOGD("setDnsIfaceForUidRange iface = %s, range = [%d,%d]\n", iface, uid_start, uid_end);
    }

    return _resolv_set_iface_for_uid_range(iface, uid_start, uid_end);
}

int ResolverController::clearDnsInterfaceForUidRange(int uid_start, int uid_end) {
    if (DBG) {
        ALOGD("clearDnsIfaceForUidRange range = [%d,%d]\n", uid_start, uid_end);
    }

    return _resolv_clear_iface_for_uid_range(uid_start, uid_end);
}

int ResolverController::clearDnsInterfaceMappings()
{
    if (DBG) {
        ALOGD("clearInterfaceMappings\n");
    }
    _resolv_clear_iface_uid_range_mapping();
    _resolv_clear_iface_pid_mapping();

    return 0;
}
