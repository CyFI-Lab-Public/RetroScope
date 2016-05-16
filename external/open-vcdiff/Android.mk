# Copyright (C) 2013 The Android Open Source Project
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
include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc
LOCAL_MODULE := openvcdiff_static

LOCAL_SRC_FILES := \
    src/addrcache.cc \
    src/blockhash.cc \
    src/codetable.cc \
    src/encodetable.cc \
    src/decodetable.cc \
    src/headerparser.cc \
    src/instruction_map.cc \
    src/logging.cc \
    src/varint_bigendian.cc \
    src/vcdecoder.cc \
    src/vcdiffengine.cc \
    src/vcencoder.cc \
    src/zlib/adler32.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/src/

LOCAL_SDK_VERSION := 14
LOCAL_NDK_STL_VARIANT := stlport_static

include $(BUILD_STATIC_LIBRARY)
