LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := zxing-core-1.7:core.jar
include $(BUILD_MULTI_PREBUILT)
