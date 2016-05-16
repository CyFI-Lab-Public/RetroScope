LOCAL_PATH:= $(call my-dir)

mcld_core_SRC_FILES := \
  AttributeOption.cpp \
  BitcodeOption.cpp \
  Environment.cpp \
  GeneralOptions.cpp \
  IRBuilder.cpp \
  InputTree.cpp \
  LinkerConfig.cpp  \
  LinkerScript.cpp  \
  Linker.cpp \
  Module.cpp \
  TargetOptions.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_core_SRC_FILES)
LOCAL_MODULE:= libmcldCore

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_core_SRC_FILES)
LOCAL_MODULE:= libmcldCore

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
