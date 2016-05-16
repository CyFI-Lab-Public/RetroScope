LOCAL_PATH:= $(call my-dir)

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=	\
	JIT.cpp	\
	JITEmitter.cpp	\
	JITMemoryManager.cpp

LOCAL_MODULE:= libLLVMJIT

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
