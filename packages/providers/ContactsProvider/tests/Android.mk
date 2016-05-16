LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# We only want this apk build for tests.
LOCAL_MODULE_TAGS := tests

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := ContactsProviderTests

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_INSTRUMENTATION_FOR := ContactsProvider
LOCAL_CERTIFICATE := shared

include $(BUILD_PACKAGE)
