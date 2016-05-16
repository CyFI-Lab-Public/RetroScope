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

LOCAL_PATH:= $(call my-dir)

#
# lib3gpwriter
#

include $(CLEAR_VARS)

LOCAL_MODULE:= libvideoeditor_3gpwriter

LOCAL_SRC_FILES:=          \
      M4MP4W_Interface.c \
      M4MP4W_Utils.c \
      M4MP4W_Writer.c

LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := \
    libcutils             \
    libutils              \
    libvideoeditor_osal   \

LOCAL_C_INCLUDES += \
    $(TOP)/frameworks/av/libvideoeditor/osal/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/3gpwriter/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/common/inc

LOCAL_SHARED_LIBRARIES += libdl

# All of the shared libraries we link against.
LOCAL_LDLIBS := \
    -lpthread -ldl

LOCAL_CFLAGS += -Wno-multichar \
    -DDUPLICATE_STTS_IN_LAST_AU

include $(BUILD_STATIC_LIBRARY)

