# Copyright 2013 The Android Open Source Project
LOCAL_PATH:= $(call my-dir)

src_files = \
	main.cpp \

include $(CLEAR_VARS)

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_SRC_FILES:= $(src_files)

LOCAL_MODULE := libc_test
LOCAL_MODULE_TAGS := debug

ifeq ($(TARGET_ARCH),arm)
LOCAL_ASFLAGS := -mthumb
endif # arm

include $(BUILD_EXECUTABLE)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(src_files)

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := debug
LOCAL_MODULE := libc_test_static
LOCAL_STATIC_LIBRARIES := libc libm
LOCAL_FORCE_STATIC_EXECUTABLE := true

ifeq ($(TARGET_ARCH),arm)
LOCAL_ASFLAGS := -mthumb
endif # arm

include $(BUILD_EXECUTABLE)
