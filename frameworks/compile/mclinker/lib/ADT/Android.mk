LOCAL_PATH:= $(call my-dir)

mcld_adt_SRC_FILES := \
  StringEntry.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_adt_SRC_FILES)
LOCAL_MODULE:= libmcldADT

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_adt_SRC_FILES)
LOCAL_MODULE:= libmcldADT

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
