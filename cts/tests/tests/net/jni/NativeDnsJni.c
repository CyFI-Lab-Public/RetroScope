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

#include <arpa/inet.h>
#include <jni.h>
#include <netdb.h>
#include <stdio.h>
#include <utils/Log.h>

const char *GoogleDNSIpV4Address="8.8.8.8";
const char *GoogleDNSIpV4Address2="8.8.4.4";
const char *GoogleDNSIpV6Address="2001:4860:4860::8888";
const char *GoogleDNSIpV6Address2="2001:4860:4860::8844";

JNIEXPORT jboolean Java_android_net_cts_DnsTest_testNativeDns(JNIEnv* env, jclass class)
{
    const char *node = "www.google.com";
    char *service = NULL;
    struct addrinfo *answer;

    int res = getaddrinfo(node, service, NULL, &answer);
    ALOGD("getaddrinfo(www.google.com) gave res=%d (%s)", res, gai_strerror(res));
    if (res != 0) return JNI_FALSE;

    // check for v4 & v6
    {
        int foundv4 = 0;
        int foundv6 = 0;
        struct addrinfo *current = answer;
        while (current != NULL) {
            char buf[256];
            if (current->ai_addr->sa_family == AF_INET) {
                inet_ntop(current->ai_family, &((struct sockaddr_in *)current->ai_addr)->sin_addr,
                        buf, sizeof(buf));
                foundv4 = 1;
                ALOGD("  %s", buf);
            } else if (current->ai_addr->sa_family == AF_INET6) {
                inet_ntop(current->ai_family, &((struct sockaddr_in6 *)current->ai_addr)->sin6_addr,
                        buf, sizeof(buf));
                foundv6 = 1;
                ALOGD("  %s", buf);
            }
            current = current->ai_next;
        }

        freeaddrinfo(answer);
        answer = NULL;
        if (foundv4 != 1 && foundv6 != 1) {
            ALOGD("getaddrinfo(www.google.com) didn't find either v4 or v6 address");
            return JNI_FALSE;
        }
    }

    node = "ipv6.google.com";
    res = getaddrinfo(node, service, NULL, &answer);
    ALOGD("getaddrinfo(ipv6.google.com) gave res=%d", res);
    if (res != 0) return JNI_FALSE;

    {
        int foundv4 = 0;
        int foundv6 = 0;
        struct addrinfo *current = answer;
        while (current != NULL) {
            char buf[256];
            if (current->ai_addr->sa_family == AF_INET) {
                inet_ntop(current->ai_family, &((struct sockaddr_in *)current->ai_addr)->sin_addr,
                        buf, sizeof(buf));
                ALOGD("  %s", buf);
                foundv4 = 1;
            } else if (current->ai_addr->sa_family == AF_INET6) {
                inet_ntop(current->ai_family, &((struct sockaddr_in6 *)current->ai_addr)->sin6_addr,
                        buf, sizeof(buf));
                ALOGD("  %s", buf);
                foundv6 = 1;
            }
            current = current->ai_next;
        }

        freeaddrinfo(answer);
        answer = NULL;
        if (foundv4 == 1 || foundv6 != 1) {
            ALOGD("getaddrinfo(ipv6.google.com) didn't find only v6");
            return JNI_FALSE;
        }
    }

    // getnameinfo
    struct sockaddr_in sa4;
    sa4.sin_family = AF_INET;
    sa4.sin_port = 0;
    inet_pton(AF_INET, GoogleDNSIpV4Address, &(sa4.sin_addr));

    struct sockaddr_in6 sa6;
    sa6.sin6_family = AF_INET6;
    sa6.sin6_port = 0;
    sa6.sin6_flowinfo = 0;
    sa6.sin6_scope_id = 0;
    inet_pton(AF_INET6, GoogleDNSIpV6Address2, &(sa6.sin6_addr));

    char buf[NI_MAXHOST];
    int flags = NI_NAMEREQD;

    res = getnameinfo((const struct sockaddr*)&sa4, sizeof(sa4), buf, sizeof(buf), NULL, 0, flags);
    if (res != 0) {
        ALOGD("getnameinfo(%s (GoogleDNS) ) gave error %d (%s)", GoogleDNSIpV4Address, res,
            gai_strerror(res));
        return JNI_FALSE;
    }
    if (strstr(buf, "google.com") == NULL) {
        ALOGD("getnameinfo(%s (GoogleDNS) ) didn't return google.com: %s",
            GoogleDNSIpV4Address, buf);
        return JNI_FALSE;
    }

    memset(buf, sizeof(buf), 0);
    res = getnameinfo((const struct sockaddr*)&sa6, sizeof(sa6), buf, sizeof(buf), NULL, 0, flags);
    if (res != 0) {
        ALOGD("getnameinfo(%s (GoogleDNS) ) gave error %d (%s)", GoogleDNSIpV6Address2,
            res, gai_strerror(res));
        return JNI_FALSE;
    }
    if (strstr(buf, "google.com") == NULL) {
        ALOGD("getnameinfo(%s) didn't return google.com: %s", GoogleDNSIpV6Address2, buf);
        return JNI_FALSE;
    }

    // gethostbyname
    struct hostent *my_hostent = gethostbyname("www.youtube.com");
    if (my_hostent == NULL) {
        ALOGD("gethostbyname(www.youtube.com) gave null response");
        return JNI_FALSE;
    }
    if ((my_hostent->h_addr_list == NULL) || (*my_hostent->h_addr_list == NULL)) {
        ALOGD("gethostbyname(www.youtube.com) gave 0 addresses");
        return JNI_FALSE;
    }
    {
        char **current = my_hostent->h_addr_list;
        while (*current != NULL) {
            char buf[256];
            inet_ntop(my_hostent->h_addrtype, *current, buf, sizeof(buf));
            ALOGD("gethostbyname(www.youtube.com) gave %s", buf);
            current++;
        }
    }

    // gethostbyaddr
    char addr6[16];
    inet_pton(AF_INET6, GoogleDNSIpV6Address, addr6);
    my_hostent = gethostbyaddr(addr6, sizeof(addr6), AF_INET6);
    if (my_hostent == NULL) {
        ALOGD("gethostbyaddr(%s (GoogleDNS) ) gave null response", GoogleDNSIpV6Address);
        return JNI_FALSE;
    }

    ALOGD("gethostbyaddr(%s (GoogleDNS) ) gave %s for name", GoogleDNSIpV6Address,
        my_hostent->h_name ? my_hostent->h_name : "null");

    if (my_hostent->h_name == NULL) return JNI_FALSE;
    return JNI_TRUE;
}
