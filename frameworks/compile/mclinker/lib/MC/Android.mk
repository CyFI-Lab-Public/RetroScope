LOCAL_PATH:= $(call my-dir)

mcld_mc_SRC_FILES := \
  Attribute.cpp  \
  AttributeSet.cpp  \
  CommandAction.cpp  \
  ContextFactory.cpp  \
  FileAction.cpp  \
  InputAction.cpp  \
  InputBuilder.cpp  \
  InputFactory.cpp  \
  MCLDDirectory.cpp \
  MCLDInput.cpp \
  SearchDirs.cpp  \
  SymbolCategory.cpp  \
  ZOption.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_mc_SRC_FILES)
LOCAL_MODULE:= libmcldMC

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_mc_SRC_FILES)
LOCAL_MODULE:= libmcldMC

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
