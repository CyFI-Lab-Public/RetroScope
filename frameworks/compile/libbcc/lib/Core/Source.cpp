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

#include "bcc/Source.h"

#include <new>

#include <llvm/Bitcode/ReaderWriter.h>
#include <llvm/IR/LLVMContext.h>
#include <llvm/Linker.h>
#include <llvm/IR/Module.h>
#include <llvm/Support/MemoryBuffer.h>
#include <llvm/Support/system_error.h>

#include "bcc/BCCContext.h"
#include "bcc/Support/Log.h"

#include "BCCContextImpl.h"

namespace {

// Helper function to load the bitcode. This uses "bitcode lazy load" feature to
// reduce the startup time. On success, return the LLVM module object created
// and take the ownership of input memory buffer (i.e., pInput). On error,
// return NULL and will NOT take the ownership of pInput.
static inline llvm::Module *helper_load_bitcode(llvm::LLVMContext &pContext,
                                                llvm::MemoryBuffer *pInput) {
  std::string error;
  llvm::Module *module = llvm::getLazyBitcodeModule(pInput, pContext, &error);

  if (module == NULL) {
    ALOGE("Unable to parse the given bitcode file `%s'! (%s)",
          pInput->getBufferIdentifier(), error.c_str());
  }

  return module;
}

} // end anonymous namespace

namespace bcc {

void Source::setModule(llvm::Module *pModule) {
  if (!mNoDelete && (mModule != pModule)) delete mModule;
  mModule = pModule;
}

Source *Source::CreateFromBuffer(BCCContext &pContext,
                                 const char *pName,
                                 const char *pBitcode,
                                 size_t pBitcodeSize) {
  llvm::StringRef input_data(pBitcode, pBitcodeSize);
  llvm::MemoryBuffer *input_memory =
      llvm::MemoryBuffer::getMemBuffer(input_data, "", false);

  if (input_memory == NULL) {
    ALOGE("Unable to load bitcode `%s' from buffer!", pName);
    return NULL;
  }

  llvm::Module *module = helper_load_bitcode(pContext.mImpl->mLLVMContext,
                                             input_memory);
  if (module == NULL) {
    delete input_memory;
    return NULL;
  }

  Source *result = CreateFromModule(pContext, *module, /* pNoDelete */false);
  if (result == NULL) {
    delete module;
  }

  return result;
}

Source *Source::CreateFromFile(BCCContext &pContext, const std::string &pPath) {
  llvm::OwningPtr<llvm::MemoryBuffer> input_data;

  llvm::error_code ec = llvm::MemoryBuffer::getFile(pPath, input_data);
  if (ec != llvm::error_code::success()) {
    ALOGE("Failed to load bitcode from path %s! (%s)", pPath.c_str(),
                                                       ec.message().c_str());
    return NULL;
  }

  llvm::MemoryBuffer *input_memory = input_data.take();
  llvm::Module *module = helper_load_bitcode(pContext.mImpl->mLLVMContext,
                                             input_memory);
  if (module == NULL) {
    delete input_memory;
    return NULL;
  }

  Source *result = CreateFromModule(pContext, *module, /* pNoDelete */false);
  if (result == NULL) {
    delete module;
  }

  return result;
}

Source *Source::CreateFromModule(BCCContext &pContext, llvm::Module &pModule,
                                 bool pNoDelete) {
  Source *result = new (std::nothrow) Source(pContext, pModule, pNoDelete);
  if (result == NULL) {
    ALOGE("Out of memory during Source object allocation for `%s'!",
          pModule.getModuleIdentifier().c_str());
  }
  return result;
}

Source::Source(BCCContext &pContext, llvm::Module &pModule, bool pNoDelete)
  : mContext(pContext), mModule(&pModule), mNoDelete(pNoDelete) {
    pContext.addSource(*this);
}

Source::~Source() {
  mContext.removeSource(*this);
  if (!mNoDelete)
    delete mModule;
}

bool Source::merge(Source &pSource, bool pPreserveSource) {
  std::string error;
  llvm::Linker::LinkerMode mode =
      ((pPreserveSource) ? llvm::Linker::PreserveSource :
                           llvm::Linker::DestroySource);

  if (llvm::Linker::LinkModules(mModule, &pSource.getModule(),
                                mode, &error) != 0) {
    ALOGE("Failed to link source `%s' with `%s' (%s)!",
          getIdentifier().c_str(),
          pSource.getIdentifier().c_str(),
          error.c_str());
    return false;
  }

  if (!pPreserveSource) {
    pSource.mNoDelete = true;
    delete &pSource;
  }

  return true;
}

Source *Source::CreateEmpty(BCCContext &pContext, const std::string &pName) {
  // Create an empty module
  llvm::Module *module =
      new (std::nothrow) llvm::Module(pName, pContext.mImpl->mLLVMContext);

  if (module == NULL) {
    ALOGE("Out of memory when creating empty LLVM module `%s'!", pName.c_str());
    return NULL;
  }

  Source *result = CreateFromModule(pContext, *module, /* pNoDelete */false);
  if (result == NULL) {
    delete module;
  }

  return result;
}

const std::string &Source::getIdentifier() const {
  return mModule->getModuleIdentifier();
}

} // namespace bcc
