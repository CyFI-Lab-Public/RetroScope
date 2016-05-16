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

#include "bcc/ExecutionEngine/ObjectLoader.h"

#include <utils/FileMap.h>

#include "bcc/ExecutionEngine/GDBJITRegistrar.h"
#include "bcc/Support/FileBase.h"
#include "bcc/Support/Log.h"

#include "ELFObjectLoaderImpl.h"

using namespace bcc;

ObjectLoader *ObjectLoader::Load(void *pMemStart, size_t pMemSize,
                                 const char *pName,
                                 SymbolResolverInterface &pResolver,
                                 bool pEnableGDBDebug) {
  ObjectLoader *result = NULL;

  // Check parameters.
  if ((pMemStart == NULL) || (pMemSize <= 0)) {
    ALOGE("Invalid memory '%s' was given to load (memory addr: %p, size: %u)",
          pName, pMemStart, static_cast<unsigned>(pMemSize));
    goto bail;
  }

  // Create result object
  result = new (std::nothrow) ObjectLoader();
  if (result == NULL) {
    ALOGE("Out of memory when create object loader for %s!", pName);
    goto bail;
  }

  // Currently, only ELF object loader is supported. Therefore, there's no codes
  // to detect the object file type and to select the one appropriated. Directly
  // try out the ELF object loader.
  result->mImpl = new (std::nothrow) ELFObjectLoaderImpl();
  if (result->mImpl == NULL) {
    ALOGE("Out of memory when create ELF object loader for %s", pName);
    goto bail;
  }

  // Load the object file.
  if (!result->mImpl->load(pMemStart, pMemSize)) {
    ALOGE("Failed to load %s!", pName);
    goto bail;
  }

  // Perform relocation.
  if (!result->mImpl->relocate(pResolver)) {
    ALOGE("Error occurred when performs relocation on %s!", pName);
    goto bail;
  }

  // GDB debugging is enabled. Note that error occurrs during the setup of
  // debugging won't failed the object load. Only a warning is issued to notify
  // that the debugging is disabled due to the failure.
  if (pEnableGDBDebug) {
    // GDB's JIT debugging requires the source object file corresponded to the
    // process image desired to debug with. And some fields in the object file
    // must be updated to record the runtime information after it's loaded into
    // memory. For example, GDB's JIT debugging requires an ELF file with the
    // value of sh_addr in the section header to be the memory address that the
    // section lives in the process image. Therefore, a writable memory with its
    // contents initialized to the contents of pFile is created.
    result->mDebugImage = new (std::nothrow) uint8_t [ pMemSize ];
    if (result->mDebugImage != NULL) {
      ::memcpy(result->mDebugImage, pMemStart, pMemSize);
      if (!result->mImpl->prepareDebugImage(result->mDebugImage, pMemSize)) {
        ALOGW("GDB debug for %s is enabled by the user but won't work due to "
              "failure debug image preparation!", pName);
      } else {
        registerObjectWithGDB(
            reinterpret_cast<const ObjectBuffer *>(result->mDebugImage),
            pMemSize);
      }
    }
  }

  return result;

bail:
  delete result;
  return NULL;
}

ObjectLoader *ObjectLoader::Load(FileBase &pFile,
                                 SymbolResolverInterface &pResolver,
                                 bool pEnableGDBDebug) {
  size_t file_size;
  android::FileMap *file_map = NULL;
  const char *input_filename = pFile.getName().c_str();
  ObjectLoader *result = NULL;

  // Check the inputs.
  if (pFile.hasError()) {
    ALOGE("Input file %s to the object loader is in the invalid state! (%s)",
          input_filename, pFile.getErrorMessage().c_str());
    return NULL;
  }

  // Get the file size.
  file_size = pFile.getSize();
  if (pFile.hasError()) {
    ALOGE("Failed to get size of file %s! (%s)", input_filename,
          pFile.getErrorMessage().c_str());
    return NULL;
  }

  // Abort on empty file.
  if (file_size <= 0) {
    ALOGE("Empty file %s to the object loader.", input_filename);
    return NULL;
  }

  // Create memory map for the input file.
  file_map = pFile.createMap(0, file_size, /* pIsReadOnly */true);
  if ((file_map == NULL) || pFile.hasError())  {
    ALOGE("Failed to map the file %s to the memory! (%s)", input_filename,
          pFile.getErrorMessage().c_str());
    return NULL;
  }

  // Delegate the load request.
  result = Load(file_map->getDataPtr(), file_size, input_filename, pResolver,
                pEnableGDBDebug);

  // No whether the load is successful or not, file_map is no longer needed. On
  // success, there's a copy of the object corresponded to the pFile in the
  // memory. Therefore, file_map can be safely released.
  file_map->release();

  return result;
}

void *ObjectLoader::getSymbolAddress(const char *pName) const {
  return mImpl->getSymbolAddress(pName);
}

size_t ObjectLoader::getSymbolSize(const char *pName) const {
  return mImpl->getSymbolSize(pName);
}

bool ObjectLoader::getSymbolNameList(android::Vector<const char *>& pNameList,
                                     SymbolType pType) const {
  return mImpl->getSymbolNameList(pNameList, pType);
}

ObjectLoader::~ObjectLoader() {
  delete mImpl;
  delete [] reinterpret_cast<uint8_t *>(mDebugImage);
}
