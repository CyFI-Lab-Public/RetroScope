LOCAL_PATH:= $(call my-dir)

codegen_SRC_FILES := \
  AggressiveAntiDepBreaker.cpp \
  AllocationOrder.cpp \
  Analysis.cpp \
  BasicTargetTransformInfo.cpp \
  BranchFolding.cpp \
  CalcSpillWeights.cpp \
  CallingConvLower.cpp \
  CodeGen.cpp \
  CriticalAntiDepBreaker.cpp \
  DeadMachineInstructionElim.cpp \
  DFAPacketizer.cpp \
  DwarfEHPrepare.cpp \
  EarlyIfConversion.cpp \
  EdgeBundles.cpp \
  ErlangGC.cpp \
  ExecutionDepsFix.cpp \
  ExpandISelPseudos.cpp \
  ExpandPostRAPseudos.cpp \
  GCMetadata.cpp \
  GCMetadataPrinter.cpp \
  GCStrategy.cpp \
  IfConversion.cpp \
  InlineSpiller.cpp \
  InterferenceCache.cpp \
  IntrinsicLowering.cpp \
  JITCodeEmitter.cpp \
  LatencyPriorityQueue.cpp \
  LexicalScopes.cpp \
  LiveDebugVariables.cpp \
  LiveIntervalAnalysis.cpp \
  LiveInterval.cpp \
  LiveIntervalUnion.cpp \
  LiveRangeCalc.cpp \
  LiveRangeEdit.cpp \
  LiveRegMatrix.cpp \
  LiveStackAnalysis.cpp \
  LiveVariables.cpp \
  LLVMTargetMachine.cpp \
  LocalStackSlotAllocation.cpp \
  MachineBasicBlock.cpp \
  MachineBlockFrequencyInfo.cpp \
  MachineBlockPlacement.cpp \
  MachineBranchProbabilityInfo.cpp \
  MachineCodeEmitter.cpp \
  MachineCopyPropagation.cpp \
  MachineCSE.cpp \
  MachineDominators.cpp \
  MachineFunctionAnalysis.cpp \
  MachineFunction.cpp \
  MachineFunctionPass.cpp \
  MachineFunctionPrinterPass.cpp \
  MachineInstrBundle.cpp \
  MachineInstr.cpp \
  MachineLICM.cpp \
  MachineLoopInfo.cpp \
  MachineModuleInfo.cpp \
  MachineModuleInfoImpls.cpp \
  MachinePassRegistry.cpp \
  MachinePostDominators.cpp \
  MachineRegisterInfo.cpp \
  MachineScheduler.cpp \
  MachineSink.cpp \
  MachineSSAUpdater.cpp \
  MachineTraceMetrics.cpp \
  MachineVerifier.cpp \
  OcamlGC.cpp \
  OptimizePHIs.cpp \
  Passes.cpp \
  PeepholeOptimizer.cpp \
  PHIElimination.cpp \
  PHIEliminationUtils.cpp \
  PostRASchedulerList.cpp \
  ProcessImplicitDefs.cpp \
  PrologEpilogInserter.cpp \
  PseudoSourceValue.cpp \
  RegAllocBase.cpp \
  RegAllocBasic.cpp \
  RegAllocFast.cpp \
  RegAllocGreedy.cpp \
  RegAllocPBQP.cpp \
  RegisterClassInfo.cpp \
  RegisterCoalescer.cpp \
  RegisterPressure.cpp \
  RegisterScavenging.cpp \
  ScheduleDAG.cpp \
  ScheduleDAGInstrs.cpp \
  ScheduleDAGPrinter.cpp \
  ScoreboardHazardRecognizer.cpp \
  ShadowStackGC.cpp \
  ShrinkWrapping.cpp \
  SjLjEHPrepare.cpp \
  SlotIndexes.cpp \
  Spiller.cpp \
  SpillPlacement.cpp \
  SplitKit.cpp \
  StackColoring.cpp \
  StackProtector.cpp \
  StackSlotColoring.cpp \
  StrongPHIElimination.cpp \
  TailDuplication.cpp \
  TargetFrameLoweringImpl.cpp \
  TargetInstrInfo.cpp \
  TargetLoweringBase.cpp \
  TargetLoweringObjectFileImpl.cpp \
  TargetOptionsImpl.cpp \
  TargetRegisterInfo.cpp \
  TargetSchedule.cpp \
  TwoAddressInstructionPass.cpp \
  UnreachableBlockElim.cpp \
  VirtRegMap.cpp

# For the host
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(codegen_SRC_FILES)
LOCAL_MODULE:= libLLVMCodeGen

LOCAL_MODULE_TAGS := optional

include $(LLVM_HOST_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_HOST_STATIC_LIBRARY)

# For the device
# =====================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(codegen_SRC_FILES)
LOCAL_MODULE:= libLLVMCodeGen

LOCAL_MODULE_TAGS := optional

include $(LLVM_DEVICE_BUILD_MK)
include $(LLVM_GEN_INTRINSICS_MK)
include $(BUILD_STATIC_LIBRARY)
