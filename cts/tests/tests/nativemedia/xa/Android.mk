# Build the unit tests.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

cts_src_test_path := $(LOCAL_PATH)

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES:= \
    bionic \
    bionic/libstdc++/include \
    external/gtest/include \
    $(call include-path-for, wilhelm) \
    external/stlport/stlport \
    $(call include-path-for, wilhelm-ut)

LOCAL_SRC_FILES:= \
    src/XAObjectCreationTest.cpp

LOCAL_SHARED_LIBRARIES := \
  libutils \
  liblog \
  libOpenMAXAL \
  libstlport

LOCAL_STATIC_LIBRARIES := \
    libgtest

LOCAL_MODULE:= NativeMediaTest_XA

LOCAL_MODULE_PATH := $(TARGET_OUT_DATA)/nativetest

LOCAL_CTS_TEST_PACKAGE := android.nativemedia.xa
include $(BUILD_CTS_EXECUTABLE)
