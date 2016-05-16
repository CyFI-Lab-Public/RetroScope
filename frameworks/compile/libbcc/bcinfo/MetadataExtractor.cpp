/*
 * Copyright 2011-2012, The Android Open Source Project
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

#include "bcinfo/MetadataExtractor.h"

#include "bcinfo/BitcodeWrapper.h"

#define LOG_TAG "bcinfo"
#include <cutils/log.h>
#ifdef HAVE_ANDROID_OS
#include <cutils/properties.h>
#endif

#include "llvm/ADT/OwningPtr.h"
#include "llvm/Bitcode/ReaderWriter.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/Module.h"
#include "llvm/Support/MemoryBuffer.h"

#include <cstdlib>

namespace bcinfo {

// Name of metadata node where pragma info resides (should be synced with
// slang.cpp)
static const llvm::StringRef PragmaMetadataName = "#pragma";

// Name of metadata node where exported variable names reside (should be
// synced with slang_rs_metadata.h)
static const llvm::StringRef ExportVarMetadataName = "#rs_export_var";

// Name of metadata node where exported function names reside (should be
// synced with slang_rs_metadata.h)
static const llvm::StringRef ExportFuncMetadataName = "#rs_export_func";

// Name of metadata node where exported ForEach name information resides
// (should be synced with slang_rs_metadata.h)
static const llvm::StringRef ExportForEachNameMetadataName =
    "#rs_export_foreach_name";

// Name of metadata node where exported ForEach signature information resides
// (should be synced with slang_rs_metadata.h)
static const llvm::StringRef ExportForEachMetadataName = "#rs_export_foreach";

// Name of metadata node where RS object slot info resides (should be
// synced with slang_rs_metadata.h)
static const llvm::StringRef ObjectSlotMetadataName = "#rs_object_slots";


MetadataExtractor::MetadataExtractor(const char *bitcode, size_t bitcodeSize)
    : mModule(NULL), mBitcode(bitcode), mBitcodeSize(bitcodeSize),
      mExportVarCount(0), mExportFuncCount(0), mExportForEachSignatureCount(0),
      mExportVarNameList(NULL), mExportFuncNameList(NULL),
      mExportForEachNameList(NULL), mExportForEachSignatureList(NULL),
      mPragmaCount(0), mPragmaKeyList(NULL), mPragmaValueList(NULL),
      mObjectSlotCount(0), mObjectSlotList(NULL),
      mRSFloatPrecision(RS_FP_Full) {
  BitcodeWrapper wrapper(bitcode, bitcodeSize);
  mCompilerVersion = wrapper.getCompilerVersion();
  mOptimizationLevel = wrapper.getOptimizationLevel();
}


MetadataExtractor::MetadataExtractor(const llvm::Module *module)
    : mModule(module), mBitcode(NULL), mBitcodeSize(0), mExportVarCount(0),
      mExportFuncCount(0), mExportForEachSignatureCount(0),
      mExportVarNameList(NULL), mExportFuncNameList(NULL),
      mExportForEachNameList(NULL), mExportForEachSignatureList(NULL),
      mPragmaCount(0), mPragmaKeyList(NULL), mPragmaValueList(NULL),
      mObjectSlotCount(0), mObjectSlotList(NULL),
      mRSFloatPrecision(RS_FP_Full) {
  mCompilerVersion = 0;
  mOptimizationLevel = 3;
}


MetadataExtractor::~MetadataExtractor() {
  if (mExportVarNameList) {
    for (size_t i = 0; i < mExportVarCount; i++) {
        delete [] mExportVarNameList[i];
        mExportVarNameList[i] = NULL;
    }
  }
  delete [] mExportVarNameList;
  mExportVarNameList = NULL;

  if (mExportFuncNameList) {
    for (size_t i = 0; i < mExportFuncCount; i++) {
        delete [] mExportFuncNameList[i];
        mExportFuncNameList[i] = NULL;
    }
  }
  delete [] mExportFuncNameList;
  mExportFuncNameList = NULL;

  if (mExportForEachNameList) {
    for (size_t i = 0; i < mExportForEachSignatureCount; i++) {
        delete [] mExportForEachNameList[i];
        mExportForEachNameList[i] = NULL;
    }
  }
  delete [] mExportForEachNameList;
  mExportForEachNameList = NULL;

  delete [] mExportForEachSignatureList;
  mExportForEachSignatureList = NULL;

  for (size_t i = 0; i < mPragmaCount; i++) {
    if (mPragmaKeyList) {
      delete [] mPragmaKeyList[i];
      mPragmaKeyList[i] = NULL;
    }
    if (mPragmaValueList) {
      delete [] mPragmaValueList[i];
      mPragmaValueList[i] = NULL;
    }
  }
  delete [] mPragmaKeyList;
  mPragmaKeyList = NULL;
  delete [] mPragmaValueList;
  mPragmaValueList = NULL;

  delete [] mObjectSlotList;
  mObjectSlotList = NULL;

  return;
}


bool MetadataExtractor::populateObjectSlotMetadata(
    const llvm::NamedMDNode *ObjectSlotMetadata) {
  if (!ObjectSlotMetadata) {
    return true;
  }

  mObjectSlotCount = ObjectSlotMetadata->getNumOperands();

  if (!mObjectSlotCount) {
    return true;
  }

  uint32_t *TmpSlotList = new uint32_t[mObjectSlotCount];
  memset(TmpSlotList, 0, mObjectSlotCount * sizeof(*TmpSlotList));

  for (size_t i = 0; i < mObjectSlotCount; i++) {
    llvm::MDNode *ObjectSlot = ObjectSlotMetadata->getOperand(i);
    if (ObjectSlot != NULL && ObjectSlot->getNumOperands() == 1) {
      llvm::Value *SlotMDS = ObjectSlot->getOperand(0);
      if (SlotMDS->getValueID() == llvm::Value::MDStringVal) {
        llvm::StringRef Slot =
            static_cast<llvm::MDString*>(SlotMDS)->getString();
        uint32_t USlot = 0;
        if (Slot.getAsInteger(10, USlot)) {
          ALOGE("Non-integer object slot value '%s'", Slot.str().c_str());
          return false;
        }
        TmpSlotList[i] = USlot;
      }
    }
  }

  mObjectSlotList = TmpSlotList;

  return true;
}


static const char *createStringFromValue(llvm::Value *v) {
  if (v->getValueID() != llvm::Value::MDStringVal) {
    return NULL;
  }

  llvm::StringRef ref = static_cast<llvm::MDString*>(v)->getString();

  char *c = new char[ref.size() + 1];
  memcpy(c, ref.data(), ref.size());
  c[ref.size()] = '\0';

  return c;
}


void MetadataExtractor::populatePragmaMetadata(
    const llvm::NamedMDNode *PragmaMetadata) {
  if (!PragmaMetadata) {
    return;
  }

  mPragmaCount = PragmaMetadata->getNumOperands();
  if (!mPragmaCount) {
    return;
  }

  const char **TmpKeyList = new const char*[mPragmaCount];
  const char **TmpValueList = new const char*[mPragmaCount];

  for (size_t i = 0; i < mPragmaCount; i++) {
    llvm::MDNode *Pragma = PragmaMetadata->getOperand(i);
    if (Pragma != NULL && Pragma->getNumOperands() == 2) {
      llvm::Value *PragmaKeyMDS = Pragma->getOperand(0);
      TmpKeyList[i] = createStringFromValue(PragmaKeyMDS);
      llvm::Value *PragmaValueMDS = Pragma->getOperand(1);
      TmpValueList[i] = createStringFromValue(PragmaValueMDS);
    }
  }

  mPragmaKeyList = TmpKeyList;
  mPragmaValueList = TmpValueList;

  // Check to see if we have any FP precision-related pragmas.
  std::string Relaxed("rs_fp_relaxed");
  std::string Imprecise("rs_fp_imprecise");
  std::string Full("rs_fp_full");
  bool RelaxedPragmaSeen = false;
  bool ImprecisePragmaSeen = false;

  for (size_t i = 0; i < mPragmaCount; i++) {
    if (!Relaxed.compare(mPragmaKeyList[i])) {
      if (RelaxedPragmaSeen || ImprecisePragmaSeen) {
        ALOGE("Multiple float precision pragmas specified!");
      }
      RelaxedPragmaSeen = true;
    } else if (!Imprecise.compare(mPragmaKeyList[i])) {
      if (RelaxedPragmaSeen || ImprecisePragmaSeen) {
        ALOGE("Multiple float precision pragmas specified!");
      }
      ImprecisePragmaSeen = true;
    }
  }

  // Imprecise is selected over Relaxed precision.
  // In the absence of both, we stick to the default Full precision.
  if (ImprecisePragmaSeen) {
    mRSFloatPrecision = RS_FP_Imprecise;
  } else if (RelaxedPragmaSeen) {
    mRSFloatPrecision = RS_FP_Relaxed;
  }

#ifdef HAVE_ANDROID_OS
  // Provide an override for precsion via adb shell setprop
  // adb shell setprop debug.rs.precision rs_fp_full
  // adb shell setprop debug.rs.precision rs_fp_relaxed
  // adb shell setprop debug.rs.precision rs_fp_imprecise
  char PrecisionPropBuf[PROPERTY_VALUE_MAX];
  const std::string PrecisionPropName("debug.rs.precision");
  property_get("debug.rs.precision", PrecisionPropBuf, "");
  if (PrecisionPropBuf[0]) {
    if (!Relaxed.compare(PrecisionPropBuf)) {
      ALOGE("Switching to RS FP relaxed mode via setprop");
      mRSFloatPrecision = RS_FP_Relaxed;
    } else if (!Imprecise.compare(PrecisionPropBuf)) {
      ALOGE("Switching to RS FP imprecise mode via setprop");
      mRSFloatPrecision = RS_FP_Imprecise;
    } else if (!Full.compare(PrecisionPropBuf)) {
      ALOGE("Switching to RS FP full mode via setprop");
      mRSFloatPrecision = RS_FP_Full;
    }
  }
#endif

  return;
}


bool MetadataExtractor::populateVarNameMetadata(
    const llvm::NamedMDNode *VarNameMetadata) {
  if (!VarNameMetadata) {
    return true;
  }

  mExportVarCount = VarNameMetadata->getNumOperands();
  if (!mExportVarCount) {
    return true;
  }

  const char **TmpNameList = new const char *[mExportVarCount];

  for (size_t i = 0; i < mExportVarCount; i++) {
    llvm::MDNode *Name = VarNameMetadata->getOperand(i);
    if (Name != NULL && Name->getNumOperands() > 1) {
      TmpNameList[i] = createStringFromValue(Name->getOperand(0));
    }
  }

  mExportVarNameList = TmpNameList;

  return true;
}


bool MetadataExtractor::populateFuncNameMetadata(
    const llvm::NamedMDNode *FuncNameMetadata) {
  if (!FuncNameMetadata) {
    return true;
  }

  mExportFuncCount = FuncNameMetadata->getNumOperands();
  if (!mExportFuncCount) {
    return true;
  }

  const char **TmpNameList = new const char*[mExportFuncCount];

  for (size_t i = 0; i < mExportFuncCount; i++) {
    llvm::MDNode *Name = FuncNameMetadata->getOperand(i);
    if (Name != NULL && Name->getNumOperands() == 1) {
      TmpNameList[i] = createStringFromValue(Name->getOperand(0));
    }
  }

  mExportFuncNameList = TmpNameList;

  return true;
}


bool MetadataExtractor::populateForEachMetadata(
    const llvm::NamedMDNode *Names,
    const llvm::NamedMDNode *Signatures) {
  if (!Names && !Signatures && mCompilerVersion == 0) {
    // Handle legacy case for pre-ICS bitcode that doesn't contain a metadata
    // section for ForEach. We generate a full signature for a "root" function
    // which means that we need to set the bottom 5 bits in the mask.
    mExportForEachSignatureCount = 1;
    char **TmpNameList = new char*[mExportForEachSignatureCount];
    TmpNameList[0] = new char[5];
    strncpy(TmpNameList[0], "root", 5);

    uint32_t *TmpSigList = new uint32_t[mExportForEachSignatureCount];
    TmpSigList[0] = 0x1f;

    mExportForEachNameList = (const char**)TmpNameList;
    mExportForEachSignatureList = TmpSigList;
    return true;
  }

  if (Signatures) {
    mExportForEachSignatureCount = Signatures->getNumOperands();
    if (!mExportForEachSignatureCount) {
      return true;
    }
  } else {
    mExportForEachSignatureCount = 0;
    mExportForEachSignatureList = NULL;
    return true;
  }

  uint32_t *TmpSigList = new uint32_t[mExportForEachSignatureCount];
  const char **TmpNameList = new const char*[mExportForEachSignatureCount];

  for (size_t i = 0; i < mExportForEachSignatureCount; i++) {
    llvm::MDNode *SigNode = Signatures->getOperand(i);
    if (SigNode != NULL && SigNode->getNumOperands() == 1) {
      llvm::Value *SigVal = SigNode->getOperand(0);
      if (SigVal->getValueID() == llvm::Value::MDStringVal) {
        llvm::StringRef SigString =
            static_cast<llvm::MDString*>(SigVal)->getString();
        uint32_t Signature = 0;
        if (SigString.getAsInteger(10, Signature)) {
          ALOGE("Non-integer signature value '%s'", SigString.str().c_str());
          return false;
        }
        TmpSigList[i] = Signature;
      }
    }
  }

  if (Names) {
    for (size_t i = 0; i < mExportForEachSignatureCount; i++) {
      llvm::MDNode *Name = Names->getOperand(i);
      if (Name != NULL && Name->getNumOperands() == 1) {
        TmpNameList[i] = createStringFromValue(Name->getOperand(0));
      }
    }
  } else {
    if (mExportForEachSignatureCount != 1) {
      ALOGE("mExportForEachSignatureCount = %zu, but should be 1",
            mExportForEachSignatureCount);
    }
    char *RootName = new char[5];
    strncpy(RootName, "root", 5);
    TmpNameList[0] = RootName;
  }

  mExportForEachNameList = TmpNameList;
  mExportForEachSignatureList = TmpSigList;

  return true;
}


bool MetadataExtractor::extract() {
  if (!(mBitcode && mBitcodeSize) && !mModule) {
    ALOGE("Invalid/empty bitcode/module");
    return false;
  }

  llvm::OwningPtr<llvm::LLVMContext> mContext;

  if (!mModule) {
    mContext.reset(new llvm::LLVMContext());
    llvm::OwningPtr<llvm::MemoryBuffer> MEM(
      llvm::MemoryBuffer::getMemBuffer(
        llvm::StringRef(mBitcode, mBitcodeSize), "", false));
    std::string error;

    // Module ownership is handled by the context, so we don't need to free it.
    mModule = llvm::ParseBitcodeFile(MEM.get(), *mContext, &error);
    if (!mModule) {
      ALOGE("Could not parse bitcode file");
      ALOGE("%s", error.c_str());
      return false;
    }
  }

  const llvm::NamedMDNode *ExportVarMetadata =
      mModule->getNamedMetadata(ExportVarMetadataName);
  const llvm::NamedMDNode *ExportFuncMetadata =
      mModule->getNamedMetadata(ExportFuncMetadataName);
  const llvm::NamedMDNode *ExportForEachNameMetadata =
      mModule->getNamedMetadata(ExportForEachNameMetadataName);
  const llvm::NamedMDNode *ExportForEachMetadata =
      mModule->getNamedMetadata(ExportForEachMetadataName);
  const llvm::NamedMDNode *PragmaMetadata =
      mModule->getNamedMetadata(PragmaMetadataName);
  const llvm::NamedMDNode *ObjectSlotMetadata =
      mModule->getNamedMetadata(ObjectSlotMetadataName);


  if (!populateVarNameMetadata(ExportVarMetadata)) {
    ALOGE("Could not populate export variable metadata");
    return false;
  }

  if (!populateFuncNameMetadata(ExportFuncMetadata)) {
    ALOGE("Could not populate export function metadata");
    return false;
  }

  if (!populateForEachMetadata(ExportForEachNameMetadata,
                               ExportForEachMetadata)) {
    ALOGE("Could not populate ForEach signature metadata");
    return false;
  }

  populatePragmaMetadata(PragmaMetadata);

  if (!populateObjectSlotMetadata(ObjectSlotMetadata)) {
    ALOGE("Could not populate object slot metadata");
    return false;
  }

  return true;
}

}  // namespace bcinfo

