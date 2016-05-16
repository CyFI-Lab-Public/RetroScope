LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files) \
    ../tests/src/com/android/apps/tag/MockNdefMessages.java \

LOCAL_STATIC_JAVA_LIBRARIES := guava

LOCAL_PACKAGE_NAME := TagCanon

LOCAL_MODULE_TAGS := tests

include $(BUILD_PACKAGE)
