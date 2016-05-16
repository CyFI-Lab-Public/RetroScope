# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	fst.cpp \
	properties.cpp \
	symbol-table.cpp \
	compat.cpp \

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/../.. \

LOCAL_CFLAGS += \
	-DFST_DL \

LOCAL_SHARED_LIBRARIES := \

LOCAL_LDLIBS += -lpthread

LOCAL_MODULE := libfst

include $(BUILD_HOST_SHARED_LIBRARY)
