LOCAL_PATH:= $(call my-dir)

mcld_support_SRC_FILES := \
  CommandLine.cpp \
  DefSymParser.cpp \
  Directory.cpp \
  FileHandle.cpp  \
  FileSystem.cpp  \
  HandleToArea.cpp  \
  LEB128.cpp  \
  MemoryArea.cpp  \
  MemoryAreaFactory.cpp \
  MemoryRegion.cpp  \
  MsgHandling.cpp \
  Path.cpp  \
  RealPath.cpp  \
  RegionFactory.cpp \
  Space.cpp \
  SystemUtils.cpp \
  TargetRegistry.cpp  \
  ToolOutputFile.cpp  \
  raw_mem_ostream.cpp \
  raw_ostream.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_support_SRC_FILES)
LOCAL_MODULE:= libmcldSupport

LOCAL_MODULE_TAGS := optional

include $(MCLD_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(mcld_support_SRC_FILES)
LOCAL_MODULE:= libmcldSupport

LOCAL_MODULE_TAGS := optional

include $(MCLD_DEVICE_BUILD_MK)
include $(BUILD_STATIC_LIBRARY)
