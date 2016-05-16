# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

L_DEFS := -DHAVE_CONFIG_H -UAF_INET6
L_CFLAGS := $(L_DEFS)
L_USE_CPU_SOURCE := netcpu_none.c

L_COMMON_SRC := hist.h netlib.c netsh.c nettest_bsd.c nettest_dlpi.c \
  nettest_unix.c nettest_xti.c nettest_sctp.c nettest_sdp.c

netperf_SOURCES := netperf.c $(L_COMMON_SRC) $(L_USE_CPU_SOURCE)
netserver_SOURCES := netserver.c $(L_COMMON_SRC) $(L_USE_CPU_SOURCE)

include $(CLEAR_VARS)
LOCAL_MODULE := netperf
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := eng
LOCAL_CFLAGS := $(L_CFLAGS)
LOCAL_SRC_FILES := $(netperf_SOURCES)
include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)
LOCAL_MODULE := netserver
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := eng
LOCAL_CFLAGS := $(L_CFLAGS)
LOCAL_SRC_FILES := $(netserver_SOURCES)
include $(BUILD_EXECUTABLE)

