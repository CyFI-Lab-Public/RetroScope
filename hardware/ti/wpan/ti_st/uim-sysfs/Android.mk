ifeq ($(TARGET_ARCH),arm)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

#
# UIM Application
#

LOCAL_C_INCLUDES:= uim.h

LOCAL_SRC_FILES:= \
	uim.c
LOCAL_CFLAGS:= -g -c -W -Wall -O2 -D_POSIX_SOURCE
LOCAL_SHARED_LIBRARIES:= libnetutils libcutils liblog
LOCAL_MODULE:=uim-sysfs
LOCAL_MODULE_TAGS:= eng
include $(BUILD_EXECUTABLE)

endif
