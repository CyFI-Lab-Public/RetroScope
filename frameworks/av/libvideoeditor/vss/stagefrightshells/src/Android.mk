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
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
    MediaBufferPuller.cpp \
    VideoEditorVideoDecoder.cpp \
    VideoEditorAudioDecoder.cpp \
    VideoEditorMp3Reader.cpp \
    VideoEditor3gpReader.cpp \
    VideoEditorUtils.cpp \
    VideoEditorBuffer.c \
    VideoEditorVideoEncoder.cpp \
    VideoEditorAudioEncoder.cpp

LOCAL_C_INCLUDES += \
    $(TOP)/frameworks/av/media/libmediaplayerservice \
    $(TOP)/frameworks/av/media/libstagefright \
    $(TOP)/frameworks/av/media/libstagefright/include \
    $(TOP)/frameworks/av/media/libstagefright/rtsp \
    $(call include-path-for, corecg graphics) \
    $(TOP)/frameworks/av/libvideoeditor/lvpp \
    $(TOP)/frameworks/av/libvideoeditor/osal/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/common/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/mcs/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/stagefrightshells/inc \
    $(TOP)/frameworks/native/include/media/editor \
    $(TOP)/frameworks/native/include/media/openmax

LOCAL_SHARED_LIBRARIES :=     \
    libcutils                 \
    libutils                  \
    libmedia                  \
    libbinder                 \
    libstagefright            \
    libstagefright_foundation \
    libstagefright_omx        \
    libgui                    \
    libvideoeditor_osal       \
    libvideoeditorplayer      \

LOCAL_CFLAGS += \

LOCAL_STATIC_LIBRARIES := \
    libstagefright_color_conversion


LOCAL_MODULE:= libvideoeditor_stagefrightshells

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)
