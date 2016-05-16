LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= ConnectivityManager.cpp

LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	libbinder

LOCAL_MODULE:= libconnectivitymanager

include $(BUILD_SHARED_LIBRARY)
