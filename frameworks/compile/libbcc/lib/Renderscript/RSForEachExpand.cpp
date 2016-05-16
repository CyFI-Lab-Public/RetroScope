/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "bcc/Assert.h"
#include "bcc/Renderscript/RSTransforms.h"

#include <cstdlib>

#include <llvm/IR/DerivedTypes.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/Instructions.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/MDBuilder.h>
#include <llvm/IR/Module.h>
#include <llvm/Pass.h>
#include <llvm/Support/raw_ostream.h>
#include <llvm/IR/DataLayout.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/Type.h>
#include <llvm/Transforms/Utils/BasicBlockUtils.h>

#include "bcc/Config/Config.h"
#include "bcc/Renderscript/RSInfo.h"
#include "bcc/Support/Log.h"

#include "bcinfo/MetadataExtractor.h"

using namespace bcc;

namespace {

/* RSForEachExpandPass - This pass operates on functions that are able to be
 * called via rsForEach() or "foreach_<NAME>". We create an inner loop for the
 * ForEach-able function to be invoked over the appropriate data cells of the
 * input/output allocations (adjusting other relevant parameters as we go). We
 * support doing this for any ForEach-able compute kernels. The new function
 * name is the original function name followed by ".expand". Note that we
 * still generate code for the original function.
 */
class RSForEachExpandPass : public llvm::ModulePass {
private:
  static char ID;

  llvm::Module *M;
  llvm::LLVMContext *C;

  const RSInfo::ExportForeachFuncListTy &mFuncs;

  // Turns on optimization of allocation stride values.
  bool mEnableStepOpt;

  uint32_t getRootSignature(llvm::Function *F) {
    const llvm::NamedMDNode *ExportForEachMetadata =
        M->getNamedMetadata("#rs_export_foreach");

    if (!ExportForEachMetadata) {
      llvm::SmallVector<llvm::Type*, 8> RootArgTys;
      for (llvm::Function::arg_iterator B = F->arg_begin(),
                                        E = F->arg_end();
           B != E;
           ++B) {
        RootArgTys.push_back(B->getType());
      }

      // For pre-ICS bitcode, we may not have signature information. In that
      // case, we use the size of the RootArgTys to select the number of
      // arguments.
      return (1 << RootArgTys.size()) - 1;
    }

    if (ExportForEachMetadata->getNumOperands() == 0) {
      return 0;
    }

    bccAssert(ExportForEachMetadata->getNumOperands() > 0);

    // We only handle the case for legacy root() functions here, so this is
    // hard-coded to look at only the first such function.
    llvm::MDNode *SigNode = ExportForEachMetadata->getOperand(0);
    if (SigNode != NULL && SigNode->getNumOperands() == 1) {
      llvm::Value *SigVal = SigNode->getOperand(0);
      if (SigVal->getValueID() == llvm::Value::MDStringVal) {
        llvm::StringRef SigString =
            static_cast<llvm::MDString*>(SigVal)->getString();
        uint32_t Signature = 0;
        if (SigString.getAsInteger(10, Signature)) {
          ALOGE("Non-integer signature value '%s'", SigString.str().c_str());
          return 0;
        }
        return Signature;
      }
    }

    return 0;
  }

  // Get the actual value we should use to step through an allocation.
  //
  // Normally the value we use to step through an allocation is given to us by
  // the driver. However, for certain primitive data types, we can derive an
  // integer constant for the step value. We use this integer constant whenever
  // possible to allow further compiler optimizations to take place.
  //
  // DL - Target Data size/layout information.
  // T - Type of allocation (should be a pointer).
  // OrigStep - Original step increment (root.expand() input from driver).
  llvm::Value *getStepValue(llvm::DataLayout *DL, llvm::Type *T,
                            llvm::Value *OrigStep) {
    bccAssert(DL);
    bccAssert(T);
    bccAssert(OrigStep);
    llvm::PointerType *PT = llvm::dyn_cast<llvm::PointerType>(T);
    llvm::Type *VoidPtrTy = llvm::Type::getInt8PtrTy(*C);
    if (mEnableStepOpt && T != VoidPtrTy && PT) {
      llvm::Type *ET = PT->getElementType();
      uint64_t ETSize = DL->getTypeAllocSize(ET);
      llvm::Type *Int32Ty = llvm::Type::getInt32Ty(*C);
      return llvm::ConstantInt::get(Int32Ty, ETSize);
    } else {
      return OrigStep;
    }
  }

