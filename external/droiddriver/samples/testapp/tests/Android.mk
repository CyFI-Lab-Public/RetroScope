LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests optional

LOCAL_STATIC_JAVA_LIBRARIES += droiddriver

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := droiddriver.samples.testapp.tests

LOCAL_INSTRUMENTATION_FOR := droiddriver.samples.testapp

LOCAL_SDK_VERSION := 16

include $(BUILD_PACKAGE)
