#
#
# Copyright 2001-2011 Texas Instruments, Inc. - http://www.ti.com/
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

ALSA_PATH := external/alsa-lib/

include $(CLEAR_VARS)


LOCAL_C_INCLUDES :=\
	$(JNI_H_INCLUDE) \
        external/tinyalsa/include/tinyalsa


LOCAL_CFLAGS:= -g -c -W -Wall -O2 -D_POSIX_SOURCE

LOCAL_SRC_FILES +=	JFmRxNative.cpp \
			JFmTxNative.cpp

LOCAL_SHARED_LIBRARIES := \
	libnativehelper \
	libcutils \
	libutils \
	liblog


LOCAL_STATIC_LIBRARIES :=

LOCAL_PRELINK_MODULE := false
LOCAL_MODULE := libfmradio
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

