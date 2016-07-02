LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := pmd
LOCAL_SRC_FILES := pmd.c
LOCAL_CPPFLAGS := -O3 -Wall -fPIE
LOCAL_LDLIBS := -O3 -llog -fPIE -pie

include $(BUILD_EXECUTABLE)
