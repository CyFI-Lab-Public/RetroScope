LOCAL_PATH:= $(call my-dir)

transforms_objcarc_SRC_FILES := \
  DependencyAnalysis.cpp \
  ObjCARCAliasAnalysis.cpp \
  ObjCARCAPElim.cpp \
  ObjCARCContract.cpp \
  ObjCARC.cpp \
  ObjCARCExpand.cpp \
  ObjCARCOpts.cpp \
  ObjCARCUtil.cpp \
  ProvenanceAnalysis.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(transforms_objcarc_SRC_FILES)
LOCAL_MODULE:= libLLVMTransformObjCARC

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(transforms_objcarc_SRC_FILES)
LOCAL_MODULE:= libLLVMTransformObjCARC

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)
