/*
 * Copyright 2010-2012, The Android Open Source Project
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

#ifndef BCC_SOURCE_H
#define BCC_SOURCE_H

#include <string>

namespace llvm {
  class Module;
}

namespace bcc {

class BCCContext;

class Source {
private:
  BCCContext &mContext;
  llvm::Module *mModule;

  // If true, destructor won't destroy the mModule.
  bool mNoDelete;

private:
  Source(BCCContext &pContext, llvm::Module &pModule, bool pNoDelete = false);

public:
  static Source *CreateFromBuffer(BCCContext &pContext,
                                  const char *pName,
                                  const char *pBitcode,
                                  size_t pBitcodeSize);

  static Source *CreateFromFile(BCCContext &pContext,
                                const std::string &pPath);

  // Create a Source object from an existing module. If pNoDelete
  // is true, destructor won't call delete on the given module.
  static Source *CreateFromModule(BCCContext &pContext,
                                  llvm::Module &pModule,
                                  bool pNoDelete = false);

  static Source *CreateEmpty(BCCContext &pContext, const std::string &pName);

  // Merge the current source with pSource. If pPreserveSource is false, pSource
  // will be destroyed after successfully merged. Return false on error.
  bool merge(Source &pSource, bool pPreserveSource = false);

  inline BCCContext &getContext()
  { return mContext; }
  inline const BCCContext &getContext() const
  { return mContext; }

  void setModule(llvm::Module *pModule);

  inline llvm::Module &getModule()
  { return *mModule;  }
  inline const llvm::Module &getModule() const
  { return *mModule;  }

  // Get the "identifier" of the bitcode. This will return the value of pName
  // when it's created using CreateFromBuffer and pPath if CreateFromFile().
  const std::string &getIdentifier() const;

  ~Source();
};

} // namespace bcc

#endif // BCC_SOURCE_H
