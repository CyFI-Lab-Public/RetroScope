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

#include "bcc/Support/Properties.h"
#include "bcc/Support/TargetCompilerConfigs.h"

#include "llvm/ADT/StringMap.h"
#include "llvm/Support/Host.h"

// Get ARM version number (i.e., __ARM_ARCH__)
#ifdef __arm__
#include <machine/cpu-features.h>
#endif

using namespace bcc;

//===----------------------------------------------------------------------===//
// ARM
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_ARM_CODEGEN)

bool ARMBaseCompilerConfig::HasThumb2() {
#if !defined(TARGET_BUILD)
  // Cross-compiler can always generate Thumb-2 instructions.
  return true;
#else // defined(TARGET_BUILD)
#  if (__ARM_ARCH__ >= 7) || defined(__ARM_ARCH_6T2__)
  return true;
#  else
  // ARM prior to V6T2 doesn't support Thumb-2.
  return false;
#  endif
#endif
}

void
ARMBaseCompilerConfig::GetFeatureVector(std::vector<std::string> &pAttributes,
                                        bool pInThumbMode, bool pEnableNEON) {
  llvm::StringMap<bool> Features;
  llvm::sys::getHostCPUFeatures(Features);

#if defined(ARCH_ARM_HAVE_VFP)
  pAttributes.push_back("+vfp3");
#  if !defined(ARCH_ARM_HAVE_VFP_D32)
  pAttributes.push_back("+d16");
#  endif
#endif

  if (pInThumbMode) {
    if (HasThumb2()) {
      pAttributes.push_back("+thumb2");
    } else {
      pAttributes.push_back("-thumb2");
    }
  }

  if (pEnableNEON && Features.count("neon") && Features["neon"]) {
    pAttributes.push_back("+neon");
  } else {
    pAttributes.push_back("-neon");
    pAttributes.push_back("-neonfp");
  }

  if (!getProperty("debug.rs.arm-no-hwdiv")) {
    if (Features.count("hwdiv-arm") && Features["hwdiv-arm"])
      pAttributes.push_back("+hwdiv-arm");

    if (Features.count("hwdiv") && Features["hwdiv"])
      pAttributes.push_back("+hwdiv");
  }

  return;
}

ARMBaseCompilerConfig::ARMBaseCompilerConfig(const std::string &pTriple,
                                             bool pInThumbMode)
  : CompilerConfig(pTriple), mInThumbMode(pInThumbMode) {

  // Enable NEON by default.
  mEnableNEON = true;

  if (!getProperty("debug.rs.arm-no-tune-for-cpu"))
    setCPU(llvm::sys::getHostCPUName());

  std::vector<std::string> attributes;
  GetFeatureVector(attributes, mInThumbMode, mEnableNEON);
  setFeatureString(attributes);

  return;
}

bool ARMBaseCompilerConfig::enableNEON(bool pEnable) {
#if defined(ARCH_ARM_HAVE_NEON)
  if (mEnableNEON != pEnable) {
    std::vector<std::string> attributes;
    GetFeatureVector(attributes, mInThumbMode, pEnable);
    setFeatureString(attributes);
    mEnableNEON = pEnable;
    return true;
  }
  // Fall-through
#endif
  return false;
}
#endif // defined(PROVIDE_ARM_CODEGEN)