  /// @brief Returns the type of the ForEach stub parameter structure.
  ///
  /// Renderscript uses a single structure in which all parameters are passed
  /// to keep the signature of the expanded function independent of the
  /// parameters passed to it.
  llvm::Type *getForeachStubTy() {
    llvm::Type *VoidPtrTy = llvm::Type::getInt8PtrTy(*C);
    llvm::Type *Int32Ty = llvm::Type::getInt32Ty(*C);
    llvm::Type *SizeTy = Int32Ty;
    /* Defined in frameworks/base/libs/rs/rs_hal.h:
     *
     * struct RsForEachStubParamStruct {
     *   const void *in;
     *   void *out;
     *   const void *usr;
     *   size_t usr_len;
     *   uint32_t x;
     *   uint32_t y;
     *   uint32_t z;
     *   uint32_t lod;
     *   enum RsAllocationCubemapFace face;
     *   uint32_t ar[16];
     * };
     */
    llvm::SmallVector<llvm::Type*, 9> StructTys;
    StructTys.push_back(VoidPtrTy);  // const void *in
    StructTys.push_back(VoidPtrTy);  // void *out
    StructTys.push_back(VoidPtrTy);  // const void *usr
    StructTys.push_back(SizeTy);     // size_t usr_len
    StructTys.push_back(Int32Ty);    // uint32_t x
    StructTys.push_back(Int32Ty);    // uint32_t y
    StructTys.push_back(Int32Ty);    // uint32_t z
    StructTys.push_back(Int32Ty);    // uint32_t lod
    StructTys.push_back(Int32Ty);    // enum RsAllocationCubemapFace
    StructTys.push_back(llvm::ArrayType::get(Int32Ty, 16));  // uint32_t ar[16]

    return llvm::StructType::create(StructTys, "RsForEachStubParamStruct");
  }

  /// @brief Create skeleton of the expanded function.
  ///
  /// This creates a function with the following signature:
  ///
  ///   void (const RsForEachStubParamStruct *p, uint32_t x1, uint32_t x2,
  ///         uint32_t instep, uint32_t outstep)
  ///
  llvm::Function *createEmptyExpandedFunction(llvm::StringRef OldName) {
    llvm::Type *ForEachStubPtrTy = getForeachStubTy()->getPointerTo();
    llvm::Type *Int32Ty = llvm::Type::getInt32Ty(*C);

    llvm::SmallVector<llvm::Type*, 8> ParamTys;
    ParamTys.push_back(ForEachStubPtrTy);  // const RsForEachStubParamStruct *p
    ParamTys.push_back(Int32Ty);           // uint32_t x1
    ParamTys.push_back(Int32Ty);           // uint32_t x2
    ParamTys.push_back(Int32Ty);           // uint32_t instep
    ParamTys.push_back(Int32Ty);           // uint32_t outstep

    llvm::FunctionType *FT =
        llvm::FunctionType::get(llvm::Type::getVoidTy(*C), ParamTys, false);
    llvm::Function *F =
        llvm::Function::Create(FT, llvm::GlobalValue::ExternalLinkage,
                               OldName + ".expand", M);

    llvm::Function::arg_iterator AI = F->arg_begin();

    AI->setName("p");
    AI++;
    AI->setName("x1");
    AI++;
    AI->setName("x2");
    AI++;
    AI->setName("arg_instep");
    AI++;
    AI->setName("arg_outstep");
    AI++;

    assert(AI == F->arg_end());

    llvm::BasicBlock *Begin = llvm::BasicBlock::Create(*C, "Begin", F);
    llvm::IRBuilder<> Builder(Begin);
    Builder.CreateRetVoid();

    return F;
  }

