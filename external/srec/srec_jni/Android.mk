# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

LOCAL_SRC_FILES := \
  android_speech_srec_MicrophoneInputStream.cpp \
  android_speech_srec_Recognizer.cpp \


LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/src \
  $(LOCAL_PATH)/include \
  $(LOCAL_PATH)/../include \
  $(ASR_ROOT_DIR)/shared/include \
  $(ASR_ROOT_DIR)/portable/include \
  $(ASR_ROOT_DIR)/srec/include \
  $(ASR_ROOT_DIR)/srec/cr \
  $(ASR_ROOT_DIR)/srec/EventLog/include \
  $(ASR_ROOT_DIR)/srec/Session/include \
  $(ASR_ROOT_DIR)/srec/Semproc/include \
  $(ASR_ROOT_DIR)/srec/Recognizer/include \
  $(ASR_ROOT_DIR)/srec/Grammar/include \
  $(ASR_ROOT_DIR)/srec/Nametag/include \
  $(ASR_ROOT_DIR)/srec/Vocabulary/include \
  $(ASR_ROOT_DIR)/srec/AcousticModels/include \
  $(ASR_ROOT_DIR)/srec/AcousticState/include \
  $(ASR_ROOT_DIR)/audio/AudioIn/UNIX/include \

LOCAL_C_INCLUDES += \
  $(JNI_H_INCLUDE) \

#  include/javavm  \

LOCAL_CFLAGS += \
  $(ASR_GLOBAL_DEFINES) \
  $(ASR_GLOBAL_CPPFLAGS) \

LOCAL_SHARED_LIBRARIES := \
  libutils \
  libhardware_legacy \
  libcutils \
  liblog \
  libmedia

LOCAL_STATIC_LIBRARIES := \
  libzipfile \
  libunz \

LOCAL_WHOLE_STATIC_LIBRARIES := \
  libESR_Shared \
  libESR_Portable \
  libSR_AcousticModels \
  libSR_AcousticState \
  libSR_Core \
  libSR_EventLog \
  libSR_Grammar \
  libSR_G2P \
  libSR_Nametag \
  libSR_Recognizer \
  libSR_Semproc \
  libSR_Session \
  libSR_Vocabulary \


LOCAL_LDLIBS += -lpthread

LOCAL_MODULE := libsrec_jni

LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)
