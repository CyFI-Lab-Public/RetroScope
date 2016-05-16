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

//===----------------------------------------------------------------------===//
// This file implements RSInfo::ReadFromFile()
//===----------------------------------------------------------------------===//

#include "bcc/Renderscript/RSInfo.h"

#include <new>

#include <utils/FileMap.h>

#include "bcc/Support/Log.h"
#include "bcc/Support/InputFile.h"

using namespace bcc;

namespace {

template<typename ItemType, typename ItemContainer>
inline bool helper_read_list_item(const ItemType &pItem,
                                  const RSInfo &pInfo,
                                  ItemContainer &pResult);

// Process DependencyTableItem in the file
template<> inline bool
helper_read_list_item<rsinfo::DependencyTableItem, RSInfo::DependencyTableTy>(
    const rsinfo::DependencyTableItem &pItem,
    const RSInfo &pInfo,
    RSInfo::DependencyTableTy &pResult)
{
  const char *id = pInfo.getStringFromPool(pItem.id);
  const uint8_t *sha1 =
      reinterpret_cast<const uint8_t *>(pInfo.getStringFromPool(pItem.sha1));

  if (id == NULL) {
    ALOGE("Invalid string index %d for source id in RS dependenct table.",
          pItem.id);
    return false;
  }

  if (sha1 == NULL) {
    ALOGE("Invalid string index %d for SHA-1 checksum in RS dependenct table.",
          pItem.id);
    return false;
  }

  pResult.push(std::make_pair(id, sha1));
  return true;
}

// Process PragmaItem in the file
template<> inline bool
helper_read_list_item<rsinfo::PragmaItem, RSInfo::PragmaListTy>(
    const rsinfo::PragmaItem &pItem,
    const RSInfo &pInfo,
    RSInfo::PragmaListTy &pResult)
{
  const char *key = pInfo.getStringFromPool(pItem.key);
  const char *value =pInfo.getStringFromPool(pItem.value);

  if (key == NULL) {
    ALOGE("Invalid string index %d for key in RS pragma list.", pItem.key);
    return false;
  }

  if (value == NULL) {
    ALOGE("Invalid string index %d for value in RS pragma list.", pItem.value);
    return false;
  }

  pResult.push(std::make_pair(key, value));
  return true;
}

// Procee ObjectSlotItem in the file
template<> inline bool
helper_read_list_item<rsinfo::ObjectSlotItem, RSInfo::ObjectSlotListTy>(
    const rsinfo::ObjectSlotItem &pItem,
    const RSInfo &pInfo,
    RSInfo::ObjectSlotListTy &pResult)
{
  pResult.push(pItem.slot);
  return true;
}

// Procee ExportVarNameItem in the file
template<> inline bool
helper_read_list_item<rsinfo::ExportVarNameItem, RSInfo::ExportVarNameListTy>(
    const rsinfo::ExportVarNameItem &pItem,
    const RSInfo &pInfo,
    RSInfo::ExportVarNameListTy &pResult)
{
  const char *name = pInfo.getStringFromPool(pItem.name);

  if (name == NULL) {
    ALOGE("Invalid string index %d for name in RS export vars.", pItem.name);
    return false;
  }

  pResult.push(name);
  return true;
}

// Procee ExportFuncNameItem in the file
template<> inline bool
helper_read_list_item<rsinfo::ExportFuncNameItem, RSInfo::ExportFuncNameListTy>(
    const rsinfo::ExportFuncNameItem &pItem,
    const RSInfo &pInfo,
    RSInfo::ExportFuncNameListTy &pResult)
{
  const char *name = pInfo.getStringFromPool(pItem.name);

  if (name == NULL) {
    ALOGE("Invalid string index %d for name in RS export funcs.", pItem.name);
    return false;
  }

  pResult.push(name);
  return true;
}

// Procee ExportForeachFuncItem in the file
template<> inline bool
helper_read_list_item<rsinfo::ExportForeachFuncItem, RSInfo::ExportForeachFuncListTy>(
    const rsinfo::ExportForeachFuncItem &pItem,
    const RSInfo &pInfo,
    RSInfo::ExportForeachFuncListTy &pResult)
{
  const char *name = pInfo.getStringFromPool(pItem.name);

  if (name == NULL) {
    ALOGE("Invalid string index %d for name in RS export foreachs.", pItem.name);
    return false;
  }

  pResult.push(std::make_pair(name, pItem.signature));
  return true;
}

template<typename ItemType, typename ItemContainer>
inline bool helper_read_list(const uint8_t *pData,
                             const RSInfo &pInfo,
                             const rsinfo::ListHeader &pHeader,
                             ItemContainer &pResult) {
  const ItemType *item;

  // Out-of-range exception has been checked.
  for (uint32_t i = 0; i < pHeader.count; i++) {
    item = reinterpret_cast<const ItemType *>(pData +
                                              pHeader.offset +
                                              i * pHeader.itemSize);
    if (!helper_read_list_item<ItemType, ItemContainer>(*item, pInfo, pResult)) {
      return false;
    }
  }
  return true;
}

} // end anonymous namespace

