LOCAL_PATH:= $(call my-dir)

mcld_object_SRC_FILES := \
  ObjectBuilder.cpp \
  ObjectLinker.cpp \
  SectionMap.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_object_SRC_FILES)
LOCAL_MODULE:= libmcldObject

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_object_SRC_FILES)
LOCAL_MODULE:= libmcldObject

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
