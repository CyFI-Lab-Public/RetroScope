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
// This file implements RSInfo::write()
//===----------------------------------------------------------------------===//

#include "bcc/Renderscript/RSInfo.h"

#include "bcc/Support/Log.h"
#include "bcc/Support/OutputFile.h"

using namespace bcc;

namespace {

template<typename ItemType, typename ItemContainer> inline bool
helper_adapt_list_item(ItemType &pResult, const RSInfo &pInfo,
                       const typename ItemContainer::const_iterator &pItem);

template<> inline bool
helper_adapt_list_item<rsinfo::DependencyTableItem, RSInfo::DependencyTableTy>(
    rsinfo::DependencyTableItem &pResult,
    const RSInfo &pInfo,
    const RSInfo::DependencyTableTy::const_iterator &pItem) {
  pResult.id = pInfo.getStringIdxInPool(pItem->first);
  pResult.sha1 =
      pInfo.getStringIdxInPool(reinterpret_cast<const char *>(pItem->second));

  if (pResult.id == rsinfo::gInvalidStringIndex) {
    ALOGE("RS dependency table contains invalid source id string '%s'.",
          pItem->first);
    return false;
  }

  if (pResult.sha1 == rsinfo::gInvalidStringIndex) {
    ALOGE("RS dependency table contains invalid SHA-1 checksum string in '%s'.",
          pItem->first);
    return false;
  }

  return true;
}

template<> inline bool
helper_adapt_list_item<rsinfo::PragmaItem, RSInfo::PragmaListTy>(
    rsinfo::PragmaItem &pResult,
    const RSInfo &pInfo,
    const RSInfo::PragmaListTy::const_iterator &pItem) {
  pResult.key = pInfo.getStringIdxInPool(pItem->first);
  pResult.value = pInfo.getStringIdxInPool(pItem->second);

  if (pResult.key == rsinfo::gInvalidStringIndex) {
    ALOGE("RS pragma list contains invalid string '%s' for key.", pItem->first);
    return false;
  }

  if (pResult.value == rsinfo::gInvalidStringIndex) {
    ALOGE("RS pragma list contains invalid string '%s' for value.",
          pItem->second);
    return false;
  }

  return true;
}

template<> inline bool
helper_adapt_list_item<rsinfo::ObjectSlotItem, RSInfo::ObjectSlotListTy>(
    rsinfo::ObjectSlotItem &pResult,
    const RSInfo &pInfo,
    const RSInfo::ObjectSlotListTy::const_iterator &pItem) {
  pResult.slot = *pItem;
  return true;
}

template<> inline bool
helper_adapt_list_item<rsinfo::ExportVarNameItem, RSInfo::ExportVarNameListTy>(
    rsinfo::ExportVarNameItem &pResult,
    const RSInfo &pInfo,
    const RSInfo::ExportVarNameListTy::const_iterator &pItem) {
  pResult.name = pInfo.getStringIdxInPool(*pItem);

  if (pResult.name == rsinfo::gInvalidStringIndex) {
    ALOGE("RS export vars contains invalid string '%s' for name.", *pItem);
    return false;
  }

  return true;
}

template<> inline bool
helper_adapt_list_item<rsinfo::ExportFuncNameItem,
                       RSInfo::ExportFuncNameListTy>(
    rsinfo::ExportFuncNameItem &pResult,
    const RSInfo &pInfo,
    const RSInfo::ExportFuncNameListTy::const_iterator &pItem) {
  pResult.name = pInfo.getStringIdxInPool(*pItem);

  if (pResult.name == rsinfo::gInvalidStringIndex) {
    ALOGE("RS export funcs contains invalid string '%s' for name.", *pItem);
    return false;
  }

  return true;
}

template<> inline bool
helper_adapt_list_item<rsinfo::ExportForeachFuncItem,
                       RSInfo::ExportForeachFuncListTy>(
    rsinfo::ExportForeachFuncItem &pResult,
    const RSInfo &pInfo,
    const RSInfo::ExportForeachFuncListTy::const_iterator &pItem) {
  pResult.name = pInfo.getStringIdxInPool(pItem->first);
  pResult.signature = pItem->second;

  if (pResult.name == rsinfo::gInvalidStringIndex) {
    ALOGE("RS export foreach contains invalid string '%s' for name.",
          pItem->first);
    return false;
  }

  return true;
}

template<typename ItemType, typename ItemContainer>
inline bool helper_write_list(OutputFile &pOutput,
                              const RSInfo &pInfo,
                              const rsinfo::ListHeader &pHeader,
                              ItemContainer &pList) {
  ItemType item;

  for (typename ItemContainer::const_iterator item_iter = pList.begin(),
          item_end = pList.end(); item_iter != item_end; item_iter++) {
    // Convert each entry in the pList to ItemType.
    if (!helper_adapt_list_item<ItemType, ItemContainer>(item,
                                                         pInfo,
                                                         item_iter)) {
      return false;
    }
    // And write out an item.
    if (pOutput.write(&item, sizeof(item)) != sizeof(item)) {
      ALOGE("Cannot write out item of %s for RSInfo file %s! (%s)",
            rsinfo::GetItemTypeName<ItemType>(), pOutput.getName().c_str(),
            pOutput.getErrorMessage().c_str());
      return false;
    }
  }

  return true;
}

} // end anonymous namespace

