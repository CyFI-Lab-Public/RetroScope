/*
* Copyright (C) 2012 Invensense, Inc.
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

#ifndef ANDROID_SENSOR_BASE_H
#define ANDROID_SENSOR_BASE_H

#include <stdint.h>
#include <errno.h>
#include <sys/cdefs.h>
#include <sys/types.h>

#if defined ANDROID_JELLYBEAN
//build for Jellybean
#define LOGV_IF ALOGV_IF
#define LOGE_IF ALOGE_IF
#define LOGI_IF ALOGI_IF
#define LOGI    ALOGI
#define LOGE    ALOGE
#define LOGV    ALOGV
#define LOGW    ALOGW
#else
//build for ICS or earlier version
#endif

/* Log enablers, each of these independent */

#define PROCESS_VERBOSE (0) /* process log messages */
#define EXTRA_VERBOSE   (0) /* verbose log messages */
#define SYSFS_VERBOSE   (0) /* log sysfs interactions as cat/echo for repro
                               purpose on a shell */
#define FUNC_ENTRY      (0) /* log entry in all one-time functions */

/* Note that enabling this logs may affect performance */
#define HANDLER_ENTRY   (0) /* log entry in all handler functions */
#define ENG_VERBOSE     (0) /* log some a lot more info about the internals */
#define INPUT_DATA      (0) /* log the data input from the events */
#define HANDLER_DATA    (0) /* log the data fetched from the handlers */

#define FUNC_LOG \
            LOGV("%s", __PRETTY_FUNCTION__)
#define VFUNC_LOG \
            LOGV_IF(FUNC_ENTRY, "Entering function '%s'", __PRETTY_FUNCTION__)
#define VHANDLER_LOG \
            LOGV_IF(HANDLER_ENTRY, "Entering handler '%s'", __PRETTY_FUNCTION__)
#define CALL_MEMBER_FN(pobject, ptrToMember) ((pobject)->*(ptrToMember))

#define MAX_SYSFS_NAME_LEN  (100)
#define IIO_BUFFER_LENGTH   (480)

/*****************************************************************************/

struct sensors_event_t;

class SensorBase {
protected:
    const char *dev_name;
    const char *data_name;
    char input_name[PATH_MAX];
    int dev_fd;
    int data_fd;

    int openInput(const char* inputName);
    static int64_t getTimestamp();
    static int64_t timevalToNano(timeval const& t) {
        return t.tv_sec * 1000000000LL + t.tv_usec * 1000;
    }

    int open_device();
    int close_device();

public:
    SensorBase(const char* dev_name, const char* data_name);

    virtual ~SensorBase();

    virtual int readEvents(sensors_event_t* data, int count) = 0;
    int readSample(long *data, int64_t *timestamp);
    int readRawSample(float *data, int64_t *timestamp);
    virtual bool hasPendingEvents() const;
    virtual int getFd() const;
    virtual int setDelay(int32_t handle, int64_t ns);
    virtual int enable(int32_t handle, int enabled);
    virtual int query(int what, int* value);
    virtual int batch(int handle, int flags, int64_t period_ns, int64_t timeout);
};

/*****************************************************************************/

#endif  // ANDROID_SENSOR_BASE_H
