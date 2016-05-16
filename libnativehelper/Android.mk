# Copyright (C) 2009 The Android Open Source Project
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

local_src_files := \
    JNIHelp.cpp \
    JniConstants.cpp \
    toStringArray.cpp


#
# Build for the target (device).
#

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
    $(local_src_files) \
    JniInvocation.cpp
LOCAL_SHARED_LIBRARIES := liblog
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libnativehelper
LOCAL_CFLAGS := -Werror
LOCAL_C_INCLUDES := external/stlport/stlport bionic/ bionic/libstdc++/include libcore/include
LOCAL_SHARED_LIBRARIES += libcutils libstlport libdl
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk
include $(BUILD_SHARED_LIBRARY)

#
# NDK-only build for the target (device).
# - Relies only on NDK exposed functionality.
# - This doesn't include JniInvocation.
#

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libnativehelper_compat
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/include/nativehelper
LOCAL_CFLAGS := -Werror
LOCAL_SRC_FILES := $(local_src_files)
LOCAL_LDFLAGS := -llog
LOCAL_SDK_VERSION := 17
LOCAL_NDK_STL_VARIANT := stlport_static
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk
include $(BUILD_SHARED_LIBRARY)



#
# Build for the host.
#

include $(CLEAR_VARS)
LOCAL_MODULE := libnativehelper
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := \
    $(local_src_files) \
    JniInvocation.cpp
LOCAL_CFLAGS := -Werror
LOCAL_C_INCLUDES := libcore/include
LOCAL_SHARED_LIBRARIES := liblog
LOCAL_LDFLAGS := -ldl
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk
include $(BUILD_HOST_SHARED_LIBRARY)
