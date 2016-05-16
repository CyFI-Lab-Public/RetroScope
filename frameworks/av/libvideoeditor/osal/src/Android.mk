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
# libvideoeditor_osal
#

include $(CLEAR_VARS)

LOCAL_MODULE:= libvideoeditor_osal

LOCAL_SRC_FILES:=          \
    M4OSA_CharStar.c \
    M4OSA_Clock.c \
    M4OSA_FileCommon.c \
    M4OSA_FileReader.c \
    M4OSA_FileWriter.c \
    M4OSA_Mutex.c \
    M4OSA_Random.c \
    M4OSA_Semaphore.c \
    M4OSA_Thread.c \
    M4PSW_DebugTrace.c \
    M4PSW_MemoryInterface.c \
    M4PSW_Trace.c \
    LVOSA_FileReader_optim.c

LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES := libcutils libutils liblog

LOCAL_C_INCLUDES += \
    $(TOP)/frameworks/av/libvideoeditor/osal/inc \

LOCAL_SHARED_LIBRARIES += libdl

# All of the shared libraries we link against.
LOCAL_LDLIBS := \
    -lpthread -ldl

LOCAL_CFLAGS += -Wno-multichar \
    -D__ANDROID__ \
    -DM4OSA_FILE_BLOCK_WITH_SEMAPHORE \
    -DUSE_STAGEFRIGHT_CODECS \
    -DUSE_STAGEFRIGHT_AUDIODEC \
    -DUSE_STAGEFRIGHT_VIDEODEC \
    -DUSE_STAGEFRIGHT_AUDIOENC \
    -DUSE_STAGEFRIGHT_VIDEOENC \
    -DUSE_STAGEFRIGHT_READERS \
    -DUSE_STAGEFRIGHT_3GPP_READER

include $(BUILD_SHARED_LIBRARY)
