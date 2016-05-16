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

#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <signal.h>
#include <poll.h>
#include <unistd.h>

#include "config.h"
#include "gcmalloc.h"
#include "schedule.h"
#include "plog.h"

#ifdef ANDROID_CHANGES

#include <openssl/engine.h>

#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <linux/if.h>
#include <linux/if_tun.h>

#include <android/log.h>
#include <cutils/sockets.h>
#include <private/android_filesystem_config.h>

static void notify_death()
{
    creat("/data/misc/vpn/abort", 0);
}

static int android_get_control_and_arguments(int *argc, char ***argv)
{
    static char *args[32];
    int control;
    int i;

    atexit(notify_death);

    if ((i = android_get_control_socket("racoon")) == -1) {
        return -1;
    }
    do_plog(LLV_DEBUG, "Waiting for control socket");
    if (listen(i, 1) == -1 || (control = accept(i, NULL, 0)) == -1) {
        do_plog(LLV_ERROR, "Cannot get control socket");
        exit(1);
    }
    close(i);
    fcntl(control, F_SETFD, FD_CLOEXEC);

    args[0] = (*argv)[0];
    for (i = 1; i < 32; ++i) {
        unsigned char bytes[2];
        if (recv(control, &bytes[0], 1, 0) != 1 ||
                recv(control, &bytes[1], 1, 0) != 1) {
            do_plog(LLV_ERROR, "Cannot get argument length");
            exit(1);
        } else {
            int length = bytes[0] << 8 | bytes[1];
            int offset = 0;

            if (length == 0xFFFF) {
                break;
            }
            args[i] = malloc(length + 1);
            while (offset < length) {
                int n = recv(control, &args[i][offset], length - offset, 0);
                if (n > 0) {
                    offset += n;
                } else {
                    do_plog(LLV_ERROR, "Cannot get argument value");
                    exit(1);
                }
            }
            args[i][length] = 0;
        }
    }
    do_plog(LLV_DEBUG, "Received %d arguments", i - 1);

    *argc = i;
    *argv = args;
    return control;
}

const char *android_hook(char **envp)
{
    struct ifreq ifr = {.ifr_flags = IFF_TUN};
    int tun = open("/dev/tun", 0);

    /* Android does not support INTERNAL_WINS4_LIST, so we just use it. */
    while (*envp && strncmp(*envp, "INTERNAL_WINS4_LIST=", 20)) {
        ++envp;
    }
    if (!*envp) {
        do_plog(LLV_ERROR, "Cannot find environment variable\n");
        exit(1);
    }
    if (ioctl(tun, TUNSETIFF, &ifr)) {
        do_plog(LLV_ERROR, "Cannot allocate TUN: %s\n", strerror(errno));
        exit(1);
    }
    sprintf(*envp, "INTERFACE=%s", ifr.ifr_name);
    return "/etc/ppp/ip-up-vpn";
}

#endif

extern void setup(int argc, char **argv);

static int monitors;
static void (*callbacks[10])(int fd);
static struct pollfd pollfds[10];

char *pname;

static void terminate(int signal)
{
    exit(1);
}

static void terminated()
{
    do_plog(LLV_INFO, "Bye\n");
}

void monitor_fd(int fd, void (*callback)(int))
{
    if (fd < 0 || monitors == 10) {
        do_plog(LLV_ERROR, "Cannot monitor fd");
        exit(1);
    }
    callbacks[monitors] = callback;
    pollfds[monitors].fd = fd;
    pollfds[monitors].events = callback ? POLLIN : 0;
    ++monitors;
}

int main(int argc, char **argv)
{
#ifdef ANDROID_CHANGES
    int control = android_get_control_and_arguments(&argc, &argv);
    ENGINE *e;
    if (control != -1) {
        pname = "%p";
        monitor_fd(control, NULL);

        ENGINE_load_dynamic();
        e = ENGINE_by_id("keystore");
        if (!e || !ENGINE_init(e)) {
            do_plog(LLV_ERROR, "ipsec-tools: cannot load keystore engine");
            exit(1);
        }
    }
#endif

    do_plog(LLV_INFO, "ipsec-tools 0.7.3 (http://ipsec-tools.sf.net)\n");

    signal(SIGHUP, terminate);
    signal(SIGINT, terminate);
    signal(SIGTERM, terminate);
    signal(SIGPIPE, SIG_IGN);
    atexit(terminated);

    setup(argc, argv);

#ifdef ANDROID_CHANGES
    shutdown(control, SHUT_WR);
    setuid(AID_VPN);
#endif

    while (1) {
        struct timeval *tv = schedular();
        int timeout = tv->tv_sec * 1000 + tv->tv_usec / 1000 + 1;

        if (poll(pollfds, monitors, timeout) > 0) {
            int i;
            for (i = 0; i < monitors; ++i) {
                if (pollfds[i].revents & POLLHUP) {
                    do_plog(LLV_INFO, "Connection is closed\n", pollfds[i].fd);
                    /* Wait for few seconds to consume late messages. */
                    sleep(5);
                    exit(1);
                }
                if (pollfds[i].revents & POLLIN) {
                    callbacks[i](pollfds[i].fd);
                }
            }
        }
    }
#ifdef ANDROID_CHANGES
    if (e) {
        ENGINE_finish(e);
        ENGINE_free(e);
    }
#endif
    return 0;
}

/* plog.h */

void do_plog(int level, char *format, ...)
{
    if (level >= 0 && level <= 5) {
#ifdef ANDROID_CHANGES
        static int levels[6] = {
            ANDROID_LOG_ERROR, ANDROID_LOG_WARN, ANDROID_LOG_INFO,
            ANDROID_LOG_INFO, ANDROID_LOG_DEBUG, ANDROID_LOG_VERBOSE
        };
        va_list ap;
        va_start(ap, format);
        __android_log_vprint(levels[level], "racoon", format, ap);
        va_end(ap);
#else
        static char *levels = "EWNIDV";
        fprintf(stderr, "%c: ", levels[level]);
        va_list ap;
        va_start(ap, format);
        vfprintf(stderr, format, ap);
        va_end(ap);
#endif
    }
}

char *binsanitize(char *data, size_t length)
{
    char *output = racoon_malloc(length + 1);
    if (output) {
        size_t i;
        for (i = 0; i < length; ++i) {
            output[i] = (data[i] < ' ' || data[i] > '~') ? '?' : data[i];
        }
        output[length] = '\0';
    }
    return output;
}
