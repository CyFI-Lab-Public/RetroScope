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

#ifndef BCC_SUPPORT_TARGET_COMPILER_CONFIGS_H
#define BCC_SUPPORT_TARGET_COMPILER_CONFIGS_H

#include "bcc/Config/Config.h"
#include "bcc/Support/CompilerConfig.h"

namespace bcc {

//===----------------------------------------------------------------------===//
// ARM
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_ARM_CODEGEN)
class ARMBaseCompilerConfig : public CompilerConfig {
private:
  bool mEnableNEON;
  bool mInThumbMode;

  static bool HasThumb2();

  static void GetFeatureVector(std::vector<std::string> &pAttributes,
                               bool pInThumbMode, bool pEnableNEON);

protected:
  ARMBaseCompilerConfig(const std::string &pTriple, bool pInThumbMode);

public:
  // Return true if config has been changed after returning from this function.
  bool enableNEON(bool pEnable = true);

  bool isInThumbMode() const
  { return mInThumbMode; }
};

class ARMCompilerConfig : public ARMBaseCompilerConfig {
public:
  ARMCompilerConfig()
    : ARMBaseCompilerConfig(DEFAULT_ARM_TRIPLE_STRING,
                            /* pInThumbMode */false) { }
};

class ThumbCompilerConfig : public ARMBaseCompilerConfig {
public:
  ThumbCompilerConfig()
    : ARMBaseCompilerConfig(DEFAULT_THUMB_TRIPLE_STRING,
                            /* pInThumbMode */true) { }
};
#endif // defined(PROVIDE_ARM_CODEGEN)

//===----------------------------------------------------------------------===//
// MIPS
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_MIPS_CODEGEN)
class MipsCompilerConfig : public CompilerConfig {
public:
  MipsCompilerConfig() : CompilerConfig(DEFAULT_MIPS_TRIPLE_STRING) {
    setRelocationModel(llvm::Reloc::Static);
  }
};
#endif // defined(PROVIDE_MIPS_CODEGEN)

//===----------------------------------------------------------------------===//
// X86 and X86_64
//===----------------------------------------------------------------------===//
#if defined(PROVIDE_X86_CODEGEN)
class X86FamilyCompilerConfigBase : public CompilerConfig {
protected:
  X86FamilyCompilerConfigBase(const std::string &pTriple)
    : CompilerConfig(pTriple) {
    // Disable frame pointer elimination optimization on x86 family.
    getTargetOptions().NoFramePointerElim = true;
    getTargetOptions().UseInitArray = true;
    return;
  }
};

class X86_32CompilerConfig : public X86FamilyCompilerConfigBase {
public:
  X86_32CompilerConfig() :
      X86FamilyCompilerConfigBase(DEFAULT_X86_TRIPLE_STRING) { }
};

class X86_64CompilerConfig : public X86FamilyCompilerConfigBase {
public:
  X86_64CompilerConfig() :
      X86FamilyCompilerConfigBase(DEFAULT_X86_64_TRIPLE_STRING) {
    setCodeModel(llvm::CodeModel::Medium);
  }
};
#endif // defined(PROVIDE_X86_CODEGEN)

//===----------------------------------------------------------------------===//
// Default target
//===----------------------------------------------------------------------===//
class DefaultCompilerConfig : public
#if defined(DEFAULT_ARM_CODEGEN)
  ARMCompilerConfig
#elif defined(DEFAULT_MIPS_CODEGEN)
  MipsCompilerConfig
#elif defined(DEFAULT_X86_CODEGEN)
  X86_32CompilerConfig
#elif defined(DEFAULT_X86_64_CODEGEN)
  X86_64CompilerConfig
#else
#  error "Unsupported Default Target!"
#endif
{ };

} // end namespace bcc

#endif // BCC_SUPPORT_TARGET_COMPILER_CONFIGS_H
