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
include $(CLEAR_VARS)

# measurements show that the ARM version of ZLib is about x1.17 faster
# than the thumb one...
LOCAL_ARM_MODE := arm

bzlib_files := \
	blocksort.c \
	huffman.c \
	crctable.c \
	randtable.c \
	compress.c \
	decompress.c \
	bzlib.c

LOCAL_SRC_FILES := $(bzlib_files)
LOCAL_MODULE := libbz
LOCAL_CFLAGS += -O3 -DUSE_MMAP
ifeq ($(TARGET_ARCH),arm)
  LOCAL_SDK_VERSION := 9
endif
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(bzlib_files)
LOCAL_MODULE := libbz
LOCAL_CFLAGS += -O3 -DUSE_MMAP
include $(BUILD_HOST_STATIC_LIBRARY)
