/*
 * Copyright (C) 2010 The Android Open Source Project
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

#ifndef _DNSPROXYLISTENER_H__
#define _DNSPROXYLISTENER_H__

#include <sysutils/FrameworkListener.h>

#include "NetdCommand.h"
#include "UidMarkMap.h"

class DnsProxyListener : public FrameworkListener {
public:
    DnsProxyListener(UidMarkMap *map);
    virtual ~DnsProxyListener() {}

private:
    UidMarkMap *mUidMarkMap;
    class GetAddrInfoCmd : public NetdCommand {
    public:
        GetAddrInfoCmd(UidMarkMap *uidMarkMap);
        virtual ~GetAddrInfoCmd() {}
        int runCommand(SocketClient *c, int argc, char** argv);
    private:
        UidMarkMap *mUidMarkMap;
    };

    class GetAddrInfoHandler {
    public:
        // Note: All of host, service, and hints may be NULL
        GetAddrInfoHandler(SocketClient *c,
                           char* host,
                           char* service,
                           struct addrinfo* hints,
                           char* iface,
                           pid_t pid,
                           uid_t uid,
                           int mark);
        ~GetAddrInfoHandler();

        static void* threadStart(void* handler);
        void start();

    private:
        void run();
        SocketClient* mClient;  // ref counted
        char* mHost;    // owned
        char* mService; // owned
        struct addrinfo* mHints;  // owned
        char* mIface; // owned
        pid_t mPid;
        uid_t mUid;
        int mMark;
    };

    /* ------ gethostbyname ------*/
    class GetHostByNameCmd : public NetdCommand {
    public:
        GetHostByNameCmd(UidMarkMap *uidMarkMap);
        virtual ~GetHostByNameCmd() {}
        int runCommand(SocketClient *c, int argc, char** argv);
    private:
        UidMarkMap *mUidMarkMap;
    };

    class GetHostByNameHandler {
    public:
        GetHostByNameHandler(SocketClient *c,
                            pid_t pid,
                            uid_t uid,
                            char *iface,
                            char *name,
                            int af,
                            int mark);
        ~GetHostByNameHandler();
        static void* threadStart(void* handler);
        void start();
    private:
        void run();
        SocketClient* mClient; //ref counted
        pid_t mPid;
        uid_t mUid;
        char* mIface; // owned
        char* mName; // owned
        int mAf;
        int mMark;
    };

    /* ------ gethostbyaddr ------*/
    class GetHostByAddrCmd : public NetdCommand {
    public:
        GetHostByAddrCmd(UidMarkMap *uidMarkMap);
        virtual ~GetHostByAddrCmd() {}
        int runCommand(SocketClient *c, int argc, char** argv);
    private:
        UidMarkMap *mUidMarkMap;
    };

    class GetHostByAddrHandler {
    public:
        GetHostByAddrHandler(SocketClient *c,
                            void* address,
                            int   addressLen,
                            int   addressFamily,
                            char* iface,
                            pid_t pid,
                            uid_t uid,
                            int mark);
        ~GetHostByAddrHandler();

        static void* threadStart(void* handler);
        void start();

    private:
        void run();
        SocketClient* mClient;  // ref counted
        void* mAddress;    // address to lookup; owned
        int   mAddressLen; // length of address to look up
        int   mAddressFamily;  // address family
        char* mIface; // owned
        pid_t mPid;
        uid_t mUid;
        int   mMark;
    };
};

#endif
