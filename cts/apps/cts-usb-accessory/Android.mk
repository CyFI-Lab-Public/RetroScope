#
# Copyright (C) 2011 The Android Open Source Project
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
#

# Build for Linux (desktop) host
ifeq ($(HOST_OS),linux)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := cts-usb-accessory.c

LOCAL_MODULE := cts-usb-accessory

LOCAL_C_INCLUDES += bionic/libc/kernel/common
LOCAL_STATIC_LIBRARIES := libusbhost libcutils
LOCAL_LDLIBS += -lpthread
LOCAL_CFLAGS := -g -O0

include $(BUILD_HOST_EXECUTABLE)

endif
