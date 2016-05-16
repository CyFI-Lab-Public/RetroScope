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

#include "bcc/Renderscript/RSExecutable.h"

#include "bcc/Config/Config.h"
#include "bcc/Support/Disassembler.h"
#include "bcc/Support/FileBase.h"
#include "bcc/Support/Log.h"
#include "bcc/Support/OutputFile.h"
#include "bcc/ExecutionEngine/SymbolResolverProxy.h"

#include <utils/String8.h>

using namespace bcc;

const char *RSExecutable::SpecialFunctionNames[] = {
  "root",      // Graphics drawing function or compute kernel.
  "init",      // Initialization routine called implicitly on startup.
  ".rs.dtor",  // Static global destructor for a script instance.
  ".rs.info",  // Variable containing string of RS metadata info.
  NULL         // Must be NULL-terminated.
};

RSExecutable *RSExecutable::Create(RSInfo &pInfo,
                                   FileBase &pObjFile,
                                   SymbolResolverProxy &pResolver) {
  // Load the object file. Enable the GDB's JIT debugging if the script contains
  // debug information.
  ObjectLoader *loader = ObjectLoader::Load(pObjFile,
                                            pResolver,
                                            pInfo.hasDebugInformation());
  if (loader == NULL) {
    return NULL;
  }

  // Now, all things required to build a RSExecutable object are ready.
  RSExecutable *result = new (std::nothrow) RSExecutable(pInfo,
                                                         pObjFile,
                                                         *loader);
  if (result == NULL) {
    ALOGE("Out of memory when create object to hold RS result file for %s!",
          pObjFile.getName().c_str());
    return NULL;
  }

  unsigned idx;
  // Resolve addresses of RS export vars.
  idx = 0;
  const RSInfo::ExportVarNameListTy &export_var_names =
      pInfo.getExportVarNames();
  for (RSInfo::ExportVarNameListTy::const_iterator
           var_iter = export_var_names.begin(),
           var_end = export_var_names.end(); var_iter != var_end;
       var_iter++, idx++) {
    const char *name = *var_iter;
    void *addr = result->getSymbolAddress(name);
    if (addr == NULL) {
        //ALOGW("RS export var at entry #%u named %s cannot be found in the result "
        //"object!", idx, name);
    }
    result->mExportVarAddrs.push_back(addr);
  }

  // Resolve addresses of RS export functions.
  idx = 0;
  const RSInfo::ExportFuncNameListTy &export_func_names =
      pInfo.getExportFuncNames();
  for (RSInfo::ExportFuncNameListTy::const_iterator
           func_iter = export_func_names.begin(),
           func_end = export_func_names.end(); func_iter != func_end;
       func_iter++, idx++) {
    const char *name = *func_iter;
    void *addr = result->getSymbolAddress(name);
    if (addr == NULL) {
        //      ALOGW("RS export func at entry #%u named %s cannot be found in the result"
        //" object!", idx, name);
    }
    result->mExportFuncAddrs.push_back(addr);
  }

  // Resolve addresses of expanded RS foreach function.
  idx = 0;
  const RSInfo::ExportForeachFuncListTy &export_foreach_funcs =
      pInfo.getExportForeachFuncs();
  for (RSInfo::ExportForeachFuncListTy::const_iterator
           foreach_iter = export_foreach_funcs.begin(),
           foreach_end = export_foreach_funcs.end();
       foreach_iter != foreach_end; foreach_iter++, idx++) {
    const char *func_name = foreach_iter->first;
    android::String8 expanded_func_name(func_name);
    expanded_func_name.append(".expand");
    void *addr = result->getSymbolAddress(expanded_func_name.string());
    if (addr == NULL) {
        //      ALOGW("Expanded RS foreach at entry #%u named %s cannot be found in the "
        //            "result object!", idx, expanded_func_name.string());
    }
    result->mExportForeachFuncAddrs.push_back(addr);
  }

  // Copy pragma key/value pairs from RSInfo::getPragmas() into mPragmaKeys and
  // mPragmaValues, respectively.
  const RSInfo::PragmaListTy &pragmas = pInfo.getPragmas();
  for (RSInfo::PragmaListTy::const_iterator pragma_iter = pragmas.begin(),
          pragma_end = pragmas.end(); pragma_iter != pragma_end;
       pragma_iter++){
    result->mPragmaKeys.push_back(pragma_iter->first);
    result->mPragmaValues.push_back(pragma_iter->second);
  }

  return result;
}

bool RSExecutable::syncInfo(bool pForce) {
  if (!pForce && !mIsInfoDirty) {
    return true;
  }

  android::String8 info_path = RSInfo::GetPath(mObjFile->getName().c_str());
  OutputFile info_file(info_path.string(), FileBase::kTruncate);

  if (info_file.hasError()) {
    ALOGE("Failed to open the info file %s for write! (%s)", info_path.string(),
          info_file.getErrorMessage().c_str());
    return false;
  }

  // Operation to the RS info file need to acquire the lock on the output file
  // first.
  if (!mObjFile->lock(FileBase::kWriteLock)) {
    ALOGE("Write to RS info file %s required the acquisition of the write lock "
          "on %s but got failure! (%s)", info_path.string(),
          mObjFile->getName().c_str(), info_file.getErrorMessage().c_str());
    return false;
  }

  // Perform the write.
  if (!mInfo->write(info_file)) {
    ALOGE("Failed to sync the RS info file %s!", info_path.string());
    mObjFile->unlock();
    return false;
  }

  mObjFile->unlock();
  mIsInfoDirty = false;
  return true;
}

void RSExecutable::dumpDisassembly(OutputFile &pOutput) const {
#if DEBUG_MC_DISASSEMBLER
  if (pOutput.hasError()) {
    return;
  }

  // Get MC codegen emitted function name list.
  android::Vector<const char *> func_list;

  if (!mLoader->getSymbolNameList(func_list, ObjectLoader::kFunctionType)) {
      ALOGW("Failed to get the list of function name in %s for disassembly!",
            mObjFile->getName().c_str());
  } else {
    // Disassemble each function
    for (size_t i = 0, e = func_list.size(); i != e; i++) {
      const char* func_name = func_list[i];
      void *func = mLoader->getSymbolAddress(func_name);
      size_t func_size = mLoader->getSymbolSize(func_name);

      if (func == NULL) {
        continue;
      }
      DisassembleResult result =
          Disassemble(pOutput, DEFAULT_TARGET_TRIPLE_STRING, func_name,
                      reinterpret_cast<const uint8_t *>(func), func_size);

      if (result != kDisassembleSuccess) {
          ALOGW("Failed to disassemble the function %s in %s (error code=%zu)!",
                func_name, mObjFile->getName().c_str(), static_cast<size_t>(result));

        if (result != kDisassembleInvalidInstruction) {
            ALOGW("And the error occured in disassembler is fatal. Abort "
                  "disassembler on remaining functions!");
          break;
        }
      }
    }
  }
#endif
  return;
}

RSExecutable::~RSExecutable() {
  syncInfo();
  delete mInfo;
  delete mObjFile;
  delete mLoader;
}
