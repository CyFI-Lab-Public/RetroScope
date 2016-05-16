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

#ifndef BCC_RS_EXECUTABLE_H
#define BCC_RS_EXECUTABLE_H

#include <cstddef>


#include "bcc/ExecutionEngine/ObjectLoader.h"
#include "bcc/Renderscript/RSInfo.h"
#include "bcc/Support/Log.h"

#include <utils/Vector.h>

namespace bcc {

class FileBase;
class OutputFile;
class SymbolResolverProxy;

/*
 * RSExecutable holds the build results of a RSScript.
 */
class RSExecutable {
private:
  RSInfo *mInfo;
  bool mIsInfoDirty;

  FileBase *mObjFile;

  ObjectLoader *mLoader;

  // Memory address of rs export stuffs
  android::Vector<void *> mExportVarAddrs;
  android::Vector<void *> mExportFuncAddrs;
  android::Vector<void *> mExportForeachFuncAddrs;

  // FIXME: These are designed for Renderscript HAL and is initialized in
  //        RSExecutable::Create(). Both of them come from RSInfo::getPragmas().
  //        If possible, read the pragma key/value pairs directly from RSInfo.
  android::Vector<const char *> mPragmaKeys;
  android::Vector<const char *> mPragmaValues;

  RSExecutable(RSInfo &pInfo, FileBase &pObjFile, ObjectLoader &pLoader)
    : mInfo(&pInfo), mIsInfoDirty(false), mObjFile(&pObjFile), mLoader(&pLoader)
  { }

public:
  // This is a NULL-terminated string array which specifies "Special" functions
  // in Renderscript (e.g., root().)
  static const char *SpecialFunctionNames[];

  // Return NULL on error. If the return object is non-NULL, it claims the
  // ownership of pInfo and pObjFile.
  static RSExecutable *Create(RSInfo &pInfo,
                              FileBase &pObjFile,
                              SymbolResolverProxy &pResolver);

  inline const RSInfo &getInfo() const
  { return *mInfo; }

  // Interfaces to RSInfo
  inline bool isThreadable() const
  { return mInfo->isThreadable(); }

  inline void setThreadable(bool pThreadable = true) {
    if (mInfo->isThreadable() != pThreadable) {
      mInfo->setThreadable(pThreadable);
      mIsInfoDirty = true;
    }
    return;
  }

  // Interfaces to ObjectLoader
  inline void *getSymbolAddress(const char *pName) const
  { return mLoader->getSymbolAddress(pName); }

  bool syncInfo(bool pForce = false);

  // Disassemble and dump the relocated functions to the pOutput.
  void dumpDisassembly(OutputFile &pOutput) const;

  inline const android::Vector<void *> &getExportVarAddrs() const
  { return mExportVarAddrs; }
  inline const android::Vector<void *> &getExportFuncAddrs() const
  { return mExportFuncAddrs; }
  inline const android::Vector<void *> &getExportForeachFuncAddrs() const
  { return mExportForeachFuncAddrs; }

  inline const android::Vector<const char *> &getPragmaKeys() const
  { return mPragmaKeys; }
  inline const android::Vector<const char *> &getPragmaValues() const
  { return mPragmaValues; }

  ~RSExecutable();
};

} // end namespace bcc

#endif // BCC_RS_EXECUTABLE_H
