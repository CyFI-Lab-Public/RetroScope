LOCAL_PATH:= $(call my-dir)

# Multichannel downmix effect library
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	EffectDownmix.c

LOCAL_SHARED_LIBRARIES := \
	libcutils liblog

LOCAL_MODULE:= libdownmix

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/soundfx

ifeq ($(TARGET_OS)-$(TARGET_SIMULATOR),linux-true)
LOCAL_LDLIBS += -ldl
endif

LOCAL_C_INCLUDES := \
	$(call include-path-for, audio-effects) \
	$(call include-path-for, audio-utils)

LOCAL_PRELINK_MODULE := false

LOCAL_CFLAGS += -fvisibility=hidden

include $(BUILD_SHARED_LIBRARY)
