#
# Copyright (C) 2010 The Android Open Source Project
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

LOCAL_SRC_FILES := \
	src/add.c \
	src/code.c \
	src/decode.c \
	src/gsm_create.c \
	src/gsm_decode.c \
	src/gsm_destroy.c \
	src/gsm_encode.c \
	src/gsm_option.c \
	src/long_term.c \
	src/lpc.c \
	src/preprocess.c \
	src/rpe.c \
	src/short_term.c \
	src/table.c

LOCAL_CFLAGS := -DSASR -DWAV49

LOCAL_C_INCLUDES += $(LOCAL_PATH)/inc

LOCAL_MODULE := libgsm

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)
