# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

common_SRC_FILES:= \
	../setiUtils/src/platform_utils.c \
	src/linklist_impl.c \
	src/run_seq_lts.c \
	src/SWIslts.c \

common_C_INCLUDES := \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/../setiUtils/include \
	$(ASR_ROOT_DIR)/shared/include \
	$(ASR_ROOT_DIR)/portable/include \

common_CFLAGS += \
	$(ASR_GLOBAL_DEFINES) \
	$(ASR_GLOBAL_CPPFLAGS) \

common_SHARED_LIBRARIES := \
	libESR_Shared \
	libESR_Portable \

common_TARGET:= libSR_G2P


# For the host
# =====================================================

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_C_INCLUDES := $(common_C_INCLUDES)
LOCAL_CFLAGS += $(common_CFLAGS)

LOCAL_SHARED_LIBRARIES := $(common_SHARED_LIBRARIES)

LOCAL_MODULE := $(common_TARGET)

include $(BUILD_HOST_SHARED_LIBRARY)


# For the device
# =====================================================

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_C_INCLUDES := $(common_C_INCLUDES)
LOCAL_CFLAGS += $(common_CFLAGS)

LOCAL_MODULE := $(common_TARGET)

include $(BUILD_STATIC_LIBRARY)
