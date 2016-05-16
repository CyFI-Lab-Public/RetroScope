LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Include all the java files.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_SDK_VERSION := 8

# The name of the jar file to create.
LOCAL_MODULE := xmp_toolkit

# Build a static jar file.
include $(BUILD_STATIC_JAVA_LIBRARY)
