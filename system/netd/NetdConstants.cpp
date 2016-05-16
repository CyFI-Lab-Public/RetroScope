/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include <fcntl.h>
#include <string.h>
#include <sys/wait.h>

#define LOG_TAG "Netd"

#include <cutils/log.h>
#include <logwrap/logwrap.h>

#include "NetdConstants.h"

const char * const OEM_SCRIPT_PATH = "/system/bin/oem-iptables-init.sh";
const char * const IPTABLES_PATH = "/system/bin/iptables";
const char * const IP6TABLES_PATH = "/system/bin/ip6tables";
const char * const TC_PATH = "/system/bin/tc";
const char * const IP_PATH = "/system/bin/ip";
const char * const ADD = "add";
const char * const DEL = "del";

static void logExecError(const char* argv[], int res, int status) {
    const char** argp = argv;
    std::string args = "";
    while (*argp) {
        args += *argp;
        args += ' ';
        argp++;
    }
    ALOGE("exec() res=%d, status=%d for %s", res, status, args.c_str());
}

static int execIptablesCommand(int argc, const char *argv[], bool silent) {
    int res;
    int status;

    res = android_fork_execvp(argc, (char **)argv, &status, false,
        !silent);
    if (res || !WIFEXITED(status) || WEXITSTATUS(status)) {
        if (!silent) {
            logExecError(argv, res, status);
        }
        if (res)
            return res;
        if (!WIFEXITED(status))
            return ECHILD;
    }
    return WEXITSTATUS(status);
}

static int execIptables(IptablesTarget target, bool silent, va_list args) {
    /* Read arguments from incoming va_list; we expect the list to be NULL terminated. */
    std::list<const char*> argsList;
    argsList.push_back(NULL);
    const char* arg;
    do {
        arg = va_arg(args, const char *);
        argsList.push_back(arg);
    } while (arg);

    int i = 0;
    const char* argv[argsList.size()];
    std::list<const char*>::iterator it;
    for (it = argsList.begin(); it != argsList.end(); it++, i++) {
        argv[i] = *it;
    }

    int res = 0;
    if (target == V4 || target == V4V6) {
        argv[0] = IPTABLES_PATH;
        res |= execIptablesCommand(argsList.size(), argv, silent);
    }
    if (target == V6 || target == V4V6) {
        argv[0] = IP6TABLES_PATH;
        res |= execIptablesCommand(argsList.size(), argv, silent);
    }
    return res;
}

int execIptables(IptablesTarget target, ...) {
    va_list args;
    va_start(args, target);
    int res = execIptables(target, false, args);
    va_end(args);
    return res;
}

int execIptablesSilently(IptablesTarget target, ...) {
    va_list args;
    va_start(args, target);
    int res = execIptables(target, true, args);
    va_end(args);
    return res;
}

int writeFile(const char *path, const char *value, int size) {
    int fd = open(path, O_WRONLY);
    if (fd < 0) {
        ALOGE("Failed to open %s: %s", path, strerror(errno));
        return -1;
    }

    if (write(fd, value, size) != size) {
        ALOGE("Failed to write %s: %s", path, strerror(errno));
        close(fd);
        return -1;
    }
    close(fd);
    return 0;
}

int readFile(const char *path, char *buf, int *sizep)
{
    int fd = open(path, O_RDONLY);
    int size;

    if (fd < 0) {
        ALOGE("Failed to open %s: %s", path, strerror(errno));
        return -1;
    }

    size = read(fd, buf, *sizep);
    if (size < 0) {
        ALOGE("Failed to write %s: %s", path, strerror(errno));
        close(fd);
        return -1;
    }
    *sizep = size;
    close(fd);
    return 0;
}