bool RSInfo::write(OutputFile &pOutput) {
  off_t initial_offset = pOutput.tell();
  const char *output_filename = pOutput.getName().c_str();

  if (pOutput.hasError()) {
    ALOGE("Invalid RS info file %s for output! (%s)",
          output_filename, pOutput.getErrorMessage().c_str());
    return false;
  }

  // Layout.
  if (!layout(initial_offset)) {
    return false;
  }

  // Write header.
  if (pOutput.write(&mHeader, sizeof(mHeader)) != sizeof(mHeader)) {
    ALOGE("Cannot write out the header for RSInfo file %s! (%s)",
          output_filename, pOutput.getErrorMessage().c_str());
    return false;
  }

  // Write string pool.
  if (static_cast<size_t>(pOutput.write(mStringPool, mHeader.strPoolSize))
          != mHeader.strPoolSize) {
    ALOGE("Cannot write out the string pool for RSInfo file %s! (%s)",
          output_filename, pOutput.getErrorMessage().c_str());
    return false;
  }

  // Write dependencyTable.
  if (!helper_write_list<rsinfo::DependencyTableItem, DependencyTableTy>
        (pOutput, *this, mHeader.dependencyTable, mDependencyTable)) {
    return false;
  }

  // Write pragmaList.
  if (!helper_write_list<rsinfo::PragmaItem, PragmaListTy>
        (pOutput, *this, mHeader.pragmaList, mPragmas)) {
    return false;
  }

  // Write objectSlotList.
  if (!helper_write_list<rsinfo::ObjectSlotItem, ObjectSlotListTy>
        (pOutput, *this, mHeader.objectSlotList, mObjectSlots)) {
    return false;
  }

  // Write exportVarNameList.
  if (!helper_write_list<rsinfo::ExportVarNameItem, ExportVarNameListTy>
        (pOutput, *this, mHeader.exportVarNameList, mExportVarNames)) {
    return false;
  }

  // Write exportFuncNameList.
  if (!helper_write_list<rsinfo::ExportFuncNameItem, ExportFuncNameListTy>
        (pOutput, *this, mHeader.exportFuncNameList, mExportFuncNames)) {
    return false;
  }

  // Write exportForeachFuncList.
  if (!helper_write_list<rsinfo::ExportForeachFuncItem, ExportForeachFuncListTy>
        (pOutput, *this, mHeader.exportForeachFuncList, mExportForeachFuncs)) {
    return false;
  }

  return true;
}
