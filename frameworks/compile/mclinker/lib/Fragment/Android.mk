LOCAL_PATH:= $(call my-dir)

mcld_fragment_SRC_FILES := \
  AlignFragment.cpp \
  FGNode.cpp \
  FillFragment.cpp \
  Fragment.cpp \
  FragmentLinker.cpp \
  FragmentRef.cpp \
  NullFragment.cpp \
  RegionFragment.cpp \
  Relocation.cpp \
  Stub.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_fragment_SRC_FILES)
LOCAL_MODULE:= libmcldFragment

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_fragment_SRC_FILES)
LOCAL_MODULE:= libmcldFragment

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
