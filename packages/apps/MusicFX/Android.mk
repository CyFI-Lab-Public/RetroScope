LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := framework

LOCAL_PACKAGE_NAME := MusicFX

LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)
