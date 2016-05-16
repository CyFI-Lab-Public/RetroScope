# Copyright 2012 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= corrupt_gdt_free_blocks.c
LOCAL_MODULE:= corrupt_gdt_free_blocks
LOCAL_MODULE_TAGS := debug
LOCAL_C_INCLUDES += system/extras/ext4_utils

include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)

LOCAL_SRC_FILES:= set_ext4_err_bit.c
LOCAL_MODULE:= set_ext4_err_bit
LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)


include $(CLEAR_VARS)

LOCAL_SRC_FILES:= rand_emmc_perf.c
LOCAL_MODULE:= rand_emmc_perf 
LOCAL_MODULE_TAGS := optional
LOCAL_FORCE_STATIC_EXECUTABLE := true
LOCAL_STATIC_LIBRARIES := libm libc

include $(BUILD_EXECUTABLE)

