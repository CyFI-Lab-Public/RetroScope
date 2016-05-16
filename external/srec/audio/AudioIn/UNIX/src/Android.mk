# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

LOCAL_SRC_FILES:= \
	audioin.c \
	audioinwrapper.cpp \
	filter.c \

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../../../../portable/include \

LOCAL_CFLAGS := \

LOCAL_CFLAGS += \
	$(ASR_GLOBAL_DEFINES) \
	$(ASR_GLOBAL_CPPFLAGS) \

LOCAL_STATIC_LIBRARIES := \
        libESR_Portable

LOCAL_SHARED_LIBRARIES := \
        liblog

LOCAL_MODULE:= libSR_AudioIn

include $(BUILD_SHARED_LIBRARY)
