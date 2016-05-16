LOCAL_PATH:= $(call my-dir)

analysis_SRC_FILES := \
  AliasAnalysis.cpp \
  AliasAnalysisCounter.cpp \
  AliasAnalysisEvaluator.cpp \
  AliasDebugger.cpp \
  AliasSetTracker.cpp \
  Analysis.cpp \
  BasicAliasAnalysis.cpp \
  BlockFrequencyInfo.cpp \
  BranchProbabilityInfo.cpp \
  CFG.cpp \
  CFGPrinter.cpp \
  CaptureTracking.cpp \
  CodeMetrics.cpp \
  ConstantFolding.cpp \
  CostModel.cpp \
  DependenceAnalysis.cpp \
  DomPrinter.cpp \
  DominanceFrontier.cpp \
  IVUsers.cpp \
  InstCount.cpp \
  InstructionSimplify.cpp \
  Interval.cpp \
  IntervalPartition.cpp \
  LazyValueInfo.cpp \
  LibCallAliasAnalysis.cpp \
  LibCallSemantics.cpp \
  Lint.cpp \
  Loads.cpp \
  LoopInfo.cpp \
  LoopPass.cpp \
  MemDepPrinter.cpp \
  MemoryBuiltins.cpp \
  MemoryDependenceAnalysis.cpp \
  ModuleDebugInfoPrinter.cpp \
  NoAliasAnalysis.cpp \
  PHITransAddr.cpp \
  PathNumbering.cpp \
  PathProfileInfo.cpp \
  PathProfileVerifier.cpp \
  PostDominators.cpp \
  ProfileDataLoader.cpp \
  ProfileDataLoaderPass.cpp \
  ProfileEstimatorPass.cpp \
  ProfileInfo.cpp \
  ProfileInfoLoader.cpp \
  ProfileInfoLoaderPass.cpp \
  ProfileVerifierPass.cpp \
  PtrUseVisitor.cpp \
  RegionInfo.cpp \
  RegionPass.cpp \
  RegionPrinter.cpp \
  ScalarEvolution.cpp \
  ScalarEvolutionAliasAnalysis.cpp \
  ScalarEvolutionExpander.cpp \
  ScalarEvolutionNormalization.cpp \
  SparsePropagation.cpp \
  TargetTransformInfo.cpp \
  Trace.cpp \
  TypeBasedAliasAnalysis.cpp \
  ValueTracking.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVMAnalysis
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(analysis_SRC_FILES)

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_MODULE:= libLLVMAnalysis
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(analysis_SRC_FILES)

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)
