LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	BatteryProperties.cpp \
	IBatteryPropertiesListener.cpp \
	IBatteryPropertiesRegistrar.cpp

LOCAL_STATIC_LIBRARIES := \
	libutils \
	libbinder

LOCAL_MODULE:= libbatteryservice

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_LIBRARY)
