# Copyright 2005 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	yaffs2/utils/mkyaffs2image.c \
	yaffs2/yaffs_packedtags2.c \
	yaffs2/yaffs_ecc.c \
	yaffs2/yaffs_tagsvalidity.c

LOCAL_CFLAGS =   -O2 -Wall -DCONFIG_YAFFS_UTIL -DCONFIG_YAFFS_DOES_ECC
LOCAL_CFLAGS+=   -Wshadow -Wpointer-arith -Wwrite-strings -Wstrict-prototypes -Wmissing-declarations
LOCAL_CFLAGS+=   -Wmissing-prototypes -Wredundant-decls -Wnested-externs -Winline

LOCAL_C_INCLUDES += $(LOCAL_PATH)/yaffs2

LOCAL_MODULE := mkyaffs2image

LOCAL_STATIC_LIBRARIES := libselinux

include $(BUILD_HOST_EXECUTABLE)

$(call dist-for-goals, dist_files, $(LOCAL_BUILT_MODULE))
