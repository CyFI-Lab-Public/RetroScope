/*
 * Copyright (C) 2008 The Android Open Source Project
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

#ifndef _SECONDARY_TABLE_CONTROLLER_H
#define _SECONDARY_TABLE_CONTROLLER_H

#include <sysutils/FrameworkListener.h>

#include <net/if.h>
#include "UidMarkMap.h"
#include "NetdConstants.h"

#ifndef IFNAMSIZ
#define IFNAMSIZ 16
#endif

static const int INTERFACES_TRACKED = 10;
static const int BASE_TABLE_NUMBER = 60;
static int MAX_TABLE_NUMBER = BASE_TABLE_NUMBER + INTERFACES_TRACKED;
static const int PROTECT_MARK = 0x1;
static const char *EXEMPT_PRIO = "99";
static const char *RULE_PRIO = "100";

class SecondaryTableController {

public:
    SecondaryTableController(UidMarkMap *map);
    virtual ~SecondaryTableController();

    int addRoute(SocketClient *cli, char *iface, char *dest, int prefixLen, char *gateway);
    int removeRoute(SocketClient *cli, char *iface, char *dest, int prefixLen, char *gateway);
    int findTableNumber(const char *iface);
    int modifyFromRule(int tableIndex, const char *action, const char *addr);
    int modifyLocalRoute(int tableIndex, const char *action, const char *iface, const char *addr);
    int addUidRule(const char *iface, int uid_start, int uid_end);
    int removeUidRule(const char *iface, int uid_start, int uid_end);
    int addFwmarkRule(const char *iface);
    int removeFwmarkRule(const char *iface);
    int addFwmarkRoute(const char* iface, const char *dest, int prefix);
    int removeFwmarkRoute(const char* iface, const char *dest, int prefix);
    int addHostExemption(const char *host);
    int removeHostExemption(const char *host);
    void getUidMark(SocketClient *cli, int uid);
    void getProtectMark(SocketClient *cli);

    int setupIptablesHooks();

    static const char* LOCAL_MANGLE_OUTPUT;
    static const char* LOCAL_MANGLE_POSTROUTING;
    static const char* LOCAL_MANGLE_EXEMPT;
    static const char* LOCAL_MANGLE_IFACE_FORMAT;
    static const char* LOCAL_NAT_POSTROUTING;
    static const char* LOCAL_FILTER_OUTPUT;


private:
    UidMarkMap *mUidMarkMap;

    int setUidRule(const char* iface, int uid_start, int uid_end, bool add);
    int setFwmarkRule(const char *iface, bool add);
    int setFwmarkRoute(const char* iface, const char *dest, int prefix, bool add);
    int setHostExemption(const char *host, bool add);
    int modifyRoute(SocketClient *cli, const char *action, char *iface, char *dest, int prefix,
            char *gateway, int tableIndex);

    char mInterfaceTable[INTERFACES_TRACKED][IFNAMSIZ + 1];
    int mInterfaceRuleCount[INTERFACES_TRACKED];
    void modifyRuleCount(int tableIndex, const char *action);
    int verifyTableIndex(int tableIndex);
    const char *getVersion(const char *addr);
    IptablesTarget getIptablesTarget(const char *addr);

    int runCmd(int argc, const char **argv);
};

#endif
