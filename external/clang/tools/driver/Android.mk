LOCAL_PATH:= $(call my-dir)

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

LOCAL_MODULE := clang

LOCAL_MODULE_CLASS := EXECUTABLES

TBLGEN_TABLES := \
  DiagnosticCommonKinds.inc \
  DiagnosticDriverKinds.inc \
  DiagnosticFrontendKinds.inc \
  CC1Options.inc \
  CC1AsOptions.inc

clang_SRC_FILES := \
  cc1_main.cpp \
  cc1as_main.cpp \
  driver.cpp

LOCAL_SRC_FILES := $(clang_SRC_FILES)

LOCAL_STATIC_LIBRARIES := \
  libclangFrontendTool \
  libclangFrontend \
  libclangARCMigrate \
  libclangDriver \
  libclangSerialization \
  libclangCodeGen \
  libclangRewriteFrontend \
  libclangRewriteCore \
  libclangParse \
  libclangSema \
  libclangStaticAnalyzerFrontend \
  libclangStaticAnalyzerCheckers \
  libclangStaticAnalyzerCore \
  libclangAnalysis \
  libclangEdit \
  libclangAST \
  libclangLex \
  libclangBasic \
  libLLVMARMAsmParser \
  libLLVMARMCodeGen \
  libLLVMARMAsmPrinter \
  libLLVMARMDisassembler \
  libLLVMARMDesc \
  libLLVMARMInfo \
  libLLVMMipsAsmParser \
  libLLVMMipsCodeGen \
  libLLVMMipsDisassembler \
  libLLVMMipsAsmPrinter \
  libLLVMMipsDesc \
  libLLVMMipsInfo \
  libLLVMX86Info \
  libLLVMX86AsmParser \
  libLLVMX86CodeGen \
  libLLVMX86Disassembler \
  libLLVMX86Desc \
  libLLVMX86AsmPrinter \
  libLLVMX86Utils \
  libLLVMIRReader \
  libLLVMAsmParser \
  libLLVMAsmPrinter \
  libLLVMBitReader \
  libLLVMBitWriter \
  libLLVMSelectionDAG \
  libLLVMipo \
  libLLVMipa \
  libLLVMInstCombine \
  libLLVMInstrumentation \
  libLLVMCodeGen \
  libLLVMObject \
  libLLVMLinker \
  libLLVMMC \
  libLLVMMCParser \
  libLLVMScalarOpts \
  libLLVMTransformObjCARC \
  libLLVMTransformUtils \
  libLLVMVectorize \
  libLLVMAnalysis \
  libLLVMCore \
  libLLVMOption \
  libLLVMSupport \
  libLLVMTarget

LOCAL_LDLIBS += -lm
ifdef USE_MINGW
LOCAL_LDLIBS += -limagehlp
else
LOCAL_LDLIBS += -lpthread -ldl
endif

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(BUILD_HOST_EXECUTABLE)

# Make sure if clang (i.e. $(LOCAL_MODULE)) get installed,
# clang++ will get installed as well.
ALL_MODULES.$(LOCAL_MODULE).INSTALLED := \
    $(ALL_MODULES.$(LOCAL_MODULE).INSTALLED) $(CLANG_CXX)
# the additional dependency is needed when you run mm/mmm.
$(LOCAL_MODULE) : $(CLANG_CXX)

# Symlink for clang++
$(CLANG_CXX) : $(LOCAL_INSTALLED_MODULE)
	@echo "Symlink $@ -> $<"
	$(hide) ln -sf $(notdir $<) $@
