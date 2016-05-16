LOCAL_PATH:= $(call my-dir)

tablegen_SRC_FILES := \
  AsmMatcherEmitter.cpp \
  AsmWriterEmitter.cpp \
  AsmWriterInst.cpp \
  CallingConvEmitter.cpp \
  CodeEmitterGen.cpp \
  CodeGenDAGPatterns.cpp \
  CodeGenInstruction.cpp \
  CodeGenMapTable.cpp \
  CodeGenRegisters.cpp \
  CodeGenSchedule.cpp \
  CodeGenTarget.cpp \
  CTagsEmitter.cpp \
  DAGISelEmitter.cpp \
  DAGISelMatcherEmitter.cpp \
  DAGISelMatcherGen.cpp \
  DAGISelMatcherOpt.cpp \
  DAGISelMatcher.cpp \
  DFAPacketizerEmitter.cpp \
  DisassemblerEmitter.cpp \
  FastISelEmitter.cpp \
  FixedLenDecoderEmitter.cpp \
  InstrInfoEmitter.cpp \
  IntrinsicEmitter.cpp \
  OptParserEmitter.cpp \
  PseudoLoweringEmitter.cpp \
  RegisterInfoEmitter.cpp \
  SetTheory.cpp \
  SubtargetEmitter.cpp \
  TGValueTypes.cpp \
  TableGen.cpp \
  X86DisassemblerTables.cpp \
  X86ModRMFilters.cpp \
  X86RecognizableInstr.cpp

include $(CLEAR_VARS)

LOCAL_MODULE := tblgen
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(tablegen_SRC_FILES)

REQUIRES_EH := 1
REQUIRES_RTTI := 1

LOCAL_STATIC_LIBRARIES := \
  libLLVMTableGen \
  libLLVMSupport

LOCAL_LDLIBS += -lm
ifeq ($(HOST_OS),windows)
  LOCAL_LDLIBS += -limagehlp -lpsapi
else
  LOCAL_LDLIBS += -lpthread -ldl
endif

include $(LLVM_HOST_BUILD_MK)
include $(BUILD_HOST_EXECUTABLE)
