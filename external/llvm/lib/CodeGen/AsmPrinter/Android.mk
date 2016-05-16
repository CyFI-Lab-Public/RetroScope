LOCAL_PATH := $(call my-dir)

codegen_asmprinter_SRC_FILES := \
  AsmPrinter.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=	\
	AsmPrinter.cpp	\
	AsmPrinterDwarf.cpp	\
	AsmPrinterInlineAsm.cpp	\
	ARMException.cpp	\
	DIE.cpp	\
	DwarfAccelTable.cpp \
	DwarfCFIException.cpp \
	DwarfCompileUnit.cpp \
	DwarfDebug.cpp	\
	DwarfException.cpp	\
	ErlangGCPrinter.cpp \
	OcamlGCPrinter.cpp \
	Win64Exception.cpp

LOCAL_MODULE:= libLLVMAsmPrinter

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES :=	\
	AsmPrinter.cpp \
	AsmPrinterDwarf.cpp \
	AsmPrinterInlineAsm.cpp \
	ARMException.cpp        \
	DIE.cpp \
	DwarfAccelTable.cpp \
	DwarfCFIException.cpp \
	DwarfCompileUnit.cpp \
	DwarfDebug.cpp  \
	DwarfException.cpp      \
	ErlangGCPrinter.cpp \
	Win64Exception.cpp \
	$(LOCAL_SRC_FILES)

LOCAL_MODULE:= libLLVMAsmPrinter

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)
