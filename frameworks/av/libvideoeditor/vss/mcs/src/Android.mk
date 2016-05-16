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
# libvideoeditor_mcs
#

include $(CLEAR_VARS)

LOCAL_MODULE:= libvideoeditor_mcs

LOCAL_SRC_FILES:=          \
      M4MCS_API.c \
      M4MCS_AudioEffects.c \
      M4MCS_Codecs.c \
      M4MCS_MediaAndCodecSubscription.c \
      M4MCS_VideoPreProcessing.c

LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := \
    libcutils             \
    libutils              \
    libvideoeditor_osal   \

LOCAL_C_INCLUDES += \
    $(TOP)/frameworks/av/libvideoeditor/osal/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/mcs/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/common/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/stagefrightshells/inc \
    $(TOP)/frameworks/native/include/media/openmax

LOCAL_SHARED_LIBRARIES += libdl

# All of the shared libraries we link against.
LOCAL_LDLIBS := \
    -lpthread -ldl

LOCAL_CFLAGS += -Wno-multichar \
    -DM4MCS_WITH_FAST_OPEN

include $(BUILD_STATIC_LIBRARY)

