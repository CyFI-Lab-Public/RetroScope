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
# libvss
#
include $(CLEAR_VARS)

LOCAL_MODULE:= libvideoeditor_core

LOCAL_SRC_FILES:=          \
      M4PTO3GPP_API.c \
      M4PTO3GPP_VideoPreProcessing.c \
      M4VIFI_xVSS_RGB565toYUV420.c \
      M4xVSS_API.c \
      M4xVSS_internal.c \
      M4VSS3GPP_AudioMixing.c \
      M4VSS3GPP_Clip.c \
      M4VSS3GPP_ClipAnalysis.c \
      M4VSS3GPP_Codecs.c \
      M4VSS3GPP_Edit.c \
      M4VSS3GPP_EditAudio.c \
      M4VSS3GPP_EditVideo.c \
      M4VSS3GPP_MediaAndCodecSubscription.c \
      M4ChannelConverter.c \
      M4VD_EXTERNAL_BitstreamParser.c \
      M4AIR_API.c \
      M4READER_Pcm.c \
      M4PCMR_CoreReader.c \
      M4AD_Null.c \
      M4AMRR_CoreReader.c \
      M4READER_Amr.c \
      M4VD_Tools.c \
      VideoEditorResampler.cpp \
      M4DECODER_Null.c


LOCAL_MODULE_TAGS := optional

LOCAL_SHARED_LIBRARIES :=       \
    libaudioflinger             \
    libaudioutils               \
    libbinder                   \
    libcutils                   \
    liblog                      \
    libmedia                    \
    libstagefright              \
    libstagefright_foundation   \
    libstagefright_omx          \
    libutils                    \
    libvideoeditor_osal         \
    libvideoeditor_videofilters \
    libvideoeditorplayer        \

LOCAL_STATIC_LIBRARIES := \
    libstagefright_color_conversion \
    libvideoeditor_mcs \
    libvideoeditor_stagefrightshells \
    libvideoeditor_3gpwriter \

LOCAL_C_INCLUDES += \
    $(TOP)/frameworks/av/libvideoeditor/osal/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/mcs/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/common/inc \
    $(TOP)/frameworks/av/libvideoeditor/vss/stagefrightshells/inc \
    $(TOP)/frameworks/av/services/audioflinger \
    $(TOP)/frameworks/native/include/media/openmax \
    $(TOP)/frameworks/native/services/audioflinger \
    $(TOP)/system/media/audio_effects/include \
    $(TOP)/system/media/audio_utils/include


LOCAL_SHARED_LIBRARIES += libdl

# All of the shared libraries we link against.
LOCAL_LDLIBS := \
    -lpthread -ldl

LOCAL_CFLAGS += -Wno-multichar \
    -DM4xVSS_RESERVED_MOOV_DISK_SPACEno \
    -DDECODE_GIF_ON_SAVING

include $(BUILD_SHARED_LIBRARY)
