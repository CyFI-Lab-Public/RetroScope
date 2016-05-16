# Copyright 2006 The Android Open Source Project

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# common settings for all ASR builds, exports some variables for sub-makes
include $(ASR_MAKE_DIR)/Makefile.defs

LOCAL_SRC_FILES:= \
	src/SRecTest.c \
	src/srec_test_config.c \

LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/src \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/../include \
	$(ASR_ROOT_DIR)/shared/include \
	$(ASR_ROOT_DIR)/portable/include \
	$(ASR_ROOT_DIR)/srec/include \
	$(ASR_ROOT_DIR)/srec/cr \
	$(ASR_ROOT_DIR)/srec/EventLog/include \
	$(ASR_ROOT_DIR)/srec/Session/include \
	$(ASR_ROOT_DIR)/srec/Semproc/include \
	$(ASR_ROOT_DIR)/srec/Recognizer/include \
	$(ASR_ROOT_DIR)/srec/Grammar/include \
	$(ASR_ROOT_DIR)/srec/Nametag/include \
	$(ASR_ROOT_DIR)/srec/Vocabulary/include \
	$(ASR_ROOT_DIR)/srec/AcousticModels/include \
	$(ASR_ROOT_DIR)/srec/AcousticState/include \

LOCAL_CFLAGS += \
	$(ASR_GLOBAL_DEFINES) \
	$(ASR_GLOBAL_CPPFLAGS) \

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libsrec_jni \
	
LOCAL_MODULE:= SRecTest

LOCAL_MODULE_TAGS := optional

include $(BUILD_EXECUTABLE)
