# Build the unit tests.
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_C_INCLUDES:= \
    bionic \
    bionic/libstdc++/include \
    external/gtest/include \
    $(call include-path-for, wilhelm) \
    external/stlport/stlport \
    $(call include-path-for, wilhelm-ut)

LOCAL_SRC_FILES:= \
    BufferQueue_test.cpp

LOCAL_SHARED_LIBRARIES := \
	libutils \
	libOpenSLES \
    libstlport

LOCAL_STATIC_LIBRARIES := \
    libOpenSLESUT \
    libgtest

ifeq ($(TARGET_OS),linux)
	LOCAL_CFLAGS += -DXP_UNIX
	#LOCAL_SHARED_LIBRARIES += librt
endif

LOCAL_MODULE:= BufferQueue_test

LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/nativetest

include $(BUILD_EXECUTABLE)

# Build the manual test programs.
include $(call all-subdir-makefiles)