  /// @brief Create an empty loop
  ///
  /// Create a loop of the form:
  ///
  /// for (i = LowerBound; i < UpperBound; i++)
  ///   ;
  ///
  /// After the loop has been created, the builder is set such that
  /// instructions can be added to the loop body.
  ///
  /// @param Builder The builder to use to build this loop. The current
  ///                position of the builder is the position the loop
  ///                will be inserted.
  /// @param LowerBound The first value of the loop iterator
  /// @param UpperBound The maximal value of the loop iterator
  /// @param LoopIV A reference that will be set to the loop iterator.
  /// @return The BasicBlock that will be executed after the loop.
  llvm::BasicBlock *createLoop(llvm::IRBuilder<> &Builder,
                               llvm::Value *LowerBound,
                               llvm::Value *UpperBound,
                               llvm::PHINode **LoopIV) {
    assert(LowerBound->getType() == UpperBound->getType());

    llvm::BasicBlock *CondBB, *AfterBB, *HeaderBB;
    llvm::Value *Cond, *IVNext;
    llvm::PHINode *IV;

    CondBB = Builder.GetInsertBlock();
    AfterBB = llvm::SplitBlock(CondBB, Builder.GetInsertPoint(), this);
    HeaderBB = llvm::BasicBlock::Create(*C, "Loop", CondBB->getParent());

    // if (LowerBound < Upperbound)
    //   goto LoopHeader
    // else
    //   goto AfterBB
    CondBB->getTerminator()->eraseFromParent();
    Builder.SetInsertPoint(CondBB);
    Cond = Builder.CreateICmpULT(LowerBound, UpperBound);
    Builder.CreateCondBr(Cond, HeaderBB, AfterBB);

    // iv = PHI [CondBB -> LowerBound], [LoopHeader -> NextIV ]
    // iv.next = iv + 1
    // if (iv.next < Upperbound)
    //   goto LoopHeader
    // else
    //   goto AfterBB
    Builder.SetInsertPoint(HeaderBB);
    IV = Builder.CreatePHI(LowerBound->getType(), 2, "X");
    IV->addIncoming(LowerBound, CondBB);
    IVNext = Builder.CreateNUWAdd(IV, Builder.getInt32(1));
    IV->addIncoming(IVNext, HeaderBB);
    Cond = Builder.CreateICmpULT(IVNext, UpperBound);
    Builder.CreateCondBr(Cond, HeaderBB, AfterBB);
    AfterBB->setName("Exit");
    Builder.SetInsertPoint(HeaderBB->getFirstNonPHI());
    *LoopIV = IV;
    return AfterBB;
  }

public:
  RSForEachExpandPass(const RSInfo::ExportForeachFuncListTy &pForeachFuncs,
                      bool pEnableStepOpt)
      : ModulePass(ID), M(NULL), C(NULL), mFuncs(pForeachFuncs),
        mEnableStepOpt(pEnableStepOpt) {
  }

