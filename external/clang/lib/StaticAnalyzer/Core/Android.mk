LOCAL_PATH:= $(call my-dir)

clang_static_analyzer_core_TBLGEN_TABLES := \
  AttrList.inc \
  Attrs.inc \
  CommentCommandList.inc \
  CommentNodes.inc \
  DeclNodes.inc \
  DiagnosticCommonKinds.inc \
  StmtNodes.inc

clang_static_analyzer_core_SRC_FILES := \
  AnalysisManager.cpp \
  AnalyzerOptions.cpp \
  APSIntType.cpp \
  BasicValueFactory.cpp \
  BlockCounter.cpp \
  BugReporter.cpp \
  BugReporterVisitors.cpp \
  CallEvent.cpp \
  Checker.cpp \
  CheckerContext.cpp \
  CheckerHelpers.cpp \
  CheckerManager.cpp \
  CheckerRegistry.cpp \
  ConstraintManager.cpp \
  CoreEngine.cpp \
  Environment.cpp \
  ExplodedGraph.cpp \
  ExprEngine.cpp \
  ExprEngineC.cpp \
  ExprEngineCXX.cpp \
  ExprEngineCallAndReturn.cpp \
  ExprEngineObjC.cpp \
  FunctionSummary.cpp \
  HTMLDiagnostics.cpp \
  MemRegion.cpp \
  PathDiagnostic.cpp \
  PlistDiagnostics.cpp \
  ProgramState.cpp \
  RangeConstraintManager.cpp \
  RegionStore.cpp \
  SValBuilder.cpp \
  SVals.cpp \
  SimpleConstraintManager.cpp \
  SimpleSValBuilder.cpp \
  Store.cpp \
  SubEngine.cpp \
  SymbolManager.cpp \
  TextPathDiagnostics.cpp

# For the host only
# =====================================================
include $(CLEAR_VARS)
include $(CLEAR_TBLGEN_VARS)

TBLGEN_TABLES := $(clang_static_analyzer_core_TBLGEN_TABLES)

LOCAL_SRC_FILES := $(clang_static_analyzer_core_SRC_FILES)

LOCAL_MODULE:= libclangStaticAnalyzerCore

LOCAL_MODULE_TAGS := optional

include $(CLANG_HOST_BUILD_MK)
include $(CLANG_TBLGEN_RULES_MK)
include $(CLANG_VERSION_INC_MK)
include $(BUILD_HOST_STATIC_LIBRARY)
