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
#include <unistd.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/wait.h>

#define LOG_TAG "ClatdController"
#include <cutils/log.h>

#include "ClatdController.h"

ClatdController::ClatdController() {
    mClatdPid = 0;
}

ClatdController::~ClatdController() {
}

int ClatdController::startClatd(char *interface) {
    pid_t pid;

    if(mClatdPid != 0) {
        ALOGE("clatd already running");
        errno = EBUSY;
        return -1;
    }

    ALOGD("starting clatd");

    if ((pid = fork()) < 0) {
        ALOGE("fork failed (%s)", strerror(errno));
        return -1;
    }

    if (!pid) {
        char **args = (char **)malloc(sizeof(char *) * 4);
        args[0] = (char *)"/system/bin/clatd";
        args[1] = (char *)"-i";
        args[2] = interface;
        args[3] = NULL;

        if (execv(args[0], args)) {
            ALOGE("execv failed (%s)", strerror(errno));
        }
        ALOGE("Should never get here!");
        free(args);
        _exit(0);
    } else {
        mClatdPid = pid;
        ALOGD("clatd started");
    }

    return 0;
}

int ClatdController::stopClatd() {
    if (mClatdPid == 0) {
        ALOGE("clatd already stopped");
        return -1;
    }

    ALOGD("Stopping clatd");

    kill(mClatdPid, SIGTERM);
    waitpid(mClatdPid, NULL, 0);
    mClatdPid = 0;

    ALOGD("clatd stopped");

    return 0;
}

bool ClatdController::isClatdStarted() {
    pid_t waitpid_status;
    if(mClatdPid == 0) {
        return false;
    }
    waitpid_status = waitpid(mClatdPid, NULL, WNOHANG);
    if(waitpid_status != 0) {
        mClatdPid = 0; // child exited, don't call waitpid on it again
    }
    return waitpid_status == 0; // 0 while child is running
}
