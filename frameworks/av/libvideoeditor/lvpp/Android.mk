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
# libvideoeditorplayer
#

include $(CLEAR_VARS)

LOCAL_MODULE:= libvideoeditorplayer

LOCAL_SRC_FILES:=          \
    VideoEditorTools.cpp \
    VideoEditorPlayer.cpp \
    PreviewPlayer.cpp \
    VideoEditorAudioPlayer.cpp \
    VideoEditorPreviewController.cpp \
    VideoEditorSRC.cpp \
    DummyAudioSource.cpp \
    DummyVideoSource.cpp \
    VideoEditorBGAudioProcessing.cpp \
    PreviewRenderer.cpp \
    I420ColorConverter.cpp \
    NativeWindowRenderer.cpp

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_LIBRARIES := \
    libstagefright_color_conversion



LOCAL_SHARED_LIBRARIES :=     \
    libaudioflinger           \
    libaudioutils             \
    libbinder                 \
    libcutils                 \
    liblog                    \
    libEGL                    \
    libGLESv2                 \
    libgui                    \
    libmedia                  \
    libdrmframework           \
    libstagefright            \
    libstagefright_foundation \
    libstagefright_omx        \
    libsync                   \
    libui                     \
    libutils                  \
    libvideoeditor_osal       \


LOCAL_C_INCLUDES += \
    $(TOP)/system/media/audio_utils/include \
    $(TOP)/frameworks/av/media/libmediaplayerservice \
    $(TOP)/frameworks/av/media/libstagefright \
    $(TOP)/frameworks/av/media/libstagefright/include \
    $(TOP)/frameworks/av/media/libstagefright/rtsp \
    $(call include-path-for, corecg graphics) \
    $(TOP)/frameworks/av/libvideoeditor/osal/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/common/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/mcs/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/stagefrightshells/inc \
    $(TOP)/frameworks/av/libvideoeditor/lvpp \
    $(TOP)/frameworks/av/services/audioflinger \
    $(TOP)/frameworks/native/include/media/editor \
    $(TOP)/frameworks/native/include/media/openmax \
    $(TOP)/frameworks/native/services/audioflinger


LOCAL_SHARED_LIBRARIES += libdl

# All of the shared libraries we link against.
LOCAL_LDLIBS := \
    -lpthread -ldl

LOCAL_CFLAGS += -Wno-multichar \
     -DM4_ENABLE_RENDERINGMODE \
    -DUSE_STAGEFRIGHT_CODECS \
    -DUSE_STAGEFRIGHT_AUDIODEC \
    -DUSE_STAGEFRIGHT_VIDEODEC \
    -DUSE_STAGEFRIGHT_AUDIOENC \
    -DUSE_STAGEFRIGHT_VIDEOENC \
    -DUSE_STAGEFRIGHT_READERS \
    -DUSE_STAGEFRIGHT_3GPP_READER

include $(BUILD_SHARED_LIBRARY)

#include $(call all-makefiles-under,$(LOCAL_PATH))
