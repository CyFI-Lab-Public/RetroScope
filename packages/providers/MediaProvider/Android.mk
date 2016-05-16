LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-subdir-java-files) \
	src/com/android/providers/media/IMtpService.aidl

LOCAL_JAVA_LIBRARIES := 

LOCAL_PACKAGE_NAME := MediaProvider
LOCAL_CERTIFICATE := media
LOCAL_PRIVILEGED_MODULE := true

include $(BUILD_PACKAGE)
