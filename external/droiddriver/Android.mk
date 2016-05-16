LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_LIBRARIES := \
    libguava13

LOCAL_MODULE := droiddriver
LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

include $(BUILD_STATIC_JAVA_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
    libguava13:libs/guava-13.0.jar

include $(BUILD_MULTI_PREBUILT)

include $(call all-makefiles-under,$(LOCAL_PATH))
include $(call all-makefiles-under,$(LOCAL_PATH)/samples)
