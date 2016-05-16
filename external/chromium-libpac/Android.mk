LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CPP_EXTENSION := .cc

# Set up the target identity
LOCAL_MODULE := libpac
LOCAL_MODULE_CLASS := SHARED_LIBRARIES

LOCAL_SRC_FILES := \
  src/proxy_resolver_v8.cc \
  src/proxy_resolver_js_bindings.cc \
  src/net_util.cc

LOCAL_CFLAGS += \
  -Wno-endif-labels \
  -Wno-import \
  -Wno-format \

LOCAL_C_INCLUDES += $(LOCAL_PATH)/src external/v8

LOCAL_STATIC_LIBRARIES := libv8
LOCAL_SHARED_LIBRARIES := libutils libstlport liblog

include external/stlport/libstlport.mk

include $(BUILD_SHARED_LIBRARY)
