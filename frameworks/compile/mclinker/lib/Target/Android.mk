LOCAL_PATH:= $(call my-dir)

mcld_target_SRC_FILES := \
  ELFDynamic.cpp  \
  ELFEmulation.cpp  \
  ELFMCLinker.cpp  \
  GNUInfo.cpp \
  GNULDBackend.cpp  \
  GOT.cpp \
  OutputRelocSection.cpp  \
  PLT.cpp \
  Target.cpp  \
  TargetLDBackend.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_target_SRC_FILES)
LOCAL_MODULE:= libmcldTarget

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_target_SRC_FILES)
LOCAL_MODULE:= libmcldTarget

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
