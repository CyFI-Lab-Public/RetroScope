LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
    ../src/com/android/providers/downloads/OpenHelper.java \
    ../src/com/android/providers/downloads/Constants.java \
    ../src/com/android/providers/downloads/DownloadDrmHelper.java

LOCAL_PACKAGE_NAME := DownloadProviderUi
LOCAL_CERTIFICATE := media

include $(BUILD_PACKAGE)
