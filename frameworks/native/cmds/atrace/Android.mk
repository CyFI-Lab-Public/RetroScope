# Copyright 2012 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= atrace.cpp

LOCAL_C_INCLUDES += external/zlib

LOCAL_MODULE:= atrace

LOCAL_MODULE_TAGS:= optional

LOCAL_SHARED_LIBRARIES := \
    libbinder \
    libcutils \
    libutils \
    libz \

include $(BUILD_EXECUTABLE)