RSInfo *RSInfo::ReadFromFile(InputFile &pInput, const DependencyTableTy &pDeps) {
  android::FileMap *map = NULL;
  RSInfo *result = NULL;
  const uint8_t *data;
  const rsinfo::Header *header;
  size_t filesize;
  const char *input_filename = pInput.getName().c_str();
  const off_t cur_input_offset = pInput.tell();

  if (pInput.hasError()) {
    ALOGE("Invalid RS info file %s! (%s)", input_filename,
                                           pInput.getErrorMessage().c_str());
    goto bail;
  }

  filesize = pInput.getSize();
  if (pInput.hasError()) {
    ALOGE("Failed to get the size of RS info file %s! (%s)",
          input_filename, pInput.getErrorMessage().c_str());
    goto bail;
  }

  // Create memory map for the file.
  map = pInput.createMap(/* pOffset */cur_input_offset,
                         /* pLength */filesize - cur_input_offset);
  if (map == NULL) {
    ALOGE("Failed to map RS info file %s to the memory! (%s)",
          input_filename, pInput.getErrorMessage().c_str());
    goto bail;
  }

  data = reinterpret_cast<const uint8_t *>(map->getDataPtr());

  // Header starts at the beginning of the file.
  header = reinterpret_cast<const rsinfo::Header *>(data);

  // Check the magic.
  if (::memcmp(header->magic, RSINFO_MAGIC, sizeof(header->magic)) != 0) {
    ALOGV("Wrong magic found in the RS info file %s. Treat it as a dirty "
          "cache.", input_filename);
    goto bail;
  }

  // Check the version.
  if (::memcmp(header->version,
               RSINFO_VERSION,
               sizeof((header->version)) != 0)) {
    ALOGV("Mismatch the version of RS info file %s: (current) %s v.s. (file) "
          "%s. Treat it as as a dirty cache.", input_filename, RSINFO_VERSION,
          header->version);
    goto bail;
  }

  // Check the size.
  if ((header->headerSize != sizeof(rsinfo::Header)) ||
      (header->dependencyTable.itemSize != sizeof(rsinfo::DependencyTableItem)) ||
      (header->pragmaList.itemSize != sizeof(rsinfo::PragmaItem)) ||
      (header->objectSlotList.itemSize != sizeof(rsinfo::ObjectSlotItem)) ||
      (header->exportVarNameList.itemSize != sizeof(rsinfo::ExportVarNameItem)) ||
      (header->exportFuncNameList.itemSize != sizeof(rsinfo::ExportFuncNameItem)) ||
      (header->exportForeachFuncList.itemSize != sizeof(rsinfo::ExportForeachFuncItem))) {
    ALOGW("Corrupted RS info file %s! (unexpected size found)", input_filename);
    goto bail;
  }

  // Check the range.
#define LIST_DATA_RANGE(_list_header) \
  ((_list_header).offset + (_list_header).count * (_list_header).itemSize)
  if (((header->headerSize + header->strPoolSize) > filesize) ||
      (LIST_DATA_RANGE(header->dependencyTable) > filesize) ||
      (LIST_DATA_RANGE(header->pragmaList) > filesize) ||
      (LIST_DATA_RANGE(header->objectSlotList) > filesize) ||
      (LIST_DATA_RANGE(header->exportVarNameList) > filesize) ||
      (LIST_DATA_RANGE(header->exportFuncNameList) > filesize) ||
      (LIST_DATA_RANGE(header->exportForeachFuncList) > filesize)) {
    ALOGW("Corrupted RS info file %s! (data out of the range)", input_filename);
    goto bail;
  }
#undef LIST_DATA_RANGE

  // File seems ok, create result RSInfo object.
  result = new (std::nothrow) RSInfo(header->strPoolSize);
  if (result == NULL) {
    ALOGE("Out of memory when create RSInfo object for %s!", input_filename);
    goto bail;
  }

  // Make advice on our access pattern.
  map->advise(android::FileMap::SEQUENTIAL);

  // Copy the header.
  ::memcpy(&result->mHeader, header, sizeof(rsinfo::Header));

  if (header->strPoolSize > 0) {
    // Copy the string pool. The string pool is immediately after the header at
    // the offset header->headerSize.
    if (result->mStringPool == NULL) {
      ALOGE("Out of memory when allocate string pool for RS info file %s!",
            input_filename);
      goto bail;
    }
    ::memcpy(result->mStringPool, data + result->mHeader.headerSize,
             result->mHeader.strPoolSize);
  }

  // Populate all the data to the result object.
  if (!helper_read_list<rsinfo::DependencyTableItem, DependencyTableTy>
        (data, *result, header->dependencyTable, result->mDependencyTable)) {
    goto bail;
  }

  // Check dependency to see whether the cache is dirty or not.
  if (!CheckDependency(*result, pInput.getName().c_str(), pDeps)) {
    goto bail;
  }

  if (!helper_read_list<rsinfo::PragmaItem, PragmaListTy>
        (data, *result, header->pragmaList, result->mPragmas)) {
    goto bail;
  }

  if (!helper_read_list<rsinfo::ObjectSlotItem, ObjectSlotListTy>
        (data, *result, header->objectSlotList, result->mObjectSlots)) {
    goto bail;
  }

  if (!helper_read_list<rsinfo::ExportVarNameItem, ExportVarNameListTy>
        (data, *result, header->exportVarNameList, result->mExportVarNames)) {
    goto bail;
  }

  if (!helper_read_list<rsinfo::ExportFuncNameItem, ExportFuncNameListTy>
        (data, *result, header->exportFuncNameList, result->mExportFuncNames)) {
    goto bail;
  }

  if (!helper_read_list<rsinfo::ExportForeachFuncItem, ExportForeachFuncListTy>
        (data, *result, header->exportForeachFuncList, result->mExportForeachFuncs)) {
    goto bail;
  }

  // Clean up.
  map->release();

  return result;

bail:
  if (map != NULL) {
    map->release();
  }

  delete result;

  return NULL;
} // RSInfo::ReadFromFile
