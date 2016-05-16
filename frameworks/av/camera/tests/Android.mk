LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	main.cpp \
	ProCameraTests.cpp \

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libcutils \
	libstlport \
	libcamera_metadata \
	libcamera_client \
	libgui \
	libsync \
	libui \
	libdl \
	libbinder

LOCAL_STATIC_LIBRARIES := \
	libgtest

LOCAL_C_INCLUDES += \
	bionic \
	bionic/libstdc++/include \
	external/gtest/include \
	external/stlport/stlport \
	system/media/camera/include \
	frameworks/av/services/camera/libcameraservice \
	frameworks/av/include/camera \
	frameworks/native/include \

LOCAL_CFLAGS += -Wall -Wextra

LOCAL_MODULE:= camera_client_test
LOCAL_MODULE_TAGS := tests

include $(BUILD_NATIVE_TEST)
