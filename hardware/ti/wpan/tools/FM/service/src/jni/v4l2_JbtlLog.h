/*
 *
 * Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _V4L2_JBTL_LOG__H
#define _V4L2_JBTL_LOG__H

#include "utils/Log.h"


/*
 * Log a verbose log mesage.
 */
#define V4L2_JBTL_LOGV(...) ALOGV( __VA_ARGS__)
#define V4L2_JBTL_LOGV_IF(cond, ...) ALOGV_IF(cond, __VA_ARGS__)

/*
 * Log a debug log mesage.
 */
#define V4L2_JBTL_LOGD(...) ALOGD( __VA_ARGS__)
#define V4L2_JBTL_LOGD_IF(cond, ...) ALOGD_IF(cond, __VA_ARGS__)

/*
 * Log a informational log mesage.
 */
#define V4L2_JBTL_LOGI(...) ALOGI( __VA_ARGS__)
#define V4L2_JBTL_LOGI_IF(cond, ...) ALOGI_IF(cond, __VA_ARGS__)

/*
 * Log a warning log mesage.
 */
#define V4L2_JBTL_LOGW(...) ALOGW( __VA_ARGS__)
#define V4L2_JBTL_LOGW_IF(cond, ...) ALOGW_IF(cond, __VA_ARGS__)

/*
 * Log a error log mesage.
 */
#define V4L2_JBTL_LOGE(...) ALOGE( __VA_ARGS__)
#define V4L2_JBTL_LOGE_IF(cond, ...) ALOGE_IF(cond, __VA_ARGS__)


/*
 * Log a fatal log message.
 */
#define V4L2_JBTL_LOG_FATAL(...) ALOG_FATAL(__VA_ARGS__)
#define V4L2_JBTL_LOG_FATAL_IF(cond, ...) ALOG_FATAL_IF(cond, __VA_ARGS__)


/*
 * Assertion
 */
#define V4L2_JBTL_LOG_ASSERT(cond, ...) ALOG_ASSERT(cond, __VA_ARGS__)


#endif // _V4L2_JBTL_LOG__H

