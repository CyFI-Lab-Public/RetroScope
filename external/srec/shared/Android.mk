# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

common_SRC_FILES:= \
	src/CircularBuffer.c \
	src/CommandLine.c \
	src/ESR_Locale.c \
	src/ESR_Session.c \
	src/HashMap.c \
	src/HashMapImpl.c \
	src/Int8ArrayListImpl.c \
	src/IntArrayList.c \
	src/IntArrayListImpl.c \
	src/lstring.c \
	src/LStringImpl.c \
	src/SessionTypeImpl.c \

common_C_INCLUDES := \
	$(ASR_ROOT_DIR)/portable/include \
	$(ASR_ROOT_DIR)/shared/include \

common_CFLAGS := \
	-DESR_SHARED_EXPORTS \

common_CFLAGS += \
	$(ASR_GLOBAL_DEFINES) \
	$(ASR_GLOBAL_CPPFLAGS) \

common_SHARED_LIBRARIES := \
	libESR_Portable \

common_TARGET:= libESR_Shared


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