  /* Performs the actual optimization on a selected function. On success, the
   * Module will contain a new function of the name "<NAME>.expand" that
   * invokes <NAME>() in a loop with the appropriate parameters.
   */
  bool ExpandFunction(llvm::Function *F, uint32_t Signature) {
    ALOGV("Expanding ForEach-able Function %s", F->getName().str().c_str());

    if (!Signature) {
      Signature = getRootSignature(F);
      if (!Signature) {
        // We couldn't determine how to expand this function based on its
        // function signature.
        return false;
      }
    }

    llvm::DataLayout DL(M);

    llvm::Function *ExpandedFunc = createEmptyExpandedFunction(F->getName());

    // Create and name the actual arguments to this expanded function.
    llvm::SmallVector<llvm::Argument*, 8> ArgVec;
    for (llvm::Function::arg_iterator B = ExpandedFunc->arg_begin(),
                                      E = ExpandedFunc->arg_end();
         B != E;
         ++B) {
      ArgVec.push_back(B);
    }

    if (ArgVec.size() != 5) {
      ALOGE("Incorrect number of arguments to function: %zu",
            ArgVec.size());
      return false;
    }
    llvm::Value *Arg_p = ArgVec[0];
    llvm::Value *Arg_x1 = ArgVec[1];
    llvm::Value *Arg_x2 = ArgVec[2];
    llvm::Value *Arg_instep = ArgVec[3];
    llvm::Value *Arg_outstep = ArgVec[4];

    llvm::Value *InStep = NULL;
    llvm::Value *OutStep = NULL;

    // Construct the actual function body.
    llvm::IRBuilder<> Builder(ExpandedFunc->getEntryBlock().begin());

    // Collect and construct the arguments for the kernel().
    // Note that we load any loop-invariant arguments before entering the Loop.
    llvm::Function::arg_iterator Args = F->arg_begin();

    llvm::Type *InTy = NULL;
    llvm::Value *InBasePtr = NULL;
    if (bcinfo::MetadataExtractor::hasForEachSignatureIn(Signature)) {
      InTy = Args->getType();
      InStep = getStepValue(&DL, InTy, Arg_instep);
      InStep->setName("instep");
      InBasePtr = Builder.CreateLoad(Builder.CreateStructGEP(Arg_p, 0));
      Args++;
    }

    llvm::Type *OutTy = NULL;
    llvm::Value *OutBasePtr = NULL;
    if (bcinfo::MetadataExtractor::hasForEachSignatureOut(Signature)) {
      OutTy = Args->getType();
      OutStep = getStepValue(&DL, OutTy, Arg_outstep);
      OutStep->setName("outstep");
      OutBasePtr = Builder.CreateLoad(Builder.CreateStructGEP(Arg_p, 1));
      Args++;
    }

    llvm::Value *UsrData = NULL;
    if (bcinfo::MetadataExtractor::hasForEachSignatureUsrData(Signature)) {
      llvm::Type *UsrDataTy = Args->getType();
      UsrData = Builder.CreatePointerCast(Builder.CreateLoad(
          Builder.CreateStructGEP(Arg_p, 2)), UsrDataTy);
      UsrData->setName("UsrData");
      Args++;
    }

    if (bcinfo::MetadataExtractor::hasForEachSignatureX(Signature)) {
      Args++;
    }

    llvm::Value *Y = NULL;
    if (bcinfo::MetadataExtractor::hasForEachSignatureY(Signature)) {
      Y = Builder.CreateLoad(Builder.CreateStructGEP(Arg_p, 5), "Y");
      Args++;
    }

    bccAssert(Args == F->arg_end());

    llvm::PHINode *IV;
    createLoop(Builder, Arg_x1, Arg_x2, &IV);

    // Populate the actual call to kernel().
    llvm::SmallVector<llvm::Value*, 8> RootArgs;

    llvm::Value *InPtr = NULL;
    llvm::Value *OutPtr = NULL;

    // Calculate the current input and output pointers
    //
    // We always calculate the input/output pointers with a GEP operating on i8
    // values and only cast at the very end to OutTy. This is because the step
    // between two values is given in bytes.
    //
    // TODO: We could further optimize the output by using a GEP operation of
    // type 'OutTy' in cases where the element type of the allocation allows.
    if (OutBasePtr) {
      llvm::Value *OutOffset = Builder.CreateSub(IV, Arg_x1);
      OutOffset = Builder.CreateMul(OutOffset, OutStep);
      OutPtr = Builder.CreateGEP(OutBasePtr, OutOffset);
      OutPtr = Builder.CreatePointerCast(OutPtr, OutTy);
    }
    if (InBasePtr) {
      llvm::Value *InOffset = Builder.CreateSub(IV, Arg_x1);
      InOffset = Builder.CreateMul(InOffset, InStep);
      InPtr = Builder.CreateGEP(InBasePtr, InOffset);
      InPtr = Builder.CreatePointerCast(InPtr, InTy);
    }

    if (InPtr) {
      RootArgs.push_back(InPtr);
    }

    if (OutPtr) {
      RootArgs.push_back(OutPtr);
    }

    if (UsrData) {
      RootArgs.push_back(UsrData);
    }

    llvm::Value *X = IV;
    if (bcinfo::MetadataExtractor::hasForEachSignatureX(Signature)) {
      RootArgs.push_back(X);
    }

    if (Y) {
      RootArgs.push_back(Y);
    }

    Builder.CreateCall(F, RootArgs);

    return true;
  }

