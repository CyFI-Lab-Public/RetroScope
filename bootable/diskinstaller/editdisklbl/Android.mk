LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

ifeq ($(TARGET_ARCH),x86)

LOCAL_SRC_FILES := \
	editdisklbl.c

LOCAL_CFLAGS := -O2 -g -W -Wall -Werror

LOCAL_MODULE := editdisklbl
LOCAL_STATIC_LIBRARIES := libdiskconfig_host libcutils liblog

include $(BUILD_HOST_EXECUTABLE)

endif   # TARGET_ARCH == x86
