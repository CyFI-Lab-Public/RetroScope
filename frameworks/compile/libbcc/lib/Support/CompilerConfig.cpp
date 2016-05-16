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

#include "bcc/Support/CompilerConfig.h"

#include <llvm/CodeGen/SchedulerRegistry.h>
#include <llvm/MC/SubtargetFeature.h>
#include <llvm/Support/TargetRegistry.h>

#include "bcc/Support/Log.h"
#include "bcc/Support/TargetCompilerConfigs.h"

using namespace bcc;

CompilerConfig::CompilerConfig(const std::string &pTriple)
  : mTriple(pTriple), mTarget(NULL) {
  //===--------------------------------------------------------------------===//
  // Default setting of register sheduler
  //===--------------------------------------------------------------------===//
  llvm::RegisterScheduler::setDefault(llvm::createDefaultScheduler);

  //===--------------------------------------------------------------------===//
  // Default setting of target options
  //===--------------------------------------------------------------------===//
  // Use hardfloat ABI by default.
  //
  // TODO(all): Need to detect the CPU capability and decide whether to use
  // softfp. To use softfp, change the following 2 lines to
  //
  // options.FloatABIType = llvm::FloatABI::Soft;
  // options.UseSoftFloat = true;
  mTargetOpts.FloatABIType = llvm::FloatABI::Soft;
  mTargetOpts.UseSoftFloat = false;

  // Enable frame pointer elimination optimization by default.
  mTargetOpts.NoFramePointerElim = false;

  //===--------------------------------------------------------------------===//
  // Default setting for code model
  //===--------------------------------------------------------------------===//
  mCodeModel = llvm::CodeModel::Small;

  //===--------------------------------------------------------------------===//
  // Default setting for relocation model
  //===--------------------------------------------------------------------===//
  mRelocModel = llvm::Reloc::Default;

  //===--------------------------------------------------------------------===//
  // Default setting for optimization level (-O2)
  //===--------------------------------------------------------------------===//
  mOptLevel = llvm::CodeGenOpt::Default;

  //===--------------------------------------------------------------------===//
  // Default setting for architecture type
  //===--------------------------------------------------------------------===//
  mArchType = llvm::Triple::UnknownArch;

  initializeTarget();
  initializeArch();

  return;
}

bool CompilerConfig::initializeTarget() {
  std::string error;
  mTarget = llvm::TargetRegistry::lookupTarget(mTriple, error);
  if (mTarget != NULL) {
    return true;
  } else {
    ALOGE("Cannot initialize llvm::Target for given triple '%s'! (%s)",
          mTriple.c_str(), error.c_str());
    return false;
  }
}

void CompilerConfig::initializeArch() {
  if (mTarget != NULL) {
    mArchType = llvm::Triple::getArchTypeForLLVMName(mTarget->getName());
  } else {
    mArchType = llvm::Triple::UnknownArch;
  }
  return;
}

void CompilerConfig::setFeatureString(const std::vector<std::string> &pAttrs) {
  llvm::SubtargetFeatures f;

  for (std::vector<std::string>::const_iterator attr_iter = pAttrs.begin(),
           attr_end = pAttrs.end();
       attr_iter != attr_end; attr_iter++) {
    f.AddFeature(*attr_iter);
  }

  mFeatureString = f.getString();
  return;
}
