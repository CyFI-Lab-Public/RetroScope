LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PREBUILT_JAVA_LIBRARIES := \
	ninepatch-prebuilt$(COMMON_JAVA_PACKAGE_SUFFIX)

LOCAL_MODULE_TAGS := optional

include $(BUILD_HOST_PREBUILT)
