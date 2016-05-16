# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

common_SRC_FILES:= \
	src/AcousticModels.c \
	src/AcousticModelsImpl.c \

common_C_INCLUDES := \
	$(ASR_ROOT_DIR)/portable/include \
	$(ASR_ROOT_DIR)/shared/include \
	$(ASR_ROOT_DIR)/srec/include \
	$(ASR_ROOT_DIR)/srec/AcousticModels/include \
	$(ASR_ROOT_DIR)/srec/AcousticState/include \
	$(ASR_ROOT_DIR)/srec/EventLog/include \
	$(ASR_ROOT_DIR)/srec/Grammar/include \
	$(ASR_ROOT_DIR)/srec/Nametag/include \
	$(ASR_ROOT_DIR)/srec/Recognizer/include \
	$(ASR_ROOT_DIR)/srec/Semproc/include \
	$(ASR_ROOT_DIR)/srec/Session/include \
	$(ASR_ROOT_DIR)/srec/Vocabulary/include \

common_CFLAGS := \
	-DSREC_ACOUSTICMODELS_EXPORTS \

common_CFLAGS += \
	$(ASR_GLOBAL_DEFINES) \
	$(ASR_GLOBAL_CPPFLAGS) \

common_SHARED_LIBRARIES := \
	libESR_Shared \
	libESR_Portable \
	libSR_EventLog \
	libSR_Core \

common_TARGET:= libSR_AcousticModels


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
