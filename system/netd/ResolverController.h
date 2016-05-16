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

#ifndef _RESOLVER_CONTROLLER_H_
#define _RESOLVER_CONTROLLER_H_

#include <netinet/in.h>
#include <linux/in.h>

class ResolverController {
public:
    ResolverController() {};
    virtual ~ResolverController() {};

    int setDefaultInterface(const char* iface);
    int setInterfaceDnsServers(const char* iface, const char * domains, const char** servers,
            int numservers);
    int setInterfaceAddress(const char* iface, struct in_addr* addr);
    int flushDefaultDnsCache();
    int flushInterfaceDnsCache(const char* iface);
    int setDnsInterfaceForPid(const char* iface, int pid);
    int clearDnsInterfaceForPid(int pid);
    int setDnsInterfaceForUidRange(const char* iface, int uid_start, int uid_end);
    int clearDnsInterfaceForUidRange(int uid_start, int uid_end);
    int clearDnsInterfaceMappings();
};

#endif /* _RESOLVER_CONTROLLER_H_ */
