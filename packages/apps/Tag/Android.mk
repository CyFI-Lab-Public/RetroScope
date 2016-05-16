LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := guava com.android.vcard

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := Tag
LOCAL_PRIVILEGED_MODULE := true

#LOCAL_SDK_VERSION := current

include $(BUILD_PACKAGE)
