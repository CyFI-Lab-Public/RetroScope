LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SHARED_LIBRARIES := libstlport
include external/stlport/libstlport.mk

LOCAL_SRC_FILES := \
    src/d8.cc \
    src/d8-posix.cc

LOCAL_MODULE := d8

LOCAL_CPP_EXTENSION := .cc

LOCAL_STATIC_LIBRARIES := libv8
LOCAL_SHARED_LIBRARIES += liblog

LOCAL_MODULE_TAGS := optional

LOCAL_C_INCLUDES += $(LOCAL_PATH)/include

include $(BUILD_EXECUTABLE)
