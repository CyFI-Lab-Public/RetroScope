LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# don't include this package in any target
LOCAL_MODULE_TAGS := optional
# and when built explicitly put it in the data partition
LOCAL_MODULE_PATH := $(TARGET_OUT_DATA_APPS)

LOCAL_JAVA_LIBRARIES := android.test.runner

# Include all test java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SignatureTestTests

LOCAL_INSTRUMENTATION_FOR := SignatureTest

LOCAL_PROGUARD_FLAGS := -ignorewarnings

LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)
