LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_RESOURCE_DIR := \
    external/actionbarsherlock/library/res \
    $(LOCAL_PATH)/res

LOCAL_STATIC_JAVA_LIBRARIES := \
    ActionBarSherlock \
    libguava13

LOCAL_AAPT_FLAGS += --auto-add-overlay

LOCAL_PACKAGE_NAME := droiddriver.samples.testapp
LOCAL_MODULE_TAGS := tests optional
LOCAL_SDK_VERSION := 16

include $(BUILD_PACKAGE)

include $(call all-makefiles-under,$(LOCAL_PATH))
