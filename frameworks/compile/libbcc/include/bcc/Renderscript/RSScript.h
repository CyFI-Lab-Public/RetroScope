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

#ifndef BCC_RS_SCRIPT_H
#define BCC_RS_SCRIPT_H

#include "bcc/Script.h"
#include "bcc/Renderscript/RSInfo.h"
#include "bcc/Support/Sha1Util.h"

namespace llvm {
  class Module;
}  // end namespace llvm

namespace bcc {

class RSScript;
class Source;

class RSScript : public Script {
public:
  // This is one-one mapping with the llvm::CodeGenOpt::Level in
  // llvm/Support/CodeGen.h. Therefore, value of this type can safely cast
  // to llvm::CodeGenOpt::Level. This makes RSScript LLVM-free.
  enum OptimizationLevel {
    kOptLvl0, // -O0
    kOptLvl1, // -O1
    kOptLvl2, // -O2, -Os
    kOptLvl3  // -O3
  };

private:
  const RSInfo *mInfo;

  unsigned mCompilerVersion;

  OptimizationLevel mOptimizationLevel;

  RSLinkRuntimeCallback mLinkRuntimeCallback;

  bool mEmbedInfo;

private:
  // This will be invoked when the containing source has been reset.
  virtual bool doReset();

public:
  static bool LinkRuntime(RSScript &pScript, const char *rt_path = NULL);

  RSScript(Source &pSource);

  virtual ~RSScript() {
    delete mInfo;
  }

  // Set the associated RSInfo of the script.
  void setInfo(const RSInfo *pInfo) {
    mInfo = pInfo;
  }

  const RSInfo *getInfo() const {
    return mInfo;
  }

  void setCompilerVersion(unsigned pCompilerVersion) {
    mCompilerVersion = pCompilerVersion;
  }

  unsigned getCompilerVersion() const {
    return mCompilerVersion;
  }

  void setOptimizationLevel(OptimizationLevel pOptimizationLevel) {
    mOptimizationLevel = pOptimizationLevel;
  }

  OptimizationLevel getOptimizationLevel() const {
    return mOptimizationLevel;
  }

  void setLinkRuntimeCallback(RSLinkRuntimeCallback fn){
    mLinkRuntimeCallback = fn;
  }

  void setEmbedInfo(bool pEnable) {
    mEmbedInfo = pEnable;
  }

  bool getEmbedInfo() const {
    return mEmbedInfo;
  }
};

} // end namespace bcc

#endif // BCC_RS_SCRIPT_H
