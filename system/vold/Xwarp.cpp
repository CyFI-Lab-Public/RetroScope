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

#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>

#include <sys/types.h>
#include <sys/stat.h>

#define LOG_TAG "Vold"

#include <cutils/log.h>

#include "Xwarp.h"
const char *Xwarp::XWARP_BACKINGFILE = "/mnt/secure/asec/xwarp.img";
const char *Xwarp::XWARP_CFG = "/sys/fs/yaffs/mtd3/xwarp-backing-store";
const char *Xwarp::XWARP_READY = "/sys/fs/yaffs/mtd3/xwarp-ready";
const char *Xwarp::XWARP_MIRROR_STATUS = "/sys/fs/yaffs/mtd3/xwarp-mirror";

int Xwarp::enable() {
    return doEnableDisable(true);
}

int Xwarp::disable() {
    return doEnableDisable(false);
}

int Xwarp::status(bool *ready, unsigned *mirrorPos, unsigned *maxSize) {
    FILE *fp;

    *ready = false;
    *mirrorPos = 0;
    *maxSize = 0;
    if (!(fp = fopen(XWARP_READY, "r"))) {
        return -1;
    }

    fscanf(fp, "%d", (int *) ready);
    fclose(fp);

    if (!(fp = fopen(XWARP_MIRROR_STATUS, "r"))) {
        return -1;
    }

    fscanf(fp, "%u %u", mirrorPos, maxSize);
    fclose(fp);
    return 0;
}

int Xwarp::doEnableDisable(bool enable) {
    const char *tmp;
    int fd = open(XWARP_CFG, O_WRONLY);

    if (fd < 0) 
        return -1;

    tmp = (enable ? XWARP_BACKINGFILE : "");

    if (write(fd, tmp, strlen(tmp)+1) < 0) {
        SLOGE("Failed to write xwarp cfg (%s)", strerror(errno));
        close(fd);
        return -1;
    }

    close(fd);
    return 0;
}
