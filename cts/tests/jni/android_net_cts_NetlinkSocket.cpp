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

#include <jni.h>
#include <stdio.h>
#include <cutils/log.h>
#include <asm/types.h>
#include <sys/socket.h>
#include <linux/netlink.h>
#include <errno.h>
#include <string.h>
#include "JNIHelp.h"

#include "android_net_cts_NetlinkSocket.h"

static void android_net_cts_NetlinkSocket_create(JNIEnv* env, jclass,
    jobject fileDescriptor)
{
    int sock = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if (sock == -1) {
        ALOGE("Can't create socket %s", strerror(errno));
        jclass SocketException = env->FindClass("java/net/SocketException");
        env->ThrowNew(SocketException, "Can't create socket");
        return;
    }
    jniSetFileDescriptorOfFD(env, fileDescriptor, sock);
}

static int android_net_cts_NetlinkSocket_sendmsg(JNIEnv *e, jclass,
    jobject fileDescriptor, jint pid, jbyteArray packet)
{
    void *bytes = (void *)e->GetByteArrayElements(packet, NULL);
    uint32_t length = (uint32_t)e->GetArrayLength(packet);
    struct sockaddr_nl snl;
    struct iovec iov = {bytes, length};
    struct msghdr msg = {&snl, sizeof(snl), &iov, 1, NULL, 0, 0};

    memset(&snl, 0, sizeof(snl));
    snl.nl_family = AF_NETLINK;
    snl.nl_pid = pid;

    int sock = jniGetFDFromFileDescriptor(e, fileDescriptor);
    int retval = sendmsg(sock, &msg, 0);
    e->ReleaseByteArrayElements(packet, (jbyte*)bytes, 0);
    return retval;
}


static JNINativeMethod gMethods[] = {
    {  "sendmsg", "(Ljava/io/FileDescriptor;I[B)I", (void *) android_net_cts_NetlinkSocket_sendmsg },
    {  "create_native", "(Ljava/io/FileDescriptor;)V", (void *) android_net_cts_NetlinkSocket_create },
};

int register_android_net_cts_NetlinkSocket(JNIEnv* env)
{
    jclass clazz = env->FindClass("android/net/cts/NetlinkSocket");

    return env->RegisterNatives(clazz, gMethods,
            sizeof(gMethods) / sizeof(JNINativeMethod));
}
