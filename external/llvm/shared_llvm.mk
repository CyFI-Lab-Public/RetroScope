# Don't build the library in unbundled branches.
ifeq (,$(TARGET_BUILD_APPS))

LOCAL_PATH:= $(call my-dir)

llvm_pre_static_libraries := \
  libLLVMLinker \
  libLLVMipo \
  libLLVMIRReader \
  libLLVMBitWriter \
  libLLVMBitReader

llvm_arm_static_libraries := \
  libLLVMARMCodeGen \
  libLLVMARMAsmParser \
  libLLVMARMAsmPrinter \
  libLLVMARMInfo \
  libLLVMARMDesc \
  libLLVMARMDisassembler

llvm_x86_static_libraries := \
  libLLVMX86CodeGen \
  libLLVMX86Info \
  libLLVMX86Desc \
  libLLVMX86AsmParser \
  libLLVMX86AsmPrinter \
  libLLVMX86Utils \
  libLLVMX86Disassembler

llvm_mips_static_libraries := \
  libLLVMMipsCodeGen \
  libLLVMMipsInfo \
  libLLVMMipsDesc \
  libLLVMMipsAsmParser \
  libLLVMMipsAsmPrinter \
  libLLVMMipsDisassembler

llvm_post_static_libraries := \
  libLLVMAsmPrinter \
  libLLVMSelectionDAG \
  libLLVMCodeGen \
  libLLVMObject \
  libLLVMScalarOpts \
  libLLVMInstCombine \
  libLLVMInstrumentation \
  libLLVMTransformObjCARC \
  libLLVMTransformUtils \
  libLLVMipa \
  libLLVMAnalysis \
  libLLVMTarget \
  libLLVMMC \
  libLLVMMCParser \
  libLLVMCore \
  libLLVMAsmParser \
  libLLVMOption \
  libLLVMSupport \
  libLLVMVectorize

# HOST LLVM shared library build
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true

LOCAL_MODULE:= libLLVM

LOCAL_MODULE_TAGS := optional

# Host build pulls in all ARM, Mips, X86 components.
LOCAL_WHOLE_STATIC_LIBRARIES := \
  $(llvm_pre_static_libraries) \
  $(llvm_arm_static_libraries) \
  $(llvm_x86_static_libraries) \
  $(llvm_mips_static_libraries) \
  $(llvm_post_static_libraries)

ifeq ($(HOST_OS),windows)
  LOCAL_LDLIBS := -limagehlp -lpsapi
else
  LOCAL_LDLIBS := -ldl -lpthread
endif

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_SHARED_LIBRARY)


# DEVICE LLVM shared library build
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVM

LOCAL_MODULE_TAGS := optional

# Device build selectively pulls in ARM, Mips, X86 components.
LOCAL_WHOLE_STATIC_LIBRARIES := \
  $(llvm_pre_static_libraries)

ifeq ($(TARGET_ARCH),arm)
  LOCAL_WHOLE_STATIC_LIBRARIES += $(llvm_arm_static_libraries)
else
  ifeq ($(TARGET_ARCH),x86)
    LOCAL_WHOLE_STATIC_LIBRARIES += $(llvm_x86_static_libraries)
  else
    ifeq ($(TARGET_ARCH),mips)
      LOCAL_WHOLE_STATIC_LIBRARIES += $(llvm_mips_static_libraries)
    else
      $(error Unsupported architecture $(TARGET_ARCH))
    endif
  endif
endif

LOCAL_WHOLE_STATIC_LIBRARIES += $(llvm_post_static_libraries)

#LOCAL_LDLIBS := -ldl -lpthread
LOCAL_SHARED_LIBRARIES := libcutils libdl libstlport

include $(LLVM_DEVICE_BUILD_MK)
include $(BUILD_SHARED_LIBRARY)

endif # don't build in unbundled branches
