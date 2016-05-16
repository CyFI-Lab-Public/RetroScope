LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := CalendarProviderTests

LOCAL_JAVA_LIBRARIES := ext android.test.runner

LOCAL_INSTRUMENTATION_FOR := CalendarProvider

include $(BUILD_PACKAGE)
