# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

common_SRC_FILES:= \
	src/ArrayList.c \
	src/ArrayListImpl.c \
	src/ESR_ReturnCode.c \
	src/LCHAR.c  \
	src/pcputimer.c \
	src/pcrc.c \
	src/pendian.c \
	src/PFileWrap.c \
	src/$(ASR_TARGET_OS)/PFileWrap$(ASR_TARGET_OS)_OS_Specific.c \
	src/phashtable.c \
	src/pLastError.c \
	src/plog.c \
	src/pmalloc.c \
	src/pmemory.c \
	src/pmemory_ext.c \
	src/PStackSize.c \
	src/ptimestamp.c \
	src/ptypes.c \
#	src/ptimer.c \

common_C_INCLUDES := \
	$(ASR_ROOT_DIR)/portable/include \
	$(ASR_ROOT_DIR)/shared/include \

common_CFLAGS := \
	-DPORTABLE_EXPORTS \

common_CFLAGS += \
	$(ASR_GLOBAL_DEFINES) \
	$(ASR_GLOBAL_CPPFLAGS) \

common_SHARED_LIBRARIES :=

common_TARGET:= libESR_Portable


# For the host
# =====================================================

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_C_INCLUDES := $(common_C_INCLUDES)
LOCAL_CFLAGS += $(common_CFLAGS)

#LOCAL_SHARED_LIBRARIES := $(common_SHARED_LIBRARIES)

LOCAL_MODULE := $(common_TARGET)

include $(BUILD_HOST_SHARED_LIBRARY)


# For the device
# =====================================================

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_C_INCLUDES := $(common_C_INCLUDES)
LOCAL_CFLAGS += $(common_CFLAGS)

LOCAL_SHARED_LIBRARIES := libcutils

LOCAL_MODULE := $(common_TARGET)

include $(BUILD_STATIC_LIBRARY)
