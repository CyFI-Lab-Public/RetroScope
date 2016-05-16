##
## Copyright (C) 2010 The Android Open Source Project
##
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##      http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##

BASE_PATH := $(call my-dir)
LOCAL_PATH:= $(call my-dir)

#############################################################
#   build the harfbuzz library
#

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES:= \
        contrib/harfbuzz-freetype.c \
        contrib/harfbuzz-unicode-icu.c \
        contrib/harfbuzz-unicode.c \
        src/harfbuzz-buffer.c \
        src/harfbuzz-stream.c \
        src/harfbuzz-dump.c \
        src/harfbuzz-gdef.c \
        src/harfbuzz-gpos.c \
        src/harfbuzz-gsub.c \
        src/harfbuzz-impl.c \
        src/harfbuzz-open.c \
        src/harfbuzz-shaper.cpp \
        src/harfbuzz-tibetan.c \
        src/harfbuzz-khmer.c \
        src/harfbuzz-indic.cpp \
        src/harfbuzz-hebrew.c \
        src/harfbuzz-arabic.c \
        src/harfbuzz-hangul.c \
        src/harfbuzz-myanmar.c \
        src/harfbuzz-thai.c \
        src/harfbuzz-greek.c \
        src/harfbuzz-debug.c

LOCAL_SHARED_LIBRARIES := \
        libcutils \
        libft2 \
        libicuuc \
        libicui18n \
        libpng \
        libutils \
        liblog \
        libz

LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/src \
        $(LOCAL_PATH)/src/contrib \
        external/icu4c/common \
        external/freetype/include

ifeq ($(NO_FALLBACK_FONT),true)
        LOCAL_CFLAGS += -DNO_FALLBACK_FONT
endif

LOCAL_LDLIBS += -lpthread

LOCAL_MODULE:= libharfbuzz



include $(BUILD_SHARED_LIBRARY)
