LOCAL_PATH:= $(call my-dir)

# LoudnessEnhancer library
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	EffectLoudnessEnhancer.cpp \
	dsp/core/dynamic_range_compression.cpp

LOCAL_CFLAGS+= -O2 -fvisibility=hidden

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	liblog \
	libstlport

LOCAL_MODULE_PATH := $(TARGET_OUT_SHARED_LIBRARIES)/soundfx
LOCAL_MODULE:= libldnhncr

LOCAL_C_INCLUDES := \
	$(call include-path-for, audio-effects) \
	bionic \
	bionic/libstdc++/include \
	external/stlport/stlport


include $(BUILD_SHARED_LIBRARY)