  /* Expand a pass-by-value kernel.
   */
  bool ExpandKernel(llvm::Function *F, uint32_t Signature) {
    bccAssert(bcinfo::MetadataExtractor::hasForEachSignatureKernel(Signature));
    ALOGV("Expanding kernel Function %s", F->getName().str().c_str());

    // TODO: Refactor this to share functionality with ExpandFunction.
    llvm::DataLayout DL(M);

    llvm::Function *ExpandedFunc = createEmptyExpandedFunction(F->getName());

    // Create and name the actual arguments to this expanded function.
    llvm::SmallVector<llvm::Argument*, 8> ArgVec;
    for (llvm::Function::arg_iterator B = ExpandedFunc->arg_begin(),
                                      E = ExpandedFunc->arg_end();
         B != E;
         ++B) {
      ArgVec.push_back(B);
    }

    if (ArgVec.size() != 5) {
      ALOGE("Incorrect number of arguments to function: %zu",
            ArgVec.size());
      return false;
    }
    llvm::Value *Arg_p = ArgVec[0];
    llvm::Value *Arg_x1 = ArgVec[1];
    llvm::Value *Arg_x2 = ArgVec[2];
    llvm::Value *Arg_instep = ArgVec[3];
    llvm::Value *Arg_outstep = ArgVec[4];

    llvm::Value *InStep = NULL;
    llvm::Value *OutStep = NULL;

    // Construct the actual function body.
    llvm::IRBuilder<> Builder(ExpandedFunc->getEntryBlock().begin());

    // Create TBAA meta-data.
    llvm::MDNode *TBAARenderScript, *TBAAAllocation, *TBAAPointer;

    llvm::MDBuilder MDHelper(*C);
    TBAARenderScript = MDHelper.createTBAARoot("RenderScript TBAA");
    TBAAAllocation = MDHelper.createTBAANode("allocation", TBAARenderScript);
    TBAAPointer = MDHelper.createTBAANode("pointer", TBAARenderScript);

    // Collect and construct the arguments for the kernel().
    // Note that we load any loop-invariant arguments before entering the Loop.
    llvm::Function::arg_iterator Args = F->arg_begin();

    llvm::Type *OutTy = NULL;
    bool PassOutByReference = false;
    llvm::LoadInst *OutBasePtr = NULL;
    if (bcinfo::MetadataExtractor::hasForEachSignatureOut(Signature)) {
      llvm::Type *OutBaseTy = F->getReturnType();
      if (OutBaseTy->isVoidTy()) {
        PassOutByReference = true;
        OutTy = Args->getType();
        Args++;
      } else {
        OutTy = OutBaseTy->getPointerTo();
        // We don't increment Args, since we are using the actual return type.
      }
      OutStep = getStepValue(&DL, OutTy, Arg_outstep);
      OutStep->setName("outstep");
      OutBasePtr = Builder.CreateLoad(Builder.CreateStructGEP(Arg_p, 1));
      OutBasePtr->setMetadata("tbaa", TBAAPointer);
    }

    llvm::Type *InBaseTy = NULL;
    llvm::Type *InTy = NULL;
    llvm::LoadInst *InBasePtr = NULL;
    if (bcinfo::MetadataExtractor::hasForEachSignatureIn(Signature)) {
      InBaseTy = Args->getType();
      InTy =InBaseTy->getPointerTo();
      InStep = getStepValue(&DL, InTy, Arg_instep);
      InStep->setName("instep");
      InBasePtr = Builder.CreateLoad(Builder.CreateStructGEP(Arg_p, 0));
      InBasePtr->setMetadata("tbaa", TBAAPointer);
      Args++;
    }

    // No usrData parameter on kernels.
    bccAssert(
        !bcinfo::MetadataExtractor::hasForEachSignatureUsrData(Signature));

    if (bcinfo::MetadataExtractor::hasForEachSignatureX(Signature)) {
      Args++;
    }

    llvm::Value *Y = NULL;
    if (bcinfo::MetadataExtractor::hasForEachSignatureY(Signature)) {
      Y = Builder.CreateLoad(Builder.CreateStructGEP(Arg_p, 5), "Y");
      Args++;
    }

    bccAssert(Args == F->arg_end());

    llvm::PHINode *IV;
    createLoop(Builder, Arg_x1, Arg_x2, &IV);

    // Populate the actual call to kernel().
    llvm::SmallVector<llvm::Value*, 8> RootArgs;

    llvm::Value *InPtr = NULL;
    llvm::Value *OutPtr = NULL;

    // Calculate the current input and output pointers
    //
    // We always calculate the input/output pointers with a GEP operating on i8
    // values and only cast at the very end to OutTy. This is because the step
    // between two values is given in bytes.
    //
    // TODO: We could further optimize the output by using a GEP operation of
    // type 'OutTy' in cases where the element type of the allocation allows.
    if (OutBasePtr) {
      llvm::Value *OutOffset = Builder.CreateSub(IV, Arg_x1);
      OutOffset = Builder.CreateMul(OutOffset, OutStep);
      OutPtr = Builder.CreateGEP(OutBasePtr, OutOffset);
      OutPtr = Builder.CreatePointerCast(OutPtr, OutTy);
    }
    if (InBasePtr) {
      llvm::Value *InOffset = Builder.CreateSub(IV, Arg_x1);
      InOffset = Builder.CreateMul(InOffset, InStep);
      InPtr = Builder.CreateGEP(InBasePtr, InOffset);
      InPtr = Builder.CreatePointerCast(InPtr, InTy);
    }

    if (PassOutByReference) {
      RootArgs.push_back(OutPtr);
    }

    if (InPtr) {
      llvm::LoadInst *In = Builder.CreateLoad(InPtr, "In");
      In->setMetadata("tbaa", TBAAAllocation);
      RootArgs.push_back(In);
    }

    llvm::Value *X = IV;
    if (bcinfo::MetadataExtractor::hasForEachSignatureX(Signature)) {
      RootArgs.push_back(X);
    }

    if (Y) {
      RootArgs.push_back(Y);
    }

    llvm::Value *RetVal = Builder.CreateCall(F, RootArgs);

    if (OutPtr && !PassOutByReference) {
      llvm::StoreInst *Store = Builder.CreateStore(RetVal, OutPtr);
      Store->setMetadata("tbaa", TBAAAllocation);
    }

    return true;
  }

