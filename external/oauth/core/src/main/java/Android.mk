LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# This will build the client static library for the Google Services Framework
LOCAL_MODULE := oauth

# Includes all the java files, and explicitly declares any aidl files
LOCAL_SRC_FILES := \
   $(call all-java-files-under, net)

LOCAL_SDK_VERSION := 8

# Build the actual static library
include $(BUILD_STATIC_JAVA_LIBRARY)
