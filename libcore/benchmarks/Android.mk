LOCAL_PATH:= $(call my-dir)
##################################################
include $(CLEAR_VARS)

# Only compile source java files in this apk.
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_MODULE := benchmarks

LOCAL_STATIC_JAVA_LIBRARIES := \
	caliper-prebuilt \
	core-tests

LOCAL_JAVA_LIBRARIES := \
	bouncycastle \
	conscrypt \
	core

LOCAL_MODULE_TAGS := tests

LOCAL_MODULE_PATH := $(PRODUCT_OUT)/data/caliperperf

include $(BUILD_JAVA_LIBRARY)

##################################################
# Prebuilt Java libraries
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
	caliper-prebuilt:libs/caliper.jar

include $(BUILD_MULTI_PREBUILT)