  /// @brief Checks if pointers to allocation internals are exposed
  ///
  /// This function verifies if through the parameters passed to the kernel
  /// or through calls to the runtime library the script gains access to
  /// pointers pointing to data within a RenderScript Allocation.
  /// If we know we control all loads from and stores to data within
  /// RenderScript allocations and if we know the run-time internal accesses
  /// are all annotated with RenderScript TBAA metadata, only then we
  /// can safely use TBAA to distinguish between generic and from-allocation
  /// pointers.
  bool allocPointersExposed(llvm::Module &M) {
    // Old style kernel function can expose pointers to elements within
    // allocations.
    // TODO: Extend analysis to allow simple cases of old-style kernels.
    for (RSInfo::ExportForeachFuncListTy::const_iterator
             func_iter = mFuncs.begin(), func_end = mFuncs.end();
         func_iter != func_end; func_iter++) {
      const char *Name = func_iter->first;
      uint32_t Signature = func_iter->second;
      if (M.getFunction(Name) &&
          !bcinfo::MetadataExtractor::hasForEachSignatureKernel(Signature)) {
        return true;
      }
    }

    // Check for library functions that expose a pointer to an Allocation or
    // that are not yet annotated with RenderScript-specific tbaa information.
    static std::vector<std::string> Funcs;

    // rsGetElementAt(...)
    Funcs.push_back("_Z14rsGetElementAt13rs_allocationj");
    Funcs.push_back("_Z14rsGetElementAt13rs_allocationjj");
    Funcs.push_back("_Z14rsGetElementAt13rs_allocationjjj");
    // rsSetElementAt()
    Funcs.push_back("_Z14rsSetElementAt13rs_allocationPvj");
    Funcs.push_back("_Z14rsSetElementAt13rs_allocationPvjj");
    Funcs.push_back("_Z14rsSetElementAt13rs_allocationPvjjj");
    // rsGetElementAtYuv_uchar_Y()
    Funcs.push_back("_Z25rsGetElementAtYuv_uchar_Y13rs_allocationjj");
    // rsGetElementAtYuv_uchar_U()
    Funcs.push_back("_Z25rsGetElementAtYuv_uchar_U13rs_allocationjj");
    // rsGetElementAtYuv_uchar_V()
    Funcs.push_back("_Z25rsGetElementAtYuv_uchar_V13rs_allocationjj");

    for (std::vector<std::string>::iterator FI = Funcs.begin(),
                                            FE = Funcs.end();
         FI != FE; ++FI) {
      llvm::Function *F = M.getFunction(*FI);

      if (!F) {
        ALOGE("Missing run-time function '%s'", FI->c_str());
        return true;
      }

      if (F->getNumUses() > 0) {
        return true;
      }
    }

    return false;
  }

