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
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# TODO: Remove the .cc extension, just .cpp.
# TODO: Change module name to use underscores not hyphens.
# TODO: Work out if the log and android libs are needed.

# Add in extra warnings.
LOCAL_CFLAGS += -Wall
LOCAL_CPPFLAGS += -Wall

LOCAL_CPP_EXTENSION := .cc
LOCAL_MODULE := libvariablespeed
LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
    variablespeed.cc \
    ring_buffer.cc \
    sola_time_scaler.cc \
    jni_entry.cc \
    decode_buffer.cc \

LOCAL_C_INCLUDES := \
    $(call include-path-for, wilhelm) \
    external/stlport/stlport \
    bionic \

LOCAL_SHARED_LIBRARIES := \
    libOpenSLES \
    libstlport \
    libutils \
    libcutils \
    liblog \

LOCAL_LDLIBS := \
    -lOpenSLES \
    -llog \
    -landroid \

include $(BUILD_SHARED_LIBRARY)