  /// @brief Connect RenderScript TBAA metadata to C/C++ metadata
  ///
  /// The TBAA metadata used to annotate loads/stores from RenderScript
  /// Allocations is generated in a separate TBAA tree with a "RenderScript TBAA"
  /// root node. LLVM does assume may-alias for all nodes in unrelated alias
  /// analysis trees. This function makes the RenderScript TBAA a subtree of the
  /// normal C/C++ TBAA tree aside of normal C/C++ types. With the connected trees
  /// every access to an Allocation is resolved to must-alias if compared to
  /// a normal C/C++ access.
  void connectRenderScriptTBAAMetadata(llvm::Module &M) {
    llvm::MDBuilder MDHelper(*C);
    llvm::MDNode *TBAARenderScript = MDHelper.createTBAARoot("RenderScript TBAA");

    llvm::MDNode *TBAARoot = MDHelper.createTBAARoot("Simple C/C++ TBAA");
    llvm::MDNode *TBAAMergedRS = MDHelper.createTBAANode("RenderScript", TBAARoot);

    TBAARenderScript->replaceAllUsesWith(TBAAMergedRS);
  }

  virtual bool runOnModule(llvm::Module &M) {
    bool Changed = false;
    this->M = &M;
    C = &M.getContext();

    bool AllocsExposed = allocPointersExposed(M);

    for (RSInfo::ExportForeachFuncListTy::const_iterator
             func_iter = mFuncs.begin(), func_end = mFuncs.end();
         func_iter != func_end; func_iter++) {
      const char *name = func_iter->first;
      uint32_t signature = func_iter->second;
      llvm::Function *kernel = M.getFunction(name);
      if (kernel) {
        if (bcinfo::MetadataExtractor::hasForEachSignatureKernel(signature)) {
          Changed |= ExpandKernel(kernel, signature);
          kernel->setLinkage(llvm::GlobalValue::InternalLinkage);
        } else if (kernel->getReturnType()->isVoidTy()) {
          Changed |= ExpandFunction(kernel, signature);
          kernel->setLinkage(llvm::GlobalValue::InternalLinkage);
        } else {
          // There are some graphics root functions that are not
          // expanded, but that will be called directly. For those
          // functions, we can not set the linkage to internal.
        }
      }
    }

    if (!AllocsExposed) {
      connectRenderScriptTBAAMetadata(M);
    }

    return Changed;
  }

  virtual const char *getPassName() const {
    return "ForEach-able Function Expansion";
  }

}; // end RSForEachExpandPass

} // end anonymous namespace

char RSForEachExpandPass::ID = 0;

namespace bcc {

llvm::ModulePass *
createRSForEachExpandPass(const RSInfo::ExportForeachFuncListTy &pForeachFuncs,
                          bool pEnableStepOpt){
  return new RSForEachExpandPass(pForeachFuncs, pEnableStepOpt);
}

} // end namespace bcc
